package io.hotwop.dungeonbl.util.math;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public record Vec3(int x, int y, int z) implements AsVec{
    public Vec3(int x, int y, int z, SideRotation rotation){
        this(x*rotation.cos+z*rotation.sin,y,z*rotation.cos-x*rotation.sin);
    }

    public Vec3(Location loc){
        this(loc.getBlockX(),loc.getBlockY(),loc.getBlockZ());
    }

    public Vec3(Vector vec){
        this(vec.getBlockX(),vec.getBlockY(),vec.getBlockZ());
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
    public double length() {
        return Math.sqrt(x*x+y*y+z*z);
    }

    @Override
    public AsVec add(AsVec other) {
        return switch(other){
            case Vec3 vec3->add(vec3);
            case Vec3d vec3d->add(vec3d);
        };
    }

    @Override
    public AsVec sub(AsVec other) {
        return switch(other){
            case Vec3 vec3->sub(vec3);
            case Vec3d vec3d->sub(vec3d);
        };
    }


    public Vec3 rotate(SideRotation rotation){
        return new Vec3(x,y,z,rotation);
    }


    public Vec2 getChunk(){
        return new Vec2(x>>4,z>>4);
    }

    public Vec3 asChunkPos(){
        return new Vec3(x&0xF,y,z&0xF);
    }


    public Vec3 add(Vec3 vec){
        return new Vec3(x+vec.x,y+vec.y,z+vec.z);
    }

    public Vec3d add(Vec3d vec){
        return new Vec3d((double)x+vec.x(),(double)y+vec.y(),(double)z+vec.z());
    }


    public Vec3 sub(Vec3 vec){
        return new Vec3(x-vec.x,y-vec.y,z-vec.z);
    }

    public Vec3d sub(Vec3d vec){
        return new Vec3d((double)x-vec.x(),(double)y-vec.y(),(double)z-vec.z());
    }


    public Vec3 abs(){
        return new Vec3(Math.abs(x),Math.abs(y),Math.abs(z));
    }


    public Vec3 mul(int value){
        return new Vec3(x*value,y*value,z*value);
    }


    public Vec3 div(int value){
        return new Vec3(x/value,y/value,z/value);
    }


    public static Vec3 min(Vec3 first,Vec3 second){
        return new Vec3(
            Math.min(first.x,second.x),
            Math.min(first.y,second.y),
            Math.min(first.z,second.z)
        );
    }

    public static Vec3 max(Vec3 first,Vec3 second){
        return new Vec3(
            Math.max(first.x,second.x),
            Math.max(first.y,second.y),
            Math.max(first.z,second.z)
        );
    }

    public static Vec3 center(Vec3 first,Vec3 second){
        return new Vec3(
            (first.x+second.x)/2,
            (first.y+second.y)/2,
            (first.z+second.z)/2
        );
    }
}