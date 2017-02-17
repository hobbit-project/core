package org.hobbit.core.data;

import java.io.InputStream;

/**
 * Wrapper for an expected an actual result. The results are InputStreams
 * starting with timestamps followed by the expected or actual received data.
 *
 * @author Ruben Taelman (ruben.taelman@ugent.be)
 */
public interface ResultPair {

    public InputStream getExpected();

    public InputStream getActual();
}
