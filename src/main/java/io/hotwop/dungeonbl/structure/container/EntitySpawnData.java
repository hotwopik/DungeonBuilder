package io.hotwop.dungeonbl.structure.container;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBTList;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.DBlVersioned;
import io.hotwop.dungeonbl.structure.StructureSerializeException;
import io.hotwop.dungeonbl.util.math.Vec3;
import io.hotwop.dungeonbl.util.math.Vec3d;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.*;
import org.jetbrains.annotations.Nullable;

public record EntitySpawnData(
    EntityPos pos,
    @Nullable Vec3 sleepPos,
    @Nullable Vec3 blockLeash,
    EntityData data
){
    public EntitySpawnData replacePositions(EntityPos pos,@Nullable Vec3 sleepPos){
        return new EntitySpawnData(pos,sleepPos,blockLeash,data);
    }

    public static EntitySpawnData read(Entity entity) throws RuntimeException{
        EntityType type=entity.getType();

        ReadWriteNBT nbt=NBT.createNBTObject();
        NBT.get(entity,nbt::mergeCompound);

        Vec3d pos;
        ReadableNBTList<Double> posNBT=nbt.getDoubleList("Pos");
        if(posNBT!=null){
            pos=DBlUtil.readVec3d(posNBT);
        }else throw new RuntimeException("Entity without position");

        Vec3 tilePos= getTilePos(nbt);
        Vec3 sleepPos= getSleepPos(nbt);
        Vec3 blockLeash= getBlockLeash(nbt);

        return new EntitySpawnData(
            tilePos==null?new EntityPos.Default(pos):new EntityPos.BlockPos(tilePos),
            sleepPos,blockLeash,new EntityData(type,nbt)
        );
    }

    public void encode(ReadWriteNBT nbt) throws StructureSerializeException{
        pos.encode(nbt);

        ReadWriteNBT dataOut=nbt.getOrCreateCompound("data");
        data.encode(dataOut);

        if(sleepPos!=null)nbt.setIntArray("sleep_pos",DBlUtil.vec3ToArray(sleepPos));
        if(blockLeash!=null)nbt.setIntArray("block_leash",DBlUtil.vec3ToArray(blockLeash));
    }

    public static EntitySpawnData decode(ReadableNBT entity,Vec3 size) throws StructureSerializeException{
        ReadableNBT dataOut=entity.getCompound("data");
        if(dataOut==null)throw new StructureSerializeException("\"data\" is missing");
        EntityData data=EntityData.decode(dataOut);

        EntityPos pos=EntityPos.decode(entity);
        try{
            pos.validate(size);
        } catch (IllegalArgumentException e) {
            throw new StructureSerializeException("Invalid position",e);
        }

        int[] sleepPosArr=entity.getIntArray("sleep_pos");
        Vec3 sleepPos;
        if(sleepPosArr!=null&&sleepPosArr.length!=0){
            sleepPos=DBlUtil.arrayToVec3(sleepPosArr);
            if(sleepPos==null)throw new StructureSerializeException("\"sleep_pos\" is invalid");
        }else sleepPos=null;

        int[] blockLeashArr=entity.getIntArray("block_leash");
        Vec3 blockLeash;
        if(blockLeashArr!=null&&blockLeashArr.length!=0){
            blockLeash=DBlUtil.arrayToVec3(blockLeashArr);
            if(blockLeash==null)throw new StructureSerializeException("\"block_leash\" is invalid");
        }else blockLeash=null;

        return new EntitySpawnData(pos,sleepPos,blockLeash,data);
    }

    public static @Nullable Vec3 getTilePos(ReadWriteNBT nbt){
        if(DBlVersioned.dataVersion()<4325){
            if(
                nbt.hasTag("TileX", NBTType.NBTTagInt)&&
                nbt.hasTag("TileY", NBTType.NBTTagInt)&&
                nbt.hasTag("TileZ", NBTType.NBTTagInt)
            ){
                Integer tileX=nbt.getInteger("TileX");
                Integer tileY=nbt.getInteger("TileY");
                Integer tileZ=nbt.getInteger("TileZ");

                return new Vec3(tileX,tileY,tileZ);
            }
        }else{
            int[] posArr = nbt.getIntArray("block_pos");

            if (posArr != null) {
                return DBlUtil.arrayToVec3(posArr);
            }
        }
        return null;
    }

    public static @Nullable Vec3 getSleepPos(ReadWriteNBT nbt){
        if(DBlVersioned.dataVersion()<4325){
            if(
                nbt.hasTag("SleepingX", NBTType.NBTTagInt)&&
                nbt.hasTag("SleepingY", NBTType.NBTTagInt)&&
                nbt.hasTag("SleepingZ", NBTType.NBTTagInt)
            ){
                Integer sleepingX=nbt.getInteger("SleepingX");
                Integer sleepingY=nbt.getInteger("SleepingY");
                Integer sleepingZ=nbt.getInteger("SleepingZ");

                return new Vec3(sleepingX,sleepingY,sleepingZ);
            }
        }else{
            int[] sleepingPos=nbt.getIntArray("sleeping_pos");

            if(sleepingPos!=null&&sleepingPos.length!=0){
                return DBlUtil.arrayToVec3(sleepingPos);
            }
        }

        return null;
    }

    public static void setSleepPos(ReadWriteNBT nbt,Vec3 pos){
        if(DBlVersioned.dataVersion()<4325){
            nbt.setInteger("SleepingX", pos.x());
            nbt.setInteger("SleepingY", pos.y());
            nbt.setInteger("SleepingZ", pos.z());
        }else{
            nbt.setIntArray("sleeping_pos",DBlUtil.vec3ToArray(pos));
        }
    }

    public static @Nullable Vec3 getBlockLeash(ReadWriteNBT nbt){
        int[] data;

        if(DBlVersioned.dataVersion()<3815){
            data=nbt.getIntArray("Leash");
        }else{
            data=nbt.getIntArray("leash");
        }

        if(data!=null){
            return DBlUtil.arrayToVec3(data);
        }

        return null;
    }

    public static void setBlockLeash(ReadWriteNBT nbt,Vec3 pos){
        if(DBlVersioned.dataVersion()<3815){
            nbt.setIntArray("Leash",DBlUtil.vec3ToArray(pos));
        }else{
            nbt.setIntArray("leash",DBlUtil.vec3ToArray(pos));
        }
    }
}
