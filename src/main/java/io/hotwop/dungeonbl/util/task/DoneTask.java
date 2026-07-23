package io.hotwop.dungeonbl.util.task;

public class DoneTask {
    private boolean done=false;
    private final Runnable onDone;

    public DoneTask(){
        onDone=null;
    }
    public DoneTask(Runnable onDone){
        this.onDone=onDone;
    }

    public void done(){
        if(done)return;
        done=true;
        if(onDone!=null)onDone.run();
    }
    public boolean isDone() {
        return done;
    }
}
