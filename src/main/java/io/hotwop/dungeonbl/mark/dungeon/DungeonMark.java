package io.hotwop.dungeonbl.mark.dungeon;

import io.hotwop.dungeonbl.mark.Mark;
import io.hotwop.dungeonbl.mark.place.MarkPlaceData;
import org.bukkit.Location;

public abstract class DungeonMark<T extends DungeonMark<T,P>,P extends MarkPlaceData> extends Mark<T>{
    protected DungeonMark(Location location, Class<T> clazz) {
        super(location,clazz);
    }
}