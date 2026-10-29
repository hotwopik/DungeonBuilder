package io.hotwop.dungeonbl.structure;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.mark.Mark;
import io.hotwop.dungeonbl.mark.MarkStorage;
import io.hotwop.dungeonbl.mark.type.BuildingOrigin;
import io.hotwop.dungeonbl.structure.builder.WorldBuilder;
import io.hotwop.dungeonbl.structure.container.StructureContainer;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.World;

public record Decoration(StructureContainer container, Vec3 origin, SideRotation rotation){
    public Decoration(StructureContainer container,Vec3 origin,SideRotation rotation){
        this.container = container;
        this.origin = origin;
        this.rotation = rotation;

        Vec3 size=container.size();
        if(origin.x()>=size.x()||origin.y()>=size.y()||origin.z()>=size.z())
            throw new IllegalArgumentException("Decoration origin is out of bounds");
    }

    public Vec3 size(){
        return container.size();
    }

    public static Decoration scan(World world, Vec3 begin, Vec3 end) throws StructureScanException{
        ThreadLocal<Vec3> origin=new ThreadLocal<>();
        ThreadLocal<SideRotation> rotation=new ThreadLocal<>();

        StructureContainer container=StructureContainer.scan(
            world,
            begin,end,
            (global,local)->{
                Mark<?> mark= MarkStorage.getMark(global.toLocation(world));
                if(mark instanceof BuildingOrigin originMark){
                    origin.set(local);
                    rotation.set(originMark.rotation());
                }
            },
            (entity,local)->false
        );

        Vec3 originValue=origin.get();
        SideRotation rotationValue=rotation.get();

        if(originValue==null||rotationValue==null)throw new StructureScanException("Origin is not set");

        return new Decoration(container,originValue,rotationValue);
    }

    public void place(Vec3 position, SideRotation rotation, WorldBuilder builder){
        SideRotation outRotation=rotation.sub(this.rotation);
        container.place(position, origin, outRotation, builder, false);
    }

    public ReadWriteNBT encode() throws StructureSerializeException{
        ReadWriteNBT out= NBT.createNBTObject();
        container.encode(out,false);

        out.setIntArray("origin", DBlUtil.vec3ToArray(origin));
        out.setInteger("rotation", rotation.ordinal());

        return out;
    }

    public static Decoration decode(ReadableNBT nbt) throws StructureSerializeException{
        StructureContainer container=StructureContainer.decode(nbt,false);

        Vec3 size=container.size();

        int[] originArr=nbt.getIntArray("origin");
        if(originArr==null||originArr.length==0)throw new StructureSerializeException("\"origin\" is missing");

        Vec3 origin= DBlUtil.arrayToVec3(originArr);
        if(origin==null)throw new StructureSerializeException("\"origin\" is invalid");

        if(origin.x()>=size.x()||origin.y()>=size.y()||origin.z()>=size.z())
            throw new IllegalArgumentException("\"origin\" is out of bounds");

        Integer rotationVal=nbt.getInteger("rotation");

        SideRotation rotation= SideRotation.get(rotationVal);

        return new Decoration(container,origin,rotation);
    }
}