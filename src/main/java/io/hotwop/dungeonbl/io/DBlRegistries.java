package io.hotwop.dungeonbl.io;

import io.hotwop.dungeonbl.config.Dungeon;
import io.hotwop.dungeonbl.mob.Mob;
import io.hotwop.dungeonbl.structure.Decoration;
import io.hotwop.dungeonbl.structure.Doorway;
import io.hotwop.dungeonbl.structure.RoomBuilding;

import java.nio.file.Path;

public final class DBlRegistries {
    public static final String roomBuildingsFolder ="room_buildings";
    public static final String doorwaysFolder="doorways";
    public static final String decorationsFolder="decorations";
    public static final String dungeonsFolder="dungeons";
    public static final String mobsFile="mobs.yml";

    public final StructRegistry<RoomBuilding> roomBuildings;
    public final StructRegistry<Doorway> doorways;
    public final StructRegistry<Decoration> decorations;
    public final YmlRegistry<Dungeon> dungeons;
    public final YmlSingleRegistry<Mob> mobs;

    public DBlRegistries(Path pluginRoot){
        roomBuildings=new StructRegistry<>(
            "Room Building",
            pluginRoot.resolve(roomBuildingsFolder),
            RoomBuilding::encode,
            RoomBuilding::decode
        );
        doorways=new StructRegistry<>(
            "Doorway",
            pluginRoot.resolve(doorwaysFolder),
            Doorway::encode,
            Doorway::decode
        );
        decorations=new StructRegistry<>(
            "Decoration",
            pluginRoot.resolve(decorationsFolder),
            Decoration::encode,
            Decoration::decode
        );
        dungeons=new YmlRegistry<>(
            "Dungeons",
            pluginRoot.resolve(dungeonsFolder),
            Dungeon.serializers,
            Dungeon.class
        );
        mobs=new YmlSingleRegistry<>(
            "Mobs",
            pluginRoot.resolve(mobsFile),
            Mob.serializers,
            Mob.class
        );
    }

    public void saveAll(){
        roomBuildings.save();
        doorways.save();
        decorations.save();
        dungeons.save();
        mobs.save();
    }

    public void loadAll(){
        roomBuildings.load();
        doorways.load();
        decorations.load();
        dungeons.load();
        mobs.load();
    }
}
