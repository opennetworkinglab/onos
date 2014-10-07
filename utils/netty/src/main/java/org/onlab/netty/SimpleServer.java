package org.onlab.netty;

//FIXME: Should be move out to test or app
public final class SimpleServer {
    private SimpleServer() {}

    public static void main(String... args) throws Exception {
        NettyMessagingService server = new NettyMessagingService(8080);
        server.activate();
        server.registerHandler("simple", new LoggingHandler());
        server.registerHandler("echo", new EchoHandler());
    }
}
