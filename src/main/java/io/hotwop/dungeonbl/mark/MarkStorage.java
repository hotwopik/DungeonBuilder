package io.hotwop.dungeonbl.mark;

import de.tr7zw.changeme.nbtapi.*;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTCompoundList;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBTList;
import de.tr7zw.changeme.nbtapi.utils.nmsmappings.ReflectionMethod;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.util.math.ImmutableLocation;
import io.hotwop.dungeonbl.util.math.Vec3;
import io.hotwop.dungeonbl.util.view.ListView;
import org.bukkit.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class MarkStorage{
    private static final Map<World,MarkList> worlds=new HashMap<>();
    private static final Map<Chunk,MarkList> chunks=new HashMap<>();
    private static final Map<Location,Mark<?>> locations=new HashMap<>();

    private record MarkList(
        List<Mark<?>> list,
        ListView<Mark<?>> listView
    ){}

    private static MarkList createMarkList(){
        List<Mark<?>> list=new ArrayList<>();
        ListView<Mark<?>> listView=new ListView<>(list);
        return new MarkList(list,listView);
    }

    protected static void add(Location location,Mark<?> mark){
        locations.put(location,mark);
        chunks.computeIfAbsent(location.getChunk(), ch->createMarkList()).list.add(mark);
        worlds.computeIfAbsent(location.getWorld(), wr->createMarkList()).list.add(mark);
    }

    protected static void remove(Location location){
        Mark<?> mark=locations.remove(location);
        if(mark!=null){
            chunks.get(location.getChunk()).list.remove(mark);
            worlds.get(location.getWorld()).list.remove(mark);
        }
    }

    public static @Nullable Mark<?> getMark(Location location){
        return locations.get((location instanceof ImmutableLocation)?location:new ImmutableLocation(location));
    }

    public static boolean isMarkHere(Location location){
        return locations.containsKey((location instanceof ImmutableLocation)?location:new ImmutableLocation(location));
    }

    public static @Nullable List<Mark<?>> getMarks(Chunk chunk){
        MarkList marks=chunks.get(chunk);
        return marks==null?null:marks.listView;
    }

    public static @Nullable List<Mark<?>> getMarks(World world){
        MarkList marks=worlds.get(world);
        return marks==null?null:marks.listView;
    }

    public static void collectToNBTWorld(ReadWriteNBT out, World world){
        MarkList marks=worlds.get(world);
        if(marks==null)return;

        Map<MarkCodec<?>,Map<Location, ReadableNBT>> map=new HashMap<>();
        marks.list.forEach(mark->{
            Map<Location,ReadableNBT> mp=map.computeIfAbsent(mark.markType(),cd->new HashMap<>());
            ReadWriteNBT data= NBT.createNBTObject();
            mark.writeData(data);
            mp.put(mark.location,data);
        });
        collectToNBT(out,map, Vec3::new);
    }

    public static <T> void collectToNBT(
        ReadWriteNBT out,
        Map<MarkCodec<?>,Map<T,ReadableNBT>> map,
        Function<T, Vec3> posMapper
    ){
        map.forEach((codec,marks)->{
            ReadWriteNBTCompoundList list=out.getCompoundList(codec.name);

            marks.forEach((pos,data)->{
                ReadWriteNBT dataTag=NBT.createNBTObject();
                Vec3 posM=posMapper.apply(pos);

                dataTag.setIntArray("pos", DBlUtil.vec3ToArray(posM));
                dataTag.set("data",data, NBTHandlers.STORE_READABLE_TAG);

                list.addCompound(dataTag);
            });
        });
    }

    public static final NamespacedKey worldMarksField=new NamespacedKey(DungeonBl.namespace,"marks");

    public static <T> Map<MarkCodec<?>,Map<T,ReadableNBT>> collectFromNBT(
        ReadableNBT tag,
        Function<Vec3,T> posMapper
    ){
        Map<MarkCodec<?>,Map<T,ReadableNBT>> out=new HashMap<>();
        for(String key:tag.getKeys()){
            if(!tag.hasTag(key, NBTType.NBTTagList))continue;

            ReadableNBTList<ReadWriteNBT> list=tag.getCompoundList(key);
            if(list.isEmpty())continue;

            MarkType type=MarkType.getType(key);
            if(type==null)continue;

            MarkCodec<?> codec=MarkType.asExtend(MarkCodec.class,type);
            Map<T,ReadableNBT> typeMap=new HashMap<>();

            list.forEach(mark->{
                int[] posArr=mark.getIntArray("pos");
                if(posArr==null)return;

                Vec3 pos= DBlUtil.arrayToVec3(posArr);
                if(pos==null)return;

                T posMapped=posMapper.apply(pos);
                ReadWriteNBT data=mark.getCompound("data");

                typeMap.put(posMapped,data);
            });

            out.put(codec,Map.copyOf(typeMap));
        }

        return out;
    }

    public static void loadWorld(World world){
        if(DungeonBl.isDungeonBlWorld(world))return;
        PersistentDataContainer worldContainer=world.getPersistentDataContainer();

        PersistentDataContainer marksContainer=worldContainer.get(worldMarksField, PersistentDataType.TAG_CONTAINER);
        if(marksContainer==null)return;

        ReadWriteNBT nbt=new NBTPersistentDataContainer(marksContainer);
        Map<MarkCodec<?>,Map<Location,ReadableNBT>> marksMap=collectFromNBT(nbt,pos->pos.toLocation(world));

        for(Map.Entry<MarkCodec<?>,Map<Location,ReadableNBT>> codecEntry:marksMap.entrySet()){
            codecEntry.getKey().fromMap(codecEntry.getValue());
        }
    }

    public static void saveWorld(World world){
        if(DungeonBl.isDungeonBlWorld(world))return;
        PersistentDataContainer worldContainer=world.getPersistentDataContainer();
        PersistentDataContainer marksContainer=worldContainer.getAdapterContext().newPersistentDataContainer();

        ReadWriteNBT nbt=new NBTPersistentDataContainer(marksContainer);
        collectToNBTWorld(nbt,world);

        worldContainer.set(worldMarksField,PersistentDataType.TAG_CONTAINER,marksContainer);
    }

    public static void loadAll(){
        logger.info("Loading marks...");
        for(World world: Bukkit.getWorlds()){
            loadWorld(world);
        }
    }

    public static void saveAll(){
        for(World world: Bukkit.getWorlds()){
            saveWorld(world);
        }
    }

    private MarkStorage(){}
}
