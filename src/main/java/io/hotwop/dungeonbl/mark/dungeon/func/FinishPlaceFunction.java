package io.hotwop.dungeonbl.mark.dungeon.func;

import io.hotwop.dungeonbl.mark.place.MarkPlaceData;
import io.hotwop.dungeonbl.session.RoomSession;
import io.hotwop.dungeonbl.session.RoomCollector;
import io.hotwop.dungeonbl.util.math.Vec3;

@FunctionalInterface
public non-sealed interface FinishPlaceFunction<P extends MarkPlaceData> extends PlaceFunction<P>{
    boolean place(P placeData, Vec3 pos, RoomSession.Builder room, RoomCollector collector);
}
