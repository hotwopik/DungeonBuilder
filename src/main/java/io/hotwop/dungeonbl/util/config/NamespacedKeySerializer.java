package io.hotwop.dungeonbl.util.config;

import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public final class NamespacedKeySerializer implements TypeSerializer<NamespacedKey> {
    public static final NamespacedKeySerializer instance=new NamespacedKeySerializer();
    private NamespacedKeySerializer(){}

    @Override
    public NamespacedKey deserialize(@NonNull Type type, @NonNull ConfigurationNode node) throws SerializationException {
        String str=node.getString();
        if(str==null)throw new SerializationException(node,type,"String expected");
        NamespacedKey key=NamespacedKey.fromString(str);
        if(key==null)throw new SerializationException(node,type,"Value is invalid id");
        return key;
    }

    @Override
    public void serialize(@NonNull Type type, @Nullable NamespacedKey obj, @NonNull ConfigurationNode node) throws SerializationException{
        if(obj==null)return;
        node.set(String.class,obj.toString());
    }
}
