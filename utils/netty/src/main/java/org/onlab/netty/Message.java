package org.onlab.netty;

import java.io.IOException;

/**
 * A unit of communication.
 * Has a payload. Also supports a feature to respond back to the sender.
 */
public interface Message {

    /**
     * Returns the payload of this message.
     * @return message payload.
     */
    public Object payload();

    /**
     * Sends a reply back to the sender of this messge.
     * @param data payload of the response.
     * @throws IOException if there is a communication error.
     */
    public void respond(Object data) throws IOException;
}
