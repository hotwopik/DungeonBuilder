package io.hotwop.dungeonbl.mark.type;

import io.hotwop.dungeonbl.DBlCommand;
import io.hotwop.dungeonbl.io.DBlRegistry;
import io.hotwop.dungeonbl.mark.codec.ArgumentContext;
import io.hotwop.dungeonbl.mark.codec.MarkData;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMark;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMarkData;
import io.hotwop.dungeonbl.mark.RoomTypePredicate;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.mark.codec.MarkCreator;
import io.hotwop.dungeonbl.mark.place.TriggerData;
import io.hotwop.dungeonbl.session.RoomCollector;
import io.hotwop.dungeonbl.session.event.RoomEvent;
import io.hotwop.dungeonbl.session.RoomSession;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.*;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;

import java.util.*;

public final class RedstoneTrigger extends DungeonMark<RedstoneTrigger, TriggerData>{
    private static final Particle.DustOptions particle=new Particle.DustOptions(Color.RED,2);

    public static final MarkCodec<RedstoneTrigger> type=MarkCodec.createCodec(
        "redstone_trigger",
        ItemType.STRIDER_SPAWN_EGG,
        ChatColor.RED+"Redstone Trigger",
        RedstoneTrigger.class,
        new DungeonMarkData<>(
            RoomTypePredicate.ALL,
            RedstoneTrigger::placing,
            TriggerData::new,
            TriggerData.class
        ),
        ArgumentContext.builder()
            .define("valid_id",String.class,String.class,(val,ctx)->val,DBlCommand::validIdArgument)
            .build()
    );

    public MarkCodec<RedstoneTrigger> markType() {
        return type;
    }

    public final @MarkData(argument="valid_id",lore="§7Signal: §e%s") String signal;
    public final @MarkData(optional=true,lore="§7Value: §e%s") int value;

    @MarkCreator({"signal","value"})
    private RedstoneTrigger(Location location, String signal, int value){
        super(location, RedstoneTrigger.class);
        this.signal = signal;
        this.value = value;
        if(!DBlRegistry.validId.test(signal))throw
            new RuntimeException("Invalid id");
    }

    protected void tick(){
        List<Player> players=getPlayersCanSee(20);
        if(players.isEmpty())return;

        world.spawnParticle(Particle.DUST,players,null,location.x()+0.5,location.y()+.5,location.z()+0.5,1,0,0,0,0,particle,false);
    }

    protected static void placing(TriggerData data, Vec3 pos, RoomSession.Builder room, RoomCollector collector){
        collector.addEvent(RoomEvent.tick,(loc,session)->{
            BlockData block=loc.getBlock().getBlockData();

            if(block instanceof Powerable pow&&pow.isPowered()){
                session.signals.get(data.signal())
                    .trigger(data.value());
            }else if(block instanceof AnaloguePowerable pow&&pow.getPower()!=0){
                session.signals.get(data.signal())
                    .trigger(data.value());
            }
        });
    }
}
