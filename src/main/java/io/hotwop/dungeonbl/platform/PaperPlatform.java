package io.hotwop.dungeonbl.platform;

import io.hotwop.dungeonbl.DBlPlatformed;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class PaperPlatform implements DBlPlatformed{
    @Override
    public void teleport(Player player, Location location, Runnable afterTeleport, Runnable onError) {
        player.teleportAsync(location).thenAccept(success->{
            if(success)afterTeleport.run();
            else onError.run();
        });
    }
}
