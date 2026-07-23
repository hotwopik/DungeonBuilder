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
import io.hotwop.dungeonbl.session.collector.RoomBuildingCollector;
import io.hotwop.dungeonbl.session.event.RoomEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import io.hotwop.dungeonbl.structure.Doorway;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.session.RoomSession;
import io.hotwop.dungeonbl.util.math.Vec3;

import java.util.*;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class RoomEntrance extends DungeonMark<RoomEntrance, DoorwayData> implements RotatableMark{
    public static final MarkCodec<RoomEntrance> type=MarkCodec.createCodec(
        "room_entrance",
        ItemType.SLIME_SPAWN_EGG,
        ChatColor.DARK_GREEN+"Room Entrance",
        RoomEntrance.class,
        new DungeonMarkData<>(
            RoomTypePredicate.BASE_END,
            RoomEntrance::placing,
            DoorwayData::new,
            DoorwayData.class,
            RoomBuildingCollector.class
        ),
        ArgumentContext.builder()
            .define("doorway",String.class,String.class,(nam,ctx)->nam,name->DBlCommand
                .stringListArgument(name,"Doorway",()-> DungeonBl.registries().doorways.idSet(),null)
            )
            .build()
    );

    public MarkCodec<RoomEntrance> markType() {
        return type;
    }

    private @MarkData(saveOnly=true) SideRotation rotation;
    public final @MarkData(argument="doorway",lore="§7Doorway: §e%s") String doorway;

    public final Doorway.Display display;

    private static final Particle.DustOptions lines =new Particle.DustOptions(Color.GREEN,1);
    private static final Particle.DustOptions arrow =new Particle.DustOptions(Color.LIME,1);

    @MarkCreator({"doorway","rotation"})
    private RoomEntrance(Location location, String doorway, SideRotation rotation){
        super(location,RoomEntrance.class);
        this.doorway=doorway;
        this.rotation=rotation;

        display =new Doorway.Display(doorway,location,rotation,lines,arrow);
    }

    protected void onRemove(){
        display.close();
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

    protected void tick(){
        List<Player> players=getPlayersCanSee(20);
        if(players.isEmpty())return;

        display.draw(players);
    }

    private static void placing(DoorwayData data, Vec3 pos, RoomSession.Builder room, RoomBuildingCollector collector){
        SideRotation rot=data.rotation();
        if(!(
            (pos.x()==0&&rot==SideRotation.R270)||
            (pos.x()==room.sizeX-1&&rot==SideRotation.R90)||
            (pos.z()==0&&rot==SideRotation.R0)||
            (pos.z()==room.sizeZ-1&&rot==SideRotation.R180))
        ){
            logger.warn(
                "Room Entrance error, entrance not in room bounds or have incorrect direction, position {} direction {}, {} {}",
                pos,rot.degrees,room.builder.dungeonName,room.room.building()
            );
            throw new DungeonBuildException("Room Entrance error");
        }

        Vec3 ps=new Vec3(-pos.x(),-pos.y(),-pos.z());
        collector.addPlacement(ps,rot);

        Doorway doorway=DungeonBl.registries().doorways
            .get(data.doorway());

        if(doorway==null){
            logger.warn(
                "Room entrance error, unknown doorway: {}, rotation: {}, {} {}",
                data.doorway(), rot, room.builder.dungeonName, room.room.building()
            );
            throw new DungeonBuildException("Room Entrance error");
        }

        collector.addEvent(RoomEvent.activation,(location,session)->doorway
            .place(new Vec3(location),session.rotation.add(rot),session.dungeonSession().worldBuilder)
        );
    }
}