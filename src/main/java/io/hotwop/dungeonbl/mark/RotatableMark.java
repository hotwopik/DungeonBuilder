package io.hotwop.dungeonbl.mark;

import io.hotwop.dungeonbl.util.math.SideRotation;

public interface RotatableMark{
    void rotateUp();
    void rotateDown();

    SideRotation rotation();
}
