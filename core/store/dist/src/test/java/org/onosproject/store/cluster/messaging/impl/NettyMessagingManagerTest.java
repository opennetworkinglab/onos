/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.store.cluster.messaging.impl;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterMetadata;
import org.onosproject.cluster.ClusterMetadataEventListener;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.HybridLogicalClockService;
import org.onosproject.core.HybridLogicalTime;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.cluster.messaging.Endpoint;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onlab.junit.TestTools.findAvailablePort;

/**
 * Unit tests for NettyMessaging.
 */
public class NettyMessagingManagerTest {

    HybridLogicalClockService testClockService = new HybridLogicalClockService() {
        AtomicLong counter = new AtomicLong();
        @Override
        public HybridLogicalTime timeNow() {
            return new HybridLogicalTime(counter.incrementAndGet(), 0);
        }

        @Override
        public void recordEventTime(HybridLogicalTime time) {
        }
    };

    NettyMessagingManager netty1;
    NettyMessagingManager netty2;

    private static final String DUMMY_NAME = "node";
    private static final String IP_STRING = "127.0.0.1";

    Endpoint ep1 = new Endpoint(IpAddress.valueOf(IP_STRING), 5001);
    Endpoint ep2 = new Endpoint(IpAddress.valueOf(IP_STRING), 5002);
    Endpoint invalidEndPoint = new Endpoint(IpAddress.valueOf(IP_STRING), 5003);

    @Before
    public void setUp() throws Exception {
        ep1 = new Endpoint(IpAddress.valueOf("127.0.0.1"), findAvailablePort(5001));
        netty1 = new NettyMessagingManager();
        netty1.clusterMetadataService = dummyMetadataService(DUMMY_NAME, IP_STRING, ep1);
        netty1.clockService = testClockService;
        netty1.activate();

        ep2 = new Endpoint(IpAddress.valueOf("127.0.0.1"), findAvailablePort(5003));
        netty2 = new NettyMessagingManager();
        netty2.clusterMetadataService = dummyMetadataService(DUMMY_NAME, IP_STRING, ep2);
        netty2.clockService = testClockService;
        netty2.activate();
    }

    /**
     * Returns a random String to be used as a test subject.
     * @return string
     */
    private String nextSubject() {
        return UUID.randomUUID().toString();
    }

    @After
    public void tearDown() throws Exception {
        if (netty1 != null) {
            netty1.deactivate();
        }

        if (netty2 != null) {
            netty2.deactivate();
        }
    }

    @Test
    public void testSendAsync() {
        String subject = nextSubject();
        CountDownLatch latch1 = new CountDownLatch(1);
        CompletableFuture<Void> response = netty1.sendAsync(ep2, subject, "hello world".getBytes());
        response.whenComplete((r, e) -> {
            assertNull(e);
            latch1.countDown();
        });
        Uninterruptibles.awaitUninterruptibly(latch1);

        CountDownLatch latch2 = new CountDownLatch(1);
        response = netty1.sendAsync(invalidEndPoint, subject, "hello world".getBytes());
        response.whenComplete((r, e) -> {
            assertNotNull(e);
            latch2.countDown();
        });
        Uninterruptibles.awaitUninterruptibly(latch2);
    }

    @Test
    public void testSendAndReceive() {
        String subject = nextSubject();
        AtomicBoolean handlerInvoked = new AtomicBoolean(false);
        AtomicReference<byte[]> request = new AtomicReference<>();
        AtomicReference<Endpoint> sender = new AtomicReference<>();

        BiFunction<Endpoint, byte[], byte[]> handler = (ep, data) -> {
            handlerInvoked.set(true);
            sender.set(ep);
            request.set(data);
            return "hello there".getBytes();
        };
        netty2.registerHandler(subject, handler, MoreExecutors.directExecutor());

        CompletableFuture<byte[]> response = netty1.sendAndReceive(ep2, subject, "hello world".getBytes());
        assertTrue(Arrays.equals("hello there".getBytes(), response.join()));
        assertTrue(handlerInvoked.get());
        assertTrue(Arrays.equals(request.get(), "hello world".getBytes()));
        assertEquals(ep1, sender.get());
    }

    /*
     * Supplies executors when registering a handler and calling sendAndReceive and verifies the request handling
     * and response completion occurs on the expected thread.
     */
    @Test
    @Ignore
    public void testSendAndReceiveWithExecutor() {
        String subject = nextSubject();
        ExecutorService completionExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "completion-thread"));
        ExecutorService handlerExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "handler-thread"));
        AtomicReference<String> handlerThreadName = new AtomicReference<>();
        AtomicReference<String> completionThreadName = new AtomicReference<>();

        final CountDownLatch latch = new CountDownLatch(1);

        BiFunction<Endpoint, byte[], byte[]> handler = (ep, data) -> {
            handlerThreadName.set(Thread.currentThread().getName());
            try {
                latch.await();
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
                fail("InterruptedException");
            }
            return "hello there".getBytes();
        };
        netty2.registerHandler(subject, handler, handlerExecutor);

        CompletableFuture<byte[]> response = netty1.sendAndReceive(ep2,
                                                                   subject,
                                                                   "hello world".getBytes(),
                                                                   completionExecutor);
        response.whenComplete((r, e) -> {
            completionThreadName.set(Thread.currentThread().getName());
        });
        latch.countDown();

        // Verify that the message was request handling and response completion happens on the correct thread.
        assertTrue(Arrays.equals("hello there".getBytes(), response.join()));
        assertEquals("completion-thread", completionThreadName.get());
        assertEquals("handler-thread", handlerThreadName.get());
    }

    private ClusterMetadataService dummyMetadataService(String name, String ipAddress, Endpoint ep) {
        return new ClusterMetadataService() {
            @Override
            public ClusterMetadata getClusterMetadata() {
                return new ClusterMetadata(new ProviderId(DUMMY_NAME, DUMMY_NAME),
                                           name, Sets.newHashSet(), Sets.newHashSet());
            }

            @Override
            public ControllerNode getLocalNode() {
                return new ControllerNode() {
                    @Override
                    public NodeId id() {
                        return null;
                    }

                    @Override
                    public IpAddress ip() {
                        return IpAddress.valueOf(ipAddress);
                    }

                    @Override
                    public int tcpPort() {
                        return ep.port();
                    }
                };
            }

            @Override
            public void addListener(ClusterMetadataEventListener listener) {}

            @Override
            public void removeListener(ClusterMetadataEventListener listener) {}
        };
    }
}
