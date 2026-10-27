package io.hotwop.dungeonbl.mark.codec.serializer;

import de.tr7zw.changeme.nbtapi.iface.NBTHandler;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import io.hotwop.dungeonbl.mark.codec.MarkDataSerializer;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

public class DoubleSerializer implements MarkDataSerializer<Double,Double,Double>{
    public static final DoubleSerializer instance=new DoubleSerializer();
    public static final NBTHandler<Double> doubleHandler=new NBTHandler<>(){
        @Override
        public boolean fuzzyMatch(Object obj) {
            return obj instanceof Double;
        }

        @Override
        public void set(@NonNull ReadWriteNBT nbt, @NonNull String key, @NonNull Double value) {
            nbt.setDouble(key,value);
        }

        @Override
        public Double get(@NonNull ReadableNBT nbt, @NonNull String key) {
            return nbt.getDouble(key);
        }
    };

    protected DoubleSerializer(){}

    @Override
    public NBTHandler<Double> getHandler() {
        return doubleHandler;
    }

    @Override
    public Double fromNBT(Double data) {
        return data;
    }

    @Override
    public Double toNBT(Double obj) {
        return obj;
    }

    @Override
    public Argument<Double> getArgument(String name) {
        return new DoubleArgument(name);
    }

    @Override
    public Double processArgument(Double obj, CommandSender ctx) {
        return obj;
    }

    @Override
    public Double defaultValue() {
        return 0.0;
    }

    @Override
    public String stringify(Double obj) {
        return obj.toString();
    }
}
