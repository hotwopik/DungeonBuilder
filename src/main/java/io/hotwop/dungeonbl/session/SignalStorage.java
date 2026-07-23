package io.hotwop.dungeonbl.session;

import java.util.HashMap;
import java.util.Map;

public final class SignalStorage{
    private final Map<String,Signal> map=new HashMap<>();

    public Signal get(String id){
        return map.computeIfAbsent(id,k->new Signal());
    }

    public void resetAll(){
        map.forEach((id,s)->s.reset());
    }
}
