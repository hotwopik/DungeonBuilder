package io.hotwop.dungeonbl.io;

public interface RegistryListener<T>{
    void update(T value);
    void remove();
}
