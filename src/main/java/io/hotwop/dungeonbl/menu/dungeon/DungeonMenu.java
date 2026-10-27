package io.hotwop.dungeonbl.menu.dungeon;

import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.config.*;
import io.hotwop.dungeonbl.io.DBlRegistry;
import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("deprecation")
public final class DungeonMenu extends AbstractSingleInventory {
    private final ItemStack roomCountButton = ItemStack.of(Material.PAPER);
    private final ItemStack deathLimitButton = ItemStack.of(Material.TOTEM_OF_UNDYING);
    private final ItemStack rewardButton = ItemStack.of(Material.CHEST);
    private final ItemStack startRoomButton = ItemStack.of(Material.GREEN_CONCRETE);
    private final ItemStack endRoomButton = ItemStack.of(Material.RED_CONCRETE);
    private final ItemStack roomsButton = ItemStack.of(Material.YELLOW_CONCRETE);
    private final ItemStack disableStarvationButton = ItemStack.of(Material.COOKED_BEEF);
    private final ItemStack nextRoomDelayButton = ItemStack.of(Material.CLOCK);

    public int roomCount=1;
    public int deathLimit=0;
    public Room startRoom=null;
    public Room endRoom=null;
    public List<Room> rooms=new ArrayList<>();
    public DungeonReward reward;
    public boolean disableStarvation=true;
    public int nextRoomDelay=20;

    public final RewardMenu rewardMenu;
    public final RoomsMenu roomsMenu;

    public final String name;

    private final boolean delete;

    public DungeonMenu(@Nullable Dungeon dungeon, String name, Player player){
        super("Edit Dungeon",3,player);
        if(!player.hasPermission("dungeonbl.command.dungeon"))throw
            new RuntimeException("Permission exception");
        if(!DBlRegistry.validId.test(name))throw
            new RuntimeException("Id is invalid");

        this.name=name;

        List<String> startRoomLore;
        List<String> endRoomLore;

        if(dungeon!=null){
            roomCount=dungeon.roomCount();
            deathLimit=dungeon.deathLimit();
            startRoom=dungeon.startRoom();
            endRoom=dungeon.endRoom();
            rooms.addAll(dungeon.rooms());
            reward=dungeon.reward();
            disableStarvation=dungeon.disableStarvation();
            nextRoomDelay= dungeon.nextRoomDelay();

            inventory.setItem(26, ButtonPrimitives.doneButton());
            inventory.setItem(8,ButtonPrimitives.deleteButton());

            delete=true;

            startRoomLore=roomLore(startRoom);
            endRoomLore=roomLore(endRoom);
        }else{
            reward=new DungeonReward(0,List.of());

            inventory.setItem(26,ButtonPrimitives.notDoneButton());
            delete=false;

            endRoomLore=startRoomLore=ButtonPrimitives.noneLore;
        }

        rewardMenu=new RewardMenu(this);
        roomsMenu=new RoomsMenu(this);

        final ItemMeta endingValueMeta=roomCountButton.getItemMeta();
        endingValueMeta.setItemName(lastRoomName());
        endingValueMeta.setLore(ButtonPrimitives.numberControllerLore);
        roomCountButton.setItemMeta(endingValueMeta);

        final ItemMeta deathMeta= deathLimitButton.getItemMeta();
        deathMeta.setRarity(ItemRarity.COMMON);
        deathMeta.setItemName(deathLimitName());
        deathMeta.setLore(ButtonPrimitives.numberControllerLore);
        deathLimitButton.setItemMeta(deathMeta);

        final ItemMeta rewardMeta = rewardButton.getItemMeta();
        rewardMeta.setItemName("Rewards");
        rewardMeta.setLore(rewardLore(reward));
        rewardButton.setItemMeta(rewardMeta);

        final ItemMeta startRoomMeta = startRoomButton.getItemMeta();
        startRoomMeta.setItemName("Start Room");
        startRoomMeta.setLore(startRoomLore);
        startRoomButton.setItemMeta(startRoomMeta);

        final ItemMeta endRoomMeta = endRoomButton.getItemMeta();
        endRoomMeta.setItemName("Last Room");
        endRoomMeta.setLore(endRoomLore);
        endRoomButton.setItemMeta(endRoomMeta);

        final ItemMeta roomsMeta = roomsButton.getItemMeta();
        roomsMeta.setItemName("Rooms");
        roomsMeta.setLore(roomsLore(rooms.size()));
        roomsButton.setItemMeta(roomsMeta);

        final ItemMeta starvationMeta=disableStarvationButton.getItemMeta();
        starvationMeta.setItemName(disableStarvationName());
        disableStarvationButton.setItemMeta(starvationMeta);

        final ItemMeta roomDelayMeta=nextRoomDelayButton.getItemMeta();
        roomDelayMeta.setItemName(nextRoomDelayName());
        roomDelayMeta.setLore(ButtonPrimitives.numberControllerLore);
        nextRoomDelayButton.setItemMeta(roomDelayMeta);

        inventory.setItem(0,roomCountButton);
        inventory.setItem(1, deathLimitButton);
        inventory.setItem(3,startRoomButton);
        inventory.setItem(4,endRoomButton);
        inventory.setItem(5,roomsButton);
        inventory.setItem(10,rewardButton);
        inventory.setItem(9,disableStarvationButton);
        inventory.setItem(18,nextRoomDelayButton);

        open();
    }

