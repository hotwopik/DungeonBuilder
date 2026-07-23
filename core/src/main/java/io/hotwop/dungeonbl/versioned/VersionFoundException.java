package io.hotwop.dungeonbl.versioned;

public class VersionFoundException extends Exception{
    public VersionFoundException(String message) {
        super(message);
    }

    public VersionFoundException(String message,Throwable cause) {
        super(message,cause);
    }
}
