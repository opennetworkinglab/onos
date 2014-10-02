package org.onlab.onos.store.messaging.impl;

import java.io.IOException;

import org.onlab.onos.store.messaging.Message;
import org.onlab.onos.store.messaging.MessageHandler;

/**
 * Message handler that echos the message back to the sender.
 */
public class EchoHandler implements MessageHandler {

    @Override
    public void handle(Message message) throws IOException {
        System.out.println("Received: " + message.payload() + ". Echoing it back to the sender.");
        message.respond(message.payload());
    }
}
