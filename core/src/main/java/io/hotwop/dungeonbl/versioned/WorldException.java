package io.hotwop.dungeonbl.versioned;

public class WorldException extends Exception{
    public WorldException(String message) {
        super(message);
    }
    public WorldException(String message,Throwable cause) {
        super(message,cause);
    }
}
