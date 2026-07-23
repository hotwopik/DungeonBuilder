package io.hotwop.dungeonbl.mark.dungeon;

import io.hotwop.dungeonbl.mark.RoomTypePredicate;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.mark.dungeon.func.AllPlaceFunction;
import io.hotwop.dungeonbl.mark.dungeon.func.FinishPlaceFunction;
import io.hotwop.dungeonbl.mark.dungeon.func.PlaceFunction;
import io.hotwop.dungeonbl.mark.dungeon.func.PlacingType;
import io.hotwop.dungeonbl.mark.place.MarkPlaceData;
import io.hotwop.dungeonbl.session.RoomType;
import io.hotwop.dungeonbl.session.RoomSession;
import io.hotwop.dungeonbl.session.RoomCollector;
import io.hotwop.dungeonbl.util.math.Vec3;

import java.util.function.Function;

import static io.hotwop.dungeonbl.mark.RoomTypePredicate.*;

public final class DungeonMarkData<T extends DungeonMark<T,P>,P extends MarkPlaceData>{
    public final RoomTypePredicate predicate;
    public final PlaceFunction<P> placing;
    public final PlacingType placingType;
    public final Function<MarkCodec.Info,P> dataRebuilder;

    public final Class<P> placeDataClass;

    public DungeonMarkData(
        RoomTypePredicate predicate,
        FinishPlaceFunction<P> placing,
        Function<MarkCodec.Info,P> dataRebuilder,
        Class<P> placeDataClass
    ){
        this.placing=placing;
        this.dataRebuilder=dataRebuilder;
        this.predicate=predicate;

        this.placeDataClass=placeDataClass;
        placingType=PlacingType.finish;
    }

    public DungeonMarkData(
        RoomTypePredicate predicate,
        AllPlaceFunction<P> placing,
        Function<MarkCodec.Info,P> dataRebuilder,
        Class<P> placeDataClass
    ){
        this.placing=placing;
        this.dataRebuilder=dataRebuilder;
        this.predicate=predicate;

        this.placeDataClass=placeDataClass;
        placingType=PlacingType.all;
    }

    public P rebuildData(MarkCodec.Info info){
        return dataRebuilder.apply(info);
    }

    @SuppressWarnings("unchecked")
    public boolean finishPlace(Vec3 pos,MarkPlaceData data,RoomSession.Builder room,RoomCollector collector){
        if(!placeDataClass.isAssignableFrom(data.getClass()))throw new RuntimeException("Supplied data is incorrect");
        P pData=(P)data;

        return ((FinishPlaceFunction<P>)placing).place(pData,pos,room,collector);
    }

    @SuppressWarnings("unchecked")
    public void allPlace(Vec3 pos,MarkPlaceData data,RoomSession.Builder room,RoomCollector collector){
        if(!placeDataClass.isAssignableFrom(data.getClass()))throw new RuntimeException("Supplied data is incorrect");
        P pData=(P)data;

        ((AllPlaceFunction<P>)placing).place(pData,pos,room,collector);
    }

    public boolean predicate(RoomType type){
        if(predicate==ALL)return true;
        switch(type){
            case START->{
                return predicate==START||predicate==START_BASE||predicate==START_END;
            }
            case BASE->{
                return predicate==BASE||predicate==START_BASE||predicate==BASE_END;
            }
            case END->{
                return predicate==END||predicate==BASE_END||predicate==START_END;
            }
        }
        return false;
    }
}