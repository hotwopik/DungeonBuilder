package io.hotwop.dungeonbl.session.collector;

import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.util.math.Vec3;

public record PlacementSuggestion(
    Vec3 pos,
    SideRotation rotation
){}