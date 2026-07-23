package io.hotwop.dungeonbl.structure.container;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBTList;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.util.math.Vec3;
import io.hotwop.dungeonbl.util.math.Vec3d;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;

import java.util.Set;

public record EntitySpawnData(
    Vec3d pos,
    EntityData data
){
    public static final Set<EntityType> falseHangingEntities=Set.of(EntityType.LEASH_KNOT);

    public EntitySpawnData replacePosition(Vec3d pos){
        return new EntitySpawnData(pos,data);
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

        Vec3 tilePos;
        if(entity instanceof Hanging&&!falseHangingEntities.contains(type)){
            Integer tileX=nbt.getInteger("TileX");
            Integer tileY=nbt.getInteger("TileY");
            Integer tileZ=nbt.getInteger("TileZ");

            if(tileX!=null&&tileY!=null&&tileZ!=null){
                tilePos=new Vec3(tileX,tileY,tileZ);
            }else throw new RuntimeException("Hanging entity without tile position");
        }else tilePos=null;

        return new EntitySpawnData(tilePos==null?pos:new Vec3d(tilePos),new EntityData(type,nbt));
    }
}
