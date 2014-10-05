package org.onlab.netty;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message handler that echos the message back to the sender.
 */
public class EchoHandler implements MessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void handle(Message message) throws IOException {
        log.info("Received message. Echoing it back to the sender.");
        message.respond(message.payload());
    }
}
