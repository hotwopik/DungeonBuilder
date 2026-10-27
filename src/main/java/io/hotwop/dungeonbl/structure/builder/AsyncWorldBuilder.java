package io.hotwop.dungeonbl.structure.builder;

import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.structure.container.BlockDat;
import io.hotwop.dungeonbl.structure.container.EntityData;
import io.hotwop.dungeonbl.util.math.Vec2;
import io.hotwop.dungeonbl.util.math.Vec3;
import io.hotwop.dungeonbl.util.math.Vec3d;
import it.unimi.dsi.fastutil.longs.*;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class AsyncWorldBuilder implements WorldBuilder{
    public record AsBlockData(
        Vec3 pos,
        BlockDat data
    ){}

    public record AsEntityData(
        Vec3d pos,
        EntityData data,
        @Nullable Vec3 sleepPos,
        @Nullable Vec3 blockLeash,
        Consumer<Entity> preload
    ){}

    public record FrameQueues(
        Queue<AsBlockData> blocks,
        Queue<AsEntityData> entities,
        @Nullable AsFrame frame
    ){
        public boolean isEmpty(){
            return blocks.isEmpty() && entities.isEmpty();
        }
    }

    public record ChunkQueue(
        Long2ObjectSortedMap<FrameQueues> frames
    ){
        public boolean isEmpty(){
            return frames.isEmpty();
        }

        public FrameQueues getOrCreateFrame(AsFrame frame){
            return frames.computeIfAbsent(frame==null?-1:frame.id,el->new FrameQueues(
                new ArrayDeque<>(),
                new ArrayDeque<>(),
                frame
            ));
        }
    }

    public record AsFrame(
        Runnable afterFrame,
        long id
    ){}

    // Frames
    private final AtomicLong lastFrame=new AtomicLong();
    private final ThreadLocal<AsFrame> currentFrame=new ThreadLocal<>();

    // To place
    private final Set<Vec2> awaits=Collections.synchronizedSet(new HashSet<>());
    private final Map<Vec2,ChunkQueue> queues=new ConcurrentHashMap<>();
    private final Map<Vec2,Chunk> active=new ConcurrentHashMap<>();

    public final World world;

    public final int maxChunkCount;
    public final int maxBlockPlace;
    public final int maxEntitySpawn;

    public AsyncWorldBuilder(World world, int maxChunkCount, int maxBlockPlace, int maxEntitySpawn) {
        this.world = world;
        this.maxChunkCount = maxChunkCount;
        this.maxBlockPlace = maxBlockPlace;
        this.maxEntitySpawn = maxEntitySpawn;
    }

    @Override
    public World world() {
        return world;
    }

    @Override
    public void beginFrame(Runnable afterFrame){
        currentFrame.set(new AsFrame(
            afterFrame,lastFrame.getAndIncrement()
        ));
    }

    @Override
    public void finishFrame() {
        currentFrame.remove();
    }

    private ChunkQueue getOrCreateQueue(Vec2 chunk){
        return queues.computeIfAbsent(chunk,ch->new ChunkQueue(
            new Long2ObjectLinkedOpenHashMap<>()
        ));
    }

    @Override
    public void place(Vec3 pos, BlockDat data){
        AsFrame frame=currentFrame.get();

        Vec2 chunk=pos.getChunk();
        ChunkQueue queue=getOrCreateQueue(chunk);
        synchronized(queue){
            if(queue.isEmpty())awaits.add(chunk);
            queue.getOrCreateFrame(frame)
                .blocks.add(new AsBlockData(pos,data));
        }
    }

    @Override
    public void spawn(
        Vec3d pos,
        EntityData data,
        @Nullable Vec3 sleepPos,
        @Nullable Vec3 blockLeash,
        Consumer<Entity> preload
    ) {
        AsFrame frame=currentFrame.get();

        Vec2 chunk=pos.getChunk();
        ChunkQueue queue=getOrCreateQueue(chunk);
        synchronized(queue){
            if(queue.isEmpty())awaits.add(chunk);
            queue.getOrCreateFrame(frame)
                .entities.add(new AsEntityData(pos,data,sleepPos,blockLeash,preload));
        }
    }

    public void tick(){
        List<Vec2> ended=new ArrayList<>();
        for(Map.Entry<Vec2,Chunk> current:active.entrySet()){
            if(!current.getValue().isLoaded())continue;
            ChunkQueue queue=queues.get(current.getKey());

            synchronized(queue){
                if(queue.isEmpty()){
                    ended.add(current.getKey());
                }else{
                    int maxBlockPlaceValue=maxBlockPlace<=0?Integer.MAX_VALUE:maxBlockPlace;
                    int blocksPlaced=0;

                    List<FrameQueues> blocksFinished=new ArrayList<>(queue.frames.size());

                    Iterator<Long> blockFrameIterator=queue.frames.sequencedKeySet().iterator();
                    FrameQueues currentFrame=queue.frames.get(blockFrameIterator.next());

                    while(blocksPlaced<maxBlockPlaceValue){
                        AsBlockData next=currentFrame.blocks.poll();
                        if(next==null){
                            blocksFinished.add(currentFrame);

                            if(!blockFrameIterator.hasNext())break;
                            currentFrame=queue.frames.get(blockFrameIterator.next());

                            continue;
                        }

                        next.data.place(world,next.pos);
                        blocksPlaced++;
                    }

                    if(blocksFinished.isEmpty())continue;

                    int maxEntitySpawnValue=maxEntitySpawn<=0?Integer.MAX_VALUE:maxEntitySpawn;
                    int entitySpawned=0;

                    Iterator<FrameQueues> entityFrameIterator=blocksFinished.iterator();
                    currentFrame=entityFrameIterator.next();

                    while(entitySpawned<maxEntitySpawnValue){
                        AsEntityData next=currentFrame.entities.poll();
                        if(next==null){
                            queue.frames.remove(currentFrame.frame.id);
                            currentFrame.frame.afterFrame.run();

                            if(!entityFrameIterator.hasNext())break;
                            currentFrame=entityFrameIterator.next();

                            continue;
                        }

                        next.data.spawn(next.pos.toLocation(world),next.sleepPos,next.blockLeash,next.preload);
                        entitySpawned++;
                    }
                }
            }
        }

        for(Vec2 chunk:ended){
            if(!awaits.contains(chunk)){
                Chunk ch=active.remove(chunk);
                ch.removePluginChunkTicket(DungeonBl.plugin());
            }else{
                awaits.remove(chunk);
            }
        }

        if(maxChunkCount<=0){
            awaits.removeIf(chunk->{
                chunkLoad(chunk);
                return true;
            });
        }else{
            int required=maxChunkCount-active.size();
            if(required>0){
                List<Vec2> toAdd=new ArrayList<>();

                for(Vec2 chunk:awaits){
                    toAdd.add(chunk);
                    required--;
                    if(required==0)break;
                }

                toAdd.forEach(chunk->{
                    awaits.remove(chunk);
                    chunkLoad(chunk);
                });
            }
        }
    }

    private void chunkLoad(Vec2 chunk){
        Chunk ch=world.getChunkAt(chunk.x(),chunk.y());
        ch.addPluginChunkTicket(DungeonBl.plugin());
        active.put(chunk,ch);
    }
}
