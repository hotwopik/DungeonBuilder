package io.hotwop.dungeonbl.versioned.v26_2;

import io.hotwop.dungeonbl.DBlCore;
import io.hotwop.dungeonbl.versioned.TemporalWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftGameRule;

import java.nio.file.Path;

public final class TemporalWorldV extends TemporalWorld{
    public final GameRules gameRules;
    public final ServerLevel level;

    protected TemporalWorldV(
        String worldName,
        NamespacedKey worldId,
        Path worldContainer,
        Path worldFolder,
        ServerLevel level,
        GameRules gameRules
    ){
        super(worldName, worldId, worldContainer, worldFolder, level.getWorld());
        this.level=level;
        this.gameRules=gameRules;
    }

    @Override
    public <T> void setGameRule(GameRule<T> gameRule, T value) {
        gameRules.set(CraftGameRule.bukkitToMinecraft(gameRule),value,level);
    }

    @Override
    public void destroy() {
        level.getDataStorage().cache
            .forEach((type, optional)->optional
                .ifPresent(d->d.setDirty(false))
            );
        Bukkit.unloadWorld(world,false);
        DBlCore.getPlugin().getExecutorService()
            .execute(this::destroyWorldFolder);
    }
}
