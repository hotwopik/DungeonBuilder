package io.hotwop.dungeonbl.util.math;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public record Vec3d(
    double x,
    double y,
    double z
) implements AsVec{
    public Vec3d(Location loc){
        this(loc.getX(),loc.getY(),loc.getZ());
    }

    public Vec3d(Vector vec){
        this(vec.getX(),vec.getY(),vec.getZ());
    }

    public Vec3d(double x, double y, double z, SideRotation rotation){
        this(x*rotation.cos+z*rotation.sin,y,z*rotation.cos-x*rotation.sin);
    }

    public Vec3d(double x, double y, double z, double radians){
        this(x*Math.cos(radians)+z*Math.sin(radians),y,z*Math.cos(radians)-x*Math.sin(radians));
    }

    public Vec3d(Vec3 vec){
        this(vec.x(),vec.y(),vec.z());
    }


    @Override
    public Location toLocation(World world){
        return new Location(world,x,y,z);
    }

    @Override
    public Vector toVector(){
        return new Vector(x,y,z);
    }

    @Override
    public double length(){
        return Math.sqrt(x*x+y*y+z*z);
    }

    @Override
    public AsVec add(AsVec other) {
        return switch(other){
            case Vec3 vec3 -> add(vec3);
            case Vec3d vec3d -> add(vec3d);
        };
    }

    @Override
    public AsVec sub(AsVec other) {
        return null;
    }


    public Vec3d rotate(SideRotation rotation){
        return new Vec3d(x,y,z,rotation);
    }

    public Vec3d rotate(double radians){
        return new Vec3d(x,y,z,radians);
    }


    public Vec2 getChunk(){
        return new Vec2(((int)Math.floor(x))>>4,((int)Math.floor(z))>>4);
    }


    public Vec3d add(Vec3d vec){
        return new Vec3d(x+vec.x,y+vec.y,z+vec.z);
    }

    public Vec3d add(Vec3 vec){
        return new Vec3d(x+vec.x(),y+vec.y(),z+vec.z());
    }

    public Vec3d add(double x, double y, double z){
        return new Vec3d(x+this.x,y+this.y,z+this.z);
    }


    public Vec3d sub(Vec3d vec){
        return new Vec3d(x-vec.x,y-vec.y,z-vec.z);
    }

    public Vec3d sub(Vec3 vec){
        return new Vec3d(x-vec.x(),y-vec.y(),z-vec.z());
    }


    public Vec3d mul(double scalar){
        return new Vec3d(x*scalar,y*scalar,z*scalar);
    }

    public static Vec3d center(Vec3 first,Vec3 second){
        return new Vec3d(
            (first.x()+second.x())/2.,
            (first.y()+second.y())/2.,
            (first.z()+second.z())/2.
        );
    }
}
