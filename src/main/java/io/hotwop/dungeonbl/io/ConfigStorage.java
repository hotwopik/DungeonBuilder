package io.hotwop.dungeonbl.io;

import io.hotwop.dungeonbl.DungeonBl;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class ConfigStorage<T>{
    private T config;

    public final String name;
    public final String fileName;
    public final Path file;

    public final YamlConfigurationLoader loader;
    public final Class<T> clazz;
    public final Supplier<T> defaultSupplier;

    public ConfigStorage(
        String name,
        String file,
        Path directory,
        TypeSerializerCollection serializers,
        Class<T> clazz,
        Supplier<T> defaultSupplier
    ){
        this.name=name;
        this.fileName=file;
        this.file=directory.resolve(file);
        this.clazz=clazz;
        this.defaultSupplier=defaultSupplier;

        loader=YamlConfigurationLoader.builder()
            .path(this.file)
            .indent(2)
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions(opts->opts.serializers(
                ser->ser.registerAll(serializers)
            ))
            .build();
    }

    public T get(){
        return config;
    }

    public void load(){
        logger.info("Loading {}...",name);

         if(!Files.isRegularFile(file)){
             config=defaultSupplier.get();
             DungeonBl.plugin()
                 .saveResource(fileName,false);
         }else{
             ConfigurationNode node;
             try{
                 node=loader.load();
             }catch(ConfigurateException e){
                 logger.warn("Error to load {} file",name,e);
                 config=defaultSupplier.get();
                 return;
             }catch(RuntimeException e){
                 logger.warn("Unexpected error appear while loading {} file",name,e);
                 config=defaultSupplier.get();
                 return;
             }

             try{
                 config=node.get(clazz);
             }catch(SerializationException e){
                 logger.warn("Error to deserialize {} file",name,e);
                 config=defaultSupplier.get();
             }catch(RuntimeException e){
                 logger.warn("Unexpected error appear while deserializing {} file",name,e);
                 config=defaultSupplier.get();
             }
         }
    }
}
