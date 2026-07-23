package io.hotwop.dungeonbl.mark.codec;

public class SerializeException extends Exception{
    public SerializeException(){
        super("Error to serialize MarkInfo");
    }
    public SerializeException(String message){
        super("Error to serialize MarkInfo: "+message);
    }
    public SerializeException(String message,Throwable cause){
        super("Error to serialize MarkInfo: "+message,cause);
    }
}
