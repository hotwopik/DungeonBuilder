package io.hotwop.dungeonbl.config;

import org.slf4j.event.Level;

public enum LogLevel{
    INFO(0),
    WARN(1),
    ERROR(2),
    OFF(3);

    public final int value;
    LogLevel(int value) {
        this.value=value;
    }

    public static int getLevelValue(Level level){
        return switch(level){
            case ERROR, TRACE -> 2;
            case WARN -> 1;
            case INFO, DEBUG -> 0;
        };
    }
}
