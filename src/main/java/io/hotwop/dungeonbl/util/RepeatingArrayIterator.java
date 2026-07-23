package io.hotwop.dungeonbl.util;

import java.util.Iterator;

public final class RepeatingArrayIterator<T> implements Iterator<T>{
    public final T[] array;
    private int index=-1;

    public RepeatingArrayIterator(T[] array){
        if(array==null||array.length==0)throw new NullPointerException("array");
        this.array=array;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public T next() {
        index++;
        if(index==array.length)index=0;
        return array[index];
    }
}
