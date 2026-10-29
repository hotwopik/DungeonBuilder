package io.hotwop.dungeonbl.menu.other;

import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.io.DBlRegistry;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.TextEditorMenu;
import io.hotwop.dungeonbl.structure.*;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class StructureSaveMenu extends AbstractSingleInventory {
    private static final ItemStack roomIcon=ItemStack.of(Material.BRICKS);
    private static final ItemStack doorwayIcon=ItemStack.of(Material.DARK_OAK_DOOR);
    private static final ItemStack decorationIcon=ItemStack.of(Material.DECORATED_POT);
    private static final ItemStack clearIcon=ItemStack.of(Material.RED_CONCRETE);

    static{
        ItemMeta roomMeta=roomIcon.getItemMeta();
        roomMeta.setItemName("Room Building");
        roomIcon.setItemMeta(roomMeta);

        ItemMeta doorwayMeta=doorwayIcon.getItemMeta();
        doorwayMeta.setItemName("Doorway");
        doorwayIcon.setItemMeta(doorwayMeta);

        ItemMeta decorationMeta=decorationIcon.getItemMeta();
        decorationMeta.setItemName("Decoration");
        decorationIcon.setItemMeta(decorationMeta);

        ItemMeta clearMeta=clearIcon.getItemMeta();
        clearMeta.setItemName("Clear Selection");
        clearIcon.setItemMeta(clearMeta);
    }

    public final World world;
    public final Vec3 first;
    public final Vec3 second;

    public StructureSaveMenu(Player player,World world,Vec3 first,Vec3 second) {
        super("Save Structure",3,player);
        if(!player.hasPermission("dungeonbl.wand"))throw new RuntimeException("Permission exception");

        this.world=world;
        this.first=first;
        this.second=second;

        inventory.setItem(11,roomIcon);
        inventory.setItem(13,doorwayIcon);
        inventory.setItem(15,decorationIcon);
        inventory.setItem(26,clearIcon);

        open();
    }

    @Override
    public void click(int slot, ClickType clickType, ItemStack cursorItem) {
        switch(slot){
            case 11->new TextEditorMenu(
                "Room Building name",
                null,
                Material.BRICKS,
                player,
                val->{
                    if(!DBlRegistry.validId.test(val)){
                        player.sendMessage(ChatColor.RED+"Provided name is invalid: "+val);
                        return;
                    }

                    RoomBuilding room=RoomBuilding.scan(world,first,second);
                    DungeonBl.registries().roomBuildings.add(val,room);
                    player.sendMessage("Room saved: "+val);
                },
                ()->{}
            );
            case 13->new TextEditorMenu(
                "Doorway name",
                null,
                Material.DARK_OAK_DOOR,
                player,
                val->{
                    if(!DBlRegistry.validId.test(val)){
                        player.sendMessage(ChatColor.RED+"Provided name is invalid: "+val);
                        return;
                    }

                    Doorway doorway;
                    try{
                        doorway=Doorway.scan(world,first,second);
                    } catch (StructureScanException e) {
                        player.sendMessage(ChatColor.RED+"Doorway is invalid: "+e.getMessage());
                        return;
                    }

                    DungeonBl.registries().doorways.add(val,doorway);
                    player.sendMessage("Doorway saved: "+val);
                },
                ()->{}
            );
            case 15->new TextEditorMenu(
                "Decoration name",
                null,
                Material.DECORATED_POT,
                player,
                val->{
                    if(!DBlRegistry.validId.test(val)){
                        player.sendMessage(ChatColor.RED+"Provided name is invalid: "+val);
                        return;
                    }

                    Decoration decoration;
                    try{
                        decoration=Decoration.scan(world,first,second);
                    } catch (StructureScanException e) {
                        player.sendMessage(ChatColor.RED+"Decoration is invalid: "+e.getMessage());
                        return;
                    }

                    DungeonBl.registries().decorations.add(val,decoration);
                    player.sendMessage("Decoration saved: "+val);
                },
                ()->{}
            );
            case 26->{
                SelectionWand.clearSelection(player);
                close();
            }
        }
    }

    @Override
    public void closed() {}

    @Override
    public void opened() {}
}
