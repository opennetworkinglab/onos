package org.onlab.onos.foo;

import org.onlab.netty.Message;
import org.onlab.netty.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MessageHandler that simply logs the information.
 */
public class NettyNothingHandler implements MessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void handle(Message message) {
      // Do nothing
    }
}
