package io.hotwop.dungeonbl;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.event.Level;

import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;

public interface DBlCore extends Plugin{
    void playerLog(Level level,String message);
    DBlLogger getDBlLogger();
    ExecutorService getExecutorService();

    static DBlCore getPlugin(){
        return (DBlCore)JavaPlugin.getProvidingPlugin(DBlCore.class);
    }
}