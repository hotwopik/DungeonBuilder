package io.hotwop.dungeonbl.structure.container;

import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTList;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBTList;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.structure.StructureSerializeException;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.util.math.Vec3;
import io.hotwop.dungeonbl.util.math.Vec3d;
import org.bukkit.entity.EntityType;

public sealed interface EntityPos permits EntityPos.Default, EntityPos.BlockPos{
    Vec3d halfBlock=new Vec3d(0.5,0,0.5);
    double maxOutOfBounds=5;

    EntityPos makeLocal(Vec3 origin);
    Vec3d release(Vec3 origin,SideRotation rotation,Vec3 position);
    void validate(Vec3 size) throws IllegalArgumentException;
    void encode(ReadWriteNBT nbt);

    record Default(Vec3d pos) implements EntityPos{
        @Override
        public EntityPos makeLocal(Vec3 origin) {
            return new Default(pos.sub(origin).sub(halfBlock));
        }

        @Override
        public Vec3d release(Vec3 origin, SideRotation rotation, Vec3 position) {
            return pos.rotateAndTranslate(origin, rotation, position).add(halfBlock);
        }

        @Override
        public void validate(Vec3 size) throws IllegalArgumentException {
            if (pos.x() < -maxOutOfBounds || pos.y() < -maxOutOfBounds || pos.z() < -maxOutOfBounds ||
                pos.x() > size.x()+maxOutOfBounds || pos.y() > size.y()+maxOutOfBounds || pos.z() > size.z()+maxOutOfBounds
            ) throw new IllegalArgumentException("Entity out of bounds: x: "+pos.x()+" y: "+pos.y()+" z: "+pos.z());
        }

        @Override
        public void encode(ReadWriteNBT nbt) {
            ReadWriteNBTList<Double> posArr=nbt.getDoubleList("pos");
            DBlUtil.writeVec3d(pos,posArr);
        }
    }
    record BlockPos(Vec3 pos) implements EntityPos{
        @Override
        public EntityPos makeLocal(Vec3 origin) {
            return new BlockPos(pos.sub(origin));
        }

        @Override
        public Vec3d release(Vec3 origin, SideRotation rotation, Vec3 position) {
            return new Vec3d(pos.rotateAndTranslate(origin, rotation, position));
        }

        @Override
        public void validate(Vec3 size) throws IllegalArgumentException {
            if (pos.x() < -maxOutOfBounds || pos.y() < -maxOutOfBounds || pos.z() < -maxOutOfBounds ||
                pos.x() > size.x()+maxOutOfBounds || pos.y() > size.y()+maxOutOfBounds || pos.z() > size.z()+maxOutOfBounds
            ) throw new IllegalArgumentException("Entity out of bounds: x: "+pos.x()+" y: "+pos.y()+" z: "+pos.z());
        }

        @Override
        public void encode(ReadWriteNBT nbt) {
            int[] posArr=DBlUtil.vec3ToArray(pos);
            nbt.setIntArray("block_pos",posArr);
        }
    }

    static EntityPos decode(ReadableNBT nbt) throws StructureSerializeException{
        int[] blockPosArr=nbt.getIntArray("block_pos");

        if(blockPosArr!=null&&blockPosArr.length!=0){
            Vec3 blockPos=DBlUtil.arrayToVec3(blockPosArr);
            if(blockPos==null)throw new StructureSerializeException("\"block_pos\" is invalid");
            return new BlockPos(blockPos);
        }

        ReadableNBTList<Double> posArr=nbt.getDoubleList("pos");
        if(posArr==null)throw new StructureSerializeException("\"pos\" is missing");

        Vec3d pos=DBlUtil.readVec3d(posArr);
        if(pos==null)throw new StructureSerializeException("\"pos\" is invalid");

        return new Default(pos);
    }
}
