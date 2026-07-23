package io.hotwop.dungeonbl.structure.container;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.*;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.structure.StructureSerializeException;
import io.hotwop.dungeonbl.structure.builder.WorldBuilder;
import io.hotwop.dungeonbl.structure.codec.BlockMapDecoder;
import io.hotwop.dungeonbl.structure.codec.BlockMapEncoder;
import io.hotwop.dungeonbl.util.math.*;
import io.hotwop.dungeonbl.util.math.SideRotation;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.material.Directional;

import java.util.*;

public record StructureContainer(Vec3 size, Map<Vec3, BlockDat> blocks, List<EntitySpawnData> entities) {
    public static final Set<EntityType> unallowedEntityTypes=Set.of(
        EntityType.PLAYER,EntityType.UNKNOWN,

        EntityType.EXPERIENCE_BOTTLE,EntityType.EXPERIENCE_ORB,

        EntityType.ARROW, EntityType.SPECTRAL_ARROW,EntityType.BREEZE_WIND_CHARGE,EntityType.WIND_CHARGE,
        EntityType.WITHER_SKULL,

        EntityType.AREA_EFFECT_CLOUD,

        EntityType.EYE_OF_ENDER, EntityType.ENDER_PEARL,

        EntityType.OMINOUS_ITEM_SPAWNER
    );

    public static final double entityMaxOutOfBounds=5;

    public StructureContainer(Vec3 size, Map<Vec3, BlockDat> blocks, List<EntitySpawnData> entities) {
        this.size = size;
        this.blocks = Map.copyOf(blocks);
        this.entities = List.copyOf(entities);

        if (sizeIsInvalid(size)) throw new IllegalArgumentException("Size is invalid");

        blocks.forEach((pos, block) -> {
            if (pos.x() < 0 || pos.y() < 0 || pos.z() < 0 ||
                pos.x() >= size.x() || pos.y() >= size.y() || pos.z() >= size.z()
            ) throw new IllegalArgumentException("Blocks out of bounds");
            if (block.data().getMaterial().isEmpty()) throw new IllegalArgumentException("Empty block in data");
        });

        entities.forEach((entity) -> {
            if(unallowedEntityTypes.contains(entity.data().type()))throw new IllegalArgumentException("Unallowed entities");

            Vec3d pos = entity.pos();
            if (pos.x() < -entityMaxOutOfBounds || pos.y() < -entityMaxOutOfBounds || pos.z() < -entityMaxOutOfBounds ||
                pos.x() > size.x()+entityMaxOutOfBounds || pos.y() > size.y()+entityMaxOutOfBounds || pos.z() > size.z()+entityMaxOutOfBounds
            ) throw new IllegalArgumentException("Entity out of bounds");
        });
    }

    public static boolean sizeIsInvalid(Vec3 size) {
        if (size.x() <= 0 || size.y() <= 0 || size.z() <= 0) return true;
        long space = (long) size.x() + (long) size.y() + (long) size.z() + 3L;
        return space >= Integer.MAX_VALUE;
    }

