package io.hotwop.dungeonbl.mark.codec.serializer;

import de.tr7zw.changeme.nbtapi.iface.NBTHandler;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.PotionEffectArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import io.hotwop.dungeonbl.mark.codec.MarkDataSerializer;
import io.hotwop.dungeonbl.mark.codec.MarkSerializeException;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffectType;

public final class PotionEffectTypeSerializer implements MarkDataSerializer<PotionEffectType,PotionEffectType,String>{
    public static final PotionEffectTypeSerializer instance=new PotionEffectTypeSerializer();
    private PotionEffectTypeSerializer(){}

    @Override
    public NBTHandler<String> getHandler() {
        return StringSerializer.stringHandler;
    }

    @Override
    public PotionEffectType fromNBT(String data) throws MarkSerializeException {
        NamespacedKey key=NamespacedKey.fromString(data);
        if(key==null)throw new MarkSerializeException("Namespaced key is invalid: "+data);
        PotionEffectType out=Registry.POTION_EFFECT_TYPE.get(key);
        if(out==null)throw new MarkSerializeException("Unknown potion effect: "+data);
        return out;
    }

    @Override
    public String toNBT(PotionEffectType obj) throws MarkSerializeException {
        return Registry.POTION_EFFECT_TYPE.getKeyOrThrow(obj).asMinimalString();
    }

    @Override
    public Argument<PotionEffectType> getArgument(String name) {
        return new PotionEffectArgument(name);
    }

    @Override
    public PotionEffectType processArgument(PotionEffectType obj, CommandSender ctx) throws WrapperCommandSyntaxException {
        return obj;
    }

    public String stringify(PotionEffectType obj) {
        return Registry.POTION_EFFECT_TYPE.getKeyOrThrow(obj).asMinimalString();
    }
}
