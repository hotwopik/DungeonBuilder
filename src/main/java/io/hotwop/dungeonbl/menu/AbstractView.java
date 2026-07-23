package io.hotwop.dungeonbl.menu;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractView<T extends InventoryView>{
    private static final Map<HumanEntity,AbstractView<?>> all=new HashMap<>();

    public final T view;
    public final Player player;

    protected AbstractView(String title,MenuType.Typed<T> type,Player player){
        view=type.create(player,title);
        this.player=player;
    }

    public abstract void click(int slot, ClickType clickType, ItemStack cursorItem);
    protected abstract void closed();

    public final void open(){
        player.openInventory(view);
        all.put(player,this);
    }

    public final void close(){
        view.close();
    }

    public final void onClose(){
        view.getTopInventory().clear();
        all.remove(player);
        closed();
    }

    public static @Nullable AbstractView<?> get(HumanEntity player){
        return all.get(player);
    }
}
