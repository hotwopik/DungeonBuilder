package io.hotwop.dungeonbl.menu.dungeon;

import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.list.AbstractListMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemRewardMenu extends AbstractListMenu {
    private int listSize;

    public final RewardMenu root;

    public ItemRewardMenu(RewardMenu root) {
        super("Reward Items",root.player);
        this.root = root;

        listSize=root.items.size();

        final ItemStack addButton=ItemStack.of(Material.BLUE_STAINED_GLASS_PANE);
        final ItemMeta addMeta=addButton.getItemMeta();
        addMeta.setItemName(ChatColor.BLUE+"Add Item");
        addButton.setItemMeta(addMeta);

        inventory.setItem(19,addButton);
        inventory.setItem(26,ButtonPrimitives.doneButton());

        updatePage();
    }

    @Override
    public int getListSize() {
        return listSize;
    }

    @Override
    protected ItemStack getItem(int index) {
        return root.items.get(index);
    }

    @Override
    protected void selectElement(int index, int slot, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;

        root.items.remove(index);

        if(listSize%18==1&&currentPage!=0){
            --currentPage;
            updatePageButton();
        }

        listSize=root.items.size();
        updatePage();
    }

    @Override
    protected void bottomMenu(int index, ClickType clickType, ItemStack cursorItem) {
        switch (index){
            case 1->{
                if(cursorItem.isEmpty())return;
                root.items.add(cursorItem.clone());

                listSize=root.items.size();

                updatePage();
            }
            case 8->root.open();
        }
    }

    @Override
    public void opened(){
    }
    @Override
    public void closed(){}
}
