package io.hotwop.dungeonbl.menu;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

@SuppressWarnings("deprecation")
public final class ButtonPrimitives{
    public static final List<String> numberControllerLore=List.of(
        ChatColor.GREEN+"LMB"+ChatColor.GRAY+" - add",
        ChatColor.RED+"RMB"+ChatColor.GRAY+" - reduce"
    );

    public static final List<String> noneLore=List.of(ChatColor.DARK_GRAY+"Not specified");

    private static final ItemStack cancelButton=ItemStack.of(Material.RED_STAINED_GLASS_PANE);
    private static final ItemStack doneButton=ItemStack.of(Material.GREEN_STAINED_GLASS_PANE);
    private static final ItemStack notDoneButton=ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
    private static final ItemStack deleteButton=ItemStack.of(Material.BARRIER);
    private static final ItemStack notDeleteButton=ItemStack.of(Material.BARRIER);

    public static String pagesName(int page){
        return ChatColor.GREEN+"Page"+ChatColor.GRAY+": "+ChatColor.YELLOW+page;
    }

    public static String booleanSwitch(boolean value){
        return value?(ChatColor.GREEN+"ON"):(ChatColor.RED+"OFF");
    }

    public static String mayNotSpecify(String value){
        return value==null?(ChatColor.DARK_GRAY+"Not specified"):(ChatColor.YELLOW+value);
    }

    public static ItemStack cancelButton(){
        return cancelButton.clone();
    }

    public static ItemStack doneButton(){
        return doneButton.clone();
    }

    public static ItemStack notDoneButton(){
        return notDoneButton.clone();
    }

    public static ItemStack deleteButton(){
        return deleteButton.clone();
    }

    public static ItemStack notDeleteButton(){
        return notDeleteButton.clone();
    }

    static{
        final ItemMeta cancelButtonMeta=cancelButton.getItemMeta();
        cancelButtonMeta.setItemName(ChatColor.RED+"Cancel");
        cancelButton.setItemMeta(cancelButtonMeta);

        final ItemMeta doneButtonMeta=doneButton.getItemMeta();
        doneButtonMeta.setItemName(ChatColor.GREEN+"Done");
        doneButton.setItemMeta(doneButtonMeta);

        final ItemMeta notDoneButtonMeta=notDoneButton.getItemMeta();
        notDoneButtonMeta.setItemName(ChatColor.DARK_GRAY+"Done");
        notDoneButton.setItemMeta(notDoneButtonMeta);

        final ItemMeta deleteButtonMeta=deleteButton.getItemMeta();
        deleteButtonMeta.setItemName(ChatColor.DARK_RED+"Delete");
        deleteButton.setItemMeta(deleteButtonMeta);

        final ItemMeta notDeleteButtonMeta=notDeleteButton.getItemMeta();
        notDeleteButtonMeta.setItemName(ChatColor.DARK_GRAY+"Delete");
        notDeleteButton.setItemMeta(deleteButtonMeta);
    }

    private ButtonPrimitives(){}
}
