package org.hobbit.core.rabbit;

import java.io.InputStream;

public interface IncomingStreamHandler {

    public void handleIncomingStream(String streamId, InputStream stream);
}
