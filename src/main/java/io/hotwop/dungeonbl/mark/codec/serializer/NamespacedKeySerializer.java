package io.hotwop.dungeonbl.mark.codec.serializer;

import de.tr7zw.changeme.nbtapi.iface.NBTHandler;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.NamespacedKeyArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import io.hotwop.dungeonbl.mark.codec.MarkDataSerializer;
import io.hotwop.dungeonbl.mark.codec.SerializeException;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;

public final class NamespacedKeySerializer implements MarkDataSerializer<NamespacedKey,NamespacedKey,String>{
    public static final NamespacedKeySerializer instance=new NamespacedKeySerializer();

    private NamespacedKeySerializer(){}


    @Override
    public NBTHandler<String> getHandler() {
        return StringSerializer.stringHandler;
    }

    @Override
    public NamespacedKey fromNBT(String data) throws SerializeException {
        NamespacedKey key=NamespacedKey.fromString(data);
        if(key==null)throw new SerializeException("Namespaced key is invalid: "+data);
        return key;
    }

    @Override
    public String toNBT(NamespacedKey obj) throws SerializeException {
        return obj.asMinimalString();
    }

    @Override
    public Argument<NamespacedKey> getArgument(String name) {
        return new NamespacedKeyArgument(name);
    }

    @Override
    public NamespacedKey processArgument(NamespacedKey obj, CommandSender ctx) throws WrapperCommandSyntaxException {
        return null;
    }

    @Override
    public String stringify(NamespacedKey obj) {
        return obj.asMinimalString();
    }
}
