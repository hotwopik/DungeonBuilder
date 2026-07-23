package io.hotwop.dungeonbl.util.view;

import java.util.ListIterator;

public final class ListIteratorView<T> implements ListIterator<T>{
    private final ListIterator<T> delegate;
    public ListIteratorView(ListIterator<T> delegate) {
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

    @Override
    public boolean hasPrevious() {
        return delegate.hasPrevious();
    }

    @Override
    public T previous() {
        return delegate.previous();
    }

    @Override
    public int nextIndex() {
        return delegate.nextIndex();
    }

    @Override
    public int previousIndex() {
        return delegate.previousIndex();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(T t) {
        throw new UnsupportedOperationException();
    }
}
