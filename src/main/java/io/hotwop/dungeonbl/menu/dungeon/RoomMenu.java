package io.hotwop.dungeonbl.menu.dungeon;

import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.config.Room;
import io.hotwop.dungeonbl.menu.ButtonPrimitives;
import io.hotwop.dungeonbl.menu.AbstractSingleInventory;
import io.hotwop.dungeonbl.menu.list.ListedMenu;
import io.hotwop.dungeonbl.menu.list.MultiListedMenu;
import io.hotwop.dungeonbl.mob.Boss;
import io.hotwop.dungeonbl.mob.CrossMob;
import io.hotwop.dungeonbl.mob.CrossMobList;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public final class RoomMenu extends AbstractSingleInventory {
    public static final List<String> notSetLore=List.of(ChatColor.GRAY+"Click to set");

    public static final Set<Material> roomBuildingIcons=Set.of(
        Material.BRICKS,Material.STONE_BRICKS,Material.CHISELED_STONE_BRICKS, Material.MOSSY_STONE_BRICKS,
        Material.CHISELED_TUFF_BRICKS,Material.TUFF_BRICKS
    );
    public static final Set<Material> decorationIcons=Set.of(
        Material.DECORATED_POT,Material.PAINTING,Material.OAK_FENCE
    );

    private final ItemStack endsWithPlayersButton=ItemStack.of(Material.PLAYER_HEAD);
    private final ItemStack endsWithClearButton=ItemStack.of(Material.ZOMBIE_SPAWN_EGG);
    private final ItemStack endsOnSignalButton=ItemStack.of(Material.PAPER);
    private final ItemStack nextRoomDelayButton=ItemStack.of(Material.CLOCK);

    private final ItemStack mobsButton = ItemStack.of(Material.ZOMBIE_HEAD);
    private final ItemStack bossesButton = ItemStack.of(Material.GOLD_INGOT);
    private final ItemStack wavesButton = ItemStack.of(Material.PAPER);

    private final ItemStack roomBuildingButton = ItemStack.of(Material.BRICKS);
    private final ItemStack decorationsButton = ItemStack.of(Material.DECORATED_POT);

    public boolean endsWithPlayers;
    public boolean endsWithClear;
    public String endsOnSignal=null;
    public Integer endSignalValue;
    public Integer nextRoomDelay=null;
    public String building=null;
    public List<List<CrossMob>> mobs=new ArrayList<>();
    public List<Boss> bosses=new ArrayList<>();
    public int waves=0;
    public List<String> decorations=new ArrayList<>();

    public final BossesMenu bossesMenu;
    public final PoolsMenu poolsMenu;
    public final SignalMenu endSignalMenu;

    private final Runnable openParent;
    private final Consumer<Room> set;
    private final Runnable del;

    private final boolean delete;

    public RoomMenu(Player player, @Nullable Room room,Runnable openParent,Consumer<Room> set,Runnable del){
        super("Edit Room",3,player);
        if(!player.hasPermission("dungeonbl.command.dungeon"))throw
            new RuntimeException("Permission exception");
        this.openParent = openParent;
        this.set = set;
        this.del = del;

        if(room!=null){
            endsWithPlayers=room.endsWithPlayers();
            endsWithClear=room.endsWithClear();
            endsOnSignal=room.endsOnSignal();
            endSignalValue=room.endSignalValue();

            nextRoomDelay=room.nextRoomDelay();

            building=room.building();

            CrossMobList originMobs=room.mobs();
            if(originMobs!=null)originMobs.list.forEach(st->mobs.add(new ArrayList<>(st)));

            List<Boss> originBosses=room.bosses();
            if(originBosses!=null)bosses.addAll(originBosses);

            waves=room.waves();

            List<String> originDecorations=room.decorations();
            if(originDecorations!=null)decorations.addAll(originDecorations);

            final ItemMeta structureMeta = roomBuildingButton.getItemMeta();
            structureMeta.setItemName(buildingName());
            roomBuildingButton.setItemMeta(structureMeta);

            inventory.setItem(26,ButtonPrimitives.doneButton());
            inventory.setItem(8,ButtonPrimitives.deleteButton());

            delete=true;
        }else{
            final ItemMeta structureMeta = roomBuildingButton.getItemMeta();
            structureMeta.setItemName(buildingName());
            roomBuildingButton.setItemMeta(structureMeta);

            inventory.setItem(26,ButtonPrimitives.notDoneButton());
            delete=false;
        }

        bossesMenu=new BossesMenu(this);
        poolsMenu=new PoolsMenu(this);
        endSignalMenu=new SignalMenu(this);

        ItemMeta ewpMeta=endsWithPlayersButton.getItemMeta();
        ewpMeta.setRarity(ItemRarity.COMMON);
        ewpMeta.setItemName(endsWithPlayersName());
        endsWithPlayersButton.setItemMeta(ewpMeta);

        ItemMeta ewcMeta=endsWithClearButton.getItemMeta();
        ewcMeta.setItemName(endsWithClearName());
        endsWithClearButton.setItemMeta(ewcMeta);

        ItemMeta eosMeta=endsOnSignalButton.getItemMeta();
        eosMeta.setItemName(endsOnSignalName());
        eosMeta.setLore(endsOnSignalLore());
        endsOnSignalButton.setItemMeta(eosMeta);

        ItemMeta nextRoomDelayMeta=nextRoomDelayButton.getItemMeta();
        nextRoomDelayMeta.setItemName(nextRoomDelayName());
        nextRoomDelayMeta.setLore(nextRoomDelayLore());
        nextRoomDelayButton.setItemMeta(nextRoomDelayMeta);

        ItemMeta mobsMeta = mobsButton.getItemMeta();
        mobsMeta.setItemName("Mobs");
        int mobsSize=0;
        for(List<CrossMob> mb:mobs)mobsSize+=mb.size();
        mobsMeta.setLore(mobsLore(mobsSize));
        mobsMeta.setRarity(ItemRarity.COMMON);
        mobsButton.setItemMeta(mobsMeta);

        ItemMeta bossesMeta = bossesButton.getItemMeta();
        bossesMeta.setItemName("Bosses");
        bossesMeta.setLore(bossesLore(bosses.size()));
        bossesButton.setItemMeta(bossesMeta);

        ItemMeta wavesMeta =wavesButton.getItemMeta();
        wavesMeta.setItemName(wavesName());
        wavesMeta.setLore(ButtonPrimitives.numberControllerLore);
        wavesButton.setItemMeta(wavesMeta);

        ItemMeta decorationsMeta = decorationsButton.getItemMeta();
        decorationsMeta.setItemName("Decorations");
        decorationsMeta.setLore(decorationsLore(decorations.size()));
        decorationsButton.setItemMeta(decorationsMeta);

        inventory.setItem(0,endsWithPlayersButton);
        inventory.setItem(9,endsWithClearButton);
        inventory.setItem(10,endsOnSignalButton);

        inventory.setItem(18,nextRoomDelayButton);

        inventory.setItem(1,roomBuildingButton);
        inventory.setItem(5,decorationsButton);

        inventory.setItem(3,mobsButton);
        inventory.setItem(12,wavesButton);

        inventory.setItem(4,bossesButton);

        inventory.setItem(17,ButtonPrimitives.cancelButton());

        open();
    }

    public String wavesName(){
        return "Waves: "+ChatColor.YELLOW+waves;
    }

    public String endsWithPlayersName(){
        return "Ends with Players: "+ButtonPrimitives.booleanSwitch(endsWithPlayers);
    }

    public String endsWithClearName(){
        return "Ends with Clear: "+ButtonPrimitives.booleanSwitch(endsWithClear);
    }

    public String endsOnSignalName(){
        return "Ends on Signal: "+ButtonPrimitives.mayNotSpecify(endsOnSignal);
    }

    public List<String> endsOnSignalLore(){
        return List.of(
            ChatColor.GRAY+"Signal Value: "+ButtonPrimitives.mayNotSpecify(endSignalValue==null?null:String.valueOf(endSignalValue))
        );
    }

    public String nextRoomDelayName(){
        return "Next Room Delay: "+ChatColor.YELLOW+(nextRoomDelay==null?"Inherit":(nextRoomDelay==0?"None":(nextRoomDelay+"ticks")));
    }

    public List<String> nextRoomDelayLore(){
        if(nextRoomDelay==null)return notSetLore;
        return ButtonPrimitives.numberControllerLore;
    }

    public String buildingName(){
        return "Building: "+ButtonPrimitives.mayNotSpecify(building);
    }

    public void click(int slot, ClickType clickType, ItemStack cursorItem) {
        if(cursorItem.getType()!=Material.AIR)return;
        switch(slot){
            case 0->{
                if(!hasEndCondition()&&building!=null)inventory.setItem(26,ButtonPrimitives.doneButton());
                endsWithPlayers=!endsWithPlayers;
                if(!hasEndCondition())inventory.setItem(26,ButtonPrimitives.notDoneButton());

                ItemMeta ewpMeta=endsWithPlayersButton.getItemMeta();
                ewpMeta.setItemName(endsWithPlayersName());
                endsWithPlayersButton.setItemMeta(ewpMeta);

                inventory.setItem(0,endsWithPlayersButton);
            }
            case 9->{
                if(!hasEndCondition()&&building!=null)inventory.setItem(26,ButtonPrimitives.doneButton());
                endsWithClear=!endsWithClear;
                if(!hasEndCondition())inventory.setItem(26,ButtonPrimitives.notDoneButton());

                ItemMeta ewcMeta=endsWithClearButton.getItemMeta();
                ewcMeta.setItemName(endsWithClearName());
                endsWithClearButton.setItemMeta(ewcMeta);

                inventory.setItem(9,endsWithClearButton);
            }
            case 10->endSignalMenu.open();
            case 18->{
                if(nextRoomDelay==null)nextRoomDelay=0;
                else{
                    if(clickType.isLeftClick())++nextRoomDelay;
                    else if(clickType.isRightClick()){
                        if(nextRoomDelay==0)nextRoomDelay=null;
                        else --nextRoomDelay;
                    }
                }

                ItemMeta nextRoomDelayMeta=nextRoomDelayButton.getItemMeta();
                nextRoomDelayMeta.setItemName(nextRoomDelayName());
                nextRoomDelayMeta.setLore(nextRoomDelayLore());
                nextRoomDelayButton.setItemMeta(nextRoomDelayMeta);

                inventory.setItem(18,nextRoomDelayButton);
            }

            case 1->new ListedMenu<>(
                player,
                roomBuildingIcons,
                DungeonBl.registries()
                    .roomBuildings.idSet().stream()
                    .sorted(Comparator.naturalOrder())
                    .toList(),
                "Room Buildings",
                name->name,
                null,
                this::open,
                (listed)->{
                    if(building==null&&hasEndCondition())inventory.setItem(26,ButtonPrimitives.doneButton());
                    building =listed;

                    final ItemMeta structureMeta = roomBuildingButton.getItemMeta();
                    structureMeta.setItemName(buildingName());
                    roomBuildingButton.setItemMeta(structureMeta);
                    inventory.setItem(1, roomBuildingButton);
                }
            );

            case 3->poolsMenu.open();
            case 4->bossesMenu.open();
            case 5->new MultiListedMenu<>(
                player,
                decorationIcons,
                Material.GREEN_STAINED_GLASS_PANE,
                DungeonBl.registries()
                    .decorations.idSet().stream()
                    .sorted(Comparator.naturalOrder())
                    .toList(),
                decorations,
                "Decorations",
                name->name,
                null,
                this::open,
                (listed)->{
                    decorations=listed;
                    final ItemMeta decorationsMeta = decorationsButton.getItemMeta();
                    decorationsMeta.setLore(decorationsLore(decorations.size()));
                    decorationsButton.setItemMeta(decorationsMeta);
                    inventory.setItem(5, decorationsButton);
                }
            );
            case 12->{
                if(clickType.isLeftClick())++waves;
                else if(waves!=0&&clickType.isRightClick())--waves;
                else return;

                final ItemMeta wavesMeta =wavesButton.getItemMeta();
                wavesMeta.setItemName(wavesName());
                wavesButton.setItemMeta(wavesMeta);
                inventory.setItem(12,wavesButton);
            }
            case 26->{
                if(building ==null||!hasEndCondition())return;
                set.accept(new Room(
                    endsWithPlayers,
                    endsWithClear,

                    endsOnSignal,
                    endSignalValue,

                    nextRoomDelay,

                    building,
                    Collections.unmodifiableList(decorations),

                    new CrossMobList(mobs),
                    waves,
                    Collections.unmodifiableList(bosses)
                ));
                close();
                openParent.run();
            }
            case 17->{
                close();
                openParent.run();
            }
            case 8->{
                if(!delete)return;
                del.run();
                close();
                openParent.run();
            }
        }
    }

    @Override
    public void closed(){}

    @Override
    public void opened(){}

    public boolean hasEndCondition(){
        return endsWithPlayers||endsWithClear||endsOnSignal!=null;
    }

    public void updateBossButton(){
        ItemMeta meta=bossesButton.getItemMeta();
        meta.setLore(bossesLore(bosses.size()));
        bossesButton.setItemMeta(meta);
        inventory.setItem(4, bossesButton);
    }

    public void updateMobsButton(){
        final ItemMeta mobsMeta = mobsButton.getItemMeta();
        int mobsSize=0;
        for(List<CrossMob> mb:mobs)mobsSize+=mb.size();

        mobsMeta.setLore(mobsLore(mobsSize));
        mobsButton.setItemMeta(mobsMeta);
        inventory.setItem(3, mobsButton);
    }

    public void updateEndsOnSignalButton(){
        if(hasEndCondition()){
            if(building!=null)inventory.setItem(26,ButtonPrimitives.doneButton());
        }else{
            inventory.setItem(26,ButtonPrimitives.notDoneButton());
        }

        if(endsOnSignal==null)endSignalValue=null;

        ItemMeta eosMeta=endsOnSignalButton.getItemMeta();
        eosMeta.setItemName(endsOnSignalName());
        eosMeta.setLore(endsOnSignalLore());
        endsOnSignalButton.setItemMeta(eosMeta);

        inventory.setItem(10,endsOnSignalButton);
    }

    public static List<String> bossesLore(int size){
        return List.of(
            ChatColor.GRAY+"Bosses: "+ChatColor.YELLOW+size
        );
    }
    public static List<String> mobsLore(int size){
        return List.of(
            ChatColor.GRAY+"Mobs: "+ChatColor.YELLOW+size
        );
    }
    public static List<String> decorationsLore(int size){
        return List.of(
            ChatColor.GRAY+"Decorations: "+ChatColor.YELLOW+size
        );
    }
}
