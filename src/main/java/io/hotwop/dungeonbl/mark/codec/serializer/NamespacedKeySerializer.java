package io.hotwop.dungeonbl.mark.codec.serializer;

import de.tr7zw.changeme.nbtapi.iface.NBTHandler;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.NamespacedKeyArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import io.hotwop.dungeonbl.mark.codec.MarkDataSerializer;
import io.hotwop.dungeonbl.mark.codec.MarkSerializeException;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;

public class NamespacedKeySerializer implements MarkDataSerializer<NamespacedKey,NamespacedKey,String>{
    public static final NamespacedKeySerializer instance=new NamespacedKeySerializer();
    private NamespacedKeySerializer(){}

    @Override
    public NBTHandler<String> getHandler() {
        return StringSerializer.stringHandler;
    }

    @Override
    public NamespacedKey fromNBT(String data) throws MarkSerializeException {
        NamespacedKey key=NamespacedKey.fromString(data);
        if(key==null)throw new MarkSerializeException("Namespaced key is invalid: "+data);
        return key;
    }

    @Override
    public String toNBT(NamespacedKey obj) throws MarkSerializeException {
        return obj.asMinimalString();
    }

    @Override
    public Argument<NamespacedKey> getArgument(String name) {
        return new NamespacedKeyArgument(name);
    }

    @Override
    public NamespacedKey processArgument(NamespacedKey obj, CommandSender ctx) throws WrapperCommandSyntaxException {
        return obj;
    }

    @Override
    public String stringify(NamespacedKey obj) {
        return obj.asMinimalString();
    }
}
