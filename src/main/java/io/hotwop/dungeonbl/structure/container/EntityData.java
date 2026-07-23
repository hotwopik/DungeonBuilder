package io.hotwop.dungeonbl.structure.container;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Painting;

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

            rw.removeKey("UUID");
            rw.removeKey("PortalCooldown");
            rw.removeKey("Brain");
            rw.removeKey("DeathTime");
            rw.removeKey("DeathLootTableSeed");
            rw.removeKey("home_pos");
            rw.removeKey("home_radius");
            rw.removeKey("HurtTime");
            rw.removeKey("last_hurt_by_mob");
            rw.removeKey("last_hurt_by_player");
            rw.removeKey("last_hurt_by_player_memory_time");
            rw.removeKey("ticks_since_last_hurt_by_mob");
            rw.removeKey("leash");
            rw.removeKey("sleeping_pos");

            rw.removeKey("Bukkit.updateLevel");
            rw.removeKey("WorldUUIDMost");
            rw.removeKey("WorldUUIDLeast");

            rw.removeKey("Spigot.ticksLived");

            rw.removeKey("Paper.SpawnReason");
            rw.removeKey("Paper.OriginWorld");
            rw.removeKey("Paper.Origin");
        }
    }

    public void spawn(Location loc,Consumer<Entity> preload){
        loc.getWorld().spawn(loc,type.getEntityClass(),false,ent->{
            NBT.modify(ent,data->{
                data.mergeCompound(nbt);
            });
            preload.accept(ent);
        });
    }
}