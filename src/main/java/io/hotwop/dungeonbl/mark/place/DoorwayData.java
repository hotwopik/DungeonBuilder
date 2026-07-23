package io.hotwop.dungeonbl.mark.place;

import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.util.math.SideRotation;

public record DoorwayData(
    SideRotation rotation,
    String doorway
) implements MarkPlaceData {
    public DoorwayData(MarkCodec.Info info){
        this(
            info.get("rotation", SideRotation.class),
            info.get("doorway",String.class)
        );
    }
}
