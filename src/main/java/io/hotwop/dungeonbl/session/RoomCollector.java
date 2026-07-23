package io.hotwop.dungeonbl.session;

import io.hotwop.dungeonbl.session.event.EventConsumer;
import io.hotwop.dungeonbl.session.event.RoomEvent;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.util.math.Vec3;
import io.hotwop.dungeonbl.util.task.DoneTaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RoomCollector{
    private final List<PlacementSuggestion> placements=new ArrayList<>();
    private final List<NextSuggestion> exits=new ArrayList<>();
    private final Map<RoomEvent, EventConsumer> events=new HashMap<>();

    public void addPlacement(Vec3 pos, SideRotation rotation){
        placements.add(new PlacementSuggestion(pos,rotation));
    }

    public void addExit(Vec3 pos, SideRotation rotation, DoneTaskResult<RoomSession> task){
        exits.add(new NextSuggestion(pos,rotation,task));
    }

    public void addEvent(RoomEvent event,EventConsumer consumer){
        events.put(event,consumer);
    }

    public Result done(){
        return new Result(
            List.copyOf(placements),
            List.copyOf(exits),
            Map.copyOf(events)
        );
    }

    public record Result(
        List<PlacementSuggestion> placements,
        List<NextSuggestion> exits,
        Map<RoomEvent,EventConsumer> events
    ){}

    public record NextSuggestion(
        Vec3 pos,
        SideRotation rotation,
        DoneTaskResult<RoomSession> task
    ){}

    public static record PlacementSuggestion(
        Vec3 pos,
        SideRotation rotation
    ){}
}
