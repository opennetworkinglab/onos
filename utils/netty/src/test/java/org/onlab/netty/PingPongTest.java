package org.onlab.netty;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

/**
 * Simple ping-pong test that exercises NettyMessagingService.
 */
public class PingPongTest {

    @Test
    public void testPingPong() throws Exception {
        NettyMessagingService pinger = new NettyMessagingService(8085);
        NettyMessagingService ponger = new NettyMessagingService(9086);
        try {
            pinger.activate();
            ponger.activate();
            pinger.setPayloadSerializer(new KryoSerializer());
            ponger.setPayloadSerializer(new KryoSerializer());
            ponger.registerHandler("echo", new EchoHandler());
            Response<String> response = pinger.sendAndReceive(new Endpoint("localhost", 9086), "echo", "hello");
            Assert.assertEquals("hello", response.get(10000, TimeUnit.MILLISECONDS));
        } finally {
            pinger.deactivate();
            ponger.deactivate();
        }
    }
}
