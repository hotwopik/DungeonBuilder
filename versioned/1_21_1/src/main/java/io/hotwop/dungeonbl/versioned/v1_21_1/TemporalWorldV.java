package io.hotwop.dungeonbl.versioned.v1_21_1;

import io.hotwop.dungeonbl.DBlCore;
import io.hotwop.dungeonbl.versioned.TemporalWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftWorld;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

public final class TemporalWorldV extends TemporalWorld{
    public final GameRules gameRules;
    public final ServerLevel level;

    protected TemporalWorldV(String worldName,NamespacedKey worldId, Path worldContainer, Path worldFolder, ServerLevel level){
        super(worldName, worldId, worldContainer, worldFolder, level.getWorld());
        this.level=level;
        this.gameRules = level.getGameRules();
    }

    @Override
    public <T> void setGameRule(GameRule<T> gameRule, T value) {
        GameRules.Value<?> val=gameRules.getRule(CraftWorld.getGameRulesNMS().get(gameRule.getName()));
        val.deserialize(String.valueOf(value));
        val.onChanged(level);
    }

    @Override
    public void destroy() {
        Bukkit.unloadWorld(world,false);
        level.getRaids().setDirty(false);
        DBlCore.getPlugin().getExecutorService()
            .execute(this::destroyWorldFolder);
    }
}
