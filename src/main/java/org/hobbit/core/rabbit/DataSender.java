package org.hobbit.core.rabbit;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public interface DataSender extends Closeable {

    public void sendData(byte[] data) throws IOException;

    public void sendData(byte[] data, String dataId) throws IOException;

    public void sendData(InputStream is) throws IOException;

    public void sendData(InputStream is, String dataId) throws IOException;

    public void closeWhenFinished();

}
