package io.hotwop.dungeonbl.versioned.v26_2;


import io.hotwop.dungeonbl.DBlCore;
import io.hotwop.dungeonbl.DBlLogger;
import io.hotwop.dungeonbl.versioned.DBlAttributeMapping;
import io.hotwop.dungeonbl.versioned.TemporalWorld;
import io.hotwop.dungeonbl.versioned.VersionedInterface;
import io.hotwop.dungeonbl.versioned.WorldException;
import io.papermc.paper.world.PaperWorldLoader;
import io.papermc.paper.world.saveddata.PaperLevelOverrides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.*;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class VersionHolder implements VersionedInterface {
    public VersionHolder(){}

    public static final DBlLogger logger= DBlCore.getPlugin().getDBlLogger();
    public static final DedicatedServer vanillaServer=(DedicatedServer) MinecraftServer.getServer();

    private static final WorldOptions tempWorldOptions=new WorldOptions(0,false,false);
    private static final DedicatedServerProperties.WorldDimensionData tempWorldProperties=new DedicatedServerProperties.WorldDimensionData(GsonHelper.parse("{\"layers\":[],\"biome\":\"the_void\"}"),"flat");

    @Override
    public TemporalWorld createTemporalWorld(Path worldContainer, String folderName, String worldName, NamespacedKey worldId) throws WorldException {
        logger.info("Creating temporal world \"{}\"", worldName);

        // Creating local world registry
        RegistryAccess.Frozen registryAccess = vanillaServer.worldLoaderContext.datapackDimensions();
        Registry<LevelStem> worldRegistry = registryAccess.lookupOrThrow(Registries.LEVEL_STEM);
        WorldDimensions dimensions = tempWorldProperties.create(vanillaServer.worldLoaderContext.datapackWorldgen());
        WorldGenSettings worldGenSettings = new WorldGenSettings(tempWorldOptions,dimensions);

        WorldDimensions.Complete complete = dimensions.bake(worldRegistry);
        RegistryAccess.Frozen localRegistryAccess = complete.dimensionsRegistryAccess();
        Registry<LevelStem> localWorldRegistry = localRegistryAccess.lookupOrThrow(Registries.LEVEL_STEM);

        // World data
        LevelStem localWorld = worldGenSettings.dimensions().get(LevelStem.OVERWORLD)
            .orElseGet(()->localWorldRegistry.getValue(LevelStem.OVERWORLD));

        if(localWorld==null)throw new WorldException("World wasn't registered");

        Path worldFolder=worldContainer.resolve(folderName);
        SavedDataStorage dataStorage=new SavedDataStorage(
            worldFolder,
            vanillaServer.getFixerUpper(),
            vanillaServer.registryAccess()
        );
        dataStorage.set(WorldGenSettings.TYPE,worldGenSettings);

        PaperWorldLoader.LoadedWorldData paperData=new PaperWorldLoader.LoadedWorldData(
            worldName,
            UUID.randomUUID(),
            null,
            PaperLevelOverrides.createFromLiveLevelData((PrimaryLevelData)vanillaServer.getWorldData())
        );

        ResourceKey<Level> worldKey = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(
                worldId.getNamespace(),
                worldId.getKey()
            )
        );

        paperData.levelOverrides()
            .attach((PrimaryLevelData)vanillaServer.getWorldData(),worldKey);

        ServerLevel level=new ServerLevel(
            vanillaServer, Util.backgroundExecutor(),vanillaServer.storageSource,
            worldGenSettings,worldKey,localWorld,
            false,
            0,
            List.of(),
            true,
            LevelStem.OVERWORLD,
            World.Environment.CUSTOM,null,null,
            dataStorage,paperData
        );
        level.noSave=true;

        vanillaServer.addLevel(level);

        // Initializing world
        WorldBorder worldBorder=level.getWorldBorder();
        worldBorder.world=level;
        worldBorder.setAbsoluteMaxSize(vanillaServer.getAbsoluteMaxWorldSize());
        vanillaServer.getPlayerList().addWorldborderListener(level);

        BlockPos spawnPos=new BlockPos(0,0,0);
        GlobalPos globalPos=new GlobalPos(worldKey,spawnPos);

        LevelLoadListener levelLoadListener=level.levelLoadListener;

        levelLoadListener.start(LevelLoadListener.Stage.PREPARE_GLOBAL_SPAWN,0);
        levelLoadListener.updateFocus(worldKey,new ChunkPos(0,0));
        levelLoadListener.finish(LevelLoadListener.Stage.PREPARE_GLOBAL_SPAWN);

        levelLoadListener.start(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS,0);
        levelLoadListener.finish(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS);

        PaperLevelOverrides levelData=level.serverLevelData;

        levelData.setSpawn(new LevelData.RespawnData(globalPos,0,0));
        levelData.setInitialized(true);

        level.setSpawnSettings(true);

        return new TemporalWorldV(worldName,worldId,worldContainer,worldFolder,level,level.getGameRules());
    }

    public static final DBlAttributeMapping attributeMapping=new DBlAttributeMapping(
        Attribute.MOVEMENT_SPEED,
        Attribute.ATTACK_DAMAGE,
        Attribute.JUMP_STRENGTH
    );

    @Override
    public DBlAttributeMapping getAttributeMapping() {
        return attributeMapping;
    }
}
