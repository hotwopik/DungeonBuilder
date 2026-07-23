package io.hotwop.dungeonbl.menu.dungeon;

import io.hotwop.dungeonbl.config.DungeonReward;
import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public final class RewardMenu extends AbstractSingleInventory {
    public final DungeonMenu root;

    public int money=0;
    public final List<ItemStack> items=new ArrayList<>();

    private final ItemStack coinsButton=ItemStack.of(Material.SUNFLOWER);

    public final ItemRewardMenu itemRewardMenu;

    public RewardMenu(DungeonMenu root){
        super("Reward",3,root.player);
        this.root=root;

        if(root.reward!=null){
            money=root.reward.money();
            items.addAll(root.reward.items());
        }

        itemRewardMenu=new ItemRewardMenu(this);

        ItemMeta coinsMeta=coinsButton.getItemMeta();
        coinsMeta.setItemName(moneyName());
        coinsMeta.setLore(ButtonPrimitives.numberControllerLore);
        coinsButton.setItemMeta(coinsMeta);

        ItemStack itemsButton=ItemStack.of(Material.ENDER_CHEST);
        ItemMeta itemsMeta=itemsButton.getItemMeta();
        itemsMeta.setItemName("Items");
        itemsButton.setItemMeta(itemsMeta);

        inventory.setItem(12,coinsButton);
        inventory.setItem(14,itemsButton);
        inventory.setItem(26,ButtonPrimitives.doneButton());
    }

    public String moneyName(){
        return "Money: "+ ChatColor.YELLOW +money;
    }

    public void click(int slot, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;
        switch(slot){
            case 12->{
                if(clickType.isLeftClick())++money;
                else if(clickType.isRightClick()&&money!=0)--money;
                else return;

                ItemMeta coinsMeta=coinsButton.getItemMeta();
                coinsMeta.setItemName(moneyName());
                coinsButton.setItemMeta(coinsMeta);
                inventory.setItem(12,coinsButton);
            }
            case 14->itemRewardMenu.open();
            case 26->{
                root.reward=new DungeonReward(money,items);
                root.updateRewardButton();
                root.open();
            }
        }
    }

    @Override
    public void opened(){}
    @Override
    public void closed(){}
}
