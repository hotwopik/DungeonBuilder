package io.hotwop.dungeonbl.menu.dungeon;

import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.list.AbstractListMenu;
import io.hotwop.dungeonbl.menu.mob.MultiCrossMobMenu;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Random;

public final class PoolsMenu extends AbstractListMenu {
    private static final ItemStack createButton=ItemStack.of(Material.BLUE_CONCRETE);

    public static final Random iconRandom=new Random();
    public static final List<Material> poolIcons=List.of(
        Material.ZOMBIE_HEAD,Material.SKELETON_SKULL,Material.CREEPER_HEAD,
        Material.WITHER_SKELETON_SKULL
    );

    public final RoomMenu root;

    private int listSize;

    static{
        final ItemMeta meta=createButton.getItemMeta();
        meta.setItemName("Create New");
        createButton.setItemMeta(meta);
    }

    public PoolsMenu( RoomMenu root){
        super("Mob Pools",root.player);
        this.root=root;

        listSize=root.mobs.size();

        inventory.setItem(19,createButton);
        inventory.setItem(26,ButtonPrimitives.doneButton());

        updatePage();
    }

    @Override
    public int getListSize() {
        return listSize;
    }

    @Override
    protected ItemStack getItem(int index) {
        ItemStack item=ItemStack.of(poolIcons.get(iconRandom.nextInt(poolIcons.size())));
        ItemMeta meta=item.getItemMeta();

        meta.setItemName("Pool "+index);
        meta.setLore(RoomMenu.mobsLore(root.mobs.get(index).size()));
        item.setItemMeta(meta);

        return item;
    }

    @Override
    protected void selectElement(int index, int slot, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;

        new MultiCrossMobMenu(
            player,
            root.mobs.get(index),
            this::open,
            list->{
                root.mobs.set(index,list);
                updatePage();
            }
        );
    }

    @Override
    protected void bottomMenu(int index, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;

        switch(index){
            case 1->new MultiCrossMobMenu(
                player,
                List.of(),
                this::open,
                list->{
                    root.mobs.add(list);

                    listSize=root.mobs.size();

                    updatePage();
                }
            );
            case 8->{
                root.updateMobsButton();
                root.open();
            }
        }
    }

    @Override
    public void opened(){
    }
    @Override
    public void closed(){}
}
