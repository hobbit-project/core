package org.hobbit.core.utils;

import java.util.Random;

public class RandomIdGenerator implements IdGenerator {

    private Random random;

    public RandomIdGenerator() {
        random = new Random();
    }

    public RandomIdGenerator(long seed) {
        random = new Random(seed);
    }

    @Override
    public String getNextId() {
        return Integer.toString(random.nextInt());
    }

}
