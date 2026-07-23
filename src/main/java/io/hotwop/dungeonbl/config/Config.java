package io.hotwop.dungeonbl.config;

import io.hotwop.dungeonbl.structure.builder.AsyncWorldBuilder;
import org.bukkit.World;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

@ConfigSerializable
public final class Config{
    @Required
    private int saveTimer=5;

    public int saveTimer(){
        return saveTimer;
    }

    @Required
    private BuilderSettings builder=new BuilderSettings();

    public BuilderSettings builder(){
        return builder;
    }

    @ConfigSerializable
    public static final class BuilderSettings{
        private int maxChunkCount=8;
        private int maxBlockPlace=1000;
        private int maxEntitySpawn=100;

        public AsyncWorldBuilder makeBuilder(World world){
            return new AsyncWorldBuilder(world,maxChunkCount,maxBlockPlace,maxEntitySpawn);
        }
    }

    private LogLevel logLevel=LogLevel.WARN;

    public LogLevel logLevel(){
        return logLevel;
    }

    public static final TypeSerializerCollection serializers=TypeSerializerCollection.builder()
        .build();
}
