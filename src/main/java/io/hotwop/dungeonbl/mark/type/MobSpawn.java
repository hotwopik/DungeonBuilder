package io.hotwop.dungeonbl.mark.type;

import dev.jorel.commandapi.arguments.IntegerArgument;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.mark.RotatableMark;
import io.hotwop.dungeonbl.mark.dungeon.*;
import io.hotwop.dungeonbl.mark.RoomTypePredicate;
import io.hotwop.dungeonbl.mark.codec.ArgumentContext;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.mark.codec.MarkCreator;
import io.hotwop.dungeonbl.mark.codec.MarkData;
import io.hotwop.dungeonbl.mark.place.MobSpawnData;
import io.hotwop.dungeonbl.mob.CrossMobList;
import io.hotwop.dungeonbl.session.DungeonBuildException;
import io.hotwop.dungeonbl.session.collector.InRoomCollector;
import io.hotwop.dungeonbl.session.event.RoomEvent;
import io.hotwop.dungeonbl.util.ArrowDrawer;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.session.RoomSession;
import io.hotwop.dungeonbl.mob.CrossMob;

import java.util.*;

public final class MobSpawn extends DungeonMark<MobSpawn, MobSpawnData> implements RotatableMark{
    private static final Particle.DustOptions particle=new Particle.DustOptions(Color.GREEN,1);

    @SuppressWarnings("deprecation")
    public static final MarkCodec<MobSpawn> type=MarkCodec.createCodec(
        "mob_spawn",
        ItemType.ZOMBIE_SPAWN_EGG,
        ChatColor.GREEN+"Mob spawn",
        MobSpawn.class,
        new DungeonMarkData<>(
            RoomTypePredicate.ALL,
            MobSpawn::placing,
            MobSpawnData::new,
            MobSpawnData.class,
            InRoomCollector.class
        ),
        ArgumentContext.builder()
            .define("positive",int.class,int.class,(num,ctx)->num,name->new IntegerArgument(name,0))
            .build()
    );

    public MarkCodec<MobSpawn> markType() {
        return type;
    }

    private static final Random mobSelector=new Random();

    private @MarkData(saveOnly=true) SideRotation rotation;
    private final @MarkData(argument="positive",lore="§7Pool: §e%s",optional=true) int pool;

    private final ArrowDrawer arrowDrawer;

    @MarkCreator({"rotation","pool"})
    private MobSpawn(Location location, SideRotation rotation, int pool){
        super(location,MobSpawn.class);
        this.rotation=rotation;
        this.pool=pool;

        this.arrowDrawer=new ArrowDrawer(location,particle,rotation);
    }

    @Override
    public void rotateUp() {
        rotation=rotation.addOne();
        arrowDrawer.rotate(rotation);
    }

    @Override
    public void rotateDown() {
        rotation=rotation.subOne();
        arrowDrawer.rotate(rotation);
    }

    @Override
    public SideRotation rotation() {
        return rotation;
    }

    protected void tick(){
        List<Player> players=getPlayersCanSee(20);
        if(players.isEmpty())return;
        arrowDrawer.draw(players);
    }

    private static void placing(MobSpawnData data, Vec3 pos, RoomSession.Builder room, InRoomCollector collector){
        CrossMobList pools=room.room.mobs();
        if(pools==null||data.pool()>=pools.list.size()){
            DungeonBl.logger
                .warn(
                    "Error to generate mob spawn, pool index out of bounds, position: {}, {} {}",
                    pos, room.builder.dungeonName, room.room.building()
                );
            throw new DungeonBuildException("Mob Spawn Error");
        }

        List<CrossMob> mobs=pools.list.get(data.pool());
        if(mobs.isEmpty())return;

        int index=mobSelector.nextInt(mobs.size());
        CrossMob mob=mobs.get(index);

        collector.addEvent(RoomEvent.spawn,(loc,session)->{
            loc.setYaw(data.rotation().add(session.rotation).degrees);
            loc.add(.5,0,.5);

            Entity entity=mob.spawn(loc);
            if(entity!=null)session.hookMob(entity);
        });
    }

}
