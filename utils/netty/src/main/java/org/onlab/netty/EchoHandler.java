package org.onlab.netty;

import java.io.IOException;

/**
 * Message handler that echos the message back to the sender.
 */
public class EchoHandler implements MessageHandler {

    @Override
    public void handle(Message message) throws IOException {
        System.out.println("Received message. Echoing it back to the sender.");
        message.respond(message.payload());
    }
}
