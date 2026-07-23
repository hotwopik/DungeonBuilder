package io.hotwop.dungeonbl.mark.dungeon.func;

import io.hotwop.dungeonbl.mark.place.MarkPlaceData;
import io.hotwop.dungeonbl.session.collector.RoomCollector;

public sealed interface PlaceFunction<P extends MarkPlaceData,C extends RoomCollector> permits AllPlaceFunction, FinishPlaceFunction{}