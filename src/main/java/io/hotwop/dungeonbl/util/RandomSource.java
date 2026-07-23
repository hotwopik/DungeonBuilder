package io.hotwop.dungeonbl.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class RandomSource{
    private final Random random=new Random();
    private final Map<String,Random> randomStorage=new HashMap<>();

    public RandomSource(){}

    public Random get(String name){
        return randomStorage.computeIfAbsent(name,k->new Random(random.nextLong()^System.nanoTime()));
    }
}
