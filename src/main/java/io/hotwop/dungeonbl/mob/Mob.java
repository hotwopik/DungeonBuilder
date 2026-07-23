package io.hotwop.dungeonbl.mob;

import io.hotwop.dungeonbl.util.config.MobSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

public record Mob(EntitySnapshot snapshot){
    public static final TypeSerializerCollection serializers=TypeSerializerCollection.builder()
        .register(Mob.class,MobSerializer.instance)
        .build();

    public Entity spawn(Location location){
        return snapshot.createEntity(location);
    }
}
