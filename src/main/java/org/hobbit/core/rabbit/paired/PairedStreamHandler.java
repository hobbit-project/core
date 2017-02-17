package org.hobbit.core.rabbit.paired;

import java.io.InputStream;

import org.hobbit.core.rabbit.IncomingStreamHandler;

public interface PairedStreamHandler extends IncomingStreamHandler {

    public void handleIncomingStreams(String streamId, InputStream[] streams);
}