    public void place(Vec3 position, Vec3 origin, SideRotation rotation, WorldBuilder builder) {
        blocks.forEach((pos, data)->{
            BlockDat outData=switch(data.data()){
                case org.bukkit.block.data.Directional dir->{
                    dir=(org.bukkit.block.data.Directional)dir.clone();
                    dir.setFacing(rotation.addTo(dir.getFacing()));
                    yield data.replaceData(dir);
                }
                case MultipleFacing multiFacing->{
                    multiFacing=(MultipleFacing)multiFacing.clone();
                    Set<BlockFace> faces=Set.copyOf(multiFacing.getFaces());

                    for(BlockFace face:faces){
                        multiFacing.setFace(face,false);
                    }

                    for(BlockFace face:faces){
                        multiFacing.setFace(rotation.addTo(face),true);
                    }

                    yield data.replaceData(multiFacing);
                }
                case Rotatable rotatable->{
                    rotatable=(Rotatable)rotatable.clone();
                    rotatable.setRotation(rotation.addTo(rotatable.getRotation()));
                    yield data.replaceData(rotatable);
                }
                case Wall wall->{
                    wall=(Wall)wall.clone();
                    Wall.Height[] heights={
                        wall.getHeight(BlockFace.NORTH),
                        wall.getHeight(BlockFace.WEST),
                        wall.getHeight(BlockFace.SOUTH),
                        wall.getHeight(BlockFace.EAST),
                    };

                    int shift=rotation.ordinal();
                    wall.setHeight(BlockFace.NORTH,heights[shift]);
                    wall.setHeight(BlockFace.WEST,heights[(1+shift)%4]);
                    wall.setHeight(BlockFace.SOUTH,heights[(2+shift)%4]);
                    wall.setHeight(BlockFace.EAST,heights[(3+shift)%4]);

                    yield data.replaceData(wall);
                }
                default->data;
            };

            builder.place(pos.sub(origin).rotate(rotation).add(position), outData);
        });

        for(Vec3 pos:Box3.withSize(new Vec3(0,0,0),size)){
            if(!blocks.containsKey(pos)){
                builder.place(pos.sub(origin).rotate(rotation).add(position),BlockDat.airData);
            }
        }

        entities.forEach((entity)->builder
            .spawn(entity.pos().sub(origin).rotate(rotation).add(position), entity.data(), ent->{
                ent.setRotation(ent.getYaw()+rotation.degrees,ent.getPitch());

                if(ent instanceof Directional direct){
                    direct.setFacingDirection(rotation.addTo(direct.getFacing()));
                }
            })
        );
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
        for(Entity entity:world.getNearbyEntities(new Box3(begin,end).toBoundingBox())){
            if(unallowedEntityTypes.contains(entity.getType()))continue;

            EntitySpawnData data=EntitySpawnData.read(entity);
            Vec3d local=data.pos().sub(min);
            if(entityIterator.check(entity,local)){
                entities.add(data.replacePosition(local));
            }
        }

        chunks.forEach(ch->ch.removePluginChunkTicket(DungeonBl.plugin()));

        return new StructureContainer(size.add(new Vec3(1,1,1)),blocks,entities);
    }

    public void encode(ReadWriteNBT nbt,boolean encodeEntities) throws StructureSerializeException {
        nbt.setIntArray("size", DBlUtil.vec3ToArray(size));

        if (encodeEntities&&!entities.isEmpty()) {
            ReadWriteNBTCompoundList entityList = nbt.getCompoundList("entities");
            for (EntitySpawnData entry : entities) {
                ReadWriteNBT out = entityList.addCompound();

                DBlUtil.writeVec3d(entry.pos(), out.getDoubleList("pos"));

                NamespacedKey type;
                try {
                    type = entry.data().type().getKey();
                } catch (IllegalArgumentException e) {
                    throw new StructureSerializeException("Entity is unidentified", e);
                }

                out.setString("type", type.asMinimalString());
                out.set("data", entry.data().nbt(), NBTHandlers.STORE_READABLE_TAG);
            }
        }

        if (!blocks.isEmpty()) {
            List<BlockDat> palette = new ArrayList<>(new HashSet<>(blocks.values()));

            ReadWriteNBTCompoundList paletteList = nbt.getCompoundList("palette");
            for (BlockDat block : palette) {
                ReadWriteNBT out=paletteList.addCompound();

                out.setString("state",block.data().getAsString(true));
                if(block.nbt()!=null)out.set("nbt",block.nbt(),NBTHandlers.STORE_READABLE_TAG);
            }

            BlockMapEncoder encoder = new BlockMapEncoder(size.x(), size.y(), size.z(), palette, blocks);
            nbt.setIntArray("blocks", encoder.run());
        }
    }

