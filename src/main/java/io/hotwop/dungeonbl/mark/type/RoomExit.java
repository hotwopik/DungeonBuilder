package io.hotwop.dungeonbl.mark.type;

import io.hotwop.dungeonbl.DBlCommand;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.mark.RotatableMark;
import io.hotwop.dungeonbl.mark.place.DoorwayData;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMark;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMarkData;
import io.hotwop.dungeonbl.mark.RoomTypePredicate;
import io.hotwop.dungeonbl.mark.codec.ArgumentContext;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.mark.codec.MarkCreator;
import io.hotwop.dungeonbl.mark.codec.MarkData;
import io.hotwop.dungeonbl.session.DungeonBuildException;
import io.hotwop.dungeonbl.session.collector.OutRoomCollector;
import io.hotwop.dungeonbl.session.event.RoomEvent;
import io.hotwop.dungeonbl.util.task.DoneTaskResult;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import io.hotwop.dungeonbl.structure.Doorway;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.session.RoomSession;
import io.hotwop.dungeonbl.util.math.Vec3;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class RoomExit extends DungeonMark<RoomExit, DoorwayData> implements RotatableMark{
    public static final MarkCodec<RoomExit> type=MarkCodec.createCodec(
        "room_exit",
        ItemType.SPIDER_SPAWN_EGG,
        ChatColor.DARK_RED+"Room Exit",
        RoomExit.class,
        new DungeonMarkData<>(
            RoomTypePredicate.START_BASE,
            RoomExit::placing,
            DoorwayData::new,
            DoorwayData.class,
            OutRoomCollector.class
        ),
        ArgumentContext.builder()
            .define("doorway",String.class,String.class,(nam,ctx)->nam,name->DBlCommand
                .stringListArgument(name,"Doorway",()->DungeonBl.registries().doorways.idSet(),null)
            )
            .build()
    );

    public MarkCodec<RoomExit> markType() {
        return type;
    }

    private @MarkData(saveOnly=true) SideRotation rotation;
    public final @MarkData(argument="doorway",lore="§7Doorway: §e%s") String doorway;
    public final Doorway.Display display;

    private static final Particle.DustOptions lines=new Particle.DustOptions(Color.MAROON,1);
    private static final Particle.DustOptions arrow=new Particle.DustOptions(Color.RED,1);

    @MarkCreator({"doorway","rotation"})
    private RoomExit(Location location, String doorway, SideRotation rotation){
        super(location,RoomExit.class);
        this.doorway=doorway;
        this.rotation=rotation;

        display=new Doorway.Display(
            doorway,
            location,
            rotation,
            lines,arrow
        );
    }

    @Override
    public void rotateUp() {
        rotation=rotation.addOne();
        display.rotate(rotation);
    }

    @Override
    public void rotateDown() {
        rotation=rotation.subOne();
        display.rotate(rotation);
    }

    @Override
    public SideRotation rotation() {
        return rotation;
    }

    @Override
    protected void onRemove(){
        display.close();
    }

    @Override
    protected void tick(){
        List<Player> players=getPlayersCanSee(20);
        if(players.isEmpty())return;

        display.draw(players);
    }

    private static void placing(DoorwayData data, Vec3 pos, RoomSession.Builder room, OutRoomCollector collector){
        SideRotation rot=data.rotation();
        if(!(
            (pos.x()==0&&rot==SideRotation.R90)||
            (pos.x()==room.sizeX-1&&rot==SideRotation.R270)||
            (pos.z()==0&&rot==SideRotation.R180)||
            (pos.z()==room.sizeZ-1&&rot==SideRotation.R0)
        )){
            logger.warn(
                "Room Exit error, entrance not in room bounds or have incorrect direction, position {} direction {}, {} {}",
                pos,rot.degrees,room.builder.dungeonName,room.room.building()
            );
            throw new DungeonBuildException("Room Exit error");
        }

        AtomicReference<RoomSession> placer=new AtomicReference<>();

        Doorway doorway=DungeonBl.registries().doorways
            .get(data.doorway());

        if(doorway==null){
            logger.warn(
                "Room exit error, unknown doorway: {}, rotation: {}, {} {}",
                data.doorway(), rot, room.builder.dungeonName, room.room.building()
            );
            throw new DungeonBuildException("Room Exit error");
        }else collector.addEvent(RoomEvent.deactivation,(loc,session)->{
            RoomSession placed=placer.get();
            if(placed!=null){
                doorway.place(new Vec3(loc),session.rotation.add(rot),session.dungeonSession().worldBuilder);
                placed.run();
            }
        });

        Vec3 ps=pos.add(new Vec3(0,0,1).rotate(rot));
        DoneTaskResult<RoomSession> task=new DoneTaskResult<>(placer::set);

        collector.addPlacement(ps,rot,task);
    }
}
