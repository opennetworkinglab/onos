package org.onlab.netty;

/**
 * A MessageHandler that simply logs the information.
 */
public class LoggingHandler implements MessageHandler {

    @Override
    public void handle(Message message) {
        System.out.println("Received: " + message.payload());
    }
}
