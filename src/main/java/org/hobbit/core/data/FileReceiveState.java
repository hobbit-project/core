package org.hobbit.core.data;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class FileReceiveState {

    public final String name;
    public final Map<Integer, byte[]> messageBuffer = new HashMap<>();
    public int nextMessageId;
    public OutputStream outputStream;

    public FileReceiveState(String name, OutputStream outputStream) {
        this.name = name;
        this.outputStream = outputStream;
        nextMessageId = 0;
    }
}
