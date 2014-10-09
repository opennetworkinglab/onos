package org.onlab.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MessageHandler that simply logs the information.
 */
public class LoggingHandler implements MessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void handle(Message message) {
        log.info("Received message. Payload has {} bytes", message.payload().length);
    }
}
