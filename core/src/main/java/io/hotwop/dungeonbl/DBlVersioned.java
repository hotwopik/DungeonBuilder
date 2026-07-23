package io.hotwop.dungeonbl;

import io.hotwop.dungeonbl.versioned.VersionFoundException;
import io.hotwop.dungeonbl.versioned.VersionedInterface;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public final class DBlVersioned{
    public static final Map<Integer,String> paperMap=Map.of(
        3955,"1_21_1",
        4082,"1_21_3",
        4554,"1_21_9",
        4671,"1_21_11",
        4786,"26_1"
    );

    @SuppressWarnings("deprecation")
    public static int dataVersion(){
        return Bukkit.getUnsafe().getDataVersion();
    }

    public static VersionedInterface getVersionedInterface() throws VersionFoundException{
        int version=dataVersion();

        Optional<Map.Entry<Integer,String>> versionEntry;
        versionEntry=paperMap.entrySet()
            .stream().filter(el->el.getKey()<=version)
            .max(Map.Entry.comparingByKey());

        if(versionEntry.isEmpty())throw new VersionFoundException("Error to load versioned Dungeon Builder");
        String pack=versionEntry.get().getValue();

        ClassLoader loader=DBlVersioned.class.getClassLoader();
        Class<?> clazz;
        try{
            clazz=loader.loadClass("io.hotwop.dungeonbl.versioned.v"+pack+".VersionHolder");
        }catch(ClassNotFoundException e){
            throw new VersionFoundException("Error to load version: "+version,e);
        }

        Constructor<?> constructor;
        try{
            constructor=clazz.getConstructor();
        }catch(NoSuchMethodException e){
            throw new VersionFoundException("Error to get version constructor: "+version,e);
        }

        try{
            return (VersionedInterface)constructor.newInstance();
        }catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new VersionFoundException("Error to invoke version constructor: "+version,e);
        }
    }

    private DBlVersioned(){}
}
