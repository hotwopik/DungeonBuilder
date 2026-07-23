package io.hotwop.dungeonbl.util.view;

import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@UnmodifiableView
public final class ListView<T> implements List<T>{
    private final List<T> delegate;
    public ListView(List<T> delegate) {
        this.delegate=delegate;
    }


    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public @NonNull Iterator<T> iterator() {
        return new IteratorView<>(delegate.iterator());
    }

    @Override
    public @NonNull Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public @NonNull <T1> T1[] toArray(@NonNull T1[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T get(int index) {
        return delegate.get(index);
    }

    @Override
    public T set(int index, T element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public @NonNull ListIterator<T> listIterator() {
        return new ListIteratorView<>(delegate.listIterator());
    }

    @Override
    public @NonNull ListIterator<T> listIterator(int index) {
        return new ListIteratorView<>(delegate.listIterator(index));
    }

    @Override
    public @NonNull List<T> subList(int fromIndex, int toIndex) {
        return new ListView<>(delegate.subList(fromIndex,toIndex));
    }
}
