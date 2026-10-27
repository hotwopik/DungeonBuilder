package io.hotwop.dungeonbl.mark.codec.serializer;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import io.hotwop.dungeonbl.mark.codec.MarkSerializeException;

public class PositiveIntSerializer extends IntSerializer{
    public static final PositiveIntSerializer instance=new PositiveIntSerializer();
    protected PositiveIntSerializer(){}

    @Override
    public Argument<Integer> getArgument(String name) {
        return new IntegerArgument(name,0);
    }

    @Override
    public void validate(Integer obj) throws MarkSerializeException {
        if(obj<0)throw new MarkSerializeException("Value can't be negative");
    }
}
