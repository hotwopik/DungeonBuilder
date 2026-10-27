package io.hotwop.dungeonbl.mark.type;

import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.mark.Mark;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.mark.codec.MarkCreator;
import io.hotwop.dungeonbl.mark.codec.MarkData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import org.bukkit.util.Vector;

import java.util.List;

public final class DungeonChest extends Mark<DungeonChest>{
    private static final Particle.DustOptions particle=new Particle.DustOptions(Color.fromRGB(128,128,0),1);

    @SuppressWarnings("deprecation")
    public static final MarkCodec<DungeonChest> type=MarkCodec.createCodec(
        "dungeon_chest",
        ItemType.CHEST,
        ChatColor.DARK_GRAY+"Dungeon Chest",
        DungeonChest.class
    );

    public MarkCodec<DungeonChest> markType() {
        return type;
    }

    private static final Vector xVector=new Vector(1,0,0);
    private static final Vector yVector=new Vector(0,1,0);
    private static final Vector zVector=new Vector(0,0,1);

    private static final List<Vector> xPoses=List.of(
        new Vector(0,0,0),
        zVector,
        yVector,
        zVector.clone().add(yVector)
    );
    private static final List<Vector> yPoses=List.of(
        new Vector(0,0,0),
        zVector,
        xVector,
        zVector.clone().add(xVector)
    );
    private static final List<Vector> zPoses=List.of(
        new Vector(0,0,0),
        xVector,
        yVector,
        xVector.clone().add(yVector)
    );

    public final @MarkData(argument="DungeonId",lore="§7Dungeon: §e%s") String dungeon;

    private final Vector origin;

    @MarkCreator({"dungeon"})
    private DungeonChest(Location location,String dungeon){
        super(location, DungeonChest.class);
        this.dungeon=dungeon;

        origin=location.toVector();
    }

    protected void tick(){
        List<Player> players=getPlayersCanSee(20);
        if(players.isEmpty())return;

        DBlUtil.drawMultipleLines(particle,world,players,origin,xVector,0.5,xPoses);
        DBlUtil.drawMultipleLines(particle,world,players,origin,yVector,0.5,yPoses);
        DBlUtil.drawMultipleLines(particle,world,players,origin,zVector,0.5,zPoses);
    }
}
