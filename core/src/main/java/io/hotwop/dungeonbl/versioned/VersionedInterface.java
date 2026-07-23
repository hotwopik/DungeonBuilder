package io.hotwop.dungeonbl.versioned;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Path;

public interface VersionedInterface{
    TemporalWorld createTemporalWorld(
        Path worldContainer,
        String folderName,
        String worldName,
        NamespacedKey worldId
    ) throws WorldException;

    DBlAttributeMapping getAttributeMapping();
}
