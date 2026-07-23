package io.hotwop.dungeonbl.config;

import io.hotwop.dungeonbl.mob.CrossMobList;
import io.hotwop.dungeonbl.util.config.ConfigIntRange;
import io.hotwop.dungeonbl.util.config.ItemStackSerializer;
import io.hotwop.dungeonbl.util.config.NamespacedKeySerializer;
import io.hotwop.dungeonbl.util.config.ValidId;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.util.List;
import java.util.Objects;

@ConfigSerializable
public final class Dungeon{
    private @Required @ConfigIntRange(min = 1) int roomCount;

    private @Required Room startRoom;
    private @Required Room endRoom;
    private @Required List<Room> rooms;

    private @ConfigIntRange(min=0) int deathLimit=0;
    private @Required DungeonReward reward;
    private boolean disableStarvation=true;
    private @ConfigIntRange(min=0) int nextRoomDelay=20;

    private Dungeon(){}

    public Dungeon(
        int roomCount,
        int deathLimit,
        Room startRoom,
        Room endRoom,
        List<Room> rooms,
        DungeonReward reward,
        boolean disableStarvation,
        int nextRoomDelay
    ){
        this.roomCount = roomCount;
        this.deathLimit = deathLimit;
        this.startRoom = startRoom;
        this.endRoom = endRoom;
        this.rooms = rooms;
        this.reward = reward;
        this.disableStarvation = disableStarvation;
        this.nextRoomDelay = nextRoomDelay;
    }

    public int roomCount() {
        return roomCount;
    }

    public int deathLimit() {
        return deathLimit;
    }

    public Room startRoom() {
        return startRoom;
    }

    public Room endRoom() {
        return endRoom;
    }

    public List<Room> rooms() {
        return rooms;
    }

    public DungeonReward reward() {
        return reward;
    }

    public boolean disableStarvation(){
        return disableStarvation;
    }

    public int nextRoomDelay(){
        return nextRoomDelay;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Dungeon) obj;
        return this.roomCount == that.roomCount &&
            this.deathLimit == that.deathLimit &&
            Objects.equals(this.startRoom, that.startRoom) &&
            Objects.equals(this.endRoom, that.endRoom) &&
            Objects.equals(this.rooms, that.rooms) &&
            Objects.equals(this.reward, that.reward) &&
            this.disableStarvation == that.disableStarvation &&
            this.nextRoomDelay == that.nextRoomDelay;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            roomCount,
            deathLimit,
            startRoom,
            endRoom,
            rooms,
            reward,
            disableStarvation,
            nextRoomDelay
        );
    }

    public static final ObjectMapper.Factory objectMappers = ObjectMapper.factoryBuilder()
        .addConstraint(ConfigIntRange.class, Number.class, ConfigIntRange.Factory.instance)
        .addConstraint(ValidId.class, String.class, ValidId.Factory.instance)
        .build();

    public static final TypeSerializerCollection serializers = TypeSerializerCollection.builder()
        .register(NamespacedKey.class, NamespacedKeySerializer.instance)
        .register(CrossMobList.class, CrossMobList.Serializer.instance)
        .register(ItemStack.class, ItemStackSerializer.instance)
        .registerAnnotatedObjects(objectMappers)
        .build();
}