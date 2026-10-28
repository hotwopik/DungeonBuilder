package io.hotwop.dungeonbl.menu.dungeon;

import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.list.AbstractListMenu;
import io.hotwop.dungeonbl.mob.Boss;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public final class BossesMenu extends AbstractListMenu {
    public static final Map<BarColor,String> colors=Map.of(
        BarColor.RED,ChatColor.RED+"Red",
        BarColor.PINK,ChatColor.LIGHT_PURPLE+"Pink",
        BarColor.BLUE,ChatColor.BLUE+"Blue",
        BarColor.GREEN,ChatColor.GREEN+"Green",
        BarColor.PURPLE,ChatColor.DARK_PURPLE+"Purple",
        BarColor.WHITE,ChatColor.WHITE+"White",
        BarColor.YELLOW,ChatColor.YELLOW+"Yellow"
    );

    private static final ItemStack createButton=ItemStack.of(Material.BLUE_CONCRETE);

    public final RoomMenu root;

    private int listSize;

    static{
        final ItemMeta meta=createButton.getItemMeta();
        meta.setItemName(ChatColor.BLUE+"Create New");
        createButton.setItemMeta(meta);
    }

    public BossesMenu(RoomMenu root){
        super("Bosses",root.player);
        this.root=root;

        listSize=root.bosses.size();

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
        final Boss boss=root.bosses.get(index);

        final ItemStack item=ItemStack.of(Material.GOLD_INGOT);
        final ItemMeta meta=item.getItemMeta();

        meta.setItemName(ChatColor.YELLOW+boss.mob());
        meta.setLore(bossLore(boss));
        item.setItemMeta(meta);

        return item;
    }

    @Override
    protected void selectElement(int index, int slot, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;

        new BossMenu(
            player,
            root.bosses.get(index),
            this::open,
            boss->{
                root.bosses.set(index,boss);
                updatePage();
            },
            ()->{
                if(listSize%18==1&&currentPage!=0){
                    currentPage--;
                    updatePageButton();
                }

                root.bosses.remove(index);
                listSize=root.bosses.size();

                updatePage();
            }
        );
    }

    @Override
    protected void bottomMenu(int index, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;

        switch(index){
            case 1->new BossMenu(
                player,
                null,
                this::open,
                boss->{
                    root.bosses.add(boss);
                    listSize=root.bosses.size();

                    updatePage();
                },
                null
            );
            case 8->{
                root.updateBossButton();
                root.open();
            }
        }
    }

    public static List<String> bossLore(Boss boss){
        return List.of(
            ChatColor.GRAY+"Type: "+boss.type().displayName,
            ChatColor.GRAY+"Boss Bar: "+(boss.color()==null?(ChatColor.DARK_GRAY+"No Boss Bar"):colors.get(boss.color()))
        );
    }

    @Override
    public void opened(){}
    @Override
    public void closed(){}
}
