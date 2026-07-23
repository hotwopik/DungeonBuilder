package io.hotwop.dungeonbl.mark.codec.serializer;

import de.tr7zw.changeme.nbtapi.iface.NBTHandler;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import io.hotwop.dungeonbl.mark.codec.MarkDataSerializer;
import io.hotwop.dungeonbl.mark.codec.SerializeException;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

public final class StringSerializer implements MarkDataSerializer<String,String,String>{
    public static final StringSerializer instance=new StringSerializer();
    public static final NBTHandler<String> stringHandler=new NBTHandler<>(){
        @Override
        public boolean fuzzyMatch(Object obj) {
            return obj instanceof String;
        }

        @Override
        public void set(@NonNull ReadWriteNBT nbt, @NonNull String key, @NonNull String value) {
            nbt.setString(key, value);
        }

        @Override
        public String get(@NonNull ReadableNBT nbt, @NonNull String key) {
            return nbt.getString(key);
        }
    };
    private StringSerializer(){}

    @Override
    public NBTHandler<String> getHandler() {
        return stringHandler;
    }

    @Override
    public String fromNBT(String data) throws SerializeException {
        return data;
    }

    @Override
    public String toNBT(String obj) throws SerializeException {
        return obj;
    }

    @Override
    public Argument<String> getArgument(String name) {
        return new StringArgument(name);
    }

    @Override
    public String processArgument(String obj, CommandSender ctx) throws WrapperCommandSyntaxException {
        return obj;
    }

    @Override
    public String stringify(String obj) {
        return obj;
    }
}
