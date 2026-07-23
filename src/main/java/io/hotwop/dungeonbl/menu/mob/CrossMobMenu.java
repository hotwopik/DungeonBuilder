package io.hotwop.dungeonbl.menu.mob;

import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.list.ListedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import io.hotwop.dungeonbl.mob.CrossMob;
import io.hotwop.dungeonbl.mob.MobType;

import java.util.*;
import java.util.function.Consumer;

public final class CrossMobMenu extends AbstractSingleInventory {
    private final Runnable openParent;
    private final Consumer<CrossMob> select;

    public final List<MobType> types;

    @SuppressWarnings("deprecation")
    public CrossMobMenu(Player player, Runnable openParent, Consumer<CrossMob> select){
        super("Set Mob",3,player);
        this.openParent = openParent;
        this.select = select;

        inventory.setItem(26, ButtonPrimitives.cancelButton());

        List<MobType> preTypes=new ArrayList<>();
        int index=-1;
        for(MobType type:MobType.values()){
            if(index>17)break;
            if(!type.active.get())continue;
            ++index;
            preTypes.add(type);

            ItemStack item=ItemStack.of(type.icon);
            ItemMeta meta=item.getItemMeta();
            meta.setItemName(type.displayName);
            item.setItemMeta(meta);

            inventory.setItem(index,item);
        }
        types=List.copyOf(preTypes);

        if(types.size()==1){
            MobType type=types.getFirst();
            new ListedMenu<>(
                player,
                type.itemIcons,
                type.list.get(),
                type.displayName+" mobs",
                val->val,
                null,
                openParent,
                (listed)->select.accept(new CrossMob(type,listed))
            );
        }else{
            open();
        }
    }

    private boolean selected=false;

    @Override
    public void click(int slot, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;

        if(slot>=0&&slot<=17){
            if(slot>=types.size())return;
            MobType type=types.get(slot);
            new ListedMenu<>(
                player,
                type.itemIcons,
                type.list.get(),
                type.displayName+" mobs",
                val->val,
                null,
                ()->{
                    if(selected)openParent.run();
                    else open();
                },
                (listed)->{
                    select.accept(new CrossMob(type,listed));
                    selected=true;
                }
            );
        }else if(slot==26){
            close();
            openParent.run();
        }
    }

    @Override
    public void closed(){}

    @Override
    public void opened(){}
}
