/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.store.primitives.impl;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.google.common.collect.Lists;
import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Client;
import io.atomix.catalyst.transport.Server;
import io.atomix.catalyst.transport.Transport;
import io.atomix.copycat.protocol.ConnectRequest;
import io.atomix.copycat.protocol.ConnectResponse;
import io.atomix.copycat.protocol.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.util.Tools;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.cluster.messaging.MessagingException;
import org.onosproject.store.cluster.messaging.MessagingService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.onlab.junit.TestTools.findAvailablePort;

/**
 * Copycat transport test.
 */
public class CopycatTransportTest {

    private static final String IP_STRING = "127.0.0.1";

    private Endpoint endpoint1 = new Endpoint(IpAddress.valueOf(IP_STRING), 5001);
    private Endpoint endpoint2 = new Endpoint(IpAddress.valueOf(IP_STRING), 5002);

    private TestMessagingService clientService;
    private TestMessagingService serverService;

    private Transport clientTransport;
    private ThreadContext clientContext;

    private Transport serverTransport;
    private ThreadContext serverContext;

    @Before
    public void setUp() throws Exception {
        Map<Endpoint, TestMessagingService> services = new ConcurrentHashMap<>();

        endpoint1 = new Endpoint(IpAddress.valueOf("127.0.0.1"), findAvailablePort(5001));
        clientService = new TestMessagingService(endpoint1, services);
        clientTransport = new CopycatTransport(PartitionId.from(1), clientService);
        clientContext = new SingleThreadContext("client-test-%d", CatalystSerializers.getSerializer());

        endpoint2 = new Endpoint(IpAddress.valueOf("127.0.0.1"), findAvailablePort(5003));
        serverService = new TestMessagingService(endpoint2, services);
        serverTransport = new CopycatTransport(PartitionId.from(1), serverService);
        serverContext = new SingleThreadContext("server-test-%d", CatalystSerializers.getSerializer());
    }

    @After
    public void tearDown() throws Exception {
        if (clientContext != null) {
            clientContext.close();
        }
        if (serverContext != null) {
            serverContext.close();
        }
    }

