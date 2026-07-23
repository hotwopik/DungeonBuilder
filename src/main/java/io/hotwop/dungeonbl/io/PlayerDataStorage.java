package io.hotwop.dungeonbl.io;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.versioned.DBlAttributeMapping;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class PlayerDataStorage{
    public final Path file;
    private final Map<UUID,PlayerData> map=new HashMap<>();

    private volatile boolean busy=false;
    private final Object busySignal=new Object();

    public PlayerDataStorage(Path file){
        this.file=file;
    }

    public PlayerData join(Player player){
        return map.computeIfAbsent(
            player.getUniqueId(),
            k->new PlayerData(null,null,null,new ArrayList<>())
        );
    }

    public PlayerData get(Player player){
        UUID uuid=player.getUniqueId();
        if(!map.containsKey(uuid))throw new NullPointerException("Requiring unknown player");
        return map.get(uuid);
    }

    public void save(){
        lockBusy();

        try{
            ReadWriteNBT out=encode();
            try{
                NBT.writeFile(file.toFile(),out);
            }catch(IOException e){
                logger.warn("Error to save Player data",e);
            }
        }catch(RuntimeException e){
            logger.warn("Unexpected error while saving Player Data",e);
        }

        unlockBusy();
    }

    public void load(){
        lockBusy();

        ReadWriteNBT in;
        try{
            in=NBT.readFile(file.toFile());
        }catch(IOException e){
            logger.warn("Error to load Player data",e);
            in=null;
        }

        if(in!=null)decode(in);

        unlockBusy();
    }

    public static final AttributeModifier noneModifier=new AttributeModifier(new NamespacedKey(DungeonBl.namespace,"transfer_none"),-1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);

    public void beginDungeon(Player player,boolean saveFood){
        if(!player.isOnline())return;

        GameMode gameMode=player.getGameMode();
        Location location=player.getLocation();

        PlayerData playerData=get(player);

        if(saveFood){
            int food=player.getFoodLevel();
            float saturation=player.getSaturation();
            playerData.setFoodData(food,saturation);
        }

        playerData.setDungeonData(gameMode,location);

        player.setGameMode(GameMode.ADVENTURE);
    }

    public void endDungeon(Player player, @Nullable Location to, Runnable afterTeleport){
        if(!player.isOnline())return;

        PlayerData playerData=get(player);
        PlayerData.DungeonData dungeonData=playerData.getAndDeleteDungeonData();
        PlayerData.FoodData foodData=playerData.getAndDeleteFoodData();

        if(dungeonData!=null){
            if(to==null&&dungeonData.worldName()!=null){
                World world=Bukkit.getWorld(dungeonData.worldName());
                if(world==null){
                    logger.warn("Error to return player {} to initial location, world {} not found",player.getName(),dungeonData.worldName());
                    player.setGameMode(dungeonData.gameMode());
                    return;
                }

                dungeonData.returnTo()
                    .setWorld(world);
            }

            DungeonBl.platformed()
                .teleport(player,to==null?dungeonData.returnTo():to,()->{
                    player.setGameMode(dungeonData.gameMode());
                    if(foodData!=null){
                        player.setFoodLevel(foodData.food());
                        player.setSaturation(foodData.saturation());
                    }
                    afterTeleport.run();
                },()->{
                    playerData.setDungeonData(dungeonData.gameMode(),dungeonData.returnTo());
                    if(foodData!=null)playerData.setFoodData(foodData.food(), foodData.saturation());
                    afterTeleport.run();
                });
        }

        playerData.setDungeonSession(null);
    }

    public void endDungeonOnDeath(PlayerRespawnEvent event){
        Player player=event.getPlayer();
        if(!player.isDead())return;

        PlayerData playerData=get(player);
        PlayerData.DungeonData dungeonData=playerData.getAndDeleteDungeonData();
        if(dungeonData!=null){
            player.setGameMode(dungeonData.gameMode());
            event.setRespawnLocation(dungeonData.returnTo());
        }

        playerData.setDungeonSession(null);
    }

    public void beginTransfer(Player player){
        if(!player.isOnline())return;

        PlayerData playerData=get(player);
        if(playerData.hasTransferData())return;

        boolean invisible=player.isInvisible();
        boolean invulnerable=player.isInvulnerable();

        playerData.setTransferData(invisible,invulnerable);

        player.setInvisible(true);
        player.setInvulnerable(true);

        try{
            DBlAttributeMapping attributeMapping=DungeonBl.versioned().getAttributeMapping();
            player.getAttribute(attributeMapping.attackDamage()).addModifier(noneModifier);
            player.getAttribute(attributeMapping.movementSpeed()).addModifier(noneModifier);
            player.getAttribute(attributeMapping.jumpStrength()).addModifier(noneModifier);
        }catch(IllegalArgumentException|NullPointerException ignore){}
    }

    public void endTransfer(Player player){
        if(!player.isOnline())return;

        PlayerData playerData=get(player);
        PlayerData.TransferData transferData=playerData.getAndDeleteTransferData();

        if(transferData!=null){
            player.setInvisible(transferData.invisible());
            player.setInvulnerable(transferData.invulnerable());

            try{
                DBlAttributeMapping attributeMapping=DungeonBl.versioned().getAttributeMapping();
                player.getAttribute(attributeMapping.attackDamage()).removeModifier(noneModifier);
                player.getAttribute(attributeMapping.movementSpeed()).removeModifier(noneModifier);
                player.getAttribute(attributeMapping.jumpStrength()).removeModifier(noneModifier);
            }catch(IllegalArgumentException|NullPointerException ignore){}
        }
    }

    private ReadWriteNBT encode(){
        ReadWriteNBT out=NBT.createNBTObject();
        for(Map.Entry<UUID,PlayerData> entry: map.entrySet()){
            ReadWriteNBT playerOut=out.getOrCreateCompound(entry.getKey().toString());
            entry.getValue().encode(playerOut);
        }

        return out;
    }

    private void decode(ReadableNBT nbt){
        for(String key:nbt.getKeys()){
            ReadableNBT playerEl=nbt.getCompound(key);
            if(playerEl==null)continue;

            UUID uuid;
            try{
                uuid=UUID.fromString(key);
            }catch(IllegalArgumentException e){
                continue;
            }

            PlayerData playerData=PlayerData.decode(playerEl);
            map.put(uuid,playerData);
        }
    }

    private void lockBusy(){
        waitBusy();
        synchronized(this){
            busy=true;
        }
    }

    private void unlockBusy(){
        synchronized(this){
            busy=false;
        }
        synchronized (busySignal) {
            busySignal.notifyAll();
        }
    }

    private void waitBusy(){
        while(true){
            synchronized(this){
                if(!busy)return;
            }

            synchronized(busySignal) {
                try {
                    busySignal.wait(100);
                } catch (InterruptedException ignore) {}
            }
        }
    }
}