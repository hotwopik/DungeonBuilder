package io.hotwop.dungeonbl.menu.dungeon;

import io.hotwop.dungeonbl.config.Room;
import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Random;

public final class RoomsMenu extends AbstractSingleInventory {
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

    private int currentPage=0;
    private final ItemStack pagesButton=ItemStack.of(Material.PAPER);

    private int pagesCount;
    private int listSize;

    public RoomsMenu(DungeonMenu root){
        super("Rooms",3,root.player);
        this.root=root;

        listSize=root.rooms.size();
        pagesCount=(listSize-1)/18;

        final ItemMeta pagesButtonMeta=pagesButton.getItemMeta();
        pagesButtonMeta.setItemName(ButtonPrimitives.pagesName(0));
        pagesButtonMeta.setLore(ButtonPrimitives.numberControllerLore);
        pagesButton.setItemMeta(pagesButtonMeta);

        inventory.setItem(18,pagesButton);
        inventory.setItem(19,createButton);

        inventory.setItem(26,ButtonPrimitives.doneButton());

        updatePage();
    }

    private void updatePage(){
        boolean emptySlots=pagesCount==currentPage&&(listSize%18!=0||listSize==0);
        int pageStart=currentPage*18;
        int pageEnd=pageStart+(emptySlots ? listSize%18 : 18);

        if(emptySlots)for(int i=listSize%18;i<18;++i)inventory.clear(i);
        if(pageEnd==0)return;
        for(int i=pageStart;i<pageEnd;++i){
            final Room room=root.rooms.get(i);

            final ItemStack item=ItemStack.of(roomIcons.get(iconRandom.nextInt(roomIcons.size())));
            final ItemMeta meta=item.getItemMeta();
            meta.setItemName(room.building());
            meta.setLore(DungeonMenu.roomLore(room));
            item.setItemMeta(meta);

            inventory.setItem(i-pageStart,item);
        }
    }

    public void click(int slot, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;
        if(slot>=0&&slot<=17){
            int index=slot+currentPage*18;
            if(index>=listSize)return;
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
                        final ItemMeta pageButtonMeta=pagesButton.getItemMeta();
                        pageButtonMeta.setItemName(ButtonPrimitives.pagesName(currentPage));
                        pagesButton.setItemMeta(pageButtonMeta);

                        inventory.setItem(18,pagesButton);
                    }
                    root.rooms.remove(index);

                    listSize=root.rooms.size();
                    pagesCount=(listSize-1)/18;

                    updatePage();
                }
            );
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
                case 19->new RoomMenu(
                    player,
                    null,
                    this::open,
                    room->{
                        root.rooms.add(room);
                        listSize=root.rooms.size();
                        pagesCount=(listSize-1)/18;
                        updatePage();
                    },
                    null
                );
                case 26->{
                    root.updateRoomsButton();
                    root.open();
                }
            }
        }
    }

    @Override
    public void opened(){}
    @Override
    public void closed(){}
}
