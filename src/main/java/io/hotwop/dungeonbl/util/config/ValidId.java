package io.hotwop.dungeonbl.util.config;

import io.hotwop.dungeonbl.io.DBlRegistry;
import org.jspecify.annotations.NonNull;
import org.spongepowered.configurate.objectmapping.meta.Constraint;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;

public @interface ValidId{
    final class Factory implements Constraint.Factory<ValidId,String>{
        public static final Factory instance=new Factory();
        private Factory(){}

        @Override
        public @NonNull Constraint<String> make(@NonNull ValidId data, @NonNull Type type) {
            return val->{
                if(val!=null&&!DBlRegistry.validId.test(val))throw
                    new SerializationException("Is invalid id: \""+val+"\"");
            };
        }
    }
}
