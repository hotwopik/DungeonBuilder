package io.hotwop.dungeonbl.mark.codec.serializer;

import dev.jorel.commandapi.arguments.Argument;
import io.hotwop.dungeonbl.DBlCommand;
import io.hotwop.dungeonbl.io.DBlRegistry;
import io.hotwop.dungeonbl.mark.codec.MarkSerializeException;

public class IdStringSerializer extends StringSerializer{
    public static final IdStringSerializer instance=new IdStringSerializer();
    protected IdStringSerializer(){}

    @Override
    public Argument<String> getArgument(String name) {
        return DBlCommand.validIdArgument(name);
    }

    @Override
    public void validate(String obj) throws MarkSerializeException {
        if(!DBlRegistry.validId.test(obj))throw new MarkSerializeException("Value contains unallowed symbols: "+obj);
    }
}
