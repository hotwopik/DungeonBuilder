package io.hotwop.dungeonbl.io;

import io.hotwop.dungeonbl.util.view.SetView;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public abstract class DBlRegistry<T>{
    public static Predicate<String> validId= Pattern.compile("[a-z0-9/_.-]+").asMatchPredicate();

    public final String name;

    protected final Map<String, T> map = new ConcurrentHashMap<>();
    protected final Set<String> removed = new HashSet<>();

    protected final Map<String,Set<RegistryListener<T>>> listeners=new ConcurrentHashMap<>();
    protected final Set<String> idsView=new SetView<>(map.keySet());

    private final BusyData busyData = new BusyData();
    private final Object busySignal = new Object();
    private final Object registeringSignal = new Object();

    protected DBlRegistry(String name){
        this.name=name;
    }

    public abstract void save();
    public abstract void load();

    public abstract void save(String id);

    public @Nullable T get(String id) {
        return map.get(id);
    }

    public Set<String> idSet(){
        return idsView;
    }

    public Map<String, T> map(){
        return Map.copyOf(map);
    }

    public final void add(String id, T value) {
        if (!validId.test(id)) throw new IllegalArgumentException(id + " is invalid id for " + name);
        waitBusy();

        map.put(id, value);
        if (removed.contains(id)) {
            synchronized (removed) {
                removed.remove(id);
            }
        }
        onAdd(id,value);
        listenersUpdate(id,value);

        decrementRegistration();
    }

    protected void onAdd(String id, T value){

    }

    public final void remove(String id) {
        waitBusy();

        if (map.containsKey(id)) {
            map.remove(id);
            synchronized (removed) {
                removed.add(id);
            }
            onRemove(id);
            listenersRemove(id);
        }

        decrementRegistration();
    }

    protected void onRemove(String id){

    }

    public final void addListener(String id, RegistryListener<T> listener){
        listeners.computeIfAbsent(id,k->new HashSet<>())
            .add(listener);

        T value=get(id);
        if(value!=null)listener.update(value);
    }

    public final void removeListener(String id, RegistryListener<T> listener){
        Set<RegistryListener<T>> set=listeners.get(id);
        if(set!=null)set.remove(listener);
    }

    protected final void listenersUpdate(String id,T value){
        Set<RegistryListener<T>> set=listeners.get(id);
        if(set!=null)set.forEach(ls->ls.update(value));
    }

    protected final void listenersRemove(String id){
        Set<RegistryListener<T>> set=listeners.get(id);
        if(set!=null)set.forEach(RegistryListener::remove);
    }

    protected final void decrementRegistration(){
        synchronized(busyData){
            busyData.registering--;
        }
        synchronized(registeringSignal){
            registeringSignal.notifyAll();
        }
    }

    protected final void waitBusy(){
        while(true){
            synchronized(busyData){
                if(!busyData.busy){
                    busyData.registering++;
                    break;
                }
            }

            synchronized (busySignal){
                try{
                    busySignal.wait(100);
                }catch(InterruptedException ignore){}
            }
        }
    }

    protected final void busyLock(){
        boolean isRegister;
        while(true){
            synchronized(busyData){
                if(!busyData.busy){
                    busyData.busy=true;
                    isRegister=busyData.registering!=0;
                    break;
                }
            }

            synchronized (busySignal){
                try{
                    busySignal.wait(100);
                }catch(InterruptedException ignore){}
            }
        }

        while(isRegister){
            synchronized(registeringSignal){
                try{
                    registeringSignal.wait(100);
                }catch(InterruptedException ignore){}
            }

            synchronized(busyData){
                isRegister=busyData.registering!=0;
            }
        }
    }

    protected final void busyUnlock(){
        synchronized(busyData){
            busyData.busy=false;
        }
        synchronized(busySignal){
            busySignal.notifyAll();
        }
    }

    private static final class BusyData{
        public volatile boolean busy=false;
        public volatile int registering=0;
    }

    public static final class IdentifyException extends Exception{
        public final Path file;

        public IdentifyException(Path file){
            this.file=file;
        }
    }
}
