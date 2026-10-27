package io.hotwop.dungeonbl.mark.codec.serializer;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import io.hotwop.dungeonbl.mark.codec.MarkSerializeException;

public class PositiveDoubleSerializer extends DoubleSerializer{
    public static final PositiveDoubleSerializer instance=new PositiveDoubleSerializer();
    protected PositiveDoubleSerializer(){}

    @Override
    public Argument<Double> getArgument(String name) {
        return new DoubleArgument(name,0);
    }

    @Override
    public void validate(Double obj) throws MarkSerializeException {
        if(obj<0)throw new MarkSerializeException("Value can't be negative");
    }
}