    /**
     * Tests sending a message from the client side of a Copycat connection to the server side.
     */
    @Test
    public void testCopycatClientConnectionSend() throws Exception {
        Client client = clientTransport.client();
        Server server = serverTransport.server();

        CountDownLatch latch = new CountDownLatch(4);
        CountDownLatch listenLatch = new CountDownLatch(1);
        CountDownLatch handlerLatch = new CountDownLatch(1);
        serverContext.executor().execute(() -> {
            server.listen(new Address(IP_STRING, endpoint2.port()), connection -> {
                serverContext.checkThread();
                latch.countDown();
                connection.handler(ConnectRequest.class, request -> {
                    serverContext.checkThread();
                    latch.countDown();
                    return CompletableFuture.completedFuture(ConnectResponse.builder()
                            .withStatus(Response.Status.OK)
                            .withLeader(new Address(IP_STRING, endpoint2.port()))
                            .withMembers(Lists.newArrayList(new Address(IP_STRING, endpoint2.port())))
                            .build());
                });
                handlerLatch.countDown();
            }).thenRun(listenLatch::countDown);
        });

        listenLatch.await(5, TimeUnit.SECONDS);

        clientContext.executor().execute(() -> {
            client.connect(new Address(IP_STRING, endpoint2.port())).thenAccept(connection -> {
                clientContext.checkThread();
                latch.countDown();
                try {
                    handlerLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    fail();
                }
                connection.<ConnectRequest, ConnectResponse>sendAndReceive(ConnectRequest.builder()
                        .withClientId(UUID.randomUUID().toString())
                        .build())
                        .thenAccept(response -> {
                            clientContext.checkThread();
                            assertNotNull(response);
                            assertEquals(Response.Status.OK, response.status());
                            latch.countDown();
                        });
            });
        });

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    /**
     * Tests sending a message from the server side of a Copycat connection to the client side.
     */
    @Test
    public void testCopycatServerConnectionSend() throws Exception {
        Client client = clientTransport.client();
        Server server = serverTransport.server();

        CountDownLatch latch = new CountDownLatch(4);
        CountDownLatch listenLatch = new CountDownLatch(1);
        serverContext.executor().execute(() -> {
            server.listen(new Address(IP_STRING, endpoint2.port()), connection -> {
                serverContext.checkThread();
                latch.countDown();
                serverContext.schedule(Duration.ofMillis(100), () -> {
                    connection.<ConnectRequest, ConnectResponse>sendAndReceive(ConnectRequest.builder()
                            .withClientId("foo")
                            .build())
                            .thenAccept(response -> {
                                serverContext.checkThread();
                                assertEquals(Response.Status.OK, response.status());
                                latch.countDown();
                            });
                });
            }).thenRun(listenLatch::countDown);
        });

        listenLatch.await(5, TimeUnit.SECONDS);

        clientContext.executor().execute(() -> {
            client.connect(new Address(IP_STRING, endpoint2.port())).thenAccept(connection -> {
                clientContext.checkThread();
                latch.countDown();
                connection.handler(ConnectRequest.class, request -> {
                    clientContext.checkThread();
                    latch.countDown();
                    assertEquals("foo", request.client());
                    return CompletableFuture.completedFuture(ConnectResponse.builder()
                            .withStatus(Response.Status.OK)
                            .withLeader(new Address(IP_STRING, endpoint2.port()))
                            .withMembers(Lists.newArrayList(new Address(IP_STRING, endpoint2.port())))
                            .build());
                });
            });
        });

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    /**
     * Tests closing the server side of a Copycat connection.
     */
    @Test
    public void testCopycatClientConnectionClose() throws Exception {
        Client client = clientTransport.client();
        Server server = serverTransport.server();

        CountDownLatch latch = new CountDownLatch(5);
        CountDownLatch listenLatch = new CountDownLatch(1);
        serverContext.executor().execute(() -> {
            server.listen(new Address(IP_STRING, endpoint2.port()), connection -> {
                serverContext.checkThread();
                latch.countDown();
                connection.onClose(c -> {
                    serverContext.checkThread();
                    latch.countDown();
                });
            }).thenRun(listenLatch::countDown);
        });

        listenLatch.await(5, TimeUnit.SECONDS);

        clientContext.executor().execute(() -> {
            client.connect(new Address(IP_STRING, endpoint2.port())).thenAccept(connection -> {
                clientContext.checkThread();
                latch.countDown();
                connection.onClose(c -> {
                    clientContext.checkThread();
                    latch.countDown();
                });
                clientContext.schedule(Duration.ofMillis(100), () -> {
                    connection.close().whenComplete((result, error) -> {
                        clientContext.checkThread();
                        latch.countDown();
                    });
                });
            });
        });

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    /**
     * Tests that a client connection is closed on exception.
     */
    @Test
    public void testCopycatClientConnectionCloseOnException() throws Exception {
        Client client = clientTransport.client();
        Server server = serverTransport.server();

        CountDownLatch listenLatch = new CountDownLatch(1);
        CountDownLatch closeLatch = new CountDownLatch(1);
        CountDownLatch latch = new CountDownLatch(1);
        serverContext.executor().execute(() -> {
            server.listen(new Address(IP_STRING, endpoint2.port()), connection -> {
                serverContext.checkThread();
            }).thenRun(listenLatch::countDown);
        });

        listenLatch.await(5, TimeUnit.SECONDS);

        clientContext.executor().execute(() -> {
            client.connect(new Address(IP_STRING, endpoint2.port())).thenAccept(connection -> {
                clientContext.checkThread();
                serverService.handlers.clear();
                connection.onClose(c -> latch.countDown());
                connection.<ConnectRequest, ConnectResponse>sendAndReceive(ConnectRequest.builder()
                        .withClientId(UUID.randomUUID().toString())
                        .build())
                        .thenAccept(response -> fail());
            });
        });

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    /**
     * Tests closing the server side of a Copycat connection.
     */
    @Test
    public void testCopycatServerConnectionClose() throws Exception {
        Client client = clientTransport.client();
        Server server = serverTransport.server();

        CountDownLatch latch = new CountDownLatch(5);
        CountDownLatch listenLatch = new CountDownLatch(1);
        serverContext.executor().execute(() -> {
            server.listen(new Address(IP_STRING, endpoint2.port()), connection -> {
                serverContext.checkThread();
                latch.countDown();
                connection.onClose(c -> {
                    latch.countDown();
                });
                serverContext.schedule(Duration.ofMillis(100), () -> {
                    connection.close().whenComplete((result, error) -> {
                        serverContext.checkThread();
                        latch.countDown();
                    });
                });
            }).thenRun(listenLatch::countDown);
        });

        listenLatch.await(5, TimeUnit.SECONDS);

        clientContext.executor().execute(() -> {
            client.connect(new Address(IP_STRING, endpoint2.port())).thenAccept(connection -> {
                clientContext.checkThread();
                latch.countDown();
                connection.onClose(c -> {
                    latch.countDown();
                });
            });
        });

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    /**
     * Tests that a server connection is closed on exception.
     */
    @Test
    public void testCopycatServerConnectionCloseOnException() throws Exception {
        Client client = clientTransport.client();
        Server server = serverTransport.server();

        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch listenLatch = new CountDownLatch(1);
        CountDownLatch connectLatch = new CountDownLatch(1);
        serverContext.executor().execute(() -> {
            server.listen(new Address(IP_STRING, endpoint2.port()), connection -> {
                serverContext.checkThread();
                serverContext.executor().execute(() -> {
                    try {
                        connectLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        fail();
                    }
                    clientService.handlers.clear();
                    connection.onClose(c -> latch.countDown());
                    connection.<ConnectRequest, ConnectResponse>sendAndReceive(ConnectRequest.builder()
                            .withClientId("foo")
                            .build())
                            .thenAccept(response -> fail());
                });
            }).thenRun(listenLatch::countDown);
        });

        listenLatch.await(5, TimeUnit.SECONDS);

        clientContext.executor().execute(() -> {
            client.connect(new Address(IP_STRING, endpoint2.port())).thenAccept(connection -> {
                clientContext.checkThread();
                connectLatch.countDown();
            });
        });

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    /**
     * Custom implementation of {@code MessagingService} used for testing. Really, this should
     * be mocked but suffices for now.
     */
    public static final class TestMessagingService implements MessagingService {
        private final Endpoint endpoint;
        private final Map<Endpoint, TestMessagingService> services;
        private final Map<String, BiFunction<Endpoint, byte[], CompletableFuture<byte[]>>> handlers =
                new ConcurrentHashMap<>();

        TestMessagingService(Endpoint endpoint, Map<Endpoint, TestMessagingService> services) {
            this.endpoint = endpoint;
            this.services = services;
            services.put(endpoint, this);
        }

        private CompletableFuture<byte[]> handle(Endpoint ep, String type, byte[] message, Executor executor) {
            BiFunction<Endpoint, byte[], CompletableFuture<byte[]>> handler = handlers.get(type);
            if (handler == null) {
                return Tools.exceptionalFuture(new MessagingException.NoRemoteHandler());
            }
            return handler.apply(ep, message).thenApplyAsync(r -> r, executor);
        }

        @Override
        public CompletableFuture<Void> sendAsync(Endpoint ep, String type, byte[] payload) {
            // Unused for testing
            return null;
        }

        @Override
        public CompletableFuture<byte[]> sendAndReceive(Endpoint ep, String type, byte[] payload) {
            // Unused for testing
            return null;
        }

        @Override
        public CompletableFuture<byte[]> sendAndReceive(Endpoint ep, String type, byte[] payload, Executor executor) {
            TestMessagingService service = services.get(ep);
            if (service == null) {
                return Tools.exceptionalFuture(new IllegalStateException());
            }
            return service.handle(endpoint, type, payload, executor);
        }

        @Override
        public void registerHandler(String type, BiConsumer<Endpoint, byte[]> handler, Executor executor) {
            // Unused for testing
        }

        @Override
        public void registerHandler(String type, BiFunction<Endpoint, byte[], byte[]> handler, Executor executor) {
            // Unused for testing
        }

        @Override
        public void registerHandler(String type, BiFunction<Endpoint, byte[], CompletableFuture<byte[]>> handler) {
            handlers.put(type, handler);
        }

        @Override
        public void unregisterHandler(String type) {
            handlers.remove(type);
        }
    }

}