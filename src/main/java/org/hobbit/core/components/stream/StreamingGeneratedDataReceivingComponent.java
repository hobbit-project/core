package org.hobbit.core.components.stream;

import java.io.InputStream;

import org.hobbit.core.components.Component;

/**
 * This interface is implemented by components that want to receive data that is
 * sent by a data generator component.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface StreamingGeneratedDataReceivingComponent extends Component {

    /**
     * This method is called if a new data stream is received from a data
     * generator.
     * 
     * @param dataStream
     *            the stream from which the data received from a data generator
     *            is read
     */
    public void receiveGeneratedData(InputStream dataStream);

}
