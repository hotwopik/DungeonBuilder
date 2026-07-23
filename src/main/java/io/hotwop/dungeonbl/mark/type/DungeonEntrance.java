package io.hotwop.dungeonbl.mark.type;

import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.TimeArgument;
import io.hotwop.dungeonbl.DBlCommand;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.mark.codec.ArgumentContext;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.mark.codec.MarkCreator;
import io.hotwop.dungeonbl.mark.codec.MarkData;
import io.hotwop.dungeonbl.session.DungeonSession;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import org.bukkit.util.Vector;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.mark.Mark;

import java.util.*;

public final class DungeonEntrance extends Mark<DungeonEntrance>{
    private static final Particle.DustOptions particle=new Particle.DustOptions(Color.BLUE,1);
    private static final Particle.DustOptions particleRadius=new Particle.DustOptions(Color.AQUA,.5f);

    @SuppressWarnings("deprecation")
    public static final MarkCodec<DungeonEntrance> type=MarkCodec.createCodec(
        "dungeon_entrance",
        ItemType.ALLAY_SPAWN_EGG,
        ChatColor.AQUA+"Dungeon Entrance",
        DungeonEntrance.class,
        ArgumentContext.builder()
            .define("positive",int.class,int.class,(data,ctx)->data,name->new IntegerArgument(name,0))
            .define("time",int.class,int.class,(data,ctx)->data, TimeArgument::new)
            .define("dungeon",String.class,String.class,(str,ctx)->str,name->DBlCommand
                .stringListArgument(name,"Dungeon",()->DungeonBl.registries().dungeons.idSet(),null)
            )
            .build()
    );

    public MarkCodec<DungeonEntrance> markType() {
        return type;
    }

    public final @MarkData(argument="positive",lore="§7Radius: §e%s") int radius;
    public final @MarkData(argument="time",lore="§7Time: §e%s ticks") int time;
    public final @MarkData(argument="positive",lore="§7Minimum player count: §e%s",optional=true) int min;
    public final @MarkData(argument="positive",lore="§7Maximum player count: §e%s",optional=true) int max;
    public final @MarkData(argument="dungeon",lore="§7Dungeon: §e%s") String dungeon;
    public final @MarkData(lore="§7Exit: §e%s") Location exit;

    private final List<EntranceTimer> timers=new ArrayList<>();
    private final Vector center;

    @MarkCreator({"radius","time","min","max","dungeon","exit"})
    private DungeonEntrance(Location location,int radius,int time,int min,int max,String dungeon,Location exit){
        super(location, DungeonEntrance.class);

        center=location.toVector().add(new Vector(.5,0,.5));
        this.radius = radius;
        this.time = time;
        this.min = min;
        this.max = max;
        this.dungeon = dungeon;
        this.exit = exit;
    }

    protected void tick(){
        particles();

        List<Player> players = new ArrayList<>(location.getNearbyPlayers(radius));
        players.removeIf(player->!player.hasPermission("dungeonbl.enter_dungeon"));

        timers.removeIf(tm->tm.update(players));
        if(!players.isEmpty()){
            if(timers.isEmpty()){
                while(!players.isEmpty()){
                    new EntranceTimer(this,players);
                }
            }else{
                players.removeIf(pl->{
                    for(EntranceTimer timer:timers){
                        if(timer.add(pl))return true;
                    }
                    return false;
                });
                if(!players.isEmpty()){
                    while(!players.isEmpty()){
                        new EntranceTimer(this,players);
                    }
                }
            }
        }
    }
    private static class EntranceTimer{
        public final DungeonEntrance parent;
        public final BossBar bossbar=Bukkit.createBossBar("Entering the Dungeon...", BarColor.BLUE, BarStyle.SEGMENTED_6);
        private final List<Player> players=new ArrayList<>();
        public int timer=0;

        public EntranceTimer(DungeonEntrance parent,List<Player> pls){
            this.parent=parent;
            bossbar.setProgress(0);

            if(!pls.isEmpty()){
                Player player=pls.getFirst();
                while(add(player)){
                    pls.remove(player);
                    if(pls.isEmpty())break;
                    player=pls.getFirst();
                }
            }
            parent.timers.add(this);
        }

        public boolean add(Player player){
            if(parent.max!=0&&players.size()==parent.max)return false;
            players.add(player);
            bossbar.addPlayer(player);
            return true;
        }

        public boolean update(List<Player> pls){
            players.removeIf(pl->{
                if(!pls.contains(pl)){
                    bossbar.removePlayer(pl);
                    return true;
                }
                return false;
            });
            if(players.isEmpty()){
                bossbar.removeAll();
                return true;
            }
            pls.removeAll(players);

            ++timer;
            bossbar.setProgress((double)timer/parent.time);

            if(timer==parent.time){
                bossbar.removeAll();
                DungeonSession.create(parent.dungeon,parent.location,parent.exit,players);
                return true;
            }
            return false;
        }
    }

    private void particles(){
        List<Player> players=getPlayersCanSee(20);
        if(players.isEmpty())return;

        world.spawnParticle(Particle.DUST,players,null,center.getX(),center.getY(),center.getZ(),1,0,0,0,0,particle,false);
        DBlUtil.drawCircle(particleRadius,world,players,center,radius);
    }

    protected void onUnload(){
        timers.forEach(tm->tm.bossbar.removeAll());
        timers.clear();
    }

    protected void onRemove(){
        timers.forEach(tm->tm.bossbar.removeAll());
        timers.clear();
    }
}
