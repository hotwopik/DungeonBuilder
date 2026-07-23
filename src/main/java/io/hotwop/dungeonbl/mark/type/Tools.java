package io.hotwop.dungeonbl.mark.type;

import io.hotwop.dungeonbl.mark.MarkStorage;
import io.hotwop.dungeonbl.mark.RotatableMark;
import io.hotwop.dungeonbl.mark.ToolMarkType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import io.hotwop.dungeonbl.mark.Mark;
import org.bukkit.inventory.ItemType;

@SuppressWarnings("deprecation")
public final class Tools{
    public static final ToolMarkType remover=new ToolMarkType(
        "remover",
        Tools::remover,
        ItemType.ENDERMAN_SPAWN_EGG,
        ChatColor.RED+"Remove"
    );
    public static final ToolMarkType rotator=new ToolMarkType(
        "rotator",
        Tools::rotator,
        ItemType.ENDER_DRAGON_SPAWN_EGG,
        ChatColor.DARK_BLUE+"Rotate"
    );

    public static void remover(Location location){
        Mark<?> mark= MarkStorage.getMark(location);
        if(mark==null)return;
        mark.remove();
    }
    public static void rotator(Location location){
        Mark<?> mark= MarkStorage.getMark(location);
        if(!(mark instanceof RotatableMark rot))return;
        rot.rotateUp();
    }
}
