package io.hotwop.dungeonbl.mark.codec;

import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;

import java.util.*;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class SerializerContext{
    private final List<SerializerData<?,?,?>> serializers;

    private SerializerContext(List<SerializerData<?,?,?>> serializers){
        this.serializers=serializers;
    }

    @SuppressWarnings("unchecked")
    public <T> SerializerData<T,?,?> getSerializerData(Class<T> clazz){
        List<SerializerData<?,?,?>> candidates = new ArrayList<>(serializers.stream()
            .filter(ser->ser.serializable.isAssignableFrom(clazz))
            .toList()
        );
        List<Class<? super T>> sups=new ArrayList<>();
        getSups(sups,clazz);

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
    @SuppressWarnings("unchecked")
    private <T> void getSups(List<Class<? super T>> list,Class<? super T> clazz){
        if(clazz==null)return;
        list.add(clazz);

        getSups(list,clazz.getSuperclass());
        Arrays.stream(clazz.getInterfaces()).forEach(cl->getSups(list,(Class<? super T>)cl));
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
        return new Builder(origin.serializers);
    }

    public record SerializerData<T,V,N>(
        Class<T> serializable,
        Class<V> argument,
        Class<N> nbt,
        MarkDataSerializer<T,V,N> serializer
    ){
        @SuppressWarnings("unchecked")
        public void put(ReadWriteNBT out, String name, Object obj) throws SerializeException{
            try{
                out.set(name,serializer.toNBT((T)obj),serializer.getHandler());
            }catch(ClassCastException ignore){}
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
        ) throws SerializeException, WrapperCommandSyntaxException{
            V obj=args.getByClass(name,argument);
            if(obj==null)throw new IllegalArgumentException("No such argument: "+name);

            T val=serializer.processArgument(obj,ctx);
            put(out,name,val);
            return serializer.stringify(val);
        }

        public T get(ReadableNBT in,String name) throws SerializeException{
            return serializer.fromNBT(in.get(name,serializer.getHandler()));
        }
    }
    public static final class Builder{
        private final Map<Class<?>,SerializerData<?,?,?>> serializers=new HashMap<>();

        private Builder(){}
        private Builder(List<SerializerData<?,?,?>> serializers){
            serializers.forEach(ser->this.serializers.put(ser.serializable,ser));
        }

        public <T,V,N> Builder register(Class<T> serializable,Class<V> argument,Class<N> nbt,MarkDataSerializer<T,V,N> serializer){
            serializers.put(serializable,new SerializerData<>(serializable,argument,nbt,serializer));

            return this;
        }

        public SerializerContext build(){
            List<SerializerData<?,?,?>> data=new ArrayList<>();
            serializers.forEach((clazz,ser)->data.add(ser));
            return new SerializerContext(List.copyOf(data));
        }
    }
}
