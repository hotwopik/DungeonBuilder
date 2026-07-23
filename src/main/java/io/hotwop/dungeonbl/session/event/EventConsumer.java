package io.hotwop.dungeonbl.session.event;

import io.hotwop.dungeonbl.session.RoomSession;
import org.bukkit.Location;

@FunctionalInterface
public interface EventConsumer{
    void consume(Location location,RoomSession session);
}
