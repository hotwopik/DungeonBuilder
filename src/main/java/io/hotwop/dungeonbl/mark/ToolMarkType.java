package io.hotwop.dungeonbl.mark;

import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.Location;
import org.bukkit.inventory.ItemType;

import java.util.function.Consumer;

public final class ToolMarkType extends MarkType{
    public final Consumer<Location> tool;

    public ToolMarkType(String name,Consumer<Location> tool,ItemType item,String displayName){
        super(name,item,displayName);
        this.tool=tool;
    }

    @Override
    public void place(Location location,ReadableNBT data){
        tool.accept(location);
    }

    @Override
    public CommandAPICommand createCommand() {
        return new CommandAPICommand(name)
            .executesPlayer((player,args)->{
                giveCommand(player,args);
            });
    }
}
