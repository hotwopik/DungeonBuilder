package io.hotwop.dungeonbl.mark.place;

import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.util.math.SideRotation;

public record RotationData(SideRotation rotation) implements MarkPlaceData {
    public RotationData(MarkCodec.Info info){
        this(info.get("rotation", SideRotation.class));
    }
}
