/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onlab.nio.service;

import static org.onlab.util.Tools.groupedThreads;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.onlab.nio.AcceptorLoop;
import org.onlab.nio.SelectorLoop;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;

/**
 * MessagingService implementation based on IOLoop.
 */
public class IOLoopMessaging implements MessagingService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String REPLY_MESSAGE_TYPE = "ONOS_REQUEST_REPLY";

    static final long TIMEOUT = 1000;

    static final boolean SO_NO_DELAY = false;
    static final int SO_SEND_BUFFER_SIZE = 128 * 1024;
    static final int SO_RCV_BUFFER_SIZE = 128 * 1024;

    private static final int NUM_WORKERS = 8;

    private AcceptorLoop acceptorLoop;
    private final ExecutorService acceptorThreadPool =
            Executors.newSingleThreadExecutor(groupedThreads("onos/nio/messaging", "acceptor"));
    private final ExecutorService ioThreadPool =
            Executors.newFixedThreadPool(NUM_WORKERS, groupedThreads("onos/nio/messaging", "io-loop-worker-%d"));

    private final List<DefaultIOLoop> ioLoops = Lists.newArrayList();

    private int lastWorker = -1;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private Endpoint localEp;

    private GenericKeyedObjectPool<Endpoint, DefaultMessageStream> streams =
            new GenericKeyedObjectPool<>(new DefaultMessageStreamFactory());

    private final ConcurrentMap<String, Consumer<DefaultMessage>> handlers = new ConcurrentHashMap<>();
    private final AtomicLong messageIdGenerator = new AtomicLong(0);
    private final Cache<Long, CompletableFuture<byte[]>> responseFutures = CacheBuilder.newBuilder()
            .removalListener(new RemovalListener<Long, CompletableFuture<byte[]>>() {
                @Override
                public void onRemoval(RemovalNotification<Long, CompletableFuture<byte[]>> entry) {
                    if (entry.wasEvicted()) {
                        entry.getValue().completeExceptionally(new TimeoutException("Timedout waiting for reply"));
                    }
                }
            })
            .build();

    /**
     * Activates IO Loops.
     *
     * @param localEp local end-point
     * @throws IOException is activation fails
     */
    public void start(Endpoint localEp) throws IOException {
        if (started.get()) {
            log.warn("IOMessaging is already running at {}", localEp);
            return;
        }
        this.localEp = localEp;
        streams.setLifo(false);
        this.acceptorLoop = new DefaultAcceptorLoop(new InetSocketAddress(localEp.host().toString(), localEp.port()));

        for (int i = 0; i < NUM_WORKERS; i++) {
            ioLoops.add(new DefaultIOLoop(this::dispatchLocally));
        }

        ioLoops.forEach(ioThreadPool::execute);
        acceptorThreadPool.execute(acceptorLoop);
        ioLoops.forEach(loop -> loop.awaitStart(TIMEOUT));
        acceptorLoop.awaitStart(TIMEOUT);
        started.set(true);
    }

    /**
     * Shuts down IO loops.
     */
    public void stop() {
        if (started.get()) {
            ioLoops.forEach(SelectorLoop::shutdown);
            acceptorLoop.shutdown();
            ioThreadPool.shutdown();
            acceptorThreadPool.shutdown();
            started.set(false);
        }
    }


    @Override
    public CompletableFuture<Void> sendAsync(Endpoint ep, String type, byte[] payload) {
        DefaultMessage message = new DefaultMessage(
                messageIdGenerator.incrementAndGet(),
                localEp,
                type,
                payload);
        return sendAsync(ep, message);
    }

    protected CompletableFuture<Void> sendAsync(Endpoint ep, DefaultMessage message) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (ep.equals(localEp)) {
            dispatchLocally(message);
            future.complete(null);
            return future;
        }

        DefaultMessageStream stream = null;
        try {
            stream = streams.borrowObject(ep);
            stream.write(message);
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        } finally {
            try {
                streams.returnObject(ep, stream);
            } catch (Exception e) {
                log.warn("Failed to return stream to pool");
            }
        }
        return future;
    }

    @Override
    public CompletableFuture<byte[]> sendAndReceive(
            Endpoint ep,
            String type,
            byte[] payload) {
        CompletableFuture<byte[]> response = new CompletableFuture<>();
        Long messageId = messageIdGenerator.incrementAndGet();
        responseFutures.put(messageId, response);
        DefaultMessage message = new DefaultMessage(messageId, localEp, type, payload);
        try {
            sendAsync(ep, message);
        } catch (Exception e) {
            responseFutures.invalidate(messageId);
            response.completeExceptionally(e);
        }
        return response;
    }

    @Override
    public void registerHandler(String type, Consumer<byte[]> handler, Executor executor) {
        handlers.put(type, message -> executor.execute(() -> handler.accept(message.payload())));
    }

    @Override
    public void registerHandler(String type, Function<byte[], byte[]> handler, Executor executor) {
        handlers.put(type, message -> executor.execute(() -> {
            byte[] responsePayload = handler.apply(message.payload());
            if (responsePayload != null) {
                DefaultMessage response = new DefaultMessage(message.id(),
                        localEp,
                        REPLY_MESSAGE_TYPE,
                        responsePayload);
                sendAsync(message.sender(), response).whenComplete((result, error) -> {
                    log.debug("Failed to respond", error);
                });
            }
        }));
    }

    @Override
    public void registerHandler(String type, Function<byte[], CompletableFuture<byte[]>> handler) {
        handlers.put(type, message -> {
            handler.apply(message.payload()).whenComplete((result, error) -> {
                if (error == null) {
                    DefaultMessage response = new DefaultMessage(message.id(),
                        localEp,
                        REPLY_MESSAGE_TYPE,
                        result);
                    sendAsync(message.sender(), response).whenComplete((r, e) -> {
                        if (e != null) {
                            log.debug("Failed to respond", e);
                        }
                    });
                }
            });
        });
    }

    @Override
    public void unregisterHandler(String type) {
        handlers.remove(type);
    }

    protected void dispatchLocally(DefaultMessage message) {
        String type = message.type();
        if (REPLY_MESSAGE_TYPE.equals(type)) {
            try {
                CompletableFuture<byte[]> futureResponse =
                        responseFutures.getIfPresent(message.id());
                if (futureResponse != null) {
                    futureResponse.complete(message.payload());
                } else {
                    log.warn("Received a reply for message id:[{}]. "
                            + " from {}. But was unable to locate the"
                            + " request handle", message.id(), message.sender());
                }
            } finally {
                responseFutures.invalidate(message.id());
            }
            return;
        }
        Consumer<DefaultMessage> handler = handlers.get(type);
        if (handler != null) {
            handler.accept(message);
        } else {
            log.debug("No handler registered for {}", type);
        }
    }

    // Get the next worker to which a client should be assigned
    private synchronized DefaultIOLoop nextWorker() {
        lastWorker = (lastWorker + 1) % NUM_WORKERS;
        return ioLoops.get(lastWorker);
    }

    /**
     * Initiates open connection request and registers the pending socket
     * channel with the given IO loop.
     *
     * @param loop loop with which the channel should be registered
     * @throws java.io.IOException if the socket could not be open or connected
     */
    private DefaultMessageStream createConnection(Endpoint ep, DefaultIOLoop loop) throws IOException {
        SocketAddress sa = new InetSocketAddress(ep.host().toString(), ep.port());
        SocketChannel ch = SocketChannel.open();
        ch.configureBlocking(false);
        DefaultMessageStream stream = loop.connectStream(ch);
        ch.connect(sa);
        return stream;
    }

    // Loop for accepting client connections
    private class DefaultAcceptorLoop extends AcceptorLoop {

        public DefaultAcceptorLoop(SocketAddress address) throws IOException {
            super(DefaultIOLoop.SELECT_TIMEOUT_MILLIS, address);
        }

        @Override
        protected void acceptConnection(ServerSocketChannel channel) throws IOException {
            SocketChannel sc = channel.accept();
            sc.configureBlocking(false);

            Socket so = sc.socket();
            so.setTcpNoDelay(SO_NO_DELAY);
            so.setReceiveBufferSize(SO_RCV_BUFFER_SIZE);
            so.setSendBufferSize(SO_SEND_BUFFER_SIZE);

            nextWorker().acceptStream(sc);
        }
    }

    private class DefaultMessageStreamFactory implements KeyedPoolableObjectFactory<Endpoint, DefaultMessageStream> {

        @Override
        public void activateObject(Endpoint endpoint, DefaultMessageStream stream) throws Exception {
        }

        @Override
        public void destroyObject(Endpoint ep, DefaultMessageStream stream) throws Exception {
            stream.close();
        }

        @Override
        public DefaultMessageStream makeObject(Endpoint ep) throws Exception {
            DefaultMessageStream stream = createConnection(ep, nextWorker()).connectedFuture().get();
            log.info("Established a new connection to {}", ep);
            return stream;
        }

        @Override
        public void passivateObject(Endpoint ep, DefaultMessageStream stream) throws Exception {
        }

        @Override
        public boolean validateObject(Endpoint ep, DefaultMessageStream stream) {
            return stream.isClosed();
        }
    }
}
