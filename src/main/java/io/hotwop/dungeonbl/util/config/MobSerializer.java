package io.hotwop.dungeonbl.util.config;

import io.hotwop.dungeonbl.mob.Mob;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntitySnapshot;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public final class MobSerializer implements TypeSerializer<Mob> {
    public static final MobSerializer instance = new MobSerializer();
    private MobSerializer(){}

    @Override
    public Mob deserialize(@NonNull Type type, @NonNull ConfigurationNode node) throws SerializationException{
        String str=node.getString();
        if(str==null)throw new SerializationException(node,type,"String expected");

        EntitySnapshot out;
        try{
            out=Bukkit.getEntityFactory()
                .createEntitySnapshot(str);
        } catch (IllegalArgumentException e) {
            throw new SerializationException(node,type,"Malformed entity data",e);
        }

        return new Mob(out);
    }

    @Override
    public void serialize(@NonNull Type type, @Nullable Mob obj, @NonNull ConfigurationNode node) throws SerializationException{
        if(obj==null)return;
        node.set(obj.snapshot().getAsString());
    }
}
