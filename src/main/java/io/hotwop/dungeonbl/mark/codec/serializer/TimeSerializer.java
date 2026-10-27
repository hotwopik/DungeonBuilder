package io.hotwop.dungeonbl.mark.codec.serializer;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.TimeArgument;

public final class TimeSerializer extends PositiveIntSerializer{
    public static final TimeSerializer instance=new TimeSerializer();
    private TimeSerializer(){}

    @Override
    public Argument<Integer> getArgument(String name) {
        return new TimeArgument(name);
    }
}
