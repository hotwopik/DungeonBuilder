package io.hotwop.dungeonbl.structure;

public class StructureSerializeException extends Exception{
    public StructureSerializeException(String message) {
        super(message);
    }
    public StructureSerializeException(String message,Throwable cause) {
        super(message,cause);
    }

    public StructureSerializeException wrapListException(String name,int index){
        String message=name+" "+index+" "+getMessage();
        Throwable cause=getCause();

        if(cause==null){
            return new StructureSerializeException(message);
        }else{
            return new StructureSerializeException(message,cause);
        }
    }
}
