package io.hotwop.dungeonbl;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import io.hotwop.dungeonbl.io.PlayerData;
import io.hotwop.dungeonbl.io.PlayerDataStorage;
import io.hotwop.dungeonbl.mark.Mark;
import io.hotwop.dungeonbl.mark.MarkStorage;
import io.hotwop.dungeonbl.mark.MarkType;
import io.hotwop.dungeonbl.mark.type.DungeonChest;
import io.hotwop.dungeonbl.menu.AbstractInventory;
import io.hotwop.dungeonbl.menu.AbstractView;
import io.hotwop.dungeonbl.menu.other.DungeonRewardMenu;
import io.hotwop.dungeonbl.session.DungeonSession;
import io.hotwop.dungeonbl.session.DungeonSuccess;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class DBlEventListener implements Listener{
    @EventHandler
    public void playerJoin(PlayerJoinEvent event){
        Player player=event.getPlayer();
        PlayerDataStorage playerDataStorage=DungeonBl.playerDataStorage();
        PlayerData playerData=playerDataStorage.join(player);

        DungeonSession session=playerData.getDungeonSession();

        if(session==null){
            playerDataStorage.endTransfer(player);
            playerDataStorage.endDungeon(player,null,()->{});
        }else{
            DungeonSuccess status=session.status();
            if(status==DungeonSuccess.CONTINUOUS){
                session.afterTransfer(player);
            }else{
                playerDataStorage.endTransfer(player);
                playerDataStorage.endDungeon(
                    player,
                    status==DungeonSuccess.SUCCESS?session.to:session.from,
                    ()->{}
                );
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        HumanEntity player=event.getWhoClicked();
        Inventory inventory=event.getInventory();

        if(inventory!=null && (inventory.getHolder() instanceof AbstractInventory holder)){
            Inventory clicked=event.getClickedInventory();
            if(clicked==null||!clicked.equals(inventory)){
                if(event.isShiftClick())event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            holder.click(event.getSlot(),event.getClick(),event.getCursor(),player);
        }

        AbstractView<?> view=AbstractView.get(player);
        if(view!=null){
            Inventory clicked=event.getClickedInventory();
            if(clicked==null||!clicked.equals(inventory)){
                if(event.isShiftClick())event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            view.click(event.getSlot(),event.getClick(),event.getCursor());
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event){
        HumanEntity player=event.getPlayer();
        Inventory inventory=event.getInventory();

        if(inventory!=null && (inventory.getHolder() instanceof AbstractInventory holder)){
            holder.opened(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event){
        HumanEntity player=event.getPlayer();
        Inventory inventory=event.getInventory();

        if(inventory!=null && (inventory.getHolder() instanceof AbstractInventory holder)){
            holder.closed(player);
        }

        AbstractView<?> view=AbstractView.get(player);
        if(view!=null){
            view.onClose();
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void itemUse(PlayerInteractEvent event) {
        if(event.getAction()!=Action.RIGHT_CLICK_BLOCK)return;

        Player player=event.getPlayer();
        Location clickLocation=event.getClickedBlock().getLocation();
        if((!player.isSneaking() || !event.hasItem())){
            Mark<?> mark= MarkStorage.getMark(clickLocation);
            if(mark instanceof DungeonChest chest){
                event.setCancelled(true);
                new DungeonRewardMenu(player,chest.dungeon);
                return;
            }
        }
        if (!event.hasItem()) return;

        World world=clickLocation.getWorld();
        if(DungeonBl.isDungeonBlWorld(world))return;

        ItemStack item=event.getItem();
        if(item==null)return;

        NBT.get(item,nbt->{
            ReadableNBT spawnData=nbt.getCompound(MarkType.spawnField);
            if(spawnData==null||!spawnData.hasTag("type", NBTType.NBTTagString))return;

            String typeName=spawnData.getString("type");
            MarkType type=MarkType.getType(typeName);
            if(type==null)return;

            event.setCancelled(true);
            clickLocation.add(event.getBlockFace().getDirection());

            ReadableNBT data=spawnData.getCompound("data");
            type.place(clickLocation,data);
        });
    }

    @EventHandler
    public void worldLoad(WorldLoadEvent event){
        if(!DungeonBl.loadingEnded)return;
        MarkStorage.loadWorld(event.getWorld());
    }
    @EventHandler
    public void worldUnload(WorldUnloadEvent event){
        if(!DungeonBl.loadingEnded)return;
        MarkStorage.saveWorld(event.getWorld());
    }

    @EventHandler
    public void chunkLoad(ChunkLoadEvent event){
        List<Mark<?>> marks= MarkStorage.getMarks(event.getChunk());
        if(marks!=null)marks.forEach(Mark::startTicking);
    }

    @EventHandler
    public void chunkUnload(ChunkUnloadEvent event){
        List<Mark<?>> marks= MarkStorage.getMarks(event.getChunk());
        if(marks!=null)marks.forEach(Mark::stopTicking);
    }
}