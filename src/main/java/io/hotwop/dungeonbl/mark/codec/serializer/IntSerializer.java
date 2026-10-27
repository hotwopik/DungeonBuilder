package io.hotwop.dungeonbl.mark.codec.serializer;

import de.tr7zw.changeme.nbtapi.iface.NBTHandler;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import io.hotwop.dungeonbl.mark.codec.MarkDataSerializer;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

public class IntSerializer implements MarkDataSerializer<Integer,Integer,Integer>{
    public static final IntSerializer instance=new IntSerializer();
    public static final NBTHandler<Integer> integerHandler=new NBTHandler<>(){
        @Override
        public boolean fuzzyMatch(Object obj) {
            return obj instanceof Integer;
        }

        @Override
        public void set(@NonNull ReadWriteNBT nbt, @NonNull String key, @NonNull Integer value) {
            nbt.setInteger(key,value);
        }

        @Override
        public Integer get(@NonNull ReadableNBT nbt, @NonNull String key) {
            return nbt.getInteger(key);
        }
    };

    protected IntSerializer(){}

    @Override
    public NBTHandler<Integer> getHandler() {
        return integerHandler;
    }

    @Override
    public Integer fromNBT(Integer data) {
        return data;
    }

    @Override
    public Integer toNBT(Integer obj) {
        return obj;
    }

    @Override
    public Argument<Integer> getArgument(String name) {
        return new IntegerArgument(name);
    }

    @Override
    public Integer processArgument(Integer obj, CommandSender ctx) {
        return obj;
    }

    @Override
    public Integer defaultValue() {
        return 0;
    }

    @Override
    public String stringify(Integer obj) {
        return obj.toString();
    }
}
