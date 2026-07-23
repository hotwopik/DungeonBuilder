package io.hotwop.dungeonbl.util.task;

public class CancelTask{
    private boolean canceled=false;
    private final Runnable onCancel;

    public CancelTask(){
        onCancel=null;
    }
    public CancelTask(Runnable onCancel){
        this.onCancel=onCancel;
    }

    public void cancel(){
        if(canceled)return;
        canceled=true;
        if(onCancel!=null)onCancel.run();
    }
    public boolean isCanceled(){
        return canceled;
    }
}

