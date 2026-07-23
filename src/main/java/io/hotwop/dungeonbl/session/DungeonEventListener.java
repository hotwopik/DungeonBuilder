package io.hotwop.dungeonbl.session;

import io.hotwop.dungeonbl.DungeonBl;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class DungeonEventListener implements Listener{
    public final DungeonSession session;
    public final World world;

    public DungeonEventListener(DungeonSession session){
        this.session=session;
        world=session.world;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void playerRespawn(PlayerRespawnEvent e){
        Player player=e.getPlayer();
        if(e.getRespawnReason()!=PlayerRespawnEvent.RespawnReason.DEATH || !player.getLastDeathLocation().getWorld().equals(world))return;

        if(session.dungeon.deathLimit()==0){
            e.setRespawnLocation(session.spawnLocation);
            return;
        }

        int count=session.deathCount
            .compute(player,(pl,pr)->pr==null?1:pr+1);

        if(count>=session.dungeon.deathLimit()){
            session.players
                .remove(player);

            DungeonBl.playerDataStorage()
                .endDungeonOnDeath(e);
        }else{
            e.setRespawnLocation(session.spawnLocation);
        }
    }
}
