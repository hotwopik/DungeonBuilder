package io.hotwop.dungeonbl.versioned.v1_21_3;

import com.mojang.serialization.Lifecycle;
import io.hotwop.dungeonbl.DBlCore;
import io.hotwop.dungeonbl.DBlLogger;
import io.hotwop.dungeonbl.versioned.DBlAttributeMapping;
import io.hotwop.dungeonbl.versioned.TemporalWorld;
import io.hotwop.dungeonbl.versioned.VersionedInterface;
import io.hotwop.dungeonbl.versioned.WorldException;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.validation.ContentValidationException;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class VersionHolder implements VersionedInterface{
    public VersionHolder(){}

    public static final DBlLogger logger= DBlCore.getPlugin().getDBlLogger();
    public static final DedicatedServer vanillaServer=(DedicatedServer) MinecraftServer.getServer();

    private static final WorldOptions tempWorldOptions=new WorldOptions(0,false,false);
    private static final DedicatedServerProperties.WorldDimensionData tempWorldProperties=new DedicatedServerProperties.WorldDimensionData(GsonHelper.parse("{\"layers\":[],\"biome\":\"the_void\"}"),"flat");

    @Override
    @SuppressWarnings("deprecation")
    public TemporalWorld createTemporalWorld(Path worldContainer, String folderName, String worldName, NamespacedKey worldId) throws WorldException {
        logger.info("Creating temporal world \"{}\"", worldName);

        // Creating world folder
        LevelStorageSource.LevelStorageAccess worldSession;
        try{
            worldSession=LevelStorageSource.createDefault(worldContainer)
                .validateAndCreateAccess(folderName, LevelStem.OVERWORLD);
        } catch (IOException | ContentValidationException e) {
            throw new WorldException("Error to open world io session",e);
        }

        // Main world settings
        GameRules gameRules=new GameRules(FeatureFlagSet.of());

        LevelSettings levelSettings = new LevelSettings(
            worldName,
            GameType.ADVENTURE,
            false,
            Difficulty.NORMAL,
            false,
            gameRules,
            vanillaServer.worldLoader.dataConfiguration()
        );

        // Creating local world registry
        WorldDimensions dimensions = tempWorldProperties.create(vanillaServer.worldLoader.datapackWorldgen());
        WritableRegistry<LevelStem> writableDimensionRegistry = new MappedRegistry<>(
            Registries.LEVEL_STEM,
            Lifecycle.experimental()
        );

        // Registering worlds
        dimensions.dimensions()
            .forEach((key,dimension)->writableDimensionRegistry
                .register(
                    key,
                    dimension,
                    new RegistrationInfo(
                        Optional.empty(),
                        isStable(key,dimension)?Lifecycle.stable():Lifecycle.experimental()
                    )
                )
            );

        Registry<LevelStem> registry = writableDimensionRegistry.freeze();

        // Complete setting world
        WorldDimensions.Complete dimensionsOp=new WorldDimensions.Complete(
            registry,
            PrimaryLevelData.SpecialWorldProperty.FLAT
        );

        PrimaryLevelData levelData = new PrimaryLevelData(
            levelSettings,
            tempWorldOptions,
            dimensionsOp.specialWorldProperty(),
            Lifecycle.experimental()
        );

        levelData.customDimensions = registry;
        levelData.checkName(worldName);
        levelData.setModdedInfo(
            vanillaServer.getServerModName(),
            vanillaServer.getModdedStatus().shouldReportAsModified()
        );

        // Get world dimension and id
        ResourceKey<Level> worldKey = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(
                worldId.namespace(),
                worldId.value()
            )
        );

        Optional<LevelStem> mainWorldInstance=registry.getOptional(LevelStem.OVERWORLD);
        if(mainWorldInstance.isEmpty())throw new WorldException("World wasn't registered");

        // Create level instance
        ServerLevel level = new ServerLevel(
            vanillaServer, vanillaServer.executor, worldSession, // Server and stuff
            levelData, worldKey, mainWorldInstance.get(), // World settings
            vanillaServer.progressListenerFactory.create(0), // Create logger
            false, // isDebug
            0, // seed
            List.of(), // Entity spawn managers
            true, // Is time calculating
            vanillaServer.overworld().getRandomSequences(), // World random
            World.Environment.CUSTOM, null, null // Bukkit stuff
        );
        level.noSave=true;

        vanillaServer.addLevel(level);

        // Initializing world
        BlockPos spawnPos=new BlockPos(0,0,0);

        levelData.setSpawn(spawnPos,0);
        levelData.setInitialized(true);

        ServerChunkCache chunkCache=level.getChunkSource();
        ChunkProgressListener loadListener=chunkCache.chunkMap.progressListener;

        loadListener.updateSpawnPos(new ChunkPos(spawnPos));
        level.setDefaultSpawnPos(spawnPos,0);

        loadListener.stop();

        level.setSpawnSettings(true);

        return new TemporalWorldV(worldName,worldId,worldContainer,worldSession.levelDirectory.path(),level,gameRules);
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

    public static boolean isStable(ResourceKey<LevelStem> key, LevelStem data){
        return (key==LevelStem.OVERWORLD&&isStableOverworld(data))||(key==LevelStem.NETHER&&isStableNether(data))||(key==LevelStem.END&&isStableEnd(data));
    }

    public static boolean isStableOverworld(LevelStem dimensionOptions) {
        Holder<DimensionType> holder = dimensionOptions.type();
        return (
            holder.is(BuiltinDimensionTypes.OVERWORLD)||
                holder.is(BuiltinDimensionTypes.OVERWORLD_CAVES)
        )&&(
            !(dimensionOptions.generator().getBiomeSource() instanceof MultiNoiseBiomeSource multiNoiseBiomeSource)||
                multiNoiseBiomeSource.stable(MultiNoiseBiomeSourceParameterLists.OVERWORLD)
        );
    }

    public static boolean isStableNether(LevelStem dimensionOptions) {
        return dimensionOptions.type().is(BuiltinDimensionTypes.NETHER)
            && dimensionOptions.generator() instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator
            && noiseBasedChunkGenerator.stable(NoiseGeneratorSettings.NETHER)
            && noiseBasedChunkGenerator.getBiomeSource() instanceof MultiNoiseBiomeSource multiNoiseBiomeSource
            && multiNoiseBiomeSource.stable(MultiNoiseBiomeSourceParameterLists.NETHER);
    }

    public static boolean isStableEnd(LevelStem dimensionOptions) {
        return dimensionOptions.type().is(BuiltinDimensionTypes.END)
            && dimensionOptions.generator() instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator
            && noiseBasedChunkGenerator.stable(NoiseGeneratorSettings.END)
            && noiseBasedChunkGenerator.getBiomeSource() instanceof TheEndBiomeSource;
    }
}
