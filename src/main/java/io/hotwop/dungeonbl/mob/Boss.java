package io.hotwop.dungeonbl.mob;

import org.bukkit.boss.BarColor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record Boss(
    String mob,
    MobType type,
    @Nullable BarColor color
){
}