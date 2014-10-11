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

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws Exception the exception
     */
    public static void main(String... args) throws Exception {
                startStandalone(args);
                System.exit(0);
            }

    /**
     * Start standalone server.
     *
     * @param args the args
     * @throws Exception the exception
     */
    public static void startStandalone(String[] args) throws Exception {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 8081;
            NettyMessagingService server = new NettyMessagingService(port);
            server.activate();
            server.registerHandler("simple", new NettyNothingHandler());
            server.registerHandler("echo", new NettyEchoHandler());
            log.info("Netty Server server on port " + port);
        }
    }

