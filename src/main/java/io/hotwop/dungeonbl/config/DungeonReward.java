package io.hotwop.dungeonbl.config;

import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.List;

@ConfigSerializable
public record DungeonReward(
    int money,
    List<ItemStack> items
){}