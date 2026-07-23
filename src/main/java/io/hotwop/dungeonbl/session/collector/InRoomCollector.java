package io.hotwop.dungeonbl.session.collector;

import io.hotwop.dungeonbl.session.event.EventConsumer;
import io.hotwop.dungeonbl.session.event.RoomEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InRoomCollector implements RoomCollector{
    private final Map<RoomEvent,EventConsumer> events=new HashMap<>();

    public void addEvent(RoomEvent event,EventConsumer consumer){
        events.put(event,consumer);
    }

    public Map<RoomEvent,EventConsumer> done(){
        return Map.copyOf(events);
    }
}