    public static StructureContainer decode(ReadableNBT nbt,boolean entitiesAllowed) throws StructureSerializeException {
        int[] sizeArr = nbt.getIntArray("size");
        if (sizeArr == null) throw new StructureSerializeException("\"size\" missed");

        Vec3 size = DBlUtil.arrayToVec3(sizeArr);
        if (size == null || sizeIsInvalid(size)) throw new StructureSerializeException("\"size\" is invalid");

        ReadableNBTList<ReadWriteNBT> entityList = nbt.getCompoundList("entities");
        List<EntitySpawnData> entities = new ArrayList<>();
        if (entitiesAllowed && entityList != null && !entityList.isEmpty()) {
            int index = 0;
            for (ReadWriteNBT entity : entityList) {
                ReadableNBTList<Double> posArr = entity.getDoubleList("pos");
                if (posArr == null) throw new StructureSerializeException("Entity " + index + " \"pos\" missed");

                Vec3d pos = DBlUtil.readVec3d(posArr);
                if (pos == null || pos.x() < 0 || pos.y() < 0 || pos.z() < 0 || pos.x() > size.x() || pos.y() > size.y() || pos.z() > size.z())
                    throw new StructureSerializeException("Entity " + index + " \"pos\" is invalid");

                String typeStr = entity.getString("type");
                if (typeStr == null || typeStr.isEmpty())
                    throw new StructureSerializeException("Entity " + index + " \"type\" missing");

                NamespacedKey typeKey = NamespacedKey.fromString(typeStr);
                if (typeKey == null)
                    throw new StructureSerializeException("Entity " + index + " \"type\" is invalid: " + typeStr);

                EntityType type = Registry.ENTITY_TYPE.get(typeKey);
                if (type == null)
                    throw new StructureSerializeException("Entity " + index + " \"type\" is unknown: " + typeKey);
                if(unallowedEntityTypes.contains(type))throw
                    new StructureSerializeException("Entity " + index + " has unallowed \"type\": " + typeKey);

                ReadWriteNBT data = entity.getCompound("data");
                if (data == null) data = NBT.createNBTObject();

                entities.add(new EntitySpawnData(pos, new EntityData(type, data)));

                index++;
            }
        }

        ReadableNBTList<ReadWriteNBT> paletteList = nbt.getCompoundList("palette");
        Map<Vec3, BlockDat> blocks;
        if (paletteList != null && !paletteList.isEmpty()) {
            List<BlockDat> palette = new ArrayList<>();
            int index = 0;
            for (ReadWriteNBT paletteObj : paletteList){
                String paletteStr=paletteObj.getString("state");
                if(paletteStr==null||paletteStr.isEmpty())throw
                    new StructureSerializeException("Block " + index + " \"state\" is missing");

                BlockData data;
                try {
                    data = Bukkit.createBlockData(paletteStr);
                } catch (IllegalArgumentException e) {
                    throw new StructureSerializeException("Block " + index + " \"state\" is invalid", e);
                }

                ReadableNBT nbtOut;
                if(paletteObj.hasTag("nbt", NBTType.NBTTagCompound)){
                    nbtOut=paletteObj.getCompound("nbt");
                }else nbtOut=null;

                try{
                    palette.add(new BlockDat(data, nbtOut));
                }catch(RuntimeException e){
                    throw new StructureSerializeException("Block " + index + " is invalid", e);
                }

                index++;
            }

            int[] blockArr = nbt.getIntArray("blocks");
            if (blockArr == null) throw new StructureSerializeException("Palette was given but blocks not");

            BlockMapDecoder decoder = new BlockMapDecoder(size.x(), size.y(), size.z(), palette, blockArr);
            blocks = decoder.run();
        } else blocks = new HashMap<>();

        return new StructureContainer(size, blocks, entities);
    }

    @FunctionalInterface
    public interface BlockIterator{
        void accept(Vec3 global,Vec3 local);
    }

    @FunctionalInterface
    public interface EntityIterator{
        boolean check(Entity entity,Vec3d local);
    }
}
