/*
 * Copyright 2016 Open Networking Laboratory
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.onlab.util.Tools;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.slf4j.Logger;

import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Connection;
import io.atomix.catalyst.transport.Server;
import io.atomix.catalyst.util.concurrent.CatalystThreadFactory;
import io.atomix.catalyst.util.concurrent.SingleThreadContext;
import io.atomix.catalyst.util.concurrent.ThreadContext;

/**
 * {@link Server} implementation for {@link CopycatTransport}.
 */
public class CopycatTransportServer implements Server {

    private final Logger log = getLogger(getClass());
    private final AtomicBoolean listening = new AtomicBoolean(false);
    private CompletableFuture<Void> listenFuture = new CompletableFuture<>();
    private final ScheduledExecutorService executorService;
    private final PartitionId partitionId;
    private final MessagingService messagingService;
    private final String protocolMessageSubject;
    private final String newConnectionMessageSubject;
    private final Map<Long, CopycatTransportConnection> connections = Maps.newConcurrentMap();

    CopycatTransportServer(PartitionId partitionId, MessagingService messagingService) {
        this.partitionId = checkNotNull(partitionId);
        this.messagingService = checkNotNull(messagingService);
        this.protocolMessageSubject = String.format("onos-copycat-server-%s", partitionId);
        this.newConnectionMessageSubject = String.format("onos-copycat-server-connection-%s", partitionId);
        this.executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
                new CatalystThreadFactory("copycat-server-p" + partitionId + "-%d"));
    }

    @Override
    public CompletableFuture<Void> listen(Address address, Consumer<Connection> listener) {
        if (listening.compareAndSet(false, true)) {
            // message handler for all non-connection-establishment messages.
            messagingService.registerHandler(protocolMessageSubject, (sender, payload) -> {
                try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
                    long connectionId = input.readLong();
                    CopycatTransportConnection connection = connections.get(connectionId);
                    if (connection == null) {
                        throw new IOException("Closed connection");
                    }
                    byte[] messagePayload = IOUtils.toByteArray(input);
                    return connection.handle(messagePayload);
                } catch (IOException e) {
                    return Tools.exceptionalFuture(e);
                }
            });

            // message handler for new connection attempts.
            ThreadContext context = ThreadContext.currentContextOrThrow();
            messagingService.registerHandler(newConnectionMessageSubject, (sender, payload) -> {
                long connectionId = Longs.fromByteArray(payload);
                CopycatTransportConnection connection = new CopycatTransportConnection(connectionId,
                        CopycatTransport.Mode.SERVER,
                        partitionId,
                        CopycatTransport.toAddress(sender),
                        messagingService,
                        getOrCreateContext(context));
                connections.put(connectionId, connection);
                connection.closeListener(c -> connections.remove(connectionId, c));
                log.debug("Created new incoming connection[id={}] from {}", connectionId, sender);
                return CompletableFuture.supplyAsync(() -> {
                    listener.accept(connection);
                    // echo the connectionId back to indicate successful completion.
                    return payload;
                }, context.executor());
            });
            context.execute(() -> listenFuture.complete(null));
        }
        return listenFuture;
    }

    @Override
    public CompletableFuture<Void> close() {
        messagingService.unregisterHandler(newConnectionMessageSubject);
        messagingService.unregisterHandler(protocolMessageSubject);
        executorService.shutdown();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns the current execution context or creates one.
     */
    private ThreadContext getOrCreateContext(ThreadContext parentContext) {
        ThreadContext context = ThreadContext.currentContext();
        if (context != null) {
            return context;
        }
        return new SingleThreadContext(executorService, parentContext.serializer().clone());
    }
}
