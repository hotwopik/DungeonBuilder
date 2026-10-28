package io.hotwop.dungeonbl.menu.list;

import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("deprecation")
public final class MultiListedMenu<T> extends AbstractListMenu {
    private static final Random iconRandom=new Random();

    private final List<Material> icons;
    private final Material selected;
    private final List<T> list;
    private final List<T> selectedList;
    private final Function<T,String> getName;
    private final Function<T,String> getLore;
    private final Runnable openParent;
    private final Consumer<List<T>> updateMultiListed;

    private final int listSize;

    public MultiListedMenu(
        Player player,

        Set<Material> icons,
        Material selected,

        Collection<T> list,
        Collection<T> selectedList,

        String title,

        Function<T,String> getName,
        Function<T,String> getLore,

        Runnable openParent,
        Consumer<List<T>> updateMultiListed
    ){
        super(title,player);
        this.list=List.copyOf(list);
        this.selectedList=new ArrayList<>(selectedList);
        this.getName=getName;
        this.getLore=getLore;
        this.icons=List.copyOf(icons);
        this.selected=selected;
        this.openParent = openParent;
        this.updateMultiListed = updateMultiListed;

        listSize=list.size();

        inventory.setItem(25,ButtonPrimitives.doneButton());
        inventory.setItem(26,ButtonPrimitives.cancelButton());

        updatePage();
        open();
    }

    @Override
    public int getListSize() {
        return listSize;
    }

    @Override
    protected ItemStack getItem(int index) {
        T listed=list.get(index);

        ItemStack item;
        if(selectedList.contains(listed))item=ItemStack.of(selected);
        else item=ItemStack.of(icons.get(iconRandom.nextInt(icons.size())));

        ItemMeta meta=item.getItemMeta();

        if(getName!=null)meta.setItemName(getName.apply(listed));
        else meta.setItemName("");

        if(getLore!=null)meta.setLore(List.of(getLore.apply(listed)));
        if(getName==null&&getLore==null)meta.setHideTooltip(true);

        item.setItemMeta(meta);

        return item;
    }

    @Override
    protected void selectElement(int index, int slot, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;

        T listed=list.get(index);
        ItemStack old=inventory.getItem(slot);
        if(old==null)return;
        ItemStack item;

        if(selectedList.contains(listed)){
            selectedList.remove(listed);
            item=old.withType(icons.get(iconRandom.nextInt(icons.size())));
        }else{
            selectedList.add(listed);
            item=old.withType(selected);
        }

        inventory.setItem(slot,item);
    }

    @Override
    protected void bottomMenu(int index, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;

        switch (index){
            case 7->{
                updateMultiListed.accept(selectedList);
                close();
                openParent.run();
            }
            case 8->{
                close();
                openParent.run();
            }
        }
    }

    @Override
    public void opened(){}
    @Override
    public void closed(){}
}
