package io.hotwop.dungeonbl.structure.builder;

import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.structure.container.BlockDat;
import io.hotwop.dungeonbl.structure.container.EntityData;
import io.hotwop.dungeonbl.util.math.Vec2;
import io.hotwop.dungeonbl.util.math.Vec3;
import io.hotwop.dungeonbl.util.math.Vec3d;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AsyncWorldBuilder implements WorldBuilder{
    public record AsBlockData(
        Vec3 pos,
        BlockDat data
    ){}

    public record AsEntityData(
        Vec3d pos,
        EntityData data,
        Consumer<Entity> preload
    ){}

    public record ChunkQueue(
        Queue<AsBlockData> blockQueue,
        Queue<AsEntityData> entityQueue
    ){
        public boolean isEmpty(){
            return blockQueue.isEmpty() && entityQueue.isEmpty();
        }
    }

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

    private ChunkQueue getOrCreateQueue(Vec2 chunk){
        return queues.computeIfAbsent(chunk,ch->new ChunkQueue(new ArrayDeque<>(),new ArrayDeque<>()));
    }

    @Override
    public void place(Vec3 pos, BlockDat data){
        Vec2 chunk=pos.getChunk();
        ChunkQueue queue=getOrCreateQueue(chunk);
        synchronized(queue){
            if(queue.isEmpty())awaits.add(chunk);
            queue.blockQueue.add(new AsBlockData(pos,data));
        }
    }

    @Override
    public void spawn(Vec3d pos, EntityData data, Consumer<Entity> preload) {
        Vec2 chunk=pos.getChunk();
        ChunkQueue queue=getOrCreateQueue(chunk);
        synchronized(queue){
            if(queue.isEmpty())awaits.add(chunk);
            queue.entityQueue.add(new AsEntityData(pos,data,preload));
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
                    int maxBlockPlaceValue=maxBlockPlace<=0?queue.blockQueue.size():maxBlockPlace;
                    for(int i=0;i<maxBlockPlaceValue;i++){
                        AsBlockData next=queue.blockQueue.poll();
                        if(next==null)break;
                        next.data.place(world,next.pos);
                    }

                    int maxEntitySpawnValue=maxEntitySpawn<=0?queue.entityQueue.size():maxEntitySpawn;
                    for(int i=0;i<maxEntitySpawnValue;i++){
                        AsEntityData next=queue.entityQueue.poll();
                        if(next==null)break;

                        next.data.spawn(next.pos.toLocation(world),next.preload);
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
