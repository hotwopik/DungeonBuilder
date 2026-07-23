package io.hotwop.dungeonbl.mark.type;

import io.hotwop.dungeonbl.mark.Mark;
import io.hotwop.dungeonbl.mark.RotatableMark;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.mark.codec.MarkCreator;
import io.hotwop.dungeonbl.mark.codec.MarkData;
import io.hotwop.dungeonbl.util.ArrowDrawer;
import io.hotwop.dungeonbl.util.math.SideRotation;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;

import java.util.*;

public final class BuildingOrigin extends Mark<BuildingOrigin> implements RotatableMark{
    private static final Particle.DustOptions particle=new Particle.DustOptions(Color.fromRGB(255,255,150),1);

    @SuppressWarnings("deprecation")
    public static final MarkCodec<BuildingOrigin> type=MarkCodec.createCodec(
        "building_origin",
        ItemType.OCELOT_SPAWN_EGG,
        ChatColor.YELLOW+"Decoration Origin",
        BuildingOrigin.class
    );

    public MarkCodec<BuildingOrigin> markType() {
        return type;
    }

    private @MarkData(saveOnly=true) SideRotation rotation;

    private final ArrowDrawer arrowDrawer;

    @MarkCreator({"rotation"})
    private BuildingOrigin(Location location, SideRotation rotation){
        super(location, BuildingOrigin.class);
        this.rotation=rotation;

        arrowDrawer=new ArrowDrawer(location,particle,rotation);
    }

    @Override
    public void rotateUp() {
        rotation=rotation.addOne();
        arrowDrawer.rotate(rotation);
    }

    @Override
    public void rotateDown() {
        rotation=rotation.subOne();
        arrowDrawer.rotate(rotation);
    }

    @Override
    public SideRotation rotation() {
        return rotation;
    }

    protected void tick(){
        List<Player> players=getPlayersCanSee(20);
        if(players.isEmpty())return;
        arrowDrawer.draw(players);
    }
}
