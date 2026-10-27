package io.hotwop.dungeonbl.structure.container;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import io.hotwop.dungeonbl.structure.StructureSerializeException;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Painting;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public record EntityData(
    EntityType type,
    ReadableNBT nbt
){
    public EntityData(EntityType type,ReadableNBT nbt){
        this.type=type;
        this.nbt=nbt;

        if(nbt instanceof ReadWriteNBT rw){
            rw.removeKey("Pos");

            rw.removeKey("TileX");
            rw.removeKey("TileY");
            rw.removeKey("TileZ");
            rw.removeKey("block_pos");

            rw.removeKey("SleepingX");
            rw.removeKey("SleepingY");
            rw.removeKey("SleepingZ");
            rw.removeKey("sleeping_pos");

            rw.removeKey("home_pos");
            rw.removeKey("home_radius");

            rw.removeKey("LoveCause");

            rw.removeKey("anger_end_time");
            rw.removeKey("angry_at");

            rw.removeKey("UUID");
            rw.removeKey("PortalCooldown");
            rw.removeKey("Brain");

            rw.removeKey("DeathTime");
            rw.removeKey("DeathLootTableSeed");

            rw.removeKey("HurtTime");
            rw.removeKey("HurtByTimestamp");
            rw.removeKey("last_hurt_by_mob");
            rw.removeKey("last_hurt_by_player");
            rw.removeKey("last_hurt_by_player_memory_time");
            rw.removeKey("ticks_since_last_hurt_by_mob");

            rw.removeKey("leash");
            rw.removeKey("Leash");

            rw.removeKey("Passengers");

            rw.removeKey("Bukkit.updateLevel");
            rw.removeKey("WorldUUIDMost");
            rw.removeKey("WorldUUIDLeast");

            rw.removeKey("Spigot.ticksLived");

            rw.removeKey("Paper.SpawnReason");
            rw.removeKey("Paper.OriginWorld");
            rw.removeKey("Paper.Origin");
        }
    }

    public void spawn(
        Location loc,
        @Nullable Vec3 sleepPos,
        @Nullable Vec3 blockLeash,
        Consumer<Entity> preload
    ){
        loc.getWorld().spawn(loc,type.getEntityClass(),false,ent->{
            NBT.modify(ent,data->{
                data.mergeCompound(nbt);
                if(sleepPos!=null)EntitySpawnData.setSleepPos(data,sleepPos);
                if(blockLeash!=null)EntitySpawnData.setBlockLeash(data,blockLeash);
            });
            preload.accept(ent);
        });
    }

    public void encode(ReadWriteNBT out) throws StructureSerializeException{
        NamespacedKey typeId;
        try {
            typeId = type.getKey();
        } catch (IllegalArgumentException e) {
            throw new StructureSerializeException("Entity is unidentified", e);
        }

        out.setString("type", typeId.asMinimalString());
        out.set("data", nbt, NBTHandlers.STORE_READABLE_TAG);
    }

    public static EntityData decode(ReadableNBT nbt) throws StructureSerializeException{
        String typeStr = nbt.getString("type");
        if (typeStr == null || typeStr.isEmpty())
            throw new StructureSerializeException("\"type\" missing");

        NamespacedKey typeKey = NamespacedKey.fromString(typeStr);
        if (typeKey == null)
            throw new StructureSerializeException("\"type\" is invalid: " + typeStr);

        EntityType type = Registry.ENTITY_TYPE.get(typeKey);
        if (type == null)
            throw new StructureSerializeException("\"type\" is unknown: " + typeKey);
        if(StructureContainer.unallowedEntityTypes.contains(type))throw
            new StructureSerializeException("\"type\" is unallowed: " + typeKey);

        ReadableNBT data = nbt.getCompound("data");
        if (data == null) data = NBT.createNBTObject();

        return new EntityData(type,data);
    }
}