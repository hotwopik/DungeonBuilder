package io.hotwop.dungeonbl.integrations;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class MythicMobsIntegration {
    private MythicMobsIntegration(){}

    public static Entity spawn(String mob, Location location){
        Optional<MythicMob> mobS= MythicBukkit.inst().getMobManager().getMythicMob(mob);
        return mobS.map(mythicMob -> mythicMob.spawn(BukkitAdapter.adapt(location), 1).getEntity().getBukkitEntity()).orElse(null);
    }
    public static List<String> list(){
        return MythicBukkit.inst().getMobManager().getMobTypes().stream().map(MythicMob::getInternalName).sorted(Comparator.naturalOrder()).toList();
    }
}
