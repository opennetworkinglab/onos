package org.onlab.onos.foo;

import org.onlab.netty.EchoHandler;
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

        public static void startStandalone(String[] args) throws Exception {
            NettyMessagingService server = new NettyMessagingService(8080);
            server.activate();
            server.registerHandler("simple", new org.onlab.netty.LoggingHandler());
            server.registerHandler("echo", new EchoHandler());
        }
    }

