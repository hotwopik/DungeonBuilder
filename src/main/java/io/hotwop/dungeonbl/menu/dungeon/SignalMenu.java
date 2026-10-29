package io.hotwop.dungeonbl.menu.dungeon;

import io.hotwop.dungeonbl.io.DBlRegistry;
import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.TextEditorMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class SignalMenu extends AbstractSingleInventory {
    public final List<String> nameButtonLore=List.of(
        ChatColor.GREEN+"LMB"+ChatColor.GRAY+" - set",
        ChatColor.RED+"RMB"+ChatColor.GRAY+" - clear"
    );
    public final List<String> invalidNameButtonLore=List.of(
        ChatColor.GREEN+"LMB"+ChatColor.GRAY+" - set",
        ChatColor.RED+"RMB"+ChatColor.GRAY+" - clear",
        "", ChatColor.DARK_RED+"*Previously specified id is invalid"
    );

    private final ItemStack nameButton=ItemStack.of(Material.PAPER);
    private final ItemStack valueButton=ItemStack.of(Material.EXPERIENCE_BOTTLE);

    public final RoomMenu parent;

    public SignalMenu(RoomMenu parent){
        super("End Signal Configuration", 3, parent.player);
        this.parent=parent;

        if(parent.endSignalValue==null)parent.endSignalValue=1;

        ItemMeta nameMeta= nameButton.getItemMeta();
        nameMeta.setItemName(nameName());
        nameMeta.setLore(nameButtonLore);
        nameButton.setItemMeta(nameMeta);

        ItemMeta valueMeta=valueButton.getItemMeta();
        valueMeta.setRarity(ItemRarity.COMMON);
        valueMeta.setItemName(valueName());
        valueMeta.setLore(ButtonPrimitives.numberControllerLore);
        valueButton.setItemMeta(valueMeta);

        inventory.setItem(12,nameButton);
        inventory.setItem(14,valueButton);
        inventory.setItem(26,ButtonPrimitives.doneButton());
    }

    public String nameName(){
        return "Signal Name: "+ButtonPrimitives.mayNotSpecify(parent.endsOnSignal);
    }

    public String valueName(){
        return "Signal Value: "+ ChatColor.YELLOW+parent.endSignalValue;
    }

    @Override
    public void click(int slot, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;
        switch(slot){
            case 12->{
                if(clickType.isLeftClick()){
                    new TextEditorMenu(
                        "Signal Name",
                        parent.endsOnSignal,
                        Material.BELL,
                        player,
                        value->{
                            if(value.isEmpty())parent.endsOnSignal=null;
                            else if(DBlRegistry.validId.test(value)){
                                parent.endsOnSignal=value;

                                ItemMeta nameMeta= nameButton.getItemMeta();
                                nameMeta.setItemName(nameName());
                                nameMeta.setLore(nameButtonLore);
                                nameButton.setItemMeta(nameMeta);

                                inventory.setItem(12,nameButton);
                            }else{
                                ItemMeta nameMeta=nameButton.getItemMeta();
                                nameMeta.setLore(invalidNameButtonLore);
                                nameButton.setItemMeta(nameMeta);

                                inventory.setItem(12,nameButton);
                            }
                        },
                        this::open
                    );
                }else if(clickType.isRightClick()){
                    parent.endsOnSignal=null;

                    ItemMeta nameMeta= nameButton.getItemMeta();
                    nameMeta.setItemName(nameName());
                    nameMeta.setLore(nameButtonLore);
                    nameButton.setItemMeta(nameMeta);

                    inventory.setItem(12,nameButton);
                }
            }
            case 14->{
                if(clickType.isLeftClick())++parent.endSignalValue;
                else if(parent.endSignalValue!=1&&clickType.isRightClick())--parent.endSignalValue;
                else return;

                ItemMeta valueMeta=valueButton.getItemMeta();
                valueMeta.setItemName(valueName());
                valueButton.setItemMeta(valueMeta);

                inventory.setItem(14,valueButton);
            }
            case 26->{
                parent.updateEndsOnSignalButton();
                parent.open();
            }
        }
    }

    @Override
    public void closed() {}

    @Override
    public void opened() {}
}
