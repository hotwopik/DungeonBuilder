package io.hotwop.dungeonbl.mark.place;

import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.util.math.SideRotation;

public record MobSpawnData(
    SideRotation rotation,
    int pool
) implements MarkPlaceData {
    public MobSpawnData(MarkCodec.Info info) {
        this(
            info.get("rotation", SideRotation.class),
            info.get("pool", int.class)
        );
    }
}
