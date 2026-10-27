package io.hotwop.dungeonbl.mark.codec.serializer;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.NBTHandler;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LocationArgument;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.mark.codec.MarkDataSerializer;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

public final class LocationSerializer implements MarkDataSerializer<Location,Location,ReadableNBT>{
    public static final LocationSerializer instance=new LocationSerializer();
    private LocationSerializer(){}

    @Override
    public NBTHandler<ReadableNBT> getHandler() {
        return NBTHandlers.STORE_READABLE_TAG;
    }

    @Override
    public Location fromNBT(ReadableNBT data) {
        return DBlUtil.dataToLocation(data);
    }

    @Override
    public ReadableNBT toNBT(Location obj) {
        ReadWriteNBT nbt= NBT.createNBTObject();
        DBlUtil.locationToData(obj,nbt);
        return nbt;
    }

    @Override
    public Argument<Location> getArgument(String name) {
        return new LocationArgument(name);
    }

    @Override
    public Location processArgument(Location obj, CommandSender ctx) {
        return obj;
    }

    @Override
    public String stringify(Location obj) {
        return obj.getWorld().getName()+" "+obj.getBlockX()+" "+obj.getBlockY()+" "+obj.getBlockZ()+" "+obj.getYaw()+" "+obj.getPitch();
    }
}
