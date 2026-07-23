package io.hotwop.dungeonbl.mark.codec;

import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ArgumentContext{
    private final Map<String,ArgumentData<?,?>> map;

    private ArgumentContext(Map<String,ArgumentData<?,?>> map){
        this.map=map;
    }

    public @Nullable ArgumentData<?,?> get(String id){
        return map.get(id);
    }

    public record ArgumentData<T,V>(
        Class<T> arg,
        Class<V> type,
        BiFunction<T, CommandSender,V> processor,
        Function<String,Argument<T>> argument
    ){
        public Argument<T> argument(String name){
            return argument.apply(name);
        }

        public String process(
            String name,
            CommandSender ctx,
            SerializerContext.SerializerData<?,?,?> ser,
            ReadWriteNBT out,
            CommandArguments args
        ) throws SerializeException{
            T obj=args.getByClass(name,arg);
            if(obj==null)throw new IllegalArgumentException("No such argument: "+name);

            V proc=processor.apply(obj,ctx);
            ser.put(out,name,proc);
            return ser.stringify(proc);
        }
    }

    public static Builder builder(){
        return new Builder();
    }

    public static final class Builder{
        private final Map<String,ArgumentData<?,?>> map=new HashMap<>();

        private Builder(){}

        public <T,V> Builder define(
            String id,
            Class<T> arg,
            Class<V> type,
            BiFunction<T,CommandSender,V> processor,
            Function<String,Argument<T>> argument
        ){
            if(map.containsKey(id))throw new RuntimeException("Error build argument context, id already defined: "+id);
            map.put(id,new ArgumentData<>(arg,type,processor,argument));
            return this;
        }
        public ArgumentContext build(){
            return new ArgumentContext(Map.copyOf(map));
        }
    }

    @FunctionalInterface
    public interface ArgumentSupplier<T>{
        Argument<T> get(String name);
    }
}
