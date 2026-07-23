package io.hotwop.dungeonbl.mark.dungeon.func;

import io.hotwop.dungeonbl.mark.place.MarkPlaceData;

public sealed interface PlaceFunction<P extends MarkPlaceData> permits AllPlaceFunction, FinishPlaceFunction{}