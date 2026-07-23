package io.hotwop.dungeonbl.menu.mob;

import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.list.MultiListedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import io.hotwop.dungeonbl.mob.CrossMob;
import io.hotwop.dungeonbl.mob.MobType;

import java.util.*;
import java.util.function.Consumer;

public final class MultiCrossMobMenu extends AbstractSingleInventory {
    private final Runnable openParent;
    private final Consumer<List<CrossMob>> select;

    private final Map<MobType, List<String>> lists=new HashMap<>();
    public final List<MobType> types;

    @SuppressWarnings("deprecation")
    public MultiCrossMobMenu(Player player, List<CrossMob> selected, Runnable openParent, Consumer<List<CrossMob>> select){
        super("Set Mob list",3,player);
        this.openParent = openParent;
        this.select = select;

        List<MobType> preTypes=new ArrayList<>();
        int index=-1;
        for(MobType type:MobType.values()){
            lists.put(type,new ArrayList<>());

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

            new MultiListedMenu<>(
                player,
                type.itemIcons,
                Material.LIME_STAINED_GLASS_PANE,
                type.list.get(),
                lists.get(type),
                type.name()+" mobs",
                val->val,
                null,
                openParent,
                (listed)->select.accept(listed.stream()
                    .map(v->new CrossMob(type,v))
                    .toList())
            );
        }else{
            selected.forEach(mob->{
                List<String> type=lists.get(mob.type());
                type.add(mob.mob());
            });

            inventory.setItem(25, ButtonPrimitives.doneButton());
            inventory.setItem(26, ButtonPrimitives.cancelButton());

            open();
        }
    }

    @Override
    public void click(int slot, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;
        if(slot>=0&&slot<=17){
            if(slot>=types.size())return;
            MobType type=types.get(slot);
            new MultiListedMenu<>(
                player,
                type.itemIcons,
                Material.LIME_STAINED_GLASS_PANE,
                type.list.get(),
                lists.get(type),
                type.displayName+" mobs",
                val->val,
                null,
                this::open,
                (listed)->lists.put(type,listed)
            );
        }else {
            switch (slot){
                case 26->{
                    close();
                    openParent.run();
                }
                case 25 -> {
                    close();

                    List<CrossMob> mobs = new ArrayList<>();
                    lists.forEach((type, mobes) -> mobes.forEach(mob -> mobs.add(new CrossMob(type, mob))));

                    select.accept(mobs);
                    openParent.run();
                }
            }
        }
    }

    @Override
    public void closed(){}

    @Override
    public void opened(){}
}
