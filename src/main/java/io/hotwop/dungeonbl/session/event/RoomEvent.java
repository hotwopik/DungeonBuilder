package io.hotwop.dungeonbl.session.event;

import org.bukkit.ChatColor;

@SuppressWarnings("deprecation")
public enum RoomEvent{
    activation(ChatColor.GREEN+"Room Activation"),
    tick(ChatColor.YELLOW+"Tick Event"),
    deactivation(ChatColor.RED+"Room Deactivation"),

    spawn(ChatColor.BLUE+"Mob Spawn Event");

    public final String description;
    RoomEvent(String description){
        this.description=description;
    }
}
