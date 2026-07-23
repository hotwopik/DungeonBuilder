package io.hotwop.dungeonbl.util.task;

public final class AfterCounterTask{
    private int value;
    private final Runnable task;

    public AfterCounterTask(int value, Runnable task){
        this.value=value;
        this.task=task;
    }

    public void count(){
        value--;
        if(value==0)task.run();
    }
}
