package io.hotwop.dungeonbl.mark.place;

import io.hotwop.dungeonbl.io.DBlRegistry;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;

public record TriggerData(
    String signal,
    int value
) implements MarkPlaceData{
    public TriggerData(String signal,int value){
        if(!DBlRegistry.validId.test(signal))throw new RuntimeException("Id is invalid");
        this.signal=signal;
        this.value=Math.max(value, 1);
    }

    public TriggerData(MarkCodec.Info info){
        this(
            info.get("signal",String.class),
            info.get("value",int.class)
        );
    }
}
