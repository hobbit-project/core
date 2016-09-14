package org.hobbit.core.components;

/**
 * Wrapper for an expected an actual result.
 *
 * @author Ruben Taelman (ruben.taelman@ugent.be)
 */
public interface ResultPair {

    public Result getExpected();
    public Result getActual();
}
