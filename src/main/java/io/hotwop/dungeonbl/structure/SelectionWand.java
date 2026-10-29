package io.hotwop.dungeonbl.structure;

import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.menu.other.StructureSaveMenu;
import io.hotwop.dungeonbl.util.math.Box3;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SelectionWand implements Listener{
    private static final ItemStack wandItem=ItemStack.of(Material.STICK);
    public static final NamespacedKey wandId=new NamespacedKey(DungeonBl.namespace,"wand");

    public static final Particle.DustOptions firstParticle=new Particle.DustOptions(Color.BLUE,0.5f);
    public static final Particle.DustOptions secondParticle=new Particle.DustOptions(Color.ORANGE,0.5f);
    public static final Particle.DustOptions selectionParticle=new Particle.DustOptions(Color.WHITE,0.5f);
    public static final Particle.DustOptions airSelectionParticle=new Particle.DustOptions(Color.YELLOW,0.5f);

    static{
        ItemMeta meta=wandItem.getItemMeta();
        meta.setItemName(ChatColor.GOLD+"DBl Selection Wand");
        meta.setLore(List.of(
            ChatColor.GREEN+"LMB"+ChatColor.GRAY+" - First position",
            ChatColor.RED+"RMB"+ChatColor.GRAY+" - Second position",
            ChatColor.YELLOW+"SNEAKING in flight + CLICK"+ChatColor.GRAY+" - Select in air",
            ChatColor.BLUE+"RMB on air"+ChatColor.GRAY+" - Save building"
        ));
        meta.getPersistentDataContainer().set(wandId,PersistentDataType.BOOLEAN,true);
        meta.setEnchantmentGlintOverride(true);
        wandItem.setItemMeta(meta);
    }

    public static ItemStack getWandItem(){
        return wandItem.clone();
    }

    private static final Map<Player,Selection> selections=new HashMap<>();

    private static Selection getSelection(Player pl){
        return selections.computeIfAbsent(pl,p->new Selection());
    }

    public static final class Selection{
        public World world=null;
        public Vec3 first=null;
        public Vec3 second=null;

        public boolean isReady(){
            return first!=null&&second!=null&&world!=null;
        }

        public Box3 getBox(){
            return new Box3(first,second);
        }
    }

    public static void renderTick(){
        for(Map.Entry<Player,Selection> entry:selections.entrySet()){
            Player pl=entry.getKey();
            List<Player> pls=List.of(pl);

            Selection selection=entry.getValue();

            if(selection.first!=null){
                DBlUtil.drawBox(firstParticle,selection.world,pls,Box3.withSize(selection.first,Vec3.ONE));
            }

            if(selection.second!=null){
                DBlUtil.drawBox(secondParticle,selection.world,pls,Box3.withSize(selection.second,Vec3.ONE));
            }

            if(selection.first!=null&&selection.second!=null){
                DBlUtil.drawBox(selectionParticle,selection.world,pls,selection.getBox());
            }
        }

        for(Player player:Bukkit.getOnlinePlayers()){
            if(
                player.isFlying() && player.isSneaking() &&
                player.hasPermission("dungeonbl.wand") &&
                !DungeonBl.isDungeonBlWorld(player.getWorld())
            ){
                ItemStack item=player.getInventory().getItemInMainHand();
                if(item.isEmpty())continue;

                Boolean value = item.getPersistentDataContainer().get(wandId, PersistentDataType.BOOLEAN);
                if (value == null || !value)continue;

                Location loc=player.getEyeLocation();
                loc.add(loc.getDirection().multiply(5));
                Vec3 pos=new Vec3(loc);

                List<Player> pls=List.of(player);

                DBlUtil.drawBox(airSelectionParticle,player.getWorld(),pls,Box3.withSize(pos,Vec3.ONE));
            }
        }
    }

    @EventHandler
    private void blockClick(PlayerInteractEvent e){
        Player pl=e.getPlayer();
        if(!pl.hasPermission("dungeonbl.wand"))return;

        ItemStack item=pl.getInventory().getItemInMainHand();
        if(item.isEmpty())return;

        Boolean value = item.getPersistentDataContainer().get(wandId, PersistentDataType.BOOLEAN);
        if (value == null || !value)return;

        Action action=e.getAction();
        if(action==Action.PHYSICAL)return;

        e.setCancelled(true);
        if(DungeonBl.isDungeonBlWorld(pl.getWorld())){
            pl.sendMessage(ChatColor.RED+"Selection tool don't work in dungeons");
            return;
        }

        Selection selection = getSelection(pl);

        if(pl.isFlying()&&pl.isSneaking()){
            Location loc=pl.getEyeLocation();
            loc.add(loc.getDirection().multiply(5));
            Vec3 pos=new Vec3(loc);

            selection.world=loc.getWorld();
            if(action.isLeftClick()){
                if(pos.equals(selection.first))return;
                selection.first = pos;
                pl.sendMessage(ChatColor.AQUA+"First position set");
            }else if(action.isRightClick()){
                if(pos.equals(selection.second))return;
                selection.second = pos;
                pl.sendMessage(ChatColor.AQUA+"Second position set");
            }
        }else{
            Block block = e.getClickedBlock();
            if (block == null) {
                if(action==Action.RIGHT_CLICK_AIR){
                    if(selection.isReady())new StructureSaveMenu(pl,selection.world,selection.first,selection.second);
                    else{
                        pl.sendMessage(ChatColor.RED+"Nothing to save");
                    }
                }
            } else {
                Location loc = block.getLocation();
                Vec3 pos=new Vec3(loc);

                selection.world=loc.getWorld();
                switch(action){
                    case LEFT_CLICK_BLOCK->{
                        if(pos.equals(selection.first))return;
                        selection.first = pos;
                        pl.sendMessage(ChatColor.AQUA+"First position set");
                    }
                    case RIGHT_CLICK_BLOCK->{
                        if(pos.equals(selection.second))return;
                        selection.second = pos;
                        pl.sendMessage(ChatColor.AQUA+"Second position set");
                    }
                }
            }
        }
    }

    public static void clearSelection(Player player){
        if(selections.remove(player)!=null){
            player.sendMessage("Your selection cleared");
        }
    }

    @EventHandler
    private void playerChangeWorld(PlayerChangedWorldEvent e){
        Player pl=e.getPlayer();
        clearSelection(pl);
    }

    @EventHandler
    private void playerLeave(PlayerQuitEvent e){
        Player pl=e.getPlayer();
        selections.remove(pl);
    }
}
