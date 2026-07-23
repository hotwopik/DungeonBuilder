package io.hotwop.dungeonbl.util.math;

import org.bukkit.Location;
import org.bukkit.World;

public record Vec2(
    int x,
    int y
){
    public Location asChunkToLocation(World world,Vec3 pos){
        if(pos.x()<0||pos.y()<0||pos.z()<0||pos.x()>=16||pos.y()>=16||pos.z()>=16)throw new IllegalArgumentException("Invalid coordinates");
        return new Location(world,x*16+pos.x(),pos.y(),y*16+pos.z());
    }
}
