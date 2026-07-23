package io.hotwop.dungeonbl.util;

import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.util.math.Vec3d;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public final class ArrowDrawer{
    public final World world;
    public final Vec3d center;
    public final Particle.DustOptions particle;

    private Vec3d arrowStart;
    private Vec3d arrow;

    public ArrowDrawer(Location loc, Particle.DustOptions particle, SideRotation rotation){
        world=loc.getWorld();
        this.particle=particle;
        center=new Vec3d(loc).add(0.5f,0f,0.5f);
        arrowStart=center.add(new Vec3d(0,0,-.5,rotation));
        arrow=new Vec3d(0,0,1,rotation);
    }

    public void rotate(SideRotation rotation){
        arrowStart=center.add(new Vec3d(0,0,-.5,rotation));
        arrow=new Vec3d(0,0,1,rotation);
    }

    public void draw(List<Player> players){
        DBlUtil.drawArrow(particle,world,players,arrowStart,arrow,0.25,0.65);
    }
}
