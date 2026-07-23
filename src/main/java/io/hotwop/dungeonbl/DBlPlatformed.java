package io.hotwop.dungeonbl;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface DBlPlatformed{
    void teleport(Player player, Location location, Runnable afterTeleport, Runnable onError);
}
