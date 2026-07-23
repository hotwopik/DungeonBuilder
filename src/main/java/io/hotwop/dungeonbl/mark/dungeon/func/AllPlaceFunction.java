package io.hotwop.dungeonbl.mark.dungeon.func;

import io.hotwop.dungeonbl.mark.place.MarkPlaceData;
import io.hotwop.dungeonbl.session.RoomSession;
import io.hotwop.dungeonbl.session.collector.RoomCollector;
import io.hotwop.dungeonbl.util.math.Vec3;

@FunctionalInterface
public non-sealed interface AllPlaceFunction<P extends MarkPlaceData,C extends RoomCollector> extends PlaceFunction<P,C>{
    void place(P placeData, Vec3 pos, RoomSession.Builder room, C collector);
}
