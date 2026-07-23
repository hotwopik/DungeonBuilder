package io.hotwop.dungeonbl.util.config;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public final class BukkitRegistrySerializer<T extends Keyed> implements TypeSerializer<T>{
    public static final BukkitRegistrySerializer<ItemType> itemTypeSerializer=new BukkitRegistrySerializer<>(
        "Item Type",
        Registry.ITEM
    );
    public static final BukkitRegistrySerializer<BlockType> blockTypeSerializer=new BukkitRegistrySerializer<>(
        "Block Type",
        Registry.BLOCK
    );
    public static final BukkitRegistrySerializer<EntityType> entityTypeSerializer=new BukkitRegistrySerializer<>(
        "Entity Type",
        Registry.ENTITY_TYPE
    );

    public final String name;
    public final Registry<T> registry;

    public BukkitRegistrySerializer(String name, Registry<T> registry) {
        this.name=name;
        this.registry=registry;
    }

    @Override
    public T deserialize(@NonNull Type type, @NonNull ConfigurationNode node) throws SerializationException {
        NamespacedKey key=NamespacedKeySerializer.instance
            .deserialize(type,node);

        T value=registry.get(key);
        if(value==null)throw new SerializationException(node,type,"Unknown "+name+": "+key);

        return value;
    }

    @Override
    public void serialize(@NonNull Type type, @Nullable T obj, @NonNull ConfigurationNode node) throws SerializationException {
        if(obj==null)return;

        NamespacedKey key=registry.getKey(obj);
        if(key==null)throw new SerializationException(node,type,"Unidentifiable "+name+": "+obj);

        NamespacedKeySerializer.instance
            .serialize(type,key,node);
    }
}
