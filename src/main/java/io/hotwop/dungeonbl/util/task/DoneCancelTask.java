package io.hotwop.dungeonbl.util.task;

public class DoneCancelTask{
    private boolean canceled=false;
    private final Runnable onCancel;
    private boolean done=false;
    private final Runnable onDone;

    public DoneCancelTask(){
        onCancel=null;
        onDone=null;
    }
    public DoneCancelTask(Runnable onCancel,Runnable onDone){
        this.onCancel=onCancel;
        this.onDone=onDone;
    }

    public void cancel(){
        if(canceled)return;
        canceled=true;
        if(onCancel!=null)onCancel.run();
    }
    public boolean isCanceled(){
        return canceled;
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
