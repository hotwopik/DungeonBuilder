package io.hotwop.dungeonbl.mark.type;

import io.hotwop.dungeonbl.mark.RotatableMark;
import io.hotwop.dungeonbl.mark.codec.MarkData;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMark;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMarkData;
import io.hotwop.dungeonbl.mark.RoomTypePredicate;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.mark.codec.MarkCreator;
import io.hotwop.dungeonbl.mark.place.RotationData;
import io.hotwop.dungeonbl.session.collector.InRoomCollector;
import io.hotwop.dungeonbl.session.event.RoomEvent;
import io.hotwop.dungeonbl.util.ArrowDrawer;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.session.RoomSession;

import java.util.List;

public final class PlayerSpawn extends DungeonMark<PlayerSpawn, RotationData> implements RotatableMark {
    private static final Particle.DustOptions particle=new Particle.DustOptions(Color.fromRGB(50,180,0),1);

    public static final MarkCodec<PlayerSpawn> type=MarkCodec.createCodec(
        "player_spawn",
        ItemType.PARROT_SPAWN_EGG,
        ChatColor.GREEN+"Player Spawn",
        PlayerSpawn.class,
        new DungeonMarkData<>(
            RoomTypePredicate.ALL,
            PlayerSpawn::placing,
            RotationData::new,
            RotationData.class,
            InRoomCollector.class
        )
    );

    public MarkCodec<PlayerSpawn> markType() {
        return type;
    }

    private @MarkData(saveOnly=true) SideRotation rotation;

    private final ArrowDrawer arrowDrawer;

    @MarkCreator({"rotation"})
    private PlayerSpawn(Location location, SideRotation rotation){
        super(location, PlayerSpawn.class);
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

    private static boolean placing(RotationData data , Vec3 pos, RoomSession.Builder room, InRoomCollector collector){
        collector.addEvent(RoomEvent.activation,(loc,session)->{
            loc.add(.5,0,.5);
            loc.setYaw(data.rotation().add(session.rotation).degrees);

            session.dungeonSession()
                .updateSpawn(loc);
        });

        return true;
    }
}
