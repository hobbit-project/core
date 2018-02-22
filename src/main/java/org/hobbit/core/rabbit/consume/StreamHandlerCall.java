package org.hobbit.core.rabbit.consume;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.rabbit.IncomingStreamHandler;

/**
 * A simple {@link Runnable} implementation that calls the given
 * {@link IncomingStreamHandler} with the given {@link InputStream} and stream
 * ID.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class StreamHandlerCall implements Runnable {

    private String streamId;
    private InputStream stream;
    private IncomingStreamHandler handler;

    public StreamHandlerCall(String streamId, InputStream stream, IncomingStreamHandler handler) {
        this.streamId = streamId;
        this.stream = stream;
        this.handler = handler;
    }

    @Override
    public void run() {
        handler.handleIncomingStream(streamId, stream);
        IOUtils.closeQuietly(stream);
    }

}