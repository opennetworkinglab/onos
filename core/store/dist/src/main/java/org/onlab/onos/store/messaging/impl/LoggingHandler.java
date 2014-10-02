package org.onlab.onos.store.messaging.impl;

import org.onlab.onos.store.messaging.Message;
import org.onlab.onos.store.messaging.MessageHandler;

/**
 * A MessageHandler that simply logs the information.
 */
public class LoggingHandler implements MessageHandler {

    @Override
    public void handle(Message message) {
        System.out.println("Received: " + message.payload());
    }
}