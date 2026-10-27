package io.hotwop.dungeonbl.mark.codec;

import de.tr7zw.changeme.nbtapi.iface.NBTHandler;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public final class SerializerContext{
    private final List<SerializerData<?,?,?>> serializers;
    private final Map<String,SerializerData<?,?,?>> replaceSerializers;

    private SerializerContext(List<SerializerData<?,?,?>> serializers,Map<String,SerializerData<?,?,?>> replaceSerializers){
        this.serializers=serializers;
        this.replaceSerializers=replaceSerializers;
    }

    @SuppressWarnings("unchecked")
    public <T> SerializerData<T,?,?> getSerializerData(Class<T> clazz){
        List<SerializerData<?,?,?>> candidates = new ArrayList<>(serializers.stream()
            .filter(ser->ser.serializable.isAssignableFrom(clazz))
            .toList()
        );
        List<Class<? super T>> sups=getSups(clazz);

        int last=-1;
        SerializerData<?, ?, ?> out=null;
        for(SerializerData<?, ?, ?> candidate:candidates){
            int index=sups.indexOf(candidate.serializable);
            if(index==-1)continue;

            if(last==-1||index<last){
                last=index;
                out=candidate;
            }
        }
        return (SerializerData<T,?,?>)out;
    }

    public @Nullable SerializerData<?,?,?> getSerializerById(String id){
        return replaceSerializers.get(id);
    }

    @SuppressWarnings("unchecked")
    private <T> List<Class<? super T>> getSups(Class<? super T> clazz){
        List<Class<? super T>> out=new ArrayList<>();
        if(clazz==null)return out;

        List<Class<? super T>> current=new ArrayList<>();
        List<Class<? super T>> next=new ArrayList<>();

        current.add(clazz);
        while(!current.isEmpty()){
            for(Class<? super T> classCheck:current){
                if(classCheck==null||classCheck.equals(Object.class))continue;
                out.add(classCheck);

                next.add(classCheck.getSuperclass());
                Arrays.stream(classCheck.getInterfaces())
                    .forEach(el->next.add((Class<? super T>)el));
            }

            current.clear();
            current.addAll(next);
            next.clear();
        }

        return out;
    }

    public <T> MarkDataSerializer<T,?,?> getSerializer(Class<T> clazz){
        SerializerData<T,?,?> data=getSerializerData(clazz);
        if(data!=null)return data.serializer;
        else return null;
    }

    public static Builder builder(){
        return new Builder();
    }
    public static Builder builder(SerializerContext origin){
        return new Builder(origin.serializers,origin.replaceSerializers);
    }

    public record SerializerData<T,V,N>(
        Class<T> serializable,
        Class<V> argument,
        Class<N> nbt,
        MarkDataSerializer<T,V,N> serializer
    ){
        @SuppressWarnings("unchecked")
        public void put(ReadWriteNBT out, String name, Object obj) throws MarkSerializeException {
            T val;
            try{
                val=(T)obj;
            }catch(ClassCastException e){
                return;
            }

            serializer.validate(val);
            out.set(name,serializer.toNBT(val),serializer.getHandler());
        }

        @SuppressWarnings("unchecked")
        public String stringify(Object obj){
            try{
                return serializer.stringify((T)obj);
            }catch(ClassCastException w){
                return "";
            }
        }

        public String processArgument(
            String name,
            ReadWriteNBT out,
            CommandSender ctx,
            CommandArguments args
        ) throws MarkSerializeException, WrapperCommandSyntaxException{
            V obj=args.getByClass(name,argument);
            if(obj==null)throw new IllegalArgumentException("No such argument: "+name);

            T val=serializer.processArgument(obj,ctx);

            put(out,name,val);
            return serializer.stringify(val);
        }

        public T get(ReadableNBT in,String name) throws MarkSerializeException {
            T val=serializer.fromNBT(in.get(name,serializer.getHandler()));
            serializer.validate(val);
            return val;
        }
    }
    public static final class Builder{
        private final Map<Class<?>,SerializerData<?,?,?>> serializers=new HashMap<>();
        private final Map<String,SerializerData<?,?,?>> replaceSerializers=new HashMap<>();

        private Builder(){}
        private Builder(List<SerializerData<?,?,?>> serializers,Map<String,SerializerData<?,?,?>> replaceSerializers){
            serializers.forEach(ser->this.serializers.put(ser.serializable,ser));
            this.replaceSerializers.putAll(replaceSerializers);
        }

        public <T,V,N> Builder register(
            Class<T> serializable,Class<V> argument,Class<N> nbt,
            MarkDataSerializer<T,V,N> serializer
        ){
            serializers.put(serializable,new SerializerData<>(serializable,argument,nbt,serializer));

            return this;
        }

        public <T,V,N> Builder register(
            String id,
            Class<T> serializable,Class<V> argument,Class<N> nbt,
            MarkDataSerializer<T,V,N> serializer
        ){
            replaceSerializers.put(id,new SerializerData<>(serializable,argument,nbt,serializer));

            return this;
        }

        public <T,V,N> Builder registerOverride(
            String id,
            Class<T> serializable, Class<V> argument, Class<N> nbt,
            MarkDataSerializer<T,V,N> parent,
            T defaultValue,Validator<T> validator, Function<String, Argument<V>> argumentBuilder
        ){
            replaceSerializers.put(id,new SerializerData<>(serializable, argument, nbt, new MarkDataSerializer<>() {
                @Override
                public NBTHandler<N> getHandler(){
                    return parent.getHandler();
                }

                @Override
                public T fromNBT(N data) throws MarkSerializeException {
                    return parent.fromNBT(data);
                }

                @Override
                public N toNBT(T obj) throws MarkSerializeException {
                    return parent.toNBT(obj);
                }

                @Override
                public Argument<V> getArgument(String name) {
                    return argumentBuilder.apply(name);
                }

                @Override
                public T processArgument(V obj, CommandSender ctx) throws WrapperCommandSyntaxException {
                    return parent.processArgument(obj,ctx);
                }

                @Override
                public T defaultValue() {
                    return defaultValue;
                }

                @Override
                public String stringify(T obj) {
                    return parent.stringify(obj);
                }

                @Override
                public void validate(T obj) throws MarkSerializeException {
                    parent.validate(obj);
                    validator.validate(obj);
                }
            }));

            return this;
        }

        public SerializerContext build(){
            List<SerializerData<?,?,?>> data=new ArrayList<>();
            serializers.forEach((clazz,ser)->data.add(ser));
            return new SerializerContext(List.copyOf(data),Map.copyOf(replaceSerializers));
        }
    }

    @FunctionalInterface
    public interface Validator<T>{
        void validate(T obj) throws MarkSerializeException;
    }
}
