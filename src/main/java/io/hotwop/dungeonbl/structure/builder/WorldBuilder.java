package io.hotwop.dungeonbl.structure.builder;

import io.hotwop.dungeonbl.structure.container.BlockDat;
import io.hotwop.dungeonbl.structure.container.EntityData;
import io.hotwop.dungeonbl.util.math.Box3;
import io.hotwop.dungeonbl.util.math.Vec3;
import io.hotwop.dungeonbl.util.math.Vec3d;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface WorldBuilder{
    World world();
    // Frame control
    void beginFrame(Runnable afterFrame);

    void finishFrame();

    // Building
    void place(Vec3 pos, BlockDat data);
    void spawn(
        Vec3d pos,
        EntityData data,
        @Nullable Vec3 sleepPos,
        @Nullable Vec3 blockLeash,
        Consumer<Entity> preload
    );

    void tick();
}
