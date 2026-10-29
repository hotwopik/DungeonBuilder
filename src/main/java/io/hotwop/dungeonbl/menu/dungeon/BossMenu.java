package io.hotwop.dungeonbl.menu.dungeon;

import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.mob.CrossMobMenu;
import io.hotwop.dungeonbl.mob.Boss;
import io.hotwop.dungeonbl.mob.CrossMob;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public final class BossMenu extends AbstractSingleInventory {
    private final ItemStack mobButton=ItemStack.of(Material.ZOMBIE_HEAD);
    private final ItemStack colorButton=ItemStack.of(Material.RED_DYE);

    private final Runnable openParent;
    private final Consumer<Boss> set;
    private final Runnable del;

    private CrossMob mob=null;
    private BarColor color=null;

    private final boolean delete;

    public BossMenu(Player player, @Nullable Boss boss, Runnable openParent, Consumer<Boss> set, Runnable del){
        super("Change Boss",3,player);
        if(!player.hasPermission("dungeonbl.command.dungeon"))throw new RuntimeException("Permission exception");

        this.openParent = openParent;
        this.set = set;
        this.del = del;

        if(boss!=null){
            inventory.setItem(18, ButtonPrimitives.deleteButton());

            mob=new CrossMob(boss.type(),boss.mob());
            color=boss.color();

            inventory.setItem(25, ButtonPrimitives.doneButton());
            delete=true;
        }else{
            inventory.setItem(25, ButtonPrimitives.notDoneButton());
            delete=false;
        }

        final ItemMeta mobMeta=mobButton.getItemMeta();
        mobMeta.setItemName("Mob");

        if(mob==null)mobMeta.setLore(ButtonPrimitives.noneLore);
        else mobMeta.setLore(mobLore(mob));

        mobMeta.setRarity(ItemRarity.COMMON);
        mobButton.setItemMeta(mobMeta);

        final ItemMeta colorMeta=colorButton.getItemMeta();
        colorMeta.setItemName(bossBarName());
        colorButton.setItemMeta(colorMeta);

        inventory.setItem(12,mobButton);
        inventory.setItem(14,colorButton);

        inventory.setItem(26,ButtonPrimitives.cancelButton());
        player.openInventory(inventory);
    }

    public void click(int slot, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;
        switch(slot){
            case 12->new CrossMobMenu(
                player,
                this::open,
                (mob)->{
                    if(this.mob==null)inventory.setItem(25,ButtonPrimitives.doneButton());
                    this.mob=mob;

                    final ItemMeta mobMeta=mobButton.getItemMeta();
                    mobMeta.setLore(mobLore(mob));
                    mobButton.setItemMeta(mobMeta);

                    inventory.setItem(12,mobButton);
                }
            );
            case 14->{
                BarColor[] colors=BarColor.values();
                int index=(color==null?0:color.ordinal()+1)%(colors.length+1);
                if(index!=colors.length)color=colors[index];
                else color=null;

                final ItemMeta colorMeta=colorButton.getItemMeta();
                colorMeta.setItemName(bossBarName());
                colorButton.setItemMeta(colorMeta);

                inventory.setItem(14,colorButton);
            }
            case 25->{
                if(mob==null)return;
                set.accept(new Boss(mob.mob(),mob.type(),color));
                close();
                openParent.run();
            }
            case 26->{
                close();
                openParent.run();
            }
            case 18->{
                if(!delete)return;
                del.run();
                close();
                openParent.run();
            }
        }
    }

    public static List<String> mobLore(CrossMob mob){
        return List.of(
            ChatColor.GRAY+"Name: "+ChatColor.YELLOW+mob.mob(),
            ChatColor.GRAY+"Type: "+mob.type().displayName
        );
    }

    public String bossBarName(){
        return color==null?(ChatColor.DARK_GRAY+"No Boss Bar"):("Boss Bar: "+BossesMenu.colors.get(color));
    }

    @Override
    public void opened(){}
    @Override
    public void closed(){}
}
