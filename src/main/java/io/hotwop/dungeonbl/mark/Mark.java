package io.hotwop.dungeonbl.mark;

import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.util.math.ImmutableLocation;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class Mark<T extends Mark<T>>{
    protected Mark(Location location,Class<T> clazz){
        if(!clazz.equals(getClass()))throw new RuntimeException();

        this.location=new ImmutableLocation(location);
        world=location.getWorld();

        MarkStorage.add(location,this);
    }

    public abstract MarkCodec<T> markType();
    protected abstract void tick();

    private int tickTask;
    private boolean ticking;
    public final Location location;
    public final World world;

    public final void startTicking(){
        if(ticking)return;
        tickTask=Bukkit.getScheduler().scheduleSyncRepeatingTask(DungeonBl.plugin(),this::tick,1,1);
        if(tickTask==-1)return;
        ticking =true;
        onLoad();
    }

    public final void stopTicking(){
        if(!ticking)return;
        ticking =false;
        Bukkit.getScheduler().cancelTask(tickTask);
        tickTask=-1;
        onUnload();
    }

    public final void remove(){
        stopTicking();
        MarkStorage.remove(location);
        onRemove();
    }

    protected void onLoad(){}
    protected void onUnload(){}
    protected void onRemove(){}

    public final int tickTask(){
        return tickTask;
    }
    public final boolean isTicking(){
        return ticking;
    }

    public final void writeData(ReadWriteNBT data){
        markType().writeData(this,data);
    }

    protected final List<Player> getPlayersCanSee(int radius){
        List<Player> players=new ArrayList<>(location.getNearbyPlayers(radius));
        players.removeIf(player->!player.hasPermission("dungeonbl.admin_vision"));
        return players;
    }
}