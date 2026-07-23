package io.hotwop.dungeonbl.config;

import io.hotwop.dungeonbl.mob.CrossMobList;
import io.hotwop.dungeonbl.util.config.ConfigIntRange;
import io.hotwop.dungeonbl.mob.Boss;
import io.hotwop.dungeonbl.util.config.ValidId;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.PostProcess;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

@ConfigSerializable
public final class Room {
    private boolean endsWithPlayers=false;
    private boolean endsWithClear=false;

    private @ValidId String endsOnSignal;
    private @ConfigIntRange(min = 1) Integer endSignalValue;

    private @ConfigIntRange(min=0) Integer nextRoomDelay=null;

    private @Required @ValidId String building;
    private @ValidId List<String> decorations;

    private CrossMobList mobs;
    private @ConfigIntRange(min = 0) int waves=0;
    private List<Boss> bosses;

    private Room(){}

    public Room(
        boolean endsWithPlayers,
        boolean endsWithClear,

        @ValidId String endsOnSignal,
        Integer endSignalValue,

        Integer nextRoomDelay,

        @ValidId String building,
        @ValidId List<String> decorations,

        CrossMobList mobs,
        int waves,
        List<Boss> bosses
    ) {
        this.endsWithPlayers = endsWithPlayers;
        this.endsWithClear = endsWithClear;
        this.endsOnSignal = endsOnSignal;
        this.endSignalValue = endSignalValue;
        this.nextRoomDelay = nextRoomDelay;
        this.building = building;
        this.decorations = decorations;
        this.mobs = mobs;
        this.waves = waves;
        this.bosses = bosses;
    }

    @PostProcess
    private void postProcess() throws SerializationException {
        if (!(endsWithPlayers || endsWithClear || endsOnSignal != null)) throw
            new SerializationException("Room without end condition");
    }

    public boolean endsWithPlayers() {
        return endsWithPlayers;
    }

    public boolean endsWithClear() {
        return endsWithClear;
    }

    @ValidId
    public String endsOnSignal() {
        return endsOnSignal;
    }

    public Integer endSignalValue() {
        return endSignalValue;
    }

    public Integer nextRoomDelay(){
        return nextRoomDelay;
    }

    @ValidId
    public String building() {
        return building;
    }

    @ValidId
    public List<String> decorations() {
        return decorations;
    }

    public CrossMobList mobs() {
        return mobs;
    }

    public int waves() {
        return waves;
    }

    public List<Boss> bosses() {
        return bosses;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Room) obj;
        return this.endsWithPlayers == that.endsWithPlayers &&
            this.endsWithClear == that.endsWithClear &&
            Objects.equals(this.endsOnSignal, that.endsOnSignal) &&
            Objects.equals(this.endSignalValue, that.endSignalValue) &&
            Objects.equals(this.nextRoomDelay, that.nextRoomDelay) &&
            Objects.equals(this.building, that.building) &&
            Objects.equals(this.decorations, that.decorations) &&
            Objects.equals(this.mobs, that.mobs) &&
            this.waves==that.waves &&
            Objects.equals(this.bosses, that.bosses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            endsWithPlayers,
            endsWithClear,
            endsOnSignal,
            endSignalValue,
            nextRoomDelay,
            building,
            decorations,
            mobs,
            waves,
            bosses
        );
    }



}