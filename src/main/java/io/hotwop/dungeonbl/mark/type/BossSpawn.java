package io.hotwop.dungeonbl.mark.type;

import io.hotwop.dungeonbl.mark.codec.MarkData;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMark;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMarkData;
import io.hotwop.dungeonbl.mark.RoomTypePredicate;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.mark.codec.MarkCreator;
import io.hotwop.dungeonbl.mark.place.RotationData;
import io.hotwop.dungeonbl.session.collector.InRoomCollector;
import io.hotwop.dungeonbl.session.event.RoomEvent;
import io.hotwop.dungeonbl.util.ArrowDrawer;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.session.RoomSession;
import io.hotwop.dungeonbl.mark.RotatableMark;
import io.hotwop.dungeonbl.mob.Boss;
import io.hotwop.dungeonbl.mob.BossEntity;

import java.util.List;

public final class BossSpawn extends DungeonMark<BossSpawn, RotationData> implements RotatableMark{
    private static final Particle.DustOptions particle=new Particle.DustOptions(Color.YELLOW,1);

    @SuppressWarnings("deprecation")
    public static final MarkCodec<BossSpawn> type=MarkCodec.createCodec(
        "boss_spawn",
        ItemType.BLAZE_SPAWN_EGG,
        ChatColor.YELLOW+"Boss Spawn",
        BossSpawn.class,
        new DungeonMarkData<>(
            RoomTypePredicate.ALL,
            BossSpawn::placing,
            RotationData::new,
            RotationData.class,
            InRoomCollector.class
        )
    );
    public MarkCodec<BossSpawn> markType() {
        return type;
    }

    private @MarkData(saveOnly=true) SideRotation rotation;

    private final ArrowDrawer arrowDrawer;

    @MarkCreator({"rotation"})
    private BossSpawn(Location location, SideRotation rotation){
        super(location,BossSpawn.class);
        this.rotation=rotation;

        arrowDrawer=new ArrowDrawer(location,particle,rotation);
    }

    protected void tick(){
        List<Player> players=getPlayersCanSee(20);
        if(players.isEmpty())return;
        arrowDrawer.draw(players);
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

    private static void placing(RotationData data, Vec3 pos, RoomSession.Builder roomBuilder, InRoomCollector collector) {
        collector.addEvent(RoomEvent.activation, (location, room) -> {
            if (!room.bossIterator.hasNext())return;
            location.setYaw(data.rotation().add(room.rotation).degrees);
            location.add(.5, 0, .5);

            Boss boss = room.bossIterator.next();
            Entity entity = boss.type().spawn(boss.mob(), location);
            if (entity == null) return;

            BossBar bossbar;
            if (boss.color() != null) {
                bossbar = Bukkit.createBossBar(entity.getName(), boss.color(), BarStyle.SOLID);
                room.dungeonSession()
                    .players()
                    .forEach(bossbar::addPlayer);
            } else bossbar = null;

            LivingEntity living = (LivingEntity) entity;
            room.hookBoss(new BossEntity(living, bossbar, living.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
        });
    }
}