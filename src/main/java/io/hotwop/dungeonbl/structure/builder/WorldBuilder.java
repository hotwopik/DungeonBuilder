package io.hotwop.dungeonbl.structure.builder;

import io.hotwop.dungeonbl.structure.container.BlockDat;
import io.hotwop.dungeonbl.structure.container.EntityData;
import io.hotwop.dungeonbl.util.math.Box3;
import io.hotwop.dungeonbl.util.math.Vec3;
import io.hotwop.dungeonbl.util.math.Vec3d;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.function.Consumer;

public interface WorldBuilder{
    World world();
    void place(Vec3 pos, BlockDat data);
    void spawn(Vec3d pos, EntityData data, Consumer<Entity> preload);
    void tick();
}
