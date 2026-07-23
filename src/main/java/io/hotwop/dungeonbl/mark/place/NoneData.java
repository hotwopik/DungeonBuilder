package io.hotwop.dungeonbl.mark.place;

import io.hotwop.dungeonbl.mark.codec.MarkCodec;

public record NoneData() implements MarkPlaceData {
    public NoneData(MarkCodec.Info info){
        this();
    }
}
