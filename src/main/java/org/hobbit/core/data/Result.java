package org.hobbit.core.data;

/**
 * Data wrapper for an evaluation result.
 *
 * @author Ruben Taelman (ruben.taelman@ugent.be)
 */
@Deprecated
public interface Result {

    public long getSentTimestamp();
    public byte[] getData();
}
