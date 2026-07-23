package io.hotwop.dungeonbl.mark.type;

import dev.jorel.commandapi.arguments.IntegerArgument;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMark;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMarkData;
import io.hotwop.dungeonbl.mark.RoomTypePredicate;
import io.hotwop.dungeonbl.mark.codec.ArgumentContext;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.mark.codec.MarkCreator;
import io.hotwop.dungeonbl.mark.codec.MarkData;
import io.hotwop.dungeonbl.mark.place.PotionData;
import io.hotwop.dungeonbl.session.collector.InRoomCollector;
import io.hotwop.dungeonbl.session.event.RoomEvent;
import io.hotwop.dungeonbl.session.RoomSession;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

public final class EffectMark extends DungeonMark<EffectMark, PotionData>{
    private static final Particle.DustOptions particle=new Particle.DustOptions(Color.OLIVE,1);

    public static final MarkCodec<EffectMark> type=MarkCodec.createCodec(
        "effect",
        ItemType.GLOW_SQUID_SPAWN_EGG,
        ChatColor.DARK_AQUA+"Potion Effect",
        EffectMark.class,
        new DungeonMarkData<>(
            RoomTypePredicate.ALL,
            EffectMark::placing,
            PotionData::new,
            PotionData.class,
            InRoomCollector.class
        ),
        ArgumentContext.builder()
            .define("duration",int.class,int.class,(num,ctx)->num,name->new IntegerArgument(name,-1))
            .define("amplifier",int.class,int.class,(num,ctx)->num,name->new IntegerArgument(name,0,255))
            .build()
    );

    public MarkCodec<EffectMark> markType(){
        return type;
    }

    private static final Vector xVector=new Vector(1,0,0);
    private static final Vector zVector=new Vector(0,0,1);

    public final @MarkData(order=0,lore="§7Effect: §e%s") PotionEffectType effect;
    public final @MarkData(order=1,argument="amplifier",lore="§7Amplifier: §e%s") int amplifier;
    public final @MarkData(order=2,argument="duration",lore="§7Duration: §e%s ticks") int duration;

    private final Vector start1;
    private final Vector start2;

    @MarkCreator({"effect","duration","amplifier"})
    private EffectMark(Location location, PotionEffectType effect, int duration, int amplifier){
        super(location,EffectMark.class);
        this.effect=effect;
        this.duration=duration;
        this.amplifier=amplifier;

        start1=location.toVector().add(new Vector(.5,0,0));
        start2=location.toVector().add(new Vector(0,0,.5));
    }

    protected void tick(){
        List<Player> players=getPlayersCanSee(20);
        if(players.isEmpty())return;

        DBlUtil.drawParticleLine(particle,world,players,start2,xVector,0.25);
        DBlUtil.drawParticleLine(particle,world,players,start1,zVector,0.25);
    }

    private static void placing(PotionData data, Vec3 pos, RoomSession.Builder room, InRoomCollector collector){
        PotionEffect effect=new PotionEffect(data.effectType(),data.duration(),data.amplifier(),true,true);
        
        collector.addEvent(RoomEvent.activation,(location,session)->{
            location.add(.5,0,.5);
            session.dungeonSession()
                .addTickTask(()->location.getNearbyPlayers(1)
                    .forEach(player->player.addPotionEffect(effect))
                );
        });
    }

}
