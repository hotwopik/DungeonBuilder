package io.hotwop.dungeonbl.mob;

import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.integrations.MythicMobsIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public enum MobType{
    minecraft(
        (mob,location)->{
            Mob mobS=DungeonBl.registries().mobs.get(mob);
            if(mobS==null)return null;
            return mobS.spawn(location);
        },
        "Minecraft",
        Material.ZOMBIE_HEAD,
        Set.of(
            Material.ZOMBIE_HEAD,Material.GRASS_BLOCK,Material.CREEPER_HEAD,
            Material.ZOMBIE_SPAWN_EGG,Material.CREEPER_SPAWN_EGG
        ),
        ()->true,
        ()->DungeonBl.registries()
            .mobs.idSet().stream()
            .sorted(Comparator.naturalOrder())
            .toList()
    ),
    mythic_mobs(
        MythicMobsIntegration::spawn,
        "MythicMobs",
        Material.SKELETON_SKULL,
        Set.of(
            Material.SKELETON_SKULL,Material.ANDESITE,Material.WITHER_SKELETON_SKULL,
            Material.WITHER_SKELETON_SPAWN_EGG,Material.SKELETON_SPAWN_EGG
        ),
        ()->Bukkit.getPluginManager().isPluginEnabled("MythicMobs"),
        MythicMobsIntegration::list
    );

    private final BiFunction<String,Location,Entity> spawn;
    public final String displayName;
    public final Material icon;
    public final Set<Material> itemIcons;
    public final Supplier<Boolean> active;
    public final Supplier<List<String>> list;

    MobType(
        BiFunction<String, Location,Entity> spawn,
        String displayName,
        Material icon, Set<Material> itemIcons,
        Supplier<Boolean> active,
        Supplier<List<String>> list
    ){
        this.spawn=spawn;
        this.displayName=displayName;
        this.icon=icon;
        this.itemIcons = itemIcons;
        this.active=active;
        this.list=list;
    }
    public Entity spawn(String mob,Location location){
        return spawn.apply(mob,location);
    }
}
