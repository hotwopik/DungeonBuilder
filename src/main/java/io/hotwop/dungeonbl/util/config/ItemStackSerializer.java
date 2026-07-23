package io.hotwop.dungeonbl.util.config;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.Scalars;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public final class ItemStackSerializer implements TypeSerializer<ItemStack>{
    public static final ItemStackSerializer instance=new ItemStackSerializer();
    private ItemStackSerializer(){}

    public ItemStack deserialize(@NotNull Type typ, @NotNull ConfigurationNode node) throws SerializationException{
        ConfigurationNode itemNode=node.node("item");
        String itemStr=itemNode.getString();
        if(itemStr==null||itemStr.isEmpty())throw
            new SerializationException(itemNode,typ,"Item string expected (like apple[item_name=\"Not Apple\"])");

        ItemStack out;
        try{
            out=Bukkit.getItemFactory()
                .createItemStack(itemStr);
        }catch(IllegalArgumentException e){
            throw new SerializationException(itemNode,typ,"Malformed item string",e);
        }

        ConfigurationNode countNode=node.node("count");
        int count;
        if(countNode.virtual()||countNode.empty()){
            count=1;
        }else{
            Object countValue=countNode.rawScalar();
            if(countValue==null)throw new SerializationException(countNode,typ,"Malformed item count");

            count=Scalars.INTEGER.deserialize(countValue);
            if(count<1)throw new SerializationException(countNode,typ,"Item count should be positive");
        }

        out.setAmount(count);
        return out;
    }

    public void serialize(@NotNull Type typ, @Nullable ItemStack obj, @NotNull ConfigurationNode node) throws SerializationException{
        if(obj==null)return;
        node.node("item").set(obj.getType().getKey().toString()+obj.getItemMeta().getAsComponentString());
        node.node("count").set(obj.getAmount());
    }
}
