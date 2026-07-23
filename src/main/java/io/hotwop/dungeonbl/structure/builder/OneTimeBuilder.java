package io.hotwop.dungeonbl.structure.builder;

import io.hotwop.dungeonbl.structure.container.BlockDat;
import io.hotwop.dungeonbl.structure.container.EntityData;
import io.hotwop.dungeonbl.util.math.Vec3;
import io.hotwop.dungeonbl.util.math.Vec3d;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.function.Consumer;

public final class OneTimeBuilder implements WorldBuilder{
    public final World world;

    public OneTimeBuilder(World world){
        this.world=world;
    }

    @Override
    public World world() {
        return world;
    }

    @Override
    public void place(Vec3 pos, BlockDat data) {
        data.place(world,pos);
    }

    @Override
    public void spawn(Vec3d pos, EntityData data, Consumer<Entity> preload) {
        data.spawn(pos.toLocation(world),preload);
    }

    @Override
    public void tick(){}
}
