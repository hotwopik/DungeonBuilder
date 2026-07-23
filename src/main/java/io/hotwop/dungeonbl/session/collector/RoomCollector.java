package io.hotwop.dungeonbl.session.collector;

public sealed interface RoomCollector permits RoomBuildingCollector,InRoomCollector,OutRoomCollector{}