package org.hobbit.core.data;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class DataReceiveState {

    public final String name;
    public final Map<Integer, byte[]> messageBuffer = new HashMap<>();
    public int nextMessageId;
    public OutputStream outputStream;
    public int lastMessageId = Integer.MAX_VALUE;

    public DataReceiveState(String name, OutputStream outputStream) {
        this.name = name;
        this.outputStream = outputStream;
        nextMessageId = 0;
    }
    
    public void setLastMessageId(int lastMessageId) {
        this.lastMessageId = lastMessageId;
    }
}
