package org.onlab.onos.store.messaging.impl;

public final class SimpleServer {
    private SimpleServer() {}

    public static void main(String... args) throws Exception {
        NettyMessagingService server = new TestNettyMessagingService();
        server.activate();
        server.registerHandler("simple", new LoggingHandler());
        server.registerHandler("echo", new EchoHandler());
    }

    public static class TestNettyMessagingService extends NettyMessagingService {
        protected TestNettyMessagingService() {
            Serializer serializer = new KryoSerializer();
            this.serializer = serializer;
        }
    }
}
