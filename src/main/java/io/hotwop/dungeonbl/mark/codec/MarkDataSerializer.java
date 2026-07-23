package io.hotwop.dungeonbl.mark.codec;

import de.tr7zw.changeme.nbtapi.iface.NBTHandler;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.command.CommandSender;

public interface MarkDataSerializer<T,V,N>{
    NBTHandler<N> getHandler();

    T fromNBT(N data) throws SerializeException;
    N toNBT(T obj) throws SerializeException;

    Argument<V> getArgument(String name);
    T processArgument(V obj,CommandSender ctx) throws WrapperCommandSyntaxException;

    default T defaultValue(){
        return null;
    }
    default String stringify(T obj){
        return obj.toString();
    }
}
