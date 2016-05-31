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

import io.atomix.AtomixClient;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.local.LocalServerRegistry;
import io.atomix.catalyst.transport.local.LocalTransport;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;
import io.atomix.manager.internal.ResourceManagerState;
import io.atomix.resource.ResourceType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.onosproject.store.primitives.impl.CatalystSerializers;

import com.google.common.util.concurrent.Uninterruptibles;

/**
 * Base class for various Atomix* tests.
 */
public abstract class AtomixTestBase {
    private static final File TEST_DIR = new File("target/test-logs");
    protected LocalServerRegistry registry;
    protected int port;
    protected List<Address> members;
    protected List<CopycatClient> copycatClients = new ArrayList<>();
    protected List<CopycatServer> copycatServers = new ArrayList<>();
    protected List<AtomixClient> atomixClients = new ArrayList<>();
    protected List<CopycatServer> atomixServers = new ArrayList<>();
    protected Serializer serializer = CatalystSerializers.getSerializer();

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
    private Address nextAddress() {
        Address address = new Address("localhost", port++);
        members.add(address);
        return address;
    }

    /**
     * Creates a set of Copycat servers.
     */
    protected List<CopycatServer> createCopycatServers(int nodes) throws Throwable {
        CountDownLatch latch = new CountDownLatch(nodes);
        List<CopycatServer> servers = new ArrayList<>();

        List<Address> members = new ArrayList<>();
        for (int i = 0; i < nodes; i++) {
            members.add(nextAddress());
        }

        for (int i = 0; i < nodes; i++) {
            CopycatServer server = createCopycatServer(members.get(i));
            server.bootstrap(members).thenRun(latch::countDown);
            servers.add(server);
        }

        Uninterruptibles.awaitUninterruptibly(latch);

        return servers;
    }

    /**
     * Creates a Copycat server.
     */
    protected CopycatServer createCopycatServer(Address address) {
        CopycatServer server = CopycatServer.builder(address)
                .withTransport(new LocalTransport(registry))
                .withStorage(Storage.builder()
                        .withStorageLevel(StorageLevel.DISK)
                        .withDirectory(TEST_DIR + "/" + address.port())
                        .build())
                .withStateMachine(ResourceManagerState::new)
                .withSerializer(serializer.clone())
                .withHeartbeatInterval(Duration.ofMillis(25))
                .withElectionTimeout(Duration.ofMillis(50))
                .withSessionTimeout(Duration.ofMillis(100))
                .build();
        copycatServers.add(server);
        return server;
    }

    @Before
    @After
    public void clearTests() throws Exception {
        registry = new LocalServerRegistry();
        members = new ArrayList<>();
        port = 5000;

        CompletableFuture<Void> closeClients =
                CompletableFuture.allOf(atomixClients.stream()
                                                     .map(AtomixClient::close)
                                                     .toArray(CompletableFuture[]::new));

        closeClients.thenCompose(v -> CompletableFuture.allOf(copycatServers.stream()
                .map(CopycatServer::shutdown)
                .toArray(CompletableFuture[]::new))).join();

        deleteDirectory(TEST_DIR);

        atomixClients = new ArrayList<>();

        copycatServers = new ArrayList<>();
    }

    /**
     * Deletes a directory recursively.
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        Files.delete(file.toPath());
                    }
                }
            }
            Files.delete(directory.toPath());
        }
    }

    /**
     * Creates a Atomix client.
     */
    protected AtomixClient createAtomixClient() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomixClient client = AtomixClient.builder()
                .withTransport(new LocalTransport(registry))
                .withSerializer(serializer.clone())
                .build();
        client.connect(members).thenRun(latch::countDown);
        atomixClients.add(client);
        Uninterruptibles.awaitUninterruptibly(latch);
        return client;
    }
}
