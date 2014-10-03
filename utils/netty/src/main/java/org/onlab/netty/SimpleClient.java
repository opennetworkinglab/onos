package org.onlab.netty;

import java.util.concurrent.TimeUnit;

public final class SimpleClient {
    private SimpleClient() {}

    public static void main(String... args) throws Exception {
        NettyMessagingService messaging = new TestNettyMessagingService(9081);
        messaging.activate();

        messaging.sendAsync(new Endpoint("localhost", 8080), "simple", "Hello World");
        Response<String> response = messaging.sendAndReceive(new Endpoint("localhost", 8080), "echo", "Hello World");
        System.out.println("Got back:" + response.get(2, TimeUnit.SECONDS));
    }

    public static class TestNettyMessagingService extends NettyMessagingService {
        public TestNettyMessagingService(int port) throws Exception {
            super(port);
            Serializer serializer = new KryoSerializer();
            this.serializer = serializer;
        }
    }
}
