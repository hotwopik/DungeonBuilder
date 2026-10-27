package io.hotwop.dungeonbl.mark.codec;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.mark.MarkStorage;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMark;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMarkData;
import io.hotwop.dungeonbl.mark.Mark;
import io.hotwop.dungeonbl.mark.MarkType;
import io.hotwop.dungeonbl.mark.codec.serializer.*;
import io.hotwop.dungeonbl.mark.place.MarkPlaceData;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemType;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class MarkCodec<T extends Mark<T>> extends MarkType{
    public static final SerializerContext default_context=SerializerContext.builder()
        .register(int.class,int.class,int.class,IntSerializer.instance)
        .register(double.class,double.class,double.class,DoubleSerializer.instance)
        .register(Location.class,Location.class,ReadableNBT.class,LocationSerializer.instance)
        .register(NamespacedKey.class,NamespacedKey.class,String.class,NamespacedKeySerializer.instance)
        .register(String.class,String.class,String.class,StringSerializer.instance)
        .register(SideRotation.class, SideRotation.class,int.class, RotationSerializer.instance)
        .register(PotionEffectType.class,PotionEffectType.class,String.class,PotionEffectTypeSerializer.instance)

        .register("PositiveInt",int.class,int.class,int.class,PositiveIntSerializer.instance)
        .register("PositiveDouble",double.class,double.class,double.class,PositiveDoubleSerializer.instance)
        .register("DungeonId",String.class,String.class,String.class,DungeonIdSerializer.instance)
        .register("DoorwayId",String.class,String.class,String.class,DoorwayIdSerializer.instance)
        .register("Time",int.class,int.class,int.class,TimeSerializer.instance)
        .register("IdString",String.class,String.class,String.class,IdStringSerializer.instance)

        .build();

    public static <T extends Mark<T>> MarkCodec<T> createCodec(
        String name,
        ItemType item,
        String displayName,

        Class<T> clazz
    ){
        return createCodec(
            name,
            item,
            displayName,
            clazz,
            default_context
        );
    }

    public static <T extends Mark<T>> MarkCodec<T> createCodec(
        String name,
        ItemType item,
        String displayName,

        Class<T> clazz,
        SerializerContext context
    ){
        return new MarkCodec<>(name,item,displayName,clazz,null,getClassInfo(clazz,context));
    }


    public static <T extends DungeonMark<T,P>,P extends MarkPlaceData> MarkCodec<T> createCodec(
        String name,
        ItemType item,
        String displayName,

        Class<T> clazz,
        DungeonMarkData<T,P> dungeon
    ){
        return createCodec(
            name,
            item,
            displayName,
            clazz,
            dungeon,
            default_context
        );
    }

    public static <T extends DungeonMark<T,P>,P extends MarkPlaceData> MarkCodec<T> createCodec(
        String name,
        ItemType item,
        String displayName,

        Class<T> clazz,
        DungeonMarkData<T,P> dungeon,
        SerializerContext context
    ){
        return new MarkCodec<>(name,item,displayName,clazz,dungeon,getClassInfo(clazz,context));
    }

    private record FieldInfo(
        SerializerContext.SerializerData<?,?,?> serializerData,
        MarkData dataPoint,
        String name
    ){}

    private record ClassInfo<T extends Mark<T>>(
        Map<Constructor<T>,List<String>> constructors,
        List<Field> fields,
        Map<String,FieldInfo> fieldInfo
    ){}

    @SuppressWarnings("unchecked")
    private static <T extends Mark<T>> ClassInfo<T> getClassInfo(
        Class<T> clazz,
        SerializerContext context
    ){
        Set<Field> fieldList=new HashSet<>();
        findFields(clazz,fieldList);

        List<Field> fields=new ArrayList<>();
        Map<String,FieldInfo> info=new TreeMap<>(Comparator.naturalOrder());

        for(Field field:fieldList){
            field.setAccessible(true);

            MarkData dat=field.getAnnotationsByType(MarkData.class)[0];
            Class<?> fieldType=field.getType();

            SerializerContext.SerializerData<?,?,?> ser;

            if(dat.argument().isEmpty()){
                ser=context.getSerializerData(fieldType);

                if(ser==null)throw
                    new RuntimeException("Error to initialize MarkCodec, MarkData error: "+fieldType.getCanonicalName()+" has no serializer");
            }else{
                ser=context.getSerializerById(dat.argument());

                if(ser==null)throw
                    new RuntimeException("Error to initialize MarkCodec, MarkData error: unknown context argument: "+dat.argument());

                Class<?> serType=ser.serializable();

                if(!serType.isAssignableFrom(fieldType))throw
                    new RuntimeException(
                        "Error to initialize MarkCodec, MarkData error: context argument type("+
                        serType.getCanonicalName()+
                        ") not super of field type("+
                        fieldType.getCanonicalName()+")"
                    );
            }

            fields.add(field);
            String nam=field.getName();
            info.put(nam,new FieldInfo(ser,dat,DBlUtil.toLowerString(nam)));
        }

        Map<Constructor<T>,List<String>> constructors=new HashMap<>();
        for(Constructor<T> ct:(Constructor<T>[])clazz.getDeclaredConstructors()){
            ct.setAccessible(true);

            MarkCreator data=ct.getAnnotation(MarkCreator.class);
            if(ct.getParameterCount()!=data.value().length+1)throw
                new RuntimeException("Error to initialize MarkCodec, MarkCreator error: parameters count not equals given info count");

            Class<?>[] params=ct.getParameterTypes();
            if(!Location.class.isAssignableFrom(params[0]))throw
                new RuntimeException("Error to initialize MarkCodec, MarkCreator error: first parameter isn't Location");

            List<String> infList=new ArrayList<>();
            String[] infu=data.value();
            for(int i=0;i<infu.length;++i){
                String inf=infu[i];
                FieldInfo dat=info.get(inf);
                if(dat==null)throw new RuntimeException("Error to initialize MarkCodec, MarkCreator error: unknown info "+inf);
                if(!dat.serializerData.serializable().isAssignableFrom(params[i+1]))throw
                    new RuntimeException(
                        "Error to initialize MarkCodec, MarkCreator error: serializer class "+
                        dat.serializerData.serializable().getCanonicalName()+
                        " not equals parameter class "+params[i+1].getCanonicalName()
                    );
                infList.add(inf);
            }
            constructors.put(ct,List.copyOf(infList));
        }

        return new ClassInfo<>(
            constructors,
            fields,
            info
        );
    }

    private static void findFields(Class<?> clazz, Set<Field> set){
        Arrays.stream(clazz.getDeclaredFields())
            .filter(fl->fl.isAnnotationPresent(MarkData.class)&&checkFlags(fl.accessFlags()))
            .forEach(set::add);

        Class<?> sup=clazz.getSuperclass();
        if(sup!=null&&!sup.equals(Mark.class))findFields(sup,set);
    }

    private static boolean checkFlags(Set<AccessFlag> flags){
        return !(flags.contains(AccessFlag.STATIC)||flags.contains(AccessFlag.TRANSIENT));
    }

    public final Class<T> clazz;
    public final DungeonMarkData<?,?> dungeon;
    private final ClassInfo<T> classInfo;

    private MarkCodec(
        String name,
        ItemType item,
        String displayName,

        Class<T> clazz,
        DungeonMarkData<?,?> dungeon,
        ClassInfo<T> classInfo
    ){
        super(name,item,displayName);
        this.clazz=clazz;
        this.classInfo=classInfo;
        this.dungeon=dungeon;
    }

    @Override
    public void place(Location location, ReadableNBT data){
        fromCompound(location,data,false);
    }

    public void fromCompound(Location location,ReadableNBT data,boolean configFields){
        if(MarkStorage.isMarkHere(location))return;
        List<String> names=new ArrayList<>();
        List<Object> values=new ArrayList<>();

        for(Map.Entry<String,FieldInfo> inf:classInfo.fieldInfo.entrySet()){
            FieldInfo dat=inf.getValue();
            names.add(inf.getKey());
            if(!configFields&&dat.dataPoint.saveOnly()){
                values.add(dat.serializerData.serializer().defaultValue());
                continue;
            }

            if(data.hasTag(dat.name)){
                try{
                    values.add(dat.serializerData.get(data,dat.name));
                }catch(MarkSerializeException e){
                    logger.warn(
                        "Error to deserialize mark {} field {}, serializer {} : {}",
                        clazz.getName(),
                        inf,
                        dat.serializerData.serializer().getClass().getName(),
                        e.getMessage()
                    );
                    return;
                }
            }else if(!dat.dataPoint.optional()){
                logger.warn("Error to deserialize mark {}, missing field {}", clazz.getName(), inf.getKey());
                return;
            }else{
                values.add(dat.serializerData.serializer().defaultValue());
            }
        }

        instance(location,names,values);
    }

    public void fromMap(Map<Location,ReadableNBT> map){
        map.forEach((loc,data)->fromCompound(loc,data,true));
    }

    private void instance(Location location,List<String> names,List<Object> values){
        for(Map.Entry<Constructor<T>,List<String>> ct:classInfo.constructors.entrySet()){
            if(new HashSet<>(ct.getValue()).containsAll(names)){
                Constructor<T> constructor=ct.getKey();
                List<Object> args=new ArrayList<>();
                args.add(location);
                for(String arg:ct.getValue()){
                    int index=names.indexOf(arg);
                    if(index==-1)args.add(null);
                    else args.add(values.get(index));
                }

                try{
                    T mark=constructor.newInstance(args.toArray());
                    if(location.isChunkLoaded())mark.startTicking();
                    return;
                }catch(InstantiationException|IllegalAccessException|InvocationTargetException ex){
                    logger.error("Error to create mark instance: {}", ex.getMessage());
                    return;
                }
            }
        }

        StringBuilder list=new StringBuilder();
        boolean first=true;
        for(String info:names){
            if(first)first=false;
            else list.append(", ");
            list.append(info);
        }

        logger.error("Error to create mark instance: haven't constructor for this set of parameters: {}", list);
    }

    public <V> V toMap(T mark, ReadWriteNBT out, Function<Vec3,V> posMapper){
        writeData(mark,out);
        return posMapper.apply(new Vec3(mark.location));
    }

    public void writeData(Mark<T> mark, ReadWriteNBT out){
        for(Field fl:classInfo.fields){
            String name=fl.getName();
            FieldInfo dat=classInfo.fieldInfo.get(name);
            try{
                Object obj=fl.get(mark);
                if(obj==null){
                    if(dat.dataPoint.optional())continue;
                    else{
                        logger.error("Error to serialize mark, missing non-optional field {}", name);
                        return;
                    }
                }
                dat.serializerData.put(out,dat.name,obj);
            }catch(IllegalAccessException ignore){}
            catch(MarkSerializeException e){
                logger.error(
                    "Error to serialize mark {} field {}, serializer {} : {}",
                    clazz.getName(),
                    name,
                    dat.serializerData.serializer().getClass().getName(),
                    e.getMessage()
                );
                return;
            }
        }
    }

    public CommandAPICommand createCommand(){
        CommandAPICommand builder=new CommandAPICommand(name);

        List<Argument<?>> required=new ArrayList<>();
        List<Argument<?>> opts=new ArrayList<>();

        classInfo.fieldInfo.values()
            .stream()
            .sorted(Comparator.comparingInt(val->val.dataPoint.order()))
            .forEach((data)->{
                if(data.dataPoint.saveOnly())return;

                Argument<?> argument=data.serializerData.serializer()
                    .getArgument(data.name);

                if(data.dataPoint.optional())opts.add(argument);
                else required.add(argument);
            });

        builder.withArguments(required);
        builder.withOptionalArguments(opts);
        builder.executesPlayer(this::giveCommand);

        return builder;
    }

    @Override
    public ItemData createItemData(CommandSender ctx, CommandArguments args) throws WrapperCommandSyntaxException {
        ReadWriteNBT out=NBT.createNBTObject();
        List<String> lore=new ArrayList<>();

        for(Map.Entry<String,FieldInfo> entry:classInfo.fieldInfo.entrySet()){
            FieldInfo dat=entry.getValue();
            if(dat.dataPoint.saveOnly())continue;

            try{
                String obj=dat.serializerData.processArgument(dat.name,out,ctx,args);
                String lor=dat.dataPoint.lore();

                if(!lor.isEmpty())lore.add(lor.replace("%s",obj));
            }catch(IllegalArgumentException e){
                if(!dat.dataPoint.optional())throw CommandAPI.failWithString("Argument isn't present as expected: "+dat.name);
                else{
                    try{
                        SerializerContext.SerializerData<?,?,?> data=dat.serializerData;
                        Object val=data.serializer().defaultValue();
                        data.put(out,name,val);
                        String obj=data.stringify(val);

                        String lor=dat.dataPoint.lore();
                        if(!lor.isEmpty())lore.add(lor.replace("%s",obj));
                    }catch(MarkSerializeException ignore){}
                }
            }catch(MarkSerializeException e){
                throw CommandAPI.failWithString("Argument "+dat.name+" specified incorrectly: "+e.getMessage());
            }
        }

        return new ItemData(out,lore);
    }

    public static final class Info{
        private final ReadableNBT data;
        private final MarkCodec<?> codec;

        public Info(ReadableNBT data, MarkCodec<?> codec){
            this.data=data;
            this.codec=codec;
        }

        @SuppressWarnings("unchecked")
        public <T> T get(String name, Class<T> clazz){
            FieldInfo info=codec.classInfo.fieldInfo.get(name);

            if(info==null)throw new NullPointerException(name+" is not in codec data");
            if(!info.serializerData.serializable().isAssignableFrom(clazz))
                throw new NullPointerException(name+" recorded in codec incorrectly");

            SerializerContext.SerializerData<T,?,?> ser=(SerializerContext.SerializerData<T,?,?>)info.serializerData;

            if(!data.hasTag(name)){
                if(info.dataPoint.optional())return ser.serializer().defaultValue();
                else throw new NullPointerException(name+" is missing");
            }

            try{
                return ser.get(data,name);
            }catch(MarkSerializeException e){
                throw new RuntimeException(name+" deserialized incorrectly");
            }
        }
    }
}
