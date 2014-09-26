package org.onlab.nio;

/**
 * Representation of a message transferred via {@link MessageStream}.
 */
public interface Message {

    /**
     * Gets the message length in bytes.
     *
     * @return number of bytes
     */
    int length();

}
