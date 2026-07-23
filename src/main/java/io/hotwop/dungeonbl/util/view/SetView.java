package io.hotwop.dungeonbl.util.view;

import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

@UnmodifiableView
public final class SetView<T> implements Set<T>{
    private final Set<T> delegate;

    public SetView(Set<T> delegate) {
        this.delegate = delegate;
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
    public @NonNull Object @NonNull [] toArray() {
        return delegate.toArray();
    }

    @Override
    public @NonNull <T1> T1 @NonNull [] toArray(@NonNull T1 @NonNull [] a) {
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
    public boolean retainAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
