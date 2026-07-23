package io.hotwop.dungeonbl.mark.dungeon;

import io.hotwop.dungeonbl.mark.RoomTypePredicate;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.mark.dungeon.func.AllPlaceFunction;
import io.hotwop.dungeonbl.mark.dungeon.func.FinishPlaceFunction;
import io.hotwop.dungeonbl.mark.dungeon.func.PlaceFunction;
import io.hotwop.dungeonbl.mark.place.MarkPlaceData;
import io.hotwop.dungeonbl.session.RoomType;
import io.hotwop.dungeonbl.session.RoomSession;
import io.hotwop.dungeonbl.session.collector.InRoomCollector;
import io.hotwop.dungeonbl.session.collector.OutRoomCollector;
import io.hotwop.dungeonbl.session.collector.RoomBuildingCollector;
import io.hotwop.dungeonbl.session.collector.RoomCollector;
import io.hotwop.dungeonbl.util.math.Vec3;

import java.util.function.Function;

import static io.hotwop.dungeonbl.mark.RoomTypePredicate.*;

public final class DungeonMarkData<T extends DungeonMark<T,P>,P extends MarkPlaceData,C extends RoomCollector>{
    public final RoomTypePredicate predicate;
    public final PlaceFunction<P,C> placing;
    public final Function<MarkCodec.Info,P> dataRebuilder;

    public final Class<C> roomCollector;
    public final Class<P> placeDataClass;
    public final LoadOrder loadOrder;

    public DungeonMarkData(
        RoomTypePredicate predicate,
        FinishPlaceFunction<P,C> placing,
        Function<MarkCodec.Info,P> dataRebuilder,
        Class<P> placeDataClass,
        Class<C> collectorClass
    ){
        this.placing=placing;
        this.dataRebuilder=dataRebuilder;
        this.predicate=predicate;

        this.roomCollector=collectorClass;
        this.placeDataClass=placeDataClass;

        if(roomCollector.equals(RoomBuildingCollector.class))this.loadOrder=LoadOrder.BUILDING;
        else if(roomCollector.equals(InRoomCollector.class))this.loadOrder=LoadOrder.IN_ROOM;
        else if(roomCollector.equals(OutRoomCollector.class))this.loadOrder=LoadOrder.OUT_ROOM;
        else throw new RuntimeException("Unsupported collector type");
    }

    public DungeonMarkData(
        RoomTypePredicate predicate,
        AllPlaceFunction<P,C> placing,
        Function<MarkCodec.Info,P> dataRebuilder,
        Class<P> placeDataClass,
        Class<C> collectorClass
    ){
        this.placing=placing;
        this.dataRebuilder=dataRebuilder;
        this.predicate=predicate;

        this.roomCollector=collectorClass;
        this.placeDataClass=placeDataClass;

        if(roomCollector.equals(RoomBuildingCollector.class))this.loadOrder=LoadOrder.BUILDING;
        else if(roomCollector.equals(InRoomCollector.class))this.loadOrder=LoadOrder.IN_ROOM;
        else if(roomCollector.equals(OutRoomCollector.class))this.loadOrder=LoadOrder.OUT_ROOM;
        else throw new RuntimeException("Unsupported collector type");
    }

    public P rebuildData(MarkCodec.Info info){
        return dataRebuilder.apply(info);
    }

    @SuppressWarnings("unchecked")
    public boolean place(Vec3 pos,MarkPlaceData data,RoomSession.Builder room,RoomCollector collector){
        if(!collector.getClass().equals(roomCollector))throw new RuntimeException("Supplied collector is incorrect");
        if(!data.getClass().equals(placeDataClass))throw new RuntimeException("Supplied data is incorrect");
        C cCollector=(C)collector;
        P pData=(P)data;

        return switch(placing){
            case AllPlaceFunction<P,C> v->{
                v.place(pData,pos,room,cCollector);
                yield false;
            }
            case FinishPlaceFunction<P,C> v->v.place(pData,pos,room,cCollector);
        };
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