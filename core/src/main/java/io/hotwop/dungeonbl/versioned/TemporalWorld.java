package io.hotwop.dungeonbl.versioned;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;

public abstract class TemporalWorld{
    public final String worldName;
    public final NamespacedKey worldId;

    public final Path worldContainer;
    public final Path worldFolder;

    public final World world;

    private boolean loadFinished=false;

    protected TemporalWorld(String worldName, NamespacedKey worldId, Path worldContainer, Path worldFolder, World world) {
        this.worldName = worldName;
        this.worldId = worldId;
        this.worldContainer = worldContainer;
        this.worldFolder = worldFolder;
        this.world = world;
    }

    public abstract <T> void setGameRule(GameRule<T> gameRule,T value);
    public abstract void destroy();

    public final void finishLoad(){
        if(loadFinished)return;
        var pm=Bukkit.getPluginManager();
        pm.callEvent(new WorldInitEvent(world));
        pm.callEvent(new WorldLoadEvent(world));
        loadFinished=true;
    }

    public boolean isLoadFinished(){
        return loadFinished;
    }

    public final void destroyWorldFolder(){
        int i = 1;

        while (i <= 5) {
            try {
                Files.walkFileTree(worldFolder, new SimpleFileVisitor<>() {
                    @NotNull
                    public FileVisitResult visitFile(Path path, @NotNull BasicFileAttributes basicfileattributes) throws IOException {
                        Files.delete(path);
                        return FileVisitResult.CONTINUE;
                    }

                    @NotNull
                    public FileVisitResult postVisitDirectory(Path path, @Nullable IOException ioexception) throws IOException {
                        if (ioexception != null) {
                            throw ioexception;
                        } else {
                            Files.deleteIfExists(path);
                            return FileVisitResult.CONTINUE;
                        }
                    }
                });
                break;
            } catch (IOException ioexception) {
                if (i == 5) break;
                try {
                    Thread.sleep(500L);
                }catch(InterruptedException ignore){}
                ++i;
            }
        }
    }
}
