package io.hotwop.dungeonbl.mark.codec;

public class MarkSerializeException extends Exception{
    public MarkSerializeException(){
        super("Error to serialize MarkInfo");
    }
    public MarkSerializeException(String message){
        super("Error to serialize MarkInfo: "+message);
    }
    public MarkSerializeException(String message, Throwable cause){
        super("Error to serialize MarkInfo: "+message,cause);
    }
}
