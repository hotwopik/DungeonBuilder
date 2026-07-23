package io.hotwop.dungeonbl.util.config;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.meta.Constraint;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigIntRange {
    int min() default Integer.MIN_VALUE;
    int max() default Integer.MAX_VALUE;

    final class Factory implements Constraint.Factory<@NonNull ConfigIntRange,Number>{
        public static final Factory instance=new Factory();
        private Factory(){}

        @NotNull
        public Constraint<Number> make(ConfigIntRange data, @NotNull Type type) {
            return value->{
                if(value!=null&&(value.intValue()<data.min()||value.intValue()>data.max())){
                    if(data.min()==Integer.MIN_VALUE&&data.max()==Integer.MAX_VALUE){
                        throw new SerializationException("Value can be any number but somehow it wasn't fulfilled");
                    }else if(data.min()==Integer.MIN_VALUE){
                        throw new SerializationException("Value must be less than "+data.max());
                    }else if(data.max()==Integer.MAX_VALUE){
                        throw new SerializationException("Value must be greater than "+data.min());
                    }else{
                        throw new SerializationException("Value must be less than "+data.max()+" and greater than "+data.min());
                    }
                }
            };
        }
    }
}
