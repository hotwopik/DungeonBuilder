package io.hotwop.dungeonbl.menu.mark;

import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.list.AbstractListMenu;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

@SuppressWarnings("deprecation")
public final class DungeonRewardMenu extends AbstractListMenu {
    public static final ItemStack getAllButton=ItemStack.of(Material.GREEN_STAINED_GLASS_PANE);

    static{
        ItemMeta getAllMeta=getAllButton.getItemMeta();
        getAllMeta.setItemName(ChatColor.BLUE+"Collect All Can");
        getAllButton.setItemMeta(getAllMeta);
    }

    public final String dungeon;

    public final List<ItemStack> items;
    private int listSize;

    public DungeonRewardMenu(Player player, String dungeon){
        super("Dungeons Reward",player);

        this.dungeon=dungeon;

        items=DungeonBl.playerDataStorage()
            .get(player).getRewards();

        listSize=items.size();
        inventory.setItem(26,getAllButton);

        updatePage();
        open();
    }

    @Override
    public int getListSize() {
        return listSize;
    }

    @Override
    protected ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    protected void selectElement(int index, int slot, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;

        give(index);
        updatePage();
    }

    @Override
    protected void bottomMenu(int index, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;

        if(index==8){
            while(!items.isEmpty()&&give(0)){}
            updatePage();
        }
    }

    private boolean give(int index){
        if(DBlUtil.giveIfCan(player,items.get(index))){
            if(listSize%18==1&&currentPage!=0){
                --currentPage;
                updatePageButton();
            }

            items.remove(index);
            listSize=items.size();

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP,SoundCategory.PLAYERS,1,1);

            return true;
        }else return false;
    }

    @Override
    public void closed() {}

    @Override
    public void opened() {}
}
