package io.hotwop.dungeonbl.menu.dungeon;

import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
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
public final class BossesMenu extends AbstractSingleInventory {
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

    private int currentPage=0;
    private final ItemStack pagesButton=ItemStack.of(Material.PAPER);

    private int pagesCount;
    private int listSize;

    static{
        final ItemMeta meta=createButton.getItemMeta();
        meta.setItemName(ChatColor.BLUE+"Create New");
        createButton.setItemMeta(meta);
    }

    public BossesMenu(RoomMenu root){
        super("Bosses",3,root.player);
        this.root=root;

        listSize=root.bosses.size();
        pagesCount=(listSize-1)/18;

        final ItemMeta pagesButtonMeta=pagesButton.getItemMeta();
        pagesButtonMeta.setItemName(ButtonPrimitives.pagesName(0));
        pagesButtonMeta.setLore(ButtonPrimitives.numberControllerLore);
        pagesButton.setItemMeta(pagesButtonMeta);

        inventory.setItem(18,pagesButton);
        inventory.setItem(19,createButton);

        inventory.setItem(26,ButtonPrimitives.doneButton());

        updatePage();
    }

    private void updatePage(){
        boolean emptySlots=pagesCount==currentPage&&(listSize%18!=0||listSize==0);
        int pageStart=currentPage*18;
        int pageEnd=pageStart+(emptySlots ? listSize%18 : 18);

        if(emptySlots)for(int i=listSize%18;i<18;++i)inventory.clear(i);
        if(pageEnd==0)return;
        for(int i=pageStart;i<pageEnd;++i){
            final Boss boss=root.bosses.get(i);

            final ItemStack item=ItemStack.of(Material.GOLD_INGOT);
            final ItemMeta meta=item.getItemMeta();

            meta.setItemName(ChatColor.YELLOW+boss.mob());
            meta.setLore(bossLore(boss));
            item.setItemMeta(meta);

            inventory.setItem(i-pageStart,item);
        }
    }

    public void click(int slot, ClickType clickType, ItemStack cursorItem){
        if(cursorItem.getType()!=Material.AIR)return;
        if(slot>=0&&slot<=17){
            int index=slot+currentPage*18;
            if(index>=listSize)return;
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
                        --currentPage;
                        final ItemMeta pageButtonMeta=pagesButton.getItemMeta();
                        pageButtonMeta.setItemName(ButtonPrimitives.pagesName(currentPage));
                        pagesButton.setItemMeta(pageButtonMeta);

                        inventory.setItem(18,pagesButton);
                    }

                    root.bosses.remove(index);
                    listSize=root.bosses.size();
                    pagesCount=(listSize-1)/18;

                    updatePage();
                }
            );
        }else{
            switch(slot){
                case 18->{
                    if(currentPage!=pagesCount&&clickType.isLeftClick())++currentPage;
                    else if(currentPage!=0&& clickType.isRightClick())--currentPage;
                    else return;

                    final ItemMeta pageButtonMeta=pagesButton.getItemMeta();
                    pageButtonMeta.setItemName(ButtonPrimitives.pagesName(currentPage));
                    pagesButton.setItemMeta(pageButtonMeta);

                    inventory.setItem(18,pagesButton);
                    updatePage();
                }
                case 19->new BossMenu(
                    player,
                    null,
                    this::open,
                    boss->{
                        root.bosses.add(boss);
                        listSize=root.bosses.size();
                        pagesCount=(listSize-1)/18;

                        updatePage();
                    },
                    null
                );
                case 26->{
                    root.updateBossButton();
                    root.open();
                }
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
