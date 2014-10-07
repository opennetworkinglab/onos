package org.onlab.onos.foo;

import java.io.IOException;

import org.jboss.netty.handler.logging.LoggingHandler;
import org.onlab.netty.EchoHandler;
import org.onlab.netty.KryoSerializer;
import org.onlab.netty.NettyMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test to measure Messaging performance.
 */
    public class SimpleNettyServer {
        private static Logger log = LoggerFactory.getLogger(IOLoopTestServer.class);

            private SimpleNettyServer() {}

            public static void main(String... args) throws Exception {
                startStandalone(args);
                System.exit(0);
            }

        public static void startStandalone(String[] args) throws IOException {
                NettyMessagingService server = new NettyMessagingService(8080);
            try {
                server.activate();
            } catch (Exception e) {
                e.printStackTrace();
            }
            server.setSerializer(new KryoSerializer());
                    server.registerHandler("simple",
                        (org.onlab.netty.MessageHandler) new LoggingHandler());
                server.registerHandler("echo", new EchoHandler());
        }
    }

