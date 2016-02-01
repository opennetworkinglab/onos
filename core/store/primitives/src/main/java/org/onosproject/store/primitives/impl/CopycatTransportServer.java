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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.onlab.util.Tools;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.MessagingService;

import com.google.common.collect.Maps;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Connection;
import io.atomix.catalyst.transport.Server;
import io.atomix.catalyst.util.concurrent.SingleThreadContext;
import io.atomix.catalyst.util.concurrent.ThreadContext;

/**
 * {@link Server} implementation for {@link CopycatTransport}.
 */
public class CopycatTransportServer implements Server {

    private final AtomicBoolean listening = new AtomicBoolean(false);
    private CompletableFuture<Void> listenFuture = new CompletableFuture<>();
    private final PartitionId partitionId;
    private final MessagingService messagingService;
    private final String messageSubject;
    private final Map<Long, CopycatTransportConnection> connections = Maps.newConcurrentMap();

    CopycatTransportServer(PartitionId partitionId, MessagingService messagingService) {
        this.partitionId = checkNotNull(partitionId);
        this.messagingService = checkNotNull(messagingService);
        this.messageSubject = String.format("onos-copycat-%s", partitionId);
    }

    @Override
    public CompletableFuture<Void> listen(Address address, Consumer<Connection> listener) {
        if (listening.compareAndSet(false, true)) {
            ThreadContext context = ThreadContext.currentContextOrThrow();
            listen(address, listener, context);
        }
        return listenFuture;
    }

    private void listen(Address address, Consumer<Connection> listener, ThreadContext context) {
        messagingService.registerHandler(messageSubject, (sender, payload) -> {
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
                long connectionId = input.readLong();
                InetAddress senderHost = InetAddress.getByAddress(sender.host().toOctets());
                int senderPort = sender.port();
                Address senderAddress = new Address(new InetSocketAddress(senderHost, senderPort));
                AtomicBoolean newConnection = new AtomicBoolean(false);
                CopycatTransportConnection connection = connections.computeIfAbsent(connectionId, k -> {
                    newConnection.set(true);
                    return new CopycatTransportConnection(connectionId,
                            CopycatTransport.Mode.SERVER,
                            partitionId,
                            senderAddress,
                            messagingService,
                            getOrCreateContext(context));
                });
                byte[] request = IOUtils.toByteArray(input);
                return CompletableFuture.supplyAsync(
                        () -> {
                            if (newConnection.get()) {
                                listener.accept(connection);
                            }
                            return connection;
                        }, context.executor()).thenCompose(c -> c.handle(request));
            } catch (IOException e) {
                return Tools.exceptionalFuture(e);
            }
        });
        context.execute(() -> {
            listenFuture.complete(null);
        });
    }

    @Override
    public CompletableFuture<Void> close() {
        messagingService.unregisterHandler(messageSubject);
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
        return new SingleThreadContext("copycat-transport-server-" + partitionId, parentContext.serializer().clone());
    }
}
