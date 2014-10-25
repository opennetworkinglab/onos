package org.onlab.netty;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;

import static org.junit.Assert.*;

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
            ponger.registerHandler("echo", new EchoHandler());
            byte[] payload = RandomUtils.nextBytes(100);
            Future<byte[]> responseFuture = pinger.sendAndReceive(new Endpoint("localhost", 9086), "echo", payload);
            assertArrayEquals(payload, responseFuture.get(10000, TimeUnit.MILLISECONDS));
        } finally {
            pinger.deactivate();
            ponger.deactivate();
        }
    }
}
