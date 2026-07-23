package io.hotwop.dungeonbl.menu.list;

import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("deprecation")
public final class ListedMenu<T> extends AbstractSingleInventory {
    private static final Random iconRandom=new Random();

    private final List<Material> icons;
    private final List<T> list;
    private final Function<T,String> getName;
    private final Function<T,String> getLore;
    private final Runnable openParent;
    private final Consumer<T> updateListed;

    private int currentPage=0;
    private final ItemStack pagesButton=ItemStack.of(Material.PAPER);

    private final int pagesCount;
    private final int listSize;

    public ListedMenu(
        Player player,

        Set<Material> icons,
        Collection<T> list,

        String title,

        Function<T,String> getName,
        Function<T,String> getLore,

        Runnable openParent,
        Consumer<T> updateListed
    ){
        super(title,3,player);
        this.list=List.copyOf(list);
        this.getName=getName;
        this.getLore=getLore;
        this.openParent=openParent;
        this.updateListed=updateListed;
        this.icons=List.copyOf(icons);

        listSize=list.size();
        pagesCount=(listSize-1)/18;

        final ItemMeta pagesButtonMeta=pagesButton.getItemMeta();
        pagesButtonMeta.setItemName(ButtonPrimitives.pagesName(0));
        pagesButtonMeta.setLore(ButtonPrimitives.numberControllerLore);
        pagesButton.setItemMeta(pagesButtonMeta);

        inventory.setItem(18,pagesButton);
        inventory.setItem(26,ButtonPrimitives.cancelButton());

        updatePage();
        open();
    }

    private void updatePage(){
        boolean emptySlots=pagesCount==currentPage&&(listSize%18!=0||listSize==0);
        int pageStart=currentPage*18;
        int pageEnd=pageStart+(emptySlots ? listSize%18 : 18);

        if(emptySlots)for(int i=listSize%18;i<18;++i)inventory.clear(i);
        if(pageEnd==0)return;
        for(int i=pageStart;i<pageEnd;++i){
            final T listed=list.get(i);

            final ItemStack item=ItemStack.of(icons.get(iconRandom.nextInt(icons.size())));
            final ItemMeta meta=item.getItemMeta();

            if(getName!=null)meta.setItemName(getName.apply(listed));
            else meta.setItemName("");

            if(getLore!=null){
                meta.setLore(List.of(getLore.apply(listed)));
            }
            if(getName==null&&getLore==null)meta.setHideTooltip(true);

            item.setItemMeta(meta);

            inventory.setItem(i-pageStart,item);
        }
    }

    @Override
    public void click(int slot, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;
        if(slot>=0&&slot<=17){
            if(slot+currentPage*18>=listSize)return;
            updateListed.accept(list.get(slot+currentPage*18));
            close();
            openParent.run();
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
                case 26->{
                    close();
                    openParent.run();
                }
            }
        }
    }

    @Override
    public void opened(){}
    @Override
    public void closed(){}
}
