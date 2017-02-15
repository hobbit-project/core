package org.hobbit.core.utils;

public class SteppingIdGenerator implements IdGenerator {

    private int nextId;
    private int step;

    public SteppingIdGenerator() {
        this(0, 1);
    }

    public SteppingIdGenerator(int startId, int step) {
        this.step = step;
        nextId = startId;
    }

    @Override
    public synchronized String getNextId() {
        String id = Integer.toString(nextId);
        nextId += step;
        return id;
    }

}
