package org.onlab.onos.store.messaging.impl;

import org.onlab.onos.store.cluster.impl.MessageSerializer;

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
            MessageSerializer mgr = new MessageSerializer();
            mgr.activate();
            this.serializationService = mgr;
        }
    }
}
