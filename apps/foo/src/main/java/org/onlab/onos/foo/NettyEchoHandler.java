package org.onlab.onos.foo;

import java.io.IOException;

import org.onlab.netty.Message;
import org.onlab.netty.MessageHandler;


/**
 * Message handler that echos the message back to the sender.
 */
public class NettyEchoHandler implements MessageHandler {

    @Override
    public void handle(Message message) throws IOException {
        message.respond(message.payload());
    }
}
