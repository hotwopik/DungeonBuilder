package io.hotwop.dungeonbl.util;

import java.time.Duration;

public final class RepeatingAsyncTask{
    private volatile boolean running=false;
    private volatile boolean shutdown=false;

    public final Duration duration;
    public final Runnable task;
    private final Thread taskThread;

    public RepeatingAsyncTask(String name,Duration duration,Runnable task){
        this.duration=duration;
        this.task=task;

        taskThread=new Thread(this::runTask,name);
        taskThread.setDaemon(true);
    }

    public RepeatingAsyncTask run(){
        if(running||shutdown)return this;
        taskThread.start();
        running=true;
        return this;
    }

    public void shutdown(){
        if(!running||shutdown)return;
        shutdown=true;

        taskThread.interrupt();
        try{
            taskThread.join();
        }catch(InterruptedException ignore){}
    }

    public boolean isRunning(){
        return running;
    }

    public boolean isShutdown(){
        return shutdown;
    }

    private void runTask(){
        while(true){
            try{
                Thread.sleep(duration);
            }catch(InterruptedException ignore){}

            if(shutdown)return;
            task.run();
        }
    }
}
