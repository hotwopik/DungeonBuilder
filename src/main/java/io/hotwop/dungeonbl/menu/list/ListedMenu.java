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
public final class ListedMenu<T> extends AbstractListMenu {
    private static final Random iconRandom=new Random();

    private final List<Material> icons;
    private final List<T> list;
    private final Function<T,String> getName;
    private final Function<T,String> getLore;
    private final Runnable openParent;
    private final Consumer<T> updateListed;

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
        super(title,player);
        this.list=List.copyOf(list);
        this.getName=getName;
        this.getLore=getLore;
        this.openParent=openParent;
        this.updateListed=updateListed;
        this.icons=List.copyOf(icons);

        listSize=list.size();

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

        ItemStack item=ItemStack.of(icons.get(iconRandom.nextInt(icons.size())));
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

        updateListed.accept(list.get(index+currentPage*18));
        close();
        openParent.run();
    }

    @Override
    protected void bottomMenu(int index, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;

        if (index == 8) {
            close();
            openParent.run();
        }
    }

    @Override
    public void opened(){}
    @Override
    public void closed(){}
}