    private void doneCheck(){
        if(startRoom==null||endRoom==null||rooms.isEmpty())inventory.setItem(26,ButtonPrimitives.notDoneButton());
        else inventory.setItem(26,ButtonPrimitives.doneButton());
    }

    public String lastRoomName(){
        return "Rooms to generate: "+ChatColor.YELLOW+roomCount;
    }

    public String deathLimitName(){
        return "Death limit: "+ChatColor.YELLOW+(deathLimit==0?"Unlimited":deathLimit);
    }

    public String disableStarvationName(){
        return "Disable Starvation: "+ButtonPrimitives.booleanSwitch(disableStarvation);
    }

    public String nextRoomDelayName(){
        return "Next Room Delay: "+ChatColor.YELLOW+(nextRoomDelay==0?"None":nextRoomDelay+" ticks");
    }

    public static List<String> roomLore(Room room){
        List<String> out=new ArrayList<>();
        out.add(ChatColor.GRAY+"Building: "+ChatColor.YELLOW+room.building());

        if(room.endsWithPlayers()){
            out.add(ChatColor.GRAY+"Ends with Players");
        }

        if(room.endsWithClear()){
            out.add(ChatColor.GRAY+"Ends with Clear");
        }

        if(room.endsOnSignal()!=null){
            out.add(ChatColor.GRAY+"Ends on Signal: "+ChatColor.YELLOW+room.endsOnSignal());
            out.add(ChatColor.GRAY+"Signal value: "+ChatColor.YELLOW+room.endSignalValue());
        }

        out.addAll(List.of(
            ChatColor.GRAY+"Mobs: "+ChatColor.YELLOW+(room.mobs()==null?0:room.mobs().mobCount),
            ChatColor.GRAY+"Waves: "+ChatColor.YELLOW+room.waves(),
            ChatColor.GRAY+"Bosses: "+ChatColor.YELLOW+room.bosses().size(),
            ChatColor.GRAY+"Decorations: "+ChatColor.YELLOW+room.decorations().size()
        ));

        return out;
    }
    public static List<String> roomsLore(int size){
        return List.of(
            ChatColor.GRAY+"Room Count: "+ChatColor.YELLOW+size
        );
    }
    public static List<String> rewardLore(DungeonReward reward){
        return List.of(
            ChatColor.GRAY+"Money: "+ChatColor.YELLOW+reward.money(),
            ChatColor.GRAY+"Items: "+ChatColor.YELLOW+reward.items().size()
        );
    }

