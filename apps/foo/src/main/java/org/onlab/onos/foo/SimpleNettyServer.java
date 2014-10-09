package org.onlab.onos.foo;

import org.onlab.netty.NettyMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test to measure Messaging performance.
 */
    public final class SimpleNettyServer {
        private static Logger log = LoggerFactory.getLogger(SimpleNettyServer.class);

            private SimpleNettyServer() {}

            public static void main(String... args) throws Exception {
                startStandalone(args);
                System.exit(0);
            }

        public static void startStandalone(String[] args) throws Exception {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 8081;
            NettyMessagingService server = new NettyMessagingService(port);
            server.activate();
            server.registerHandler("simple", new NettyLoggingHandler());
            server.registerHandler("echo", new NettyEchoHandler());
        }
    }

