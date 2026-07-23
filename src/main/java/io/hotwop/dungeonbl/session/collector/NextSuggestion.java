package io.hotwop.dungeonbl.session.collector;

import io.hotwop.dungeonbl.session.RoomSession;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.util.math.Vec3;
import io.hotwop.dungeonbl.util.task.DoneTaskResult;

public record NextSuggestion(
    Vec3 pos,
    SideRotation rotation,
    DoneTaskResult<RoomSession> task
){}