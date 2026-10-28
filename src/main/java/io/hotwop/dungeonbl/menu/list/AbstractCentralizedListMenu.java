package io.hotwop.dungeonbl.menu.list;

import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.IntBinaryOperator;

@SuppressWarnings("deprecation")
public abstract class AbstractCentralizedListMenu extends AbstractSingleInventory {
    protected final ItemStack pagesButton=ItemStack.of(Material.PAPER);
    
    protected int currentPage=0;
    private final Int2IntMap slotToIndexCache=new Int2IntOpenHashMap();

    protected AbstractCentralizedListMenu(String title, Player player) {
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
            ItemStack slotItem=inventory.getItem(slot);
            if(slotItem!=null&&!slotItem.isEmpty()){
                selectElement(slotToIndexCache.get(slot),slot,cursorItem);
            }
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
        int pageSize=emptySlots ? listSize%18 : 18;
        int pageEnd=pageStart+pageSize;

        for(int i=listSize%18;i<18;++i)inventory.clear(i);

        if(pageEnd==0)return;
        for(int i=pageStart;i<pageEnd;++i){
            ItemStack item=getItem(i);
            int slot=getCentralizedSlot(i-pageStart,pageSize);
            slotToIndexCache.put(slot,i);
            inventory.setItem(slot,item);
        }
    }

    protected abstract void selectElement(int index, int slot, ItemStack cursorItem);

    protected abstract void bottomMenu(int index, ClickType clickType, ItemStack cursorItem);

    public static int getCentralizedSlot(int slot,int size){
        if(size==18){
            return slot;
        }else if(size<6){
            return 5-size+slot*2;
        }else{
            int halfSize=size/2;
            int halfSizeAsym=halfSize+size%2;

            if(size<11){
                if(slot<halfSizeAsym){
                    return 5-halfSizeAsym+slot*2;
                }else{
                    return 13-halfSize*2+slot*2-(size%2)*2;
                }
            }else{
                if(slot<halfSizeAsym){
                    int overflow=halfSizeAsym-5;

                    if(slot<=overflow*2){
                        return slot;
                    }else{
                        return (slot-overflow)*2;
                    }
                }else{
                    if(slot>=15-halfSize+size%2){
                        return 12-halfSize-size%2+slot;
                    }else{
                        return (slot-halfSizeAsym)*2;
                    }
                }
            }
        }
    }
}
