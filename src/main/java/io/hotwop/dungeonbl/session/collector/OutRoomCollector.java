package io.hotwop.dungeonbl.session.collector;

import io.hotwop.dungeonbl.session.RoomSession;
import io.hotwop.dungeonbl.session.event.EventConsumer;
import io.hotwop.dungeonbl.session.event.RoomEvent;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.util.math.Vec3;
import io.hotwop.dungeonbl.util.task.DoneTaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OutRoomCollector implements RoomCollector{
    private final List<NextSuggestion> suggestions=new ArrayList<>();
    private final Map<RoomEvent,EventConsumer> events=new HashMap<>();

    public void addPlacement(Vec3 pos, SideRotation rotation, DoneTaskResult<RoomSession> task){
        this.suggestions.add(new NextSuggestion(pos,rotation,task));
    }

    public void addEvent(RoomEvent event,EventConsumer consumer){
        events.put(event,consumer);
    }

    public Result done(){
        return new Result(List.copyOf(suggestions),Map.copyOf(events));
    }

    public record Result(
        List<NextSuggestion> suggestions,
        Map<RoomEvent,EventConsumer> events
    ){}
}
