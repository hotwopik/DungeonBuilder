package io.hotwop.dungeonbl.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public abstract class AbstractInventory implements InventoryHolder{
    public final Inventory inventory;

    @SuppressWarnings("deprecation")
    protected AbstractInventory(String title, int size){
        this.inventory = Bukkit.createInventory(this,size*9,title);
    }

    @SuppressWarnings("deprecation")
    protected AbstractInventory(String title, InventoryType type){
        this.inventory = Bukkit.createInventory(this,type,title);
    }

    public abstract void click(int slot, ClickType clickType, ItemStack cursorItem,HumanEntity clicker);
    public abstract void closed(HumanEntity player);
    public abstract void opened(HumanEntity player);

    public final @NonNull Inventory getInventory(){
        return inventory;
    }
}