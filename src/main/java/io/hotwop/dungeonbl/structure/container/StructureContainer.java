package io.hotwop.dungeonbl.structure.container;

import de.tr7zw.changeme.nbtapi.iface.*;

import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.structure.StructureSerializeException;
import io.hotwop.dungeonbl.structure.builder.WorldBuilder;
import io.hotwop.dungeonbl.structure.codec.BlockMapDecoder;
import io.hotwop.dungeonbl.structure.codec.BlockMapEncoder;
import io.hotwop.dungeonbl.util.math.*;
import io.hotwop.dungeonbl.util.math.SideRotation;

import io.papermc.paper.entity.Leashable;
import it.unimi.dsi.fastutil.ints.*;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.material.Directional;

import java.util.*;

public record StructureContainer(
    Vec3 size,
    Map<Vec3, BlockDat> blocks,

    List<EntitySpawnData> entities,
    Int2IntMap entityLeashMap,
    Int2IntMap entityRideMap
){
    public static final Set<EntityType> unallowedEntityTypes=Set.of(
        EntityType.PLAYER,EntityType.UNKNOWN,

        EntityType.EXPERIENCE_BOTTLE,EntityType.EXPERIENCE_ORB,

        EntityType.ARROW, EntityType.SPECTRAL_ARROW,EntityType.BREEZE_WIND_CHARGE,EntityType.WIND_CHARGE,
        EntityType.WITHER_SKULL,

        EntityType.AREA_EFFECT_CLOUD,

        EntityType.EYE_OF_ENDER, EntityType.ENDER_PEARL,

        EntityType.OMINOUS_ITEM_SPAWNER
    );

    public StructureContainer(
        Vec3 size,
        Map<Vec3, BlockDat> blocks,
        List<EntitySpawnData> entities,
        Int2IntMap entityLeashMap,Int2IntMap entityRideMap
    ) {
        this.size = size;
        this.blocks = Map.copyOf(blocks);
        this.entities = List.copyOf(entities);
        this.entityLeashMap = Int2IntMaps.unmodifiable(entityLeashMap);
        this.entityRideMap = Int2IntMaps.unmodifiable(entityRideMap);

        if (sizeIsInvalid(size)) throw new IllegalArgumentException("Size is invalid");

        blocks.forEach((pos, block) -> {
            if (pos.x() < 0 || pos.y() < 0 || pos.z() < 0 ||
                pos.x() >= size.x() || pos.y() >= size.y() || pos.z() >= size.z()
            ) throw new IllegalArgumentException("Blocks out of bounds");
            if (block.data().getMaterial().isEmpty()) throw new IllegalArgumentException("Empty block in data");
        });

        entities.forEach((entity) -> {
            if(unallowedEntityTypes.contains(entity.data().type()))throw new IllegalArgumentException("Unallowed entities");
            entity.pos().validate(size);
        });

        int entityCount=entities.size();

        if(
            entityLeashMap.keySet().intStream().anyMatch(v->v<0||v>=entityCount)||
            entityLeashMap.values().intStream().anyMatch(v->v<0||v>=entityCount)
        )throw new IllegalArgumentException("Entity leash data is invalid");

        if(
            entityRideMap.keySet().intStream().anyMatch(v->v<0||v>=entityCount)||
            entityRideMap.values().intStream().anyMatch(v->v<0||v>=entityCount)
        )throw new IllegalArgumentException("Entity ride data is invalid");
    }

    public static boolean sizeIsInvalid(Vec3 size) {
        if (size.x() <= 0 || size.y() <= 0 || size.z() <= 0) return true;
        long space = (long) size.x() + (long) size.y() + (long) size.z() + 3L;
        return space >= Integer.MAX_VALUE;
    }

    public void place(Vec3 position, Vec3 origin, SideRotation rotation, WorldBuilder builder, boolean pasteAir) {
        ArrayList<Entity> spawned=new ArrayList<>(entities.size());
        spawned.addAll(Collections.nCopies(entities.size(),null));

        Box3 roomBox=Box3.withSize(new Vec3(0,0,0),size);

        builder.beginFrame(()->{
            for(Int2IntMap.Entry entry:entityLeashMap.int2IntEntrySet()){
                Entity from=spawned.get(entry.getIntKey());
                Entity to=spawned.get(entry.getIntValue());

                if(from instanceof Leashable leashable){
                    leashable.setLeashHolder(to);
                }
            }

            for(Int2IntMap.Entry entry:entityRideMap.int2IntEntrySet()){
                Entity from=spawned.get(entry.getIntKey());
                Entity to=spawned.get(entry.getIntValue());

                to.addPassenger(from);
            }
        });

        blocks.forEach((pos, data)->builder.place(
            pos.rotateAndTranslate(origin, rotation, position),
            data.rotate(rotation)
        ));

        if(pasteAir){
            for(Vec3 pos:roomBox){
                if(!blocks.containsKey(pos)){
                    builder.place(pos.sub(origin).rotate(rotation).add(position),BlockDat.airData);
                }
            }
        }

        for(int i=0;i<entities.size();i++){
            EntitySpawnData entity=entities.get(i);
            final int index=i;

            builder.spawn(
                entity.pos().release(origin, rotation, position),
                entity.data(),
                entity.sleepPos()!=null?entity.sleepPos().rotateAndTranslate(origin, rotation, position):null,
                entity.blockLeash()!=null?entity.blockLeash().rotateAndTranslate(origin, rotation, position):null,
                ent->{
                    ent.setRotation(ent.getYaw()+rotation.degrees,ent.getPitch());

                    if(ent instanceof Directional direct){
                        direct.setFacingDirection(rotation.addTo(direct.getFacing()));
                    }

                    spawned.set(index,ent);
                }
            );
        }

        builder.finishFrame();
    }

    public static StructureContainer scan(
        World world,
        Vec3 begin, Vec3 end,

        BlockIterator blockIterator,
        EntityIterator entityIterator
    ){
        Vec3 size=begin.sub(end).abs();
        Vec3 min=Vec3.min(begin,end);
        Vec3 max=Vec3.max(begin,end);

        Vec2 minChunk=min.getChunk();
        Vec2 maxChunk=max.getChunk();

        List<Chunk> chunks=new ArrayList<>();
        for(int x=minChunk.x();x<=maxChunk.x();x++){
            for(int z=minChunk.y();z<=maxChunk.y();z++){
                Chunk ch=world.getChunkAt(x,z);
                ch.addPluginChunkTicket(DungeonBl.plugin());
                chunks.add(ch);
            }
        }

        Map<Vec3,BlockDat> blocks=new HashMap<>();
        for(int y=0;y<=size.y();y++){
            for(int z=0;z<=size.z();z++){
                for(int x=0;x<=size.x();x++){
                    Vec3 local=new Vec3(x,y,z);
                    Vec3 global=local.add(min);
                    blockIterator.accept(global,local);

                    BlockDat blockData=BlockDat.read(world,global);
                    if(blockData!=null)blocks.put(local,blockData);
                }
            }
        }

        List<EntitySpawnData> entities=new ArrayList<>();
        List<Entity> storedEntity=new ArrayList<>();

        for(Entity entity:world.getNearbyEntities(new Box3(begin,end).toBoundingBox())){
            if(unallowedEntityTypes.contains(entity.getType()))continue;

            EntitySpawnData data=EntitySpawnData.read(entity);
            EntityPos local=data.pos().makeLocal(min);

            Vec3 localSleepPos;
            if(data.sleepPos()!=null)localSleepPos=data.sleepPos().sub(min);
            else localSleepPos=null;

            if(entityIterator.check(entity,local)){
                entities.add(data.replacePositions(local,localSleepPos));
                storedEntity.add(entity);
            }
        }

        Int2IntMap entityLeashMap=new Int2IntArrayMap();
        Int2IntMap entityRideMap=new Int2IntArrayMap();

        for(int i=0;i<storedEntity.size();i++){
            Entity entity=storedEntity.get(i);

            leash:if(entity instanceof Leashable leashable){
                Entity holder;
                try{
                    holder=leashable.getLeashHolder();
                } catch (IllegalStateException e) {
                    break leash;
                }

                int holderId=storedEntity.indexOf(holder);
                if(holderId!=-1){
                    entityLeashMap.put(i,holderId);
                }
            }

            Entity vehicle=entity.getVehicle();
            if(vehicle!=null){
                int vehicleId= storedEntity.indexOf(vehicle);
                if(vehicleId!=-1){
                    entityRideMap.put(i,vehicleId);
                }
            }
        }

        chunks.forEach(ch->ch.removePluginChunkTicket(DungeonBl.plugin()));

        return new StructureContainer(size.add(new Vec3(1,1,1)),blocks,entities,entityLeashMap,entityRideMap);
    }

    public void encode(ReadWriteNBT nbt,boolean encodeEntities) throws StructureSerializeException {
        nbt.setIntArray("size", DBlUtil.vec3ToArray(size));

        if (encodeEntities&&!entities.isEmpty()){
            ReadWriteNBTCompoundList entityList = nbt.getCompoundList("entities");
            for (EntitySpawnData entry : entities) {
                ReadWriteNBT out = entityList.addCompound();
                entry.encode(out);
            }

            if(!entityLeashMap.isEmpty()){
                IntList ids=new IntArrayList();
                for(Int2IntMap.Entry entry:entityLeashMap.int2IntEntrySet()){
                    ids.add(entry.getIntKey());
                    ids.add(entry.getIntValue());
                }
                nbt.setIntArray("leashes",ids.toIntArray());
            }

            if(!entityRideMap.isEmpty()){
                IntList ids=new IntArrayList();
                for(Int2IntMap.Entry entry:entityRideMap.int2IntEntrySet()){
                    ids.add(entry.getIntKey());
                    ids.add(entry.getIntValue());
                }
                nbt.setIntArray("rides",ids.toIntArray());
            }
        }

        if (!blocks.isEmpty()) {
            List<BlockDat> palette = new ArrayList<>(new HashSet<>(blocks.values()));

            ReadWriteNBTCompoundList paletteList = nbt.getCompoundList("palette");
            for (BlockDat block : palette) {
                ReadWriteNBT out=paletteList.addCompound();
                block.encode(out);
            }

            BlockMapEncoder encoder = new BlockMapEncoder(size.x(), size.y(), size.z(), palette, blocks);
            nbt.setIntArray("blocks", encoder.run());
        }
    }

    public static StructureContainer decode(ReadableNBT nbt,boolean entitiesAllowed) throws StructureSerializeException {
        int[] sizeArr = nbt.getIntArray("size");
        if (sizeArr == null||sizeArr.length==0) throw new StructureSerializeException("\"size\" missed");

        Vec3 size = DBlUtil.arrayToVec3(sizeArr);
        if (size == null || sizeIsInvalid(size)) throw new StructureSerializeException("\"size\" is invalid");

        List<EntitySpawnData> entities = new ArrayList<>();
        Int2IntMap leashes = new Int2IntArrayMap();
        Int2IntMap rides = new Int2IntArrayMap();

        if (entitiesAllowed) {
            ReadableNBTList<ReadWriteNBT> entityList = nbt.getCompoundList("entities");

            if(entityList != null && !entityList.isEmpty()){
                int index = 0;
                for (ReadWriteNBT entity : entityList) {
                    EntitySpawnData data;
                    try{
                        data=EntitySpawnData.decode(entity,size);
                    }catch(StructureSerializeException e){
                        throw e.wrapListException("Entity",index);
                    }

                    entities.add(data);
                    index++;
                }
            }

            int[] leashArr = nbt.getIntArray("leashes");
            int[] rideArr = nbt.getIntArray("rides");

            if(leashArr!=null){
                if(leashArr.length%2!=0)throw new StructureSerializeException("\"leashes\" has invalid size");

                int leashCount=leashArr.length/2;
                for(int i=0;i<leashCount;i++){
                    int from=leashArr[i*2];
                    int to=leashArr[i*2+1];

                    if(from>=entities.size()||from<0||to>=entities.size()||to<0)throw
                        new StructureSerializeException("\"leashes\" element is invalid, leashing unknown entity");

                    leashes.put(from,to);
                }
            }

            if(rideArr!=null){
                if(rideArr.length%2!=0)throw new StructureSerializeException("\"rides\" has invalid size");

                int rideCount=rideArr.length/2;
                for(int i=0;i<rideCount;i++){
                    int from=rideArr[i*2];
                    int to=rideArr[i*2+1];

                    if(from>=entities.size()||from<0||to>=entities.size()||to<0)throw
                        new StructureSerializeException("\"rides\" element is invalid, riding unknown entity");

                    rides.put(from,to);
                }
            }
        }

        ReadableNBTList<ReadWriteNBT> paletteList = nbt.getCompoundList("palette");

        List<BlockDat> palette;
        if (paletteList != null && !paletteList.isEmpty()) {
            palette=new ArrayList<>();
            int index = 0;
            for (ReadWriteNBT paletteObj : paletteList){
                BlockDat block;
                try{
                    block=BlockDat.decode(paletteObj);
                } catch (StructureSerializeException e) {
                    throw e.wrapListException("Block",index);
                }
                palette.add(block);

                index++;
            }
        }else palette=null;

        int[] blockArr = nbt.getIntArray("blocks");

        Map<Vec3, BlockDat> blocks;
        if(blockArr!=null&&blockArr.length!=0){
            if (palette == null) throw new StructureSerializeException("Block map without palette");

            BlockMapDecoder decoder = new BlockMapDecoder(size.x(), size.y(), size.z(), palette, blockArr);
            blocks = decoder.run();
        } else blocks = new HashMap<>();

        return new StructureContainer(size, blocks, entities, leashes, rides);
    }

    @FunctionalInterface
    public interface BlockIterator{
        void accept(Vec3 global,Vec3 local);
    }

    @FunctionalInterface
    public interface EntityIterator{
        boolean check(Entity entity,EntityPos local);
    }
}
