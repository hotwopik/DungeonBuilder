package io.hotwop.dungeonbl.session;

public final class Signal{
    private int value=0;

    public void trigger(int value){
        if(value<1)throw new IllegalArgumentException("Unallowed value");
        this.value+=value;
    }

    public int reset(){
        int out=value;
        value=0;
        return out;
    }
}
