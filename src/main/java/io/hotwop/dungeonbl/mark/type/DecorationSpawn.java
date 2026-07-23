package io.hotwop.dungeonbl.mark.type;

import io.hotwop.dungeonbl.mark.codec.MarkData;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMark;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMarkData;
import io.hotwop.dungeonbl.mark.RoomTypePredicate;
import io.hotwop.dungeonbl.mark.RotatableMark;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.mark.codec.MarkCreator;
import io.hotwop.dungeonbl.mark.place.RotationData;
import io.hotwop.dungeonbl.session.collector.InRoomCollector;
import io.hotwop.dungeonbl.session.event.RoomEvent;
import io.hotwop.dungeonbl.structure.Decoration;
import io.hotwop.dungeonbl.util.ArrowDrawer;
import io.hotwop.dungeonbl.util.math.Vec3;
import io.hotwop.dungeonbl.util.math.SideRotation;
import org.bukkit.*;
import org.bukkit.entity.Player;
import io.hotwop.dungeonbl.session.RoomSession;
import org.bukkit.inventory.ItemType;

import java.util.*;

public final class DecorationSpawn extends DungeonMark<DecorationSpawn, RotationData> implements RotatableMark{
    private static final Particle.DustOptions particle=new Particle.DustOptions(Color.fromRGB(100,0,255),1);

    @SuppressWarnings("deprecation")
    public static final MarkCodec<DecorationSpawn> type=MarkCodec.createCodec(
        "decoration_spawn",
        ItemType.BREEZE_SPAWN_EGG,
        ChatColor.LIGHT_PURPLE+"Decoration spawn",
        DecorationSpawn.class,
        new DungeonMarkData<>(
            RoomTypePredicate.ALL,
            DecorationSpawn::placing,
            RotationData::new,
            RotationData.class,
            InRoomCollector.class
        )
    );

    public MarkCodec<DecorationSpawn> markType() {
        return type;
    }

    private static final Random decorationSelector=new Random();

    private @MarkData(saveOnly=true) SideRotation rotation;

    private final ArrowDrawer arrowDrawer;

    @MarkCreator({"rotation"})
    private DecorationSpawn(Location location, SideRotation rotation){
        super(location,DecorationSpawn.class);
        this.rotation=rotation;

        arrowDrawer=new ArrowDrawer(location,particle,rotation);
    }

    protected void tick(){
        List<Player> players=getPlayersCanSee(20);
        if(players.isEmpty())return;
        arrowDrawer.draw(players);
    }

    private static void placing(RotationData data, Vec3 pos, RoomSession.Builder roomBuilder, InRoomCollector collector){
        int size=roomBuilder.decorations.size();
        if(size==0)return;

        Decoration decoration=roomBuilder.decorations.get(decorationSelector.nextInt(size));
        collector.addEvent(RoomEvent.activation,(loc,session)->decoration
            .place(new Vec3(loc),data.rotation().add(session.rotation),session.dungeonSession().worldBuilder)
        );
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
}
