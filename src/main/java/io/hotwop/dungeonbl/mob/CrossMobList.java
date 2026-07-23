package io.hotwop.dungeonbl.mob;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

public final class CrossMobList{
    public final List<List<CrossMob>> list;
    public final int mobCount;

    public CrossMobList(List<List<CrossMob>> list){
        Objects.requireNonNull(list);
        List<List<CrossMob>> locked=new ArrayList<>();
        int mobCount=0;
        for(List<CrossMob> pool:list){
            locked.add(List.copyOf(pool));
            mobCount+=pool.size();
        }
        this.list=List.copyOf(locked);
        this.mobCount=mobCount;
    }

    public static class Serializer implements TypeSerializer<CrossMobList>{
        public static final Serializer instance=new Serializer();
        private Serializer(){}

        public CrossMobList deserialize(@NotNull Type typ, @NotNull ConfigurationNode node) throws SerializationException {
            if(node.isMap())return new CrossMobList(List.of(deserializePool(node)));
            else if(node.isList()){
                List<List<CrossMob>> list=new ArrayList<>();
                for(ConfigurationNode sub:node.childrenList())list.add(deserializePool(sub));
                return new CrossMobList(list);
            }else throw new SerializationException(node,CrossMobList.class,"Map or list expected");
        }
        private List<CrossMob> deserializePool(ConfigurationNode node) throws SerializationException{
            if(!node.isMap())throw new SerializationException(node, Map.class,"Map expected");

            List<CrossMob> mobs=new ArrayList<>();
            for(MobType type:MobType.values()){
                List<String> list=node.node(type.name()).getList(String.class);
                if(list==null)continue;

                list.forEach(el->mobs.add(new CrossMob(type,el)));
            }
            return mobs;
        }

        public void serialize(@NotNull Type type,@Nullable CrossMobList obj, @NotNull ConfigurationNode node) throws SerializationException {
            if(obj==null||obj.list.isEmpty())return;

            if(obj.list.size()==1)serializePool(obj.list.getFirst(),node);
            else for(List<CrossMob> pool:obj.list)serializePool(pool,node.appendListNode());
        }
        private void serializePool(List<CrossMob> pool,ConfigurationNode node) throws SerializationException{
            Map<String,List<String>> mobs=new HashMap<>();
            pool.forEach(mob->{
                List<String> typ=mobs.computeIfAbsent(mob.type().name(),k->new ArrayList<>());
                typ.add(mob.mob());
            });
            node.set(mobs);
        }

        @Override
        public @Nullable CrossMobList emptyValue(@NotNull Type specificType, @NotNull ConfigurationOptions options) {
            return new CrossMobList(List.of());
        }
    }
}
