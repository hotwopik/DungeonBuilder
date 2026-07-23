package io.hotwop.dungeonbl.io;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.*;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.session.DungeonSession;
import io.hotwop.dungeonbl.util.math.Vec3d;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PlayerData{
    private DungeonData dungeonData;
    private TransferData transferData;
    private DungeonSession dungeonSession;
    private FoodData foodData;
    private final List<ItemStack> rewards;

    public record TransferData(
        boolean invisible,
        boolean invulnerable
    ){}
    public record DungeonData(
        GameMode gameMode,
        Location returnTo,
        @Nullable String worldName
    ){}
    public record FoodData(
        int food,
        float saturation
    ){}

    public PlayerData(DungeonData dungeonData, TransferData transferData, FoodData foodData, List<ItemStack> rewards){
        this.dungeonData=dungeonData;
        this.dungeonSession=null;
        this.transferData = transferData;
        this.foodData=foodData;
        this.rewards = new ArrayList<>(rewards);
    }

    public DungeonData getAndDeleteDungeonData(){
        DungeonData dData=dungeonData;
        dungeonData=null;
        return dData;
    }

    public void setDungeonData(GameMode gameMode,Location returnTo){
        dungeonData=new DungeonData(gameMode,returnTo,null);
    }

    public FoodData getAndDeleteFoodData(){
        FoodData f=foodData;
        foodData=null;
        return f;
    }

    public void setFoodData(int food,float saturation){
        foodData=new FoodData(food,saturation);
    }

    public void setDungeonSession(DungeonSession dungeonSession){
        this.dungeonSession = dungeonSession;
    }

    public @Nullable DungeonSession getDungeonSession(){
        return dungeonSession;
    }

    public @Nullable TransferData getAndDeleteTransferData(){
        TransferData trData=transferData;
        transferData=null;
        return trData;
    }

    public boolean hasTransferData(){
        return transferData!=null;
    }

    public void setTransferData(boolean invisible, boolean invulnerable){
        this.transferData = new TransferData(invisible,invulnerable);
    }

    public List<ItemStack> getRewards(){
        return rewards;
    }

    public void addRewards(Collection<ItemStack> rewards){
        this.rewards.addAll(rewards);
    }

    public void encode(ReadWriteNBT nbt){
        if(dungeonData!=null){
            ReadWriteNBT dungeon=nbt.getOrCreateCompound("dungeon");
            dungeon.setInteger("gamemode",dungeonData.gameMode.getValue());

            ReadWriteNBTList<Double> returnPos=dungeon.getDoubleList("pos");
            DBlUtil.writeVec3d(new Vec3d(dungeonData.returnTo),returnPos);

            ReadWriteNBTList<Float> returnRot=dungeon.getFloatList("rotation");
            DBlUtil.writeLocRotation(dungeonData.returnTo,returnRot);

            dungeon.setString("world",dungeonData.returnTo.getWorld().getName());
        }

        if(transferData!=null){
            ReadWriteNBT transfer=nbt.getOrCreateCompound("transfer");
            transfer.setBoolean("invisible",transferData.invisible);
            transfer.setBoolean("invulnerable",transferData.invulnerable);
        }

        if(foodData!=null){
            ReadWriteNBT food=nbt.getOrCreateCompound("food");
            food.setInteger("food", foodData.food);
            food.setFloat("saturation", foodData.saturation);
        }

        ReadWriteNBTCompoundList itemList=nbt.getCompoundList("rewards");
        for(ItemStack reward:rewards){
            ReadWriteNBT itemEl=itemList.addCompound();
            itemEl.setString("type",reward.getType().getKey().toString());
            itemEl.setInteger("amount",reward.getAmount());
            NBT.getComponents(reward,el->{
                itemEl.set("components",el,NBTHandlers.STORE_READABLE_TAG);
            });
        }
    }

    public static PlayerData decode(ReadableNBT nbt){
        DungeonData dungeonData;
        ReadableNBT dungeon=nbt.getCompound("dungeon");
        if(dungeon!=null){
            Integer gameModeValue=dungeon.getInteger("gamemode");
            ReadableNBTList<Double> pos=dungeon.getDoubleList("pos");
            ReadableNBTList<Float> rotation=dungeon.getFloatList("rotation");
            String worldName=dungeon.getString("world");

            if(gameModeValue!=null&&pos!=null&&rotation!=null&&worldName!=null){
                Vec3d posVec= DBlUtil.readVec3d(pos);
                if(posVec!=null){
                    Location location=posVec.toLocation(null);
                    GameMode gameMode=GameMode.getByValue(gameModeValue);
                    if(gameMode!=null)dungeonData=new DungeonData(gameMode,location,worldName);
                    else dungeonData=null; // Cool stairs :з
                }else dungeonData=null;
            }else dungeonData=null;
        }else dungeonData=null;

        TransferData transferData;
        ReadableNBT transfer=nbt.getCompound("transfer");
        if(transfer!=null){
            Boolean invisible=transfer.getBoolean("invisible");
            Boolean invulnerable=transfer.getBoolean("invulnerable");
            if(invisible!=null&&invulnerable!=null)transferData=new TransferData(invisible,invulnerable);
            else transferData=null;
        }else transferData=null;

        FoodData foodData;
        ReadableNBT foodNBT=nbt.getCompound("food");
        if(foodNBT!=null){
            Integer food=foodNBT.getInteger("food");
            Float saturation=foodNBT.getFloat("saturation");

            if(food!=null)foodData=new FoodData(food,saturation==null?0:saturation);
            else foodData=null;
        }else foodData=null;

        ReadableNBTList<ReadWriteNBT> rewardsList=nbt.getCompoundList("rewards");
        List<ItemStack> rewards=new ArrayList<>();
        for(ReadWriteNBT itemEl:rewardsList){
            String type=itemEl.getString("type");
            int amount=itemEl.getInteger("amount");
            ReadableNBT components=itemEl.getCompound("components");

            if(type!=null&&!type.isEmpty()&&amount>0){
                NamespacedKey itemKey=NamespacedKey.fromString(type);
                if(itemKey==null)continue;

                ItemType itemType=Registry.ITEM.get(itemKey);
                if(itemType==null)continue;

                ItemStack stack=itemType.createItemStack(amount);
                NBT.modifyComponents(stack,comp->{
                    comp.clearNBT();
                    comp.mergeCompound(components);
                });

                rewards.add(stack);
            }
        }

        return new PlayerData(dungeonData,transferData,foodData,rewards);
    }
}
