package io.hotwop.dungeonbl.util.task;

import java.util.function.Consumer;

public class DoneTaskResult<T>{
    public final Consumer<T> onDone;
    private boolean done=false;

    public DoneTaskResult(){
        onDone=null;
    }
    public DoneTaskResult(Consumer<T> onDone){
        this.onDone=onDone;
    }

    public void done(T result){
        if(done)return;
        done=true;
        if(onDone!=null)onDone.accept(result);
    }
    public boolean isDone() {
        return done;
    }
}