package io.hotwop.dungeonbl;

import de.tr7zw.changeme.nbtapi.NBT;
import dev.jorel.commandapi.CommandAPI;
import io.hotwop.dungeonbl.config.Config;
import io.hotwop.dungeonbl.config.LogLevel;
import io.hotwop.dungeonbl.integrations.PlaceholderAPIIntegration;
import io.hotwop.dungeonbl.integrations.VaultIntegration;
import io.hotwop.dungeonbl.io.ConfigStorage;
import io.hotwop.dungeonbl.io.PlayerDataStorage;
import io.hotwop.dungeonbl.io.DBlRegistries;
import io.hotwop.dungeonbl.mark.MarkStorage;
import io.hotwop.dungeonbl.mark.type.*;
import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.platform.BukkitPlatform;
import io.hotwop.dungeonbl.platform.PaperPlatform;
import io.hotwop.dungeonbl.util.RepeatingAsyncTask;
import io.hotwop.dungeonbl.versioned.VersionFoundException;
import io.hotwop.dungeonbl.versioned.VersionedInterface;
import org.bukkit.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.hotwop.dungeonbl.session.DungeonSession;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class DungeonBl extends JavaPlugin implements DBlCore{
    public static final String namespace="dungeonbl";

    public static final DBlLogger logger=new DBlLogger();
    public static final ExecutorService executorService=Executors.newCachedThreadPool();
    public static final BukkitScheduler scheduler= Bukkit.getScheduler();

    private static VersionedInterface versioned;
    private static DBlPlatformed platformed;
    private static DungeonBl plugin;

    private static DBlRegistries registries;
    private static PlayerDataStorage playerDataStorage;
    private static ConfigStorage<Config> config;

    private static VaultIntegration vaultIntegration;

    private static RepeatingAsyncTask saveTask;

    protected static boolean loadingEnded=false;

    public DungeonBl(){
    }

    @Override
    public void onLoad(){
        plugin=this;
        Server server=getServer();

        platformed=new PaperPlatform();

        try{
            versioned=DBlVersioned.getVersionedInterface();
        }catch(VersionFoundException e){
            logger.error("Failed to load versioned DungeonBuilder",e);
            server.getPluginManager().disablePlugin(this);
            return;
        }

        Path dataPath=getDataPath();
        dataPath.toFile().mkdirs();
        registries=new DBlRegistries(dataPath);
        playerDataStorage=new PlayerDataStorage(dataPath.resolve("players.dat"));
        config=new ConfigStorage<>(
            "Config",
            "config.yml",
            dataPath,
            Config.serializers,
            Config.class,
            Config::new
        );

        ButtonPrimitives.noneLore.getClass();
    }

    @Override
    public void onEnable(){
        Server server=getServer();
        PluginManager pluginManager=server.getPluginManager();

        CommandAPI.onEnable();
        if(!NBT.preloadApi()){
            logger.error("Error to load NBT API");
            pluginManager.disablePlugin(this);
            return;
        }

        if(pluginManager.isPluginEnabled("PlaceholderAPI")){
            new PlaceholderAPIIntegration(this).register();
        }
        if(pluginManager.isPluginEnabled("Vault")){
            vaultIntegration=new VaultIntegration();
        }else vaultIntegration=null;

        logger.info("Loading..");

        config.load();
        registries.loadAll();
        playerDataStorage.load();

        server.getScheduler().runTask(this,()->{
            MarkStorage.loadAll();
            loadingEnded=true;
        });

        logger.info("Loaded!");

        pluginManager.registerEvents(new DBlEventListener(),this);

        int markSaveDelay=config.get().saveTimer();
        if(markSaveDelay>0) saveTask =new RepeatingAsyncTask(
            "DungeonBuilder Save Task",
            Duration.ofMinutes(markSaveDelay),
            DungeonBl::runSave
        ).run();
        else saveTask =null;
    }

    @Override
    public void onDisable(){
        if(!loadingEnded){
            executorService.shutdownNow();
            return;
        }

        logger.info("Shutdown all..");
        // Teleport can't be async at shutdown
        platformed=new BukkitPlatform();

        if(saveTask!=null)saveTask.shutdown();
        DungeonSession.shutdown();

        logger.info("Saving..");

        MarkStorage.saveAll();
        playerDataStorage.save();

        logger.info("All saved!");

        executorService.shutdown();

        try{
            executorService.awaitTermination(15L, TimeUnit.SECONDS);
        }catch(InterruptedException ignore){}

        executorService.shutdownNow();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void playerLog(Level level, String message) {
        if(!loadingEnded)return;
        if(LogLevel.getLevelValue(level)<config.get().logLevel().value)return;

        ChatColor color=switch(level){
            case INFO->ChatColor.BLUE;
            case WARN->ChatColor.YELLOW;
            case ERROR->ChatColor.RED;
            case TRACE->ChatColor.GREEN;
            case DEBUG->ChatColor.LIGHT_PURPLE;
        };

        String mess="["+ChatColor.GOLD+"DungeonBuilder"+" "+
            color+level+ChatColor.RESET+"] "+message;

        Bukkit.getOnlinePlayers().stream()
            .filter(pl->pl.hasPermission("dungeonbl.log"))
            .forEach(pl->pl.sendMessage(mess));
    }

    public static boolean isDungeonBlWorld(World world){
        PersistentDataContainer data=world.getPersistentDataContainer();
        Byte dungeonWorldBit=data.get(DungeonSession.dungeonWorldField, PersistentDataType.BYTE);
        if(dungeonWorldBit!=null&&dungeonWorldBit==(byte)1)return true;

        return false;
    }

    public static void loadConfig(){
        Config old=config.get();
        config.load();

        int newDelay=config.get().saveTimer();
        if(old.saveTimer()!=newDelay){
            if(saveTask!=null)saveTask.shutdown();

            if(newDelay>0)new RepeatingAsyncTask(
                "DungeonBuilder Save Task",
                Duration.ofMinutes(newDelay),
                DungeonBl::runSave
            ).run();
            else saveTask=null;
        }
    }

    private static void runSave(){
        playerDataStorage.save();
        MarkStorage.saveAll();
    }

    @Override
    public DBlLogger getDBlLogger() {
        return logger;
    }
    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    public static DungeonBl plugin(){
        return plugin;
    }
    public static Config config(){
        return config.get();
    }
    public static VersionedInterface versioned(){
        return versioned;
    }
    public static DBlPlatformed platformed(){
        return platformed;
    }

    public static DBlRegistries registries(){
        return registries;
    }
    public static PlayerDataStorage playerDataStorage(){
        return playerDataStorage;
    }

    public static @Nullable VaultIntegration vaultIntegration(){
        return vaultIntegration;
    }
}
