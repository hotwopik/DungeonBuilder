package io.hotwop.dungeonbl.io;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class YmlSingleRegistry<T> extends DBlRegistry<T>{
    public final Path file;
    public final YamlConfigurationLoader loader;
    public final Class<T> clazz;

    public YmlSingleRegistry(String name, Path file, TypeSerializerCollection serializers, Class<T> clazz){
        super(name);
        this.file = file;
        this.clazz = clazz;

        this.loader=YamlConfigurationLoader.builder()
            .path(file)
            .nodeStyle(NodeStyle.BLOCK)
            .indent(2)
            .defaultOptions(opts->opts
                .serializers(ser->ser.registerAll(serializers))
            )
            .build();
    }

    @Override
    public void save() {
        busyLock();
        logger.info("Saving {}...", name);

        ConfigurationNode node=loader.createNode();
        for(Map.Entry<String,T> entry:map.entrySet()){
            ConfigurationNode subNode=node.node(entry.getKey());

            try{
                subNode.set(clazz,entry.getValue());
            }catch(SerializationException e){
                logger.warn("Error to serialize {} file element {}",name,entry.getKey(),e);
            }catch (RuntimeException e){
                logger.warn("Error to serialize {} file element {}, unexpected error arrived",name,entry.getKey(),e);
            }
        }

        try{
            loader.save(node);
        } catch (ConfigurateException e) {
            logger.warn("Error to write {} file", name, e);
            busyUnlock();
            return;
        }catch (RuntimeException e){
            logger.warn("Error to write {} file, unexpected error arrived",name,e);
            busyUnlock();
            return;
        }

        removed.clear();
        busyUnlock();
    }

    @Override
    public void load() {
        busyLock();
        logger.info("Loading {}...", name);

        if(!Files.exists(file)){
            busyUnlock();
            return;
        }

        ConfigurationNode node;
        try{
            node=loader.load();
        }catch(SerializationException e){
            logger.warn("Error to deserialize {} {} file", name, file, e);
            busyUnlock();
            return;
        }catch(ConfigurateException e){
            logger.warn("Error to read {} {} file", name, file, e);
            busyUnlock();
            return;
        }

        if(node.empty()){
            busyUnlock();
            return;
        }

        if(!node.isMap()){
            logger.warn("Map expected in {} file",name);
            busyUnlock();
            return;
        }

        Set<String> removeCounter=new HashSet<>(map.keySet());

        map.clear();
        removed.clear();

        for(Map.Entry<Object,? extends ConfigurationNode> entry:node.childrenMap().entrySet()){
            String id=entry.getKey().toString();
            if(!validId.test(id)){
                logger.warn("{} file contains unallowed id: {}",name,id);
                continue;
            }

            T value;
            try{
                value=entry.getValue().get(clazz);
            } catch (SerializationException e) {
                logger.warn("Error to deserialize {} file element {}",name,id,e);
                continue;
            }

            map.put(id,value);
            listenersUpdate(id,value);
        }

        removeCounter.removeAll(map.keySet());
        removeCounter.forEach(this::listenersRemove);

        busyUnlock();
    }
}
