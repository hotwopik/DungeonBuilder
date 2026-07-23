package io.hotwop.dungeonbl.mark;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.DBlUtil;

import io.hotwop.dungeonbl.mark.type.*;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MarkType{
    public static final List<MarkType> standardTypes=List.of(
        Tools.remover,Tools.rotator,
        DungeonEntrance.type,DungeonChest.type,
        BuildingOrigin.type,
        RoomEntrance.type,RoomExit.type,
        PlayerSpawn.type,
        RedstoneTrigger.type,
        EffectMark.type,
        MobSpawn.type,
        BossSpawn.type,
        DecorationSpawn.type
    );

    private static final Map<String,MarkType> types=new HashMap<>();
    public static final String spawnField=DungeonBl.namespace+"_spawn";

    public static @Nullable MarkType getType(String type){
        return types.get(type);
    }

    public static void register(MarkType type){
        if(type.registered)throw new RuntimeException("This type already registered");
        if(types.containsKey(type.name))throw new RuntimeException("Error to register MarkType: type with that name already registered");
        types.put(type.name,type);
        type.registered=true;
    }

    public final String name;
    public final ItemType item;
    public final String displayName;
    private boolean registered=false;

    public final boolean registered(){
        return registered;
    }

    protected MarkType(String name,ItemType item,String displayName){
        this.name=name;
        this.item=item;
        this.displayName=displayName;
    }

    public abstract void place(Location location, ReadableNBT data);
    public abstract CommandAPICommand createCommand();

    public record ItemData(
        ReadableNBT nbtData,
        List<String> lore
    ){}

    @Nullable
    public ItemData createItemData(CommandSender ctx, CommandArguments args) throws WrapperCommandSyntaxException{
        return null;
    }

    @SuppressWarnings("deprecation")
    public int giveCommand(Player ctx, CommandArguments args) throws WrapperCommandSyntaxException{
        ItemStack out=item.createItemStack();

        ItemData data=createItemData(ctx,args);
        NBT.modify(out,nbt->{
            ReadWriteNBT spawnData=nbt.getOrCreateCompound(spawnField);
            spawnData.setString("type",name);
            if(data!=null)spawnData.set("data",data.nbtData,NBTHandlers.STORE_READABLE_TAG);
        });

        ItemMeta meta=out.getItemMeta();
        meta.setItemName(displayName);
        if(data!=null)meta.setLore(data.lore);
        out.setItemMeta(meta);

        return DBlUtil.giveIfCan(ctx,out)?1:0;
    }

    @SuppressWarnings("unchecked")
    public static <T extends MarkType> T asExtend(Class<T> clazz,MarkType type){
        if(type.getClass().equals(clazz))return (T)type;
        throw new RuntimeException("Required MarkType of class "+clazz.getName()+" but given "+type.getClass().getName());
    }

    public static CommandAPICommand collectCommand(){
        CommandAPICommand cmd=new CommandAPICommand("item");
        cmd.withSubcommands(types.values().stream()
            .map(MarkType::createCommand)
            .toArray(CommandAPICommand[]::new)
        );
        return cmd;
    }
}
