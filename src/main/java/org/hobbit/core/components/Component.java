package org.hobbit.core.components;

import java.io.Closeable;

/**
 * The basic interface of a hobbit component.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface Component extends Closeable {

    /**
     * This method initializes the component.
     * 
     * @throws Exception
     *             if an error occurs during the initialization
     */
    public void init() throws Exception;

    /**
     * This method executes the component.
     * 
     * @throws Exception
     *             if an error occurs during the execution
     */
    public void run() throws Exception;
}
