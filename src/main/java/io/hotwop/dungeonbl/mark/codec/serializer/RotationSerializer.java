package io.hotwop.dungeonbl.mark.codec.serializer;

import de.tr7zw.changeme.nbtapi.iface.NBTHandler;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import io.hotwop.dungeonbl.DBlCommand;
import io.hotwop.dungeonbl.mark.codec.MarkDataSerializer;
import io.hotwop.dungeonbl.mark.codec.MarkSerializeException;
import io.hotwop.dungeonbl.util.math.SideRotation;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class RotationSerializer implements MarkDataSerializer<SideRotation, SideRotation,Integer>{
    public static final RotationSerializer instance=new RotationSerializer();
    private RotationSerializer(){}

    @Override
    public NBTHandler<Integer> getHandler() {
        return IntSerializer.integerHandler;
    }

    @Override
    public SideRotation fromNBT(Integer data) throws MarkSerializeException {
        return SideRotation.get(data);
    }

    @Override
    public Integer toNBT(SideRotation obj) throws MarkSerializeException {
        return obj.ordinal();
    }

    @Override
    public Argument<SideRotation> getArgument(String name) {
        return DBlCommand.rotationArgument(name);
    }

    @Override
    public SideRotation processArgument(SideRotation obj, CommandSender ctx) throws WrapperCommandSyntaxException {
        return obj;
    }

    @Override
    public SideRotation defaultValue() {
        return SideRotation.R0;
    }

    public static final Map<SideRotation,String> map=Map.of(
        SideRotation.R0,"0",
        SideRotation.R90,"90",
        SideRotation.R180,"180",
        SideRotation.R270,"270"
    );

    @Override
    public String stringify(SideRotation obj) {
        return map.get(obj);
    }
}
