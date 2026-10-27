package io.hotwop.dungeonbl.io;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.structure.StructureSerializeException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class StructRegistry<T> extends DBlRegistry<T>{
    public final Path path;

    public final StructEncoder<T> encoder;
    public final StructDecoder<T> decoder;

    public StructRegistry(
        String name,
        Path path,

        StructEncoder<T> encoder,
        StructDecoder<T> decoder
    ) {
        super(name);
        path.toFile().mkdir();

        this.path = path;

        this.encoder = encoder;
        this.decoder = decoder;
    }

    @Override
    protected void onAdd(String id, T value){
        DungeonBl.executorService.execute(()->{
            busyLock();

            try{
                write(id);
            } catch (StructureSerializeException e) {
                logger.warn("Error to serialize {} {}", name, id, e);
            } catch (IOException e) {
                logger.warn("Error to write {} {} file", name, id, e);
            }catch (RuntimeException e) {
                logger.warn("Unexpected error arrived while serializing {} {}", name, id, e);
            }

            busyUnlock();
        });
    }

    @Override
    protected void onRemove(String id){
        DungeonBl.executorService.execute(()->{
            busyLock();

            try{
                delete(id);
                removed.remove(id);
            }catch(IOException e){
                logger.warn("Error to delete {} {} file (may be loaded on reload)", name, id, e);
            }catch (RuntimeException e) {
                logger.warn("Unexpected error arrived while deleting {} {} file (may be loaded on reload)", name, id, e);
            }

            busyUnlock();
        });
    }

    @Override
    public void save(){
        busyLock();
        logger.info("Saving {}...", name);

        path.toFile().mkdirs();

        for(String key:map.keySet()){
            try{
                write(key);
            } catch (StructureSerializeException e) {
                logger.warn("Error to serialize {} {}", name, key, e);
            } catch (IOException e) {
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
    public void load(){
        busyLock();
        logger.info("Loading {}", name);

        Set<String> removeCounter=new HashSet<>(map.keySet());
        map.clear();
        removed.clear();

        if(Files.isDirectory(path)){
            try(Stream<Path> stream=Files.walk(path)){
                stream.filter(ph->Files.isRegularFile(ph,LinkOption.NOFOLLOW_LINKS)
                        &&ph.toString().endsWith(".str"))
                    .forEach(ph->{
                        try{
                            read(ph);
                        }catch(IdentifyException e){
                            logger.warn("{} {} file contains unallowed symbols, can't record", name, path.relativize(ph));
                        }catch(IOException e){
                            logger.warn("Error to read {} {} file", name, path.relativize(ph), e);
                        }catch(StructureSerializeException e){
                            logger.warn("Error to deserialize {} {} file", name, path.relativize(ph), e);
                        } catch (RuntimeException e) {
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
        logger.info("Saving {} {}", name, id);

        path.toFile().mkdirs();

        if(map.containsKey(id)){
            try{
                write(id);
            } catch (StructureSerializeException e) {
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

    private void write(String id) throws IOException, StructureSerializeException {
        Path file = path.resolve(id.replace(File.separator, "/")+".str");
        T value = map.get(id);
        ReadWriteNBT data = encoder.toNBT(value);
        NBT.writeFile(file.toFile(),data);
        logger.info("File write {}",file);
    }

    private void delete(String id) throws IOException {
        Path file = path.resolve(id.replace(File.separator, "/")+".str");
        Files.delete(file);
    }

    private void read(Path file) throws IOException, StructureSerializeException, IdentifyException {
        Path relative=path.relativize(file);

        String fileStr=relative.toString();
        String id=fileStr.substring(0,fileStr.lastIndexOf("."));
        if(!validId.test(id))throw new IdentifyException(relative);

        ReadWriteNBT nbt=NBT.readFile(file.toFile());
        if(nbt.getKeys().isEmpty())throw new StructureSerializeException("Nothing to deserialize");

        T value=decoder.fromNBT(nbt);
        map.put(id,value);
        listenersUpdate(id,value);
    }

    @FunctionalInterface
    public interface StructEncoder<T>{
        ReadWriteNBT toNBT(T value) throws StructureSerializeException;
    }

    @FunctionalInterface
    public interface StructDecoder<T>{
        T fromNBT(ReadableNBT value) throws StructureSerializeException;
    }
}
