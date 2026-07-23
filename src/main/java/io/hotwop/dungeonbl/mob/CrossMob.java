package io.hotwop.dungeonbl.mob;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public record CrossMob(MobType type, String mob){
    public Entity spawn(Location location){
        if(!type.active.get())return null;
        return type.spawn(mob,location);
    }
}
