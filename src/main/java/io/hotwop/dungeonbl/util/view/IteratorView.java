package io.hotwop.dungeonbl.util.view;

import java.util.Iterator;

public final class IteratorView<T> implements Iterator<T>{
    private final Iterator<T> delegate;
    public IteratorView(Iterator<T> delegate){
        this.delegate = delegate;
    }


    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public T next() {
        return delegate.next();
    }
}
