package io.hotwop.dungeonbl.menu;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public abstract class AbstractSingleInventory extends AbstractInventory {
    public final Player player;
    protected boolean active=false;

    protected AbstractSingleInventory(String title, int size, Player player) {
        super(title, size);
        this.player=player;
    }

    protected AbstractSingleInventory(String title, InventoryType type, Player player) {
        super(title, type);
        this.player=player;
    }

    public final InventoryView open(){
        active=true;
        return player.openInventory(inventory);
    }

    public final void close(){
        if(player.getOpenInventory().getTopInventory().getHolder()==this)player.closeInventory();
    }

    public abstract void click(int slot, ClickType clickType, ItemStack cursorItem);
    public abstract void closed();
    public abstract void opened();

    @Override
    public final void click(int slot, ClickType clickType, ItemStack cursorItem, HumanEntity clicker){
        click(slot,clickType,cursorItem);
    }

    @Override
    public final void opened(HumanEntity player){
        if(!player.equals(this.player))player.closeInventory();
        active=true;
        opened();
    }

    @Override
    public final void closed(HumanEntity player){
        active=false;
        closed();
    }
}
