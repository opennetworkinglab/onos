package org.onlab.onos.store.messaging;

import java.io.IOException;

/**
 * Handler for a message.
 */
public interface MessageHandler {

    /**
     * Handles the message.
     * @param message message.
     * @throws IOException.
     */
    public void handle(Message message) throws IOException;
}
