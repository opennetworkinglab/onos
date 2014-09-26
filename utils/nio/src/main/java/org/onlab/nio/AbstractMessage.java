package org.onlab.nio;

/**
 * Base {@link Message} implementation.
 */
public abstract class AbstractMessage implements Message {

    protected int length;

    @Override
    public int length() {
        return length;
    }

}
