package io.hotwop.dungeonbl.io;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class YmlRegistry<T> extends DBlRegistry<T>{
    public final Class<T> clazz;
    public final Path path;
    public final TypeSerializerCollection serializers;

    private final Map<String,YamlConfigurationLoader> loaders=new HashMap<>();

    public YmlRegistry(String name, Path path, TypeSerializerCollection serializers, Class<T> clazz){
        super(name);
        this.clazz = clazz;
        this.path = path;
        this.serializers = serializers;

        path.toFile().mkdir();
    }

    public YamlConfigurationLoader makeLoader(Path path){
        return YamlConfigurationLoader.builder()
            .path(path)
            .indent(2)
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions(opts->opts
                .serializers(ser->ser.registerAll(serializers))
            )
            .build();
    }

    @Override
    public void save() {
        busyLock();
        logger.info("Saving {}", name);

        path.toFile().mkdirs();

        for(String key:map.keySet()){
            try{
                write(key);
            }catch(SerializationException e){
                logger.warn("Error to serialize {} {}", name, key, e);
            }catch(ConfigurateException e){
                logger.warn("Error to write {} {} file", name, key, e);
            }catch (RuntimeException e) {
                logger.warn("Unexpected error arrived while serializing {} {}", name, key, e);
            }
        }

        List<String> trueRemoved=new ArrayList<>();
        for(String key:removed){
            try{
                delete(key);
                trueRemoved.add(key);
            }catch(IOException e){
                logger.warn("Error to delete {} {} file (may be loaded on reload)", name, key, e);
            }catch (RuntimeException e) {
                logger.warn("Unexpected error arrived while deleting {} {} file (may be loaded on reload)", name, key, e);
            }
        }
        trueRemoved.forEach(removed::remove);

        busyUnlock();
    }

    @Override
    public void load() {
        busyLock();
        logger.info("Loading {}", name);

        Set<String> removeCounter=new HashSet<>(map.keySet());
        map.clear();
        removed.clear();

        if(Files.isDirectory(path)){
            try(Stream<Path> stream=Files.walk(path)){
                stream.filter(ph->Files.isRegularFile(ph, LinkOption.NOFOLLOW_LINKS)
                        &&ph.toString().endsWith(".yml"))
                    .forEach(ph->{
                        try{
                            read(ph);
                        }catch(IdentifyException e){
                            logger.warn("{} {} file contains unallowed symbols, can't record", name, path.relativize(ph));
                        }catch(SerializationException e){
                            logger.warn("Error to deserialize {} {} file", name, path.relativize(ph), e);
                        }catch(ConfigurateException e){
                            logger.warn("Error to read {} {} file", name, path.relativize(ph), e);
                        }catch (RuntimeException e) {
                            logger.warn("Unexpected error arrived while deserializing {} {} file", name, path.relativize(ph), e);
                        }
                    });
            }catch(IOException|RuntimeException e){
                logger.error("Error to walk {} file tree", name, e);
            }
        }

        removeCounter.removeAll(map.keySet());
        removeCounter.forEach(this::listenersRemove);

        busyUnlock();
    }

    @Override
    public void save(String id) {
        busyLock();
        logger.info("Saving {} {}...", name, id);

        path.toFile().mkdirs();

        if(map.containsKey(id)){
            try{
                write(id);
            } catch (SerializationException e) {
                logger.warn("Error to serialize {} {}", name, id, e);
            } catch (IOException e) {
                logger.warn("Error to write {} {} file", name, id, e);
            }catch (RuntimeException e) {
                logger.warn("Unexpected error arrived while serializing {} {}", name, id, e);
            }
        }else if(removed.contains(id)){
            try{
                delete(id);
                removed.remove(id);
            }catch(IOException e){
                logger.warn("Error to delete {} {} file (may be loaded on reload)", name, id, e);
            }catch (RuntimeException e) {
                logger.warn("Unexpected error arrived while deleting {} {} file (may be loaded on reload)", name, id, e);
            }
        }else{
            logger.error("Trying to save unknown element {}", id);
        }

        busyUnlock();
    }

    private YamlConfigurationLoader getLoader(String id){
        return loaders.computeIfAbsent(id, k->{
            Path file = path.resolve(id.replace(File.separator, "/")+".yml");
            return makeLoader(file);
        });
    }

    private void write(String id) throws ConfigurateException {
        YamlConfigurationLoader loader = getLoader(id);

        ConfigurationNode node=loader.createNode();
        node.set(clazz,map.get(id));

        loader.save(node);
    }

    private void delete(String id) throws IOException {
        Path file = path.resolve(id.replace(File.separator, "/")+".yml");
        loaders.remove(id);
        Files.delete(file);
    }

    private void read(Path file) throws ConfigurateException, IdentifyException {
        Path relative=path.relativize(file);

        String fileStr=relative.toString();
        String id=fileStr.substring(0,fileStr.lastIndexOf("."));
        if(!validId.test(id))throw new IdentifyException(relative);

        YamlConfigurationLoader loader = loaders.computeIfAbsent(id,k->makeLoader(file));

        ConfigurationNode node=loader.load();

        T value=node.get(clazz);
        map.put(id,value);
        listenersUpdate(id,value);
    }
}