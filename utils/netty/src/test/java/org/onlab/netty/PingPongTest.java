/*
 * Copyright 2014-2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onlab.netty;

import static org.junit.Assert.assertArrayEquals;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.store.cluster.messaging.Endpoint;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * Simple ping-pong test that exercises NettyMessagingService.
 */
public class PingPongTest {

    @Ignore("Turning off fragile test")
    @Test
    public void testPingPong() throws Exception {
        NettyMessagingService pinger = new NettyMessagingService(8085);
        NettyMessagingService ponger = new NettyMessagingService(9086);
        try {
            pinger.activate();
            ponger.activate();
            ponger.registerHandler("echo", Function.identity(), MoreExecutors.directExecutor());
            byte[] payload = RandomUtils.nextBytes(100);
            CompletableFuture<byte[]> responseFuture =
                    pinger.sendAndReceive(
                            new Endpoint(IpAddress.valueOf(InetAddress.getLocalHost()), 9086), "echo", payload);
            assertArrayEquals(payload, responseFuture.get(10000, TimeUnit.MILLISECONDS));
        } finally {
            pinger.deactivate();
            ponger.deactivate();
        }
    }
}
