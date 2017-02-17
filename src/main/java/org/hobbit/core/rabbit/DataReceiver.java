package org.hobbit.core.rabbit;

import java.io.Closeable;

/**
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface DataReceiver extends Closeable {

    public int getErrorCount();

    public void closeWhenFinished();

}
