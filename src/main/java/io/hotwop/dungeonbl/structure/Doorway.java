package io.hotwop.dungeonbl.structure;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.io.RegistryListener;
import io.hotwop.dungeonbl.mark.Mark;
import io.hotwop.dungeonbl.mark.MarkStorage;
import io.hotwop.dungeonbl.mark.type.BuildingOrigin;
import io.hotwop.dungeonbl.structure.builder.WorldBuilder;
import io.hotwop.dungeonbl.structure.container.StructureContainer;
import io.hotwop.dungeonbl.util.ArrowDrawer;
import io.hotwop.dungeonbl.util.math.*;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public record Doorway(StructureContainer container, Vec3 origin, SideRotation rotation){
    public Doorway(StructureContainer container, Vec3 origin, SideRotation rotation){
        this.container = container;
        this.origin = origin;
        this.rotation = rotation;

        Vec3 size=container.size();

        if(isInvalidRotation(rotation, size.x() > 1, size.z() > 1))throw
            new IllegalArgumentException("Doorway size is invalid");

        if(origin.x()>=size.x()||origin.y()>=size.y()||origin.z()>=size.z())
            throw new IllegalArgumentException("Doorway origin is out of bounds");
    }

    public Vec3 size(){
        return container.size();
    }

    public static Doorway scan(World world,Vec3 begin,Vec3 end) throws StructureScanException{
        ThreadLocal<Vec3> origin=new ThreadLocal<>();
        ThreadLocal<SideRotation> rotation=new ThreadLocal<>();

        StructureContainer container=StructureContainer.scan(
            world,
            begin,end,
            (global,local)->{
                Mark<?> mark=MarkStorage.getMark(global.toLocation(world));
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

        Vec3 size=container.size();

        if(isInvalidRotation(rotationValue, size.x() > 1, size.z() > 1))throw
            new StructureScanException("Doorway should be 1 block depth");

        return new Doorway(container,originValue,rotationValue);
    }

    private static boolean isInvalidRotation(SideRotation rotation, boolean isX, boolean isZ){
        return !switch (rotation) {
            case R0, R180 -> !isZ;
            case R90, R270 -> !isX;
        };
    }

    public void place(Vec3 position, SideRotation rotation, WorldBuilder builder){
        SideRotation outRotation=rotation.sub(this.rotation);
        container.place(position, origin, outRotation, builder);
    }

    public ReadWriteNBT encode() throws StructureSerializeException{
        ReadWriteNBT out=NBT.createNBTObject();
        container.encode(out,false);

        out.setIntArray("origin", DBlUtil.vec3ToArray(origin));
        out.setInteger("rotation", rotation.ordinal());

        return out;
    }

    public static Doorway decode(ReadableNBT nbt) throws StructureSerializeException{
        StructureContainer container=StructureContainer.decode(nbt,false);

        Vec3 size=container.size();

        int[] originArr=nbt.getIntArray("origin");
        if(originArr==null)throw new StructureSerializeException("\"origin\" is missing");

        Vec3 origin=DBlUtil.arrayToVec3(originArr);
        if(origin==null)throw new StructureSerializeException("\"origin\" is invalid");

        if(origin.x()>=size.x()||origin.y()>=size.y()||origin.z()>=size.z())
            throw new StructureSerializeException("\"origin\" is out of bounds");

        Integer rotationVal=nbt.getInteger("rotation");
        if(rotationVal==null)throw new StructureSerializeException("\"rotation\" is missing");

        SideRotation rotation= SideRotation.get(rotationVal);
        if(isInvalidRotation(rotation, size.x() > 1, size.z() > 1))throw
            new StructureSerializeException("Doorway size and rotation dont match");

        return new Doorway(container,origin,rotation);
    }

    public static class Display implements RegistryListener<Doorway> {
        private final String doorway;

        private final World world;

        private final Particle.DustOptions arrowParticle;
        private final Particle.DustOptions boxParticle;
        private final Vec3d center;
        private final ArrowDrawer arrowDrawer;

        private Vec3 size;
        private Vec3 origin;
        private SideRotation rotation;
        private SideRotation doorwayRotation;

        private Vec3 xVec;
        private Vec3 yVec;
        private Vec3 zVec;

        private List<AsVec> xPoses;
        private List<AsVec> yPoses;
        private List<AsVec> zPoses;

        private boolean ready=false;

        public Display(
            String doorway,
            Location location,
            SideRotation rotation,
            Particle.DustOptions boxParticle,
            Particle.DustOptions arrowParticle
        ){
            this.doorway=doorway;
            this.world=location.getWorld();
            this.rotation=rotation;
            this.boxParticle=boxParticle;
            this.arrowParticle=arrowParticle;

            arrowDrawer=new ArrowDrawer(location,arrowParticle,rotation);
            center=arrowDrawer.center;

            DungeonBl.registries().doorways
                .addListener(doorway,this);
        }

        public void rotate(SideRotation rotation){
            this.rotation=rotation;
            rotate();
        }

        private void rotate(){
            if(!ready)return;

            arrowDrawer.rotate(rotation);

            SideRotation outRotation=rotation.add(doorwayRotation);

            xVec=new Vec3(-size.x(),0,0).rotate(outRotation);
            zVec=new Vec3(0,0,-size.z()).rotate(outRotation);

            xPoses=List.of(
                new Vec3d(.5,0,-size.z()+.5).add(origin).rotate(outRotation),
                new Vec3d(.5,0,.5).add(origin).rotate(outRotation),
                new Vec3d(.5,size.y(),-size.z()+.5).add(origin).rotate(outRotation),
                new Vec3d(.5,size.y(),.5).add(origin).rotate(outRotation)
            );
            zPoses=List.of(
                new Vec3d(.5,0,.5).add(origin).rotate(outRotation),
                new Vec3d(-size.x()+.5,0,.5).add(origin).rotate(outRotation),
                new Vec3d(.5,size.y(),.5).add(origin).rotate(outRotation),
                new Vec3d(-size.x()+.5,size.y(),.5).add(origin).rotate(outRotation)
            );
            yPoses=List.of(
                new Vec3d(.5,0,-size.z()+.5).add(origin).rotate(outRotation),
                new Vec3d(.5,0,.5).add(origin).rotate(outRotation),
                new Vec3d(-size.x()+.5,0,-size.z()+.5).add(origin).rotate(outRotation),
                new Vec3d(-size.x()+.5,0,.5).add(origin).rotate(outRotation)
            );
        }

        public void update(Doorway doorway){
            ready=true;
            size=doorway.size();
            origin=new Vec3(doorway.origin.x(),-doorway.origin.y(),doorway.origin.z());
            doorwayRotation=doorway.rotation;
            yVec=new Vec3(0,size.y(),0);

            rotate();
        }

        public void remove(){
            ready=false;
        }

        public void close(){
            DungeonBl.registries().doorways
                .removeListener(doorway,this);
        }

        public void draw(List<Player> players){
            //Center
            world.spawnParticle(Particle.DUST,players,null,arrowDrawer.center.x(),arrowDrawer.center.y(),arrowDrawer.center.z(),1,0,0,0,0,arrowParticle,false);

            if(!ready)return;
            //Borders
            DBlUtil.drawMultipleLines(boxParticle,world,players,center,xVec,0.5,xPoses);
            DBlUtil.drawMultipleLines(boxParticle,world,players,center,yVec,0.5,yPoses);
            DBlUtil.drawMultipleLines(boxParticle,world,players,center,zVec,0.5,zPoses);
            //Arrow
            arrowDrawer.draw(players);
        }
    }
}
