package io.hotwop.dungeonbl.menu.dungeon;

import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemRewardMenu extends AbstractSingleInventory {

    private int currentPage=0;
    private final ItemStack pagesButton=ItemStack.of(Material.PAPER);

    private int pagesCount;
    private int listSize;

    public final RewardMenu root;

    public ItemRewardMenu(RewardMenu root) {
        super("Reward Items",3,root.player);
        this.root = root;

        listSize=root.items.size();
        pagesCount=(listSize-1)/18;

        final ItemMeta pagesButtonMeta=pagesButton.getItemMeta();
        pagesButtonMeta.setItemName(ButtonPrimitives.pagesName(0));
        pagesButtonMeta.setLore(ButtonPrimitives.numberControllerLore);
        pagesButton.setItemMeta(pagesButtonMeta);

        final ItemStack addButton=ItemStack.of(Material.BLUE_STAINED_GLASS_PANE);
        final ItemMeta addMeta=addButton.getItemMeta();
        addMeta.setItemName(ChatColor.BLUE+"Add Item");
        addButton.setItemMeta(addMeta);

        inventory.setItem(18,pagesButton);
        inventory.setItem(19,addButton);

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
            final ItemStack item=root.items.get(i);
            inventory.setItem(i-pageStart,item);
        }
    }

    public void click(int slot, ClickType clickType, ItemStack cursorItem) {
        if(slot>=0&&slot<=17){
            int index=slot+currentPage*18;
            if(listSize<=index)return;
            root.items.remove(index);

            if(listSize%18==1&&currentPage!=0){
                --currentPage;
                final ItemMeta pageButtonMeta=pagesButton.getItemMeta();
                pageButtonMeta.setItemName(ButtonPrimitives.pagesName(currentPage));
                pagesButton.setItemMeta(pageButtonMeta);

                inventory.setItem(18,pagesButton);
            }

            listSize=root.items.size();
            pagesCount=(listSize-1)/18;

            updatePage();
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
                case 19->{
                    if(cursorItem.isEmpty())return;
                    root.items.add(cursorItem.clone());

                    listSize=root.items.size();
                    pagesCount=(listSize-1)/18;

                    updatePage();
                }
                case 26->root.open();
            }
        }
    }

    @Override
    public void opened(){
    }
    @Override
    public void closed(){}
}
