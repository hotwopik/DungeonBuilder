package io.hotwop.dungeonbl.util.config;

import org.jspecify.annotations.NonNull;
import org.spongepowered.configurate.objectmapping.meta.Constraint;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;

public @interface ConfigDoubleRange{
    double min() default Double.MIN_VALUE;
    double max() default Double.MAX_VALUE;

    final class Factory implements Constraint.Factory<ConfigDoubleRange,Number>{
        public static final Factory instance=new Factory();
        private Factory(){}

        @Override
        public @NonNull Constraint<Number> make(@NonNull ConfigDoubleRange data, @NonNull Type type) {
            return value->{
                if(value!=null&&(value.doubleValue()<data.min()||value.doubleValue()>data.max())){
                    if(data.min()==Double.MIN_VALUE&&data.max()==Double.MAX_VALUE){
                        throw new SerializationException("Value can be any number but somehow it wasn't fulfilled");
                    }else if(data.min()==Double.MIN_VALUE){
                        throw new SerializationException("Value must be less than "+data.max());
                    }else if(data.max()==Double.MAX_VALUE){
                        throw new SerializationException("Value must be greater than "+data.min());
                    }else{
                        throw new SerializationException("Value must be less than "+data.max()+" and greater than "+data.min());
                    }
                }
            };
        }
    }
}
