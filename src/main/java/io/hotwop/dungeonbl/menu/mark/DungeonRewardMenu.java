package io.hotwop.dungeonbl.menu.mark;

import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

@SuppressWarnings("deprecation")
public final class DungeonRewardMenu extends AbstractSingleInventory {
    public static final ItemStack getAllButton=ItemStack.of(Material.GREEN_STAINED_GLASS_PANE);

    static{
        ItemMeta getAllMeta=getAllButton.getItemMeta();
        getAllMeta.setItemName(ChatColor.BLUE+"Collect All Can");
        getAllButton.setItemMeta(getAllMeta);
    }

    public final Player player;
    public final String dungeon;

    public final List<ItemStack> items;
    private int currentPage=0;
    private final ItemStack pagesButton=ItemStack.of(Material.PAPER);

    private int pagesCount;
    private int listSize;

    public DungeonRewardMenu(Player player, String dungeon){
        super("Dungeons Reward",3,player);

        this.player=player;
        this.dungeon=dungeon;

        items=DungeonBl.playerDataStorage()
            .get(player).getRewards();

        pagesCount=(items.size()-1)/18;
        listSize=items.size();

        final ItemMeta pagesButtonMeta=pagesButton.getItemMeta();
        pagesButtonMeta.setItemName(ButtonPrimitives.pagesName(0));
        pagesButtonMeta.setLore(ButtonPrimitives.numberControllerLore);
        pagesButton.setItemMeta(pagesButtonMeta);

        inventory.setItem(18,pagesButton);
        inventory.setItem(26,getAllButton);

        updatePage();
        open();
    }
    private void updatePage(){
        boolean emptySlots=pagesCount==currentPage&&(listSize%18!=0||listSize==0);
        int pageStart=currentPage*18;
        int pageEnd=pageStart+(emptySlots ? listSize%18 : 18);

        if(emptySlots)for(int i=listSize%18;i<18;++i)inventory.clear(i);
        if(pageEnd==0)return;
        for(int i=pageStart;i<pageEnd;++i)inventory.setItem(i-pageStart,items.get(i));
    }

    public void click(int slot, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;
        if(slot>=0&&slot<=17){
            if(listSize<=slot+currentPage*18)return;
            give(slot+currentPage*18);
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
                case 26->{
                    while(!items.isEmpty()&&give(0)){}
                    updatePage();
                }
            }
        }
    }

    private boolean give(int index){
        if(DBlUtil.giveIfCan(player,items.get(index))){
            if(listSize%18==1&&currentPage!=0){
                --currentPage;
                final ItemMeta pageButtonMeta=pagesButton.getItemMeta();
                pageButtonMeta.setItemName(ButtonPrimitives.pagesName(currentPage));
                pagesButton.setItemMeta(pageButtonMeta);

                inventory.setItem(18,pagesButton);
            }

            items.remove(index);
            pagesCount=(items.size()-1)/18;
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
