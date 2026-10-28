package io.hotwop.dungeonbl.menu.list;

import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@SuppressWarnings("deprecation")
public abstract class AbstractListMenu extends AbstractSingleInventory {
    protected final ItemStack pagesButton=ItemStack.of(Material.PAPER);

    protected int currentPage=0;

    protected AbstractListMenu(String title, Player player) {
        super(title, 3, player);

        ItemMeta pagesButtonMeta=pagesButton.getItemMeta();
        pagesButtonMeta.setItemName(ButtonPrimitives.pagesName(0));
        pagesButtonMeta.setLore(ButtonPrimitives.numberControllerLore);
        pagesButton.setItemMeta(pagesButtonMeta);

        inventory.setItem(18,pagesButton);
    }

    @Override
    public final void click(int slot, ClickType clickType, ItemStack cursorItem) {
        if(slot>=0&&slot<=17){
            int index=slot+currentPage*18;
            if(index>=getListSize())return;
            selectElement(index, slot, cursorItem);
        }else{
            if(slot == 18){
                if(cursorItem.getType()!=Material.AIR)return;

                if (currentPage != getPageCount() && clickType.isLeftClick()) ++currentPage;
                else if (currentPage != 0 && clickType.isRightClick()) --currentPage;
                else return;

                updatePageButton();
                updatePage();
            }else{
                bottomMenu(slot-18, clickType, cursorItem);
            }
        }
    }

    protected final void updatePageButton(){
        final ItemMeta pageButtonMeta = pagesButton.getItemMeta();
        pageButtonMeta.setItemName(ButtonPrimitives.pagesName(currentPage));
        pagesButton.setItemMeta(pageButtonMeta);

        inventory.setItem(18, pagesButton);
    }

    public abstract int getListSize();

    public final int getPageCount(){
        return (getListSize()-1)/18;
    }

    protected abstract ItemStack getItem(int index);

    protected final void updatePage(){
        int listSize=getListSize();
        int pageCount=getPageCount();

        boolean emptySlots=pageCount==currentPage&&(listSize%18!=0||listSize==0);
        int pageStart=currentPage*18;
        int pageEnd=pageStart+(emptySlots ? listSize%18 : 18);

        if(emptySlots)for(int i=listSize%18;i<18;++i)inventory.clear(i);
        if(pageEnd==0)return;
        for(int i=pageStart;i<pageEnd;++i){
            ItemStack item=getItem(i);
            inventory.setItem(i-pageStart,item);
        }
    }

    protected abstract void selectElement(int index, int slot, ItemStack cursorItem);

    protected abstract void bottomMenu(int index, ClickType clickType, ItemStack cursorItem);
}
