package io.hotwop.dungeonbl.menu.dungeon;

import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.mob.MultiCrossMobMenu;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Random;

public final class PoolsMenu extends AbstractSingleInventory {
    private static final ItemStack createButton=ItemStack.of(Material.BLUE_CONCRETE);

    public static final Random iconRandom=new Random();
    public static final List<Material> poolIcons=List.of(
        Material.ZOMBIE_HEAD,Material.SKELETON_SKULL,Material.CREEPER_HEAD,
        Material.WITHER_SKELETON_SKULL
    );

    public final RoomMenu root;

    private int currentPage=0;
    private final ItemStack pagesButton=ItemStack.of(Material.PAPER);

    private int pagesCount;
    private int listSize;

    static{
        final ItemMeta meta=createButton.getItemMeta();
        meta.setItemName("Create New");
        createButton.setItemMeta(meta);
    }

    public PoolsMenu( RoomMenu root){
        super("Mob Pools",3,root.player);
        this.root=root;

        listSize=root.mobs.size();
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
            final ItemStack item=ItemStack.of(poolIcons.get(iconRandom.nextInt(poolIcons.size())));
            final ItemMeta meta=item.getItemMeta();

            meta.setItemName("Pool "+i);
            meta.setLore(RoomMenu.mobsLore(root.mobs.get(i).size()));
            item.setItemMeta(meta);

            inventory.setItem(i-pageStart,item);
        }
    }

    public void click(int slot, ClickType clickType, ItemStack cursorItem){
        if(cursorItem.getType()!=Material.AIR)return;
        if(slot>=0&&slot<=17){
            int index=slot+currentPage*18;
            if(index>=listSize)return;
            new MultiCrossMobMenu(
                player,
                root.mobs.get(index),
                this::open,
                list->{
                    root.mobs.set(index,list);
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
                case 19->new MultiCrossMobMenu(
                    player,
                    List.of(),
                    this::open,
                    list->{
                        root.mobs.add(list);

                        listSize=root.mobs.size();
                        pagesCount=(listSize-1)/18;

                        updatePage();
                    }
                );
                case 26->{
                    root.updateMobsButton();
                    root.open();
                }
            }
        }
    }

    @Override
    public void opened(){
    }
    @Override
    public void closed(){}
}
