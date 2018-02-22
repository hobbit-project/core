package org.hobbit.core.rabbit.consume;

import java.io.Closeable;

import com.rabbitmq.client.Consumer;

public interface MessageConsumer extends Consumer, Closeable {

    public void finishProcessing() throws InterruptedException;

    public abstract int getOpenStreamCount();

    public void waitForTermination();
}
