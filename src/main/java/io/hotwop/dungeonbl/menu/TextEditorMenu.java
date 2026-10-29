package io.hotwop.dungeonbl.menu;


import io.hotwop.dungeonbl.DungeonBl;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.List;
import java.util.function.Consumer;

import static io.hotwop.dungeonbl.DungeonBl.scheduler;

public final class TextEditorMenu extends AbstractView<AnvilView>{
    public static final List<String> itemLore=List.of(
        ChatColor.GRAY+"Click out item to finish",
        ChatColor.GRAY+"Exit from menu to cancel"
    );

    private final Consumer<String> setter;
    private final Runnable openParent;
    private final String startName;

    public TextEditorMenu(String title, String value, Material item, Player player, Consumer<String> setter, Runnable openParent) {
        super(title,MenuType.ANVIL,player);
        this.setter = setter;
        this.openParent = openParent;
        startName=value==null?ChatColor.BLACK.toString():value;

        ItemStack renameItem=ItemStack.of(item);
        ItemMeta meta=renameItem.getItemMeta();
        meta.setItemName(startName);
        meta.setLore(itemLore);
        renameItem.setItemMeta(meta);
        view.setItem(0,renameItem);

        view.setMaximumRepairCost(0);
        open();
    }

    @Override
    public void click(int slot, ClickType clickType, ItemStack cursorItem) {
        String current=view.getRenameText();
        if(slot==2&&current!=null&&!current.equals(startName)){
            setter.accept(current);
            close();
        }
    }

    @Override
    protected void closed() {
        scheduler.scheduleSyncDelayedTask(DungeonBl.plugin(), openParent,1);
    }
}