    public void click(int slot, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;
        switch(slot){
            case 0->{
                if(clickType.isLeftClick())++roomCount;
                else if(roomCount!=1&&clickType.isRightClick())--roomCount;
                else return;

                final ItemMeta endingValueMeta =roomCountButton.getItemMeta();
                endingValueMeta.setItemName(lastRoomName());
                roomCountButton.setItemMeta(endingValueMeta);
                inventory.setItem(0,roomCountButton);
            }
            case 1->{
                if(clickType.isLeftClick())++deathLimit;
                else if(deathLimit!=0&&clickType.isRightClick())--deathLimit;
                else return;

                final ItemMeta deathMeta = deathLimitButton.getItemMeta();
                deathMeta.setItemName(deathLimitName());
                deathLimitButton.setItemMeta(deathMeta);
                inventory.setItem(1, deathLimitButton);
            }
            case 10->rewardMenu.open();
            case 9->{
                disableStarvation=!disableStarvation;

                final ItemMeta starvationMeta=disableStarvationButton.getItemMeta();
                starvationMeta.setItemName(disableStarvationName());
                disableStarvationButton.setItemMeta(starvationMeta);
                inventory.setItem(9,disableStarvationButton);
            }
            case 18->{
                if(clickType.isLeftClick())++nextRoomDelay;
                else if(nextRoomDelay!=0&&clickType.isRightClick())--nextRoomDelay;
                else return;

                final ItemMeta roomDelayMeta=nextRoomDelayButton.getItemMeta();
                roomDelayMeta.setItemName(nextRoomDelayName());
                nextRoomDelayButton.setItemMeta(roomDelayMeta);
                inventory.setItem(18,nextRoomDelayButton);
            }

            case 3->new RoomMenu(
                player,
                startRoom,
                this::open,
                room->{
                    startRoom=room;

                    ItemMeta meta=startRoomButton.getItemMeta();
                    meta.setLore(roomLore(room));
                    startRoomButton.setItemMeta(meta);
                    inventory.setItem(3, startRoomButton);
                },
                ()->{
                    startRoom=null;

                    ItemMeta meta=startRoomButton.getItemMeta();
                    meta.setLore(ButtonPrimitives.noneLore);
                    startRoomButton.setItemMeta(meta);
                    inventory.setItem(3, startRoomButton);
                }
            );
            case 4->new RoomMenu(
                player,
                endRoom,
                this::open,
                room->{
                    endRoom=room;

                    ItemMeta meta=endRoomButton.getItemMeta();
                    meta.setLore(roomLore(room));
                    endRoomButton.setItemMeta(meta);
                    inventory.setItem(4, endRoomButton);
                },
                ()->{
                    endRoom=null;

                    ItemMeta meta=endRoomButton.getItemMeta();
                    meta.setLore(ButtonPrimitives.noneLore);
                    endRoomButton.setItemMeta(meta);
                    inventory.setItem(4, endRoomButton);
                }
            );
            case 5->roomsMenu.open();

            case 26->{
                if(startRoom==null||endRoom==null||rooms.isEmpty())return;
                Dungeon dungeon=new Dungeon(
                    roomCount,
                    deathLimit,
                    startRoom,
                    endRoom,
                    Collections.unmodifiableList(rooms),
                    reward,
                    disableStarvation,
                    nextRoomDelay
                );

                DBlRegistry<Dungeon> registry=DungeonBl.registries().dungeons;
                registry.add(name,dungeon);
                registry.save(name);

                player.sendMessage("Saved");
                close();
            }
            case 8->{
                if(!delete)return;

                DBlRegistry<Dungeon> registry=DungeonBl.registries().dungeons;
                registry.remove(name);
                registry.save(name);

                player.sendMessage("Removed");
                close();
            }
        }
    }

    public void updateRoomsButton(){
        ItemMeta meta=roomsButton.getItemMeta();
        meta.setLore(roomsLore(rooms.size()));
        roomsButton.setItemMeta(meta);
        inventory.setItem(5,roomsButton);
    }

    public void updateRewardButton(){
        ItemMeta meta=rewardButton.getItemMeta();
        meta.setLore(rewardLore(reward));
        rewardButton.setItemMeta(meta);
        inventory.setItem(10,rewardButton);
    }

    @Override
    public void opened(){
        doneCheck();
    }
    @Override
    public void closed(){}
}
