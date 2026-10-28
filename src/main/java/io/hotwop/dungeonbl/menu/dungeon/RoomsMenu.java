package io.hotwop.dungeonbl.menu.dungeon;

import io.hotwop.dungeonbl.config.Room;
import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.list.AbstractListMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Random;

public final class RoomsMenu extends AbstractListMenu {
    private static final ItemStack createButton=ItemStack.of(Material.BLUE_CONCRETE);

    public static final Random iconRandom=new Random();
    public static final List<Material> roomIcons=List.of(
        Material.YELLOW_CONCRETE, Material.OAK_PLANKS,Material.DARK_OAK_PLANKS, Material.GREEN_CONCRETE
    );

    static{
        final ItemMeta meta=createButton.getItemMeta();
        meta.setItemName(ChatColor.BLUE+"Add New Room");
        createButton.setItemMeta(meta);
    }

    public final DungeonMenu root;
    private int listSize;

    public RoomsMenu(DungeonMenu root){
        super("Rooms",root.player);
        this.root=root;

        listSize=root.rooms.size();

        inventory.setItem(19,createButton);
        inventory.setItem(26,ButtonPrimitives.doneButton());

        updatePage();
    }

    @Override
    public int getListSize() {
        return listSize;
    }

    @Override
    protected ItemStack getItem(int index) {
        Room room=root.rooms.get(index);

        ItemStack item=ItemStack.of(roomIcons.get(iconRandom.nextInt(roomIcons.size())));
        ItemMeta meta=item.getItemMeta();
        meta.setItemName(room.building());
        meta.setLore(DungeonMenu.roomLore(room));
        item.setItemMeta(meta);

        return item;
    }

    @Override
    protected void selectElement(int index, int slot, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;

        new RoomMenu(
            player,
            root.rooms.get(index),
            this::open,
            room->{
                root.rooms.set(index,room);
                updatePage();
            },
            ()->{
                if(listSize%18==1&&currentPage!=0){
                    --currentPage;
                    updatePageButton();
                }
                root.rooms.remove(index);

                listSize=root.rooms.size();

                updatePage();
            }
        );
    }

    @Override
    protected void bottomMenu(int index, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;

        switch (index){
            case 1->new RoomMenu(
                player,
                null,
                this::open,
                room->{
                    root.rooms.add(room);
                    listSize=root.rooms.size();
                    updatePage();
                },
                null
            );
            case 8->{
                root.updateRoomsButton();
                root.open();
            }
        }
    }

    @Override
    public void opened(){}
    @Override
    public void closed(){}
}
