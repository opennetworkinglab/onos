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
package org.onosproject.store.primitives.resources.impl;

import com.google.common.util.concurrent.Uninterruptibles;

import io.atomix.AtomixClient;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.local.LocalServerRegistry;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;
import io.atomix.manager.internal.ResourceManagerState;
import io.atomix.resource.ResourceType;
import org.onlab.junit.TestTools;
import org.onosproject.store.primitives.impl.CatalystSerializers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for various Atomix tests.
 */
public abstract class AtomixTestBase {
    protected static LocalServerRegistry registry = new LocalServerRegistry();
    protected static List<Address> members = new ArrayList<>();
    protected static List<CopycatClient> copycatClients = new ArrayList<>();
    protected static List<CopycatServer> copycatServers = new ArrayList<>();
    protected static List<AtomixClient> atomixClients = new ArrayList<>();
    protected static List<CopycatServer> atomixServers = new ArrayList<>();
    protected static Serializer serializer = CatalystSerializers.getSerializer();
    protected static AtomicInteger port = new AtomicInteger(49200);

    /**
     * Creates a new resource state machine.
     *
     * @return A new resource state machine.
     */
    protected abstract ResourceType resourceType();

    /**
     * Returns the next server address.
     *
     * @return The next server address.
     */
    private static Address nextAddress() {
        Address address = new Address("127.0.0.1",
                          TestTools.findAvailablePort(port.getAndIncrement()));
        members.add(address);
        return address;
    }

    /**
     * Creates a set of Copycat servers.
     */
    protected static List<CopycatServer> createCopycatServers(int nodes)
            throws Throwable {
        List<CopycatServer> servers = new ArrayList<>();

        List<Address> members = new ArrayList<>();

        for (int i = 0; i < nodes; i++) {
            Address address = nextAddress();
            members.add(address);
            CopycatServer server = createCopycatServer(address);
            if (members.size() <= 1) {
                server.bootstrap().join();
            } else {
                server.join(members).join();
            }
            servers.add(server);
        }

        return servers;
    }

    /**
     * Creates a Copycat server.
     */
    protected static CopycatServer createCopycatServer(Address address) {
        CopycatServer server = CopycatServer.builder(address)
                .withTransport(NettyTransport.builder().withThreads(1).build())
                .withStorage(Storage.builder()
                             .withStorageLevel(StorageLevel.MEMORY)
                             .build())
                .withStateMachine(ResourceManagerState::new)
                .withSerializer(serializer.clone())
                .build();
        copycatServers.add(server);
        return server;
    }

    public static void clearTests() throws Exception {
        registry = new LocalServerRegistry();
        members = new ArrayList<>();

        CompletableFuture<Void> closeClients =
                CompletableFuture.allOf(atomixClients.stream()
                                                     .map(AtomixClient::close)
                                                     .toArray(CompletableFuture[]::new));
        closeClients.join();

        CompletableFuture<Void> closeServers =
                CompletableFuture.allOf(copycatServers.stream()
                                                      .map(CopycatServer::shutdown)
                                                      .toArray(CompletableFuture[]::new));
        closeServers.join();

        atomixClients.clear();
        copycatServers.clear();
    }


    /**
     * Creates a Atomix client.
     */
    protected AtomixClient createAtomixClient() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomixClient client = AtomixClient.builder()
                .withTransport(NettyTransport.builder().withThreads(1).build())
                .withSerializer(serializer.clone())
                .build();
        client.connect(members).thenRun(latch::countDown);
        atomixClients.add(client);
        Uninterruptibles.awaitUninterruptibly(latch);
        return client;
    }
}
