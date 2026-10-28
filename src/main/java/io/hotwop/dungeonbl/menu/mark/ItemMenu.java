package io.hotwop.dungeonbl.menu.mark;

import io.hotwop.dungeonbl.mark.MarkType;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemMenu extends AbstractSingleInventory {
    public ItemMenu(Player player) {
        super("Items", 3, player);

        int index=0;
        for(MarkType type: MarkType.standardTypes){
            ItemStack item=type.item.createItemStack();
            ItemMeta meta=item.getItemMeta();
            meta.setItemName(type.name);
            item.setItemMeta(meta);

            inventory.setItem(index,item);
            index+=1;
        }
    }

    @Override
    public void click(int slot, ClickType clickType, ItemStack cursorItem) {

    }

    @Override
    public void closed() {

    }

    @Override
    public void opened() {

    }
}
