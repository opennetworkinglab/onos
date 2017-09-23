/*
 * Copyright 2016-present Open Networking Foundation
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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import io.atomix.protocols.raft.RaftClient;
import io.atomix.protocols.raft.RaftError;
import io.atomix.protocols.raft.RaftServer;
import io.atomix.protocols.raft.ReadConsistency;
import io.atomix.protocols.raft.cluster.MemberId;
import io.atomix.protocols.raft.cluster.RaftMember;
import io.atomix.protocols.raft.cluster.impl.DefaultRaftMember;
import io.atomix.protocols.raft.event.RaftEvent;
import io.atomix.protocols.raft.event.impl.DefaultEventType;
import io.atomix.protocols.raft.operation.OperationType;
import io.atomix.protocols.raft.operation.RaftOperation;
import io.atomix.protocols.raft.operation.impl.DefaultOperationId;
import io.atomix.protocols.raft.protocol.AppendRequest;
import io.atomix.protocols.raft.protocol.AppendResponse;
import io.atomix.protocols.raft.protocol.CloseSessionRequest;
import io.atomix.protocols.raft.protocol.CloseSessionResponse;
import io.atomix.protocols.raft.protocol.CommandRequest;
import io.atomix.protocols.raft.protocol.CommandResponse;
import io.atomix.protocols.raft.protocol.ConfigureRequest;
import io.atomix.protocols.raft.protocol.ConfigureResponse;
import io.atomix.protocols.raft.protocol.InstallRequest;
import io.atomix.protocols.raft.protocol.InstallResponse;
import io.atomix.protocols.raft.protocol.JoinRequest;
import io.atomix.protocols.raft.protocol.JoinResponse;
import io.atomix.protocols.raft.protocol.KeepAliveRequest;
import io.atomix.protocols.raft.protocol.KeepAliveResponse;
import io.atomix.protocols.raft.protocol.LeaveRequest;
import io.atomix.protocols.raft.protocol.LeaveResponse;
import io.atomix.protocols.raft.protocol.MetadataRequest;
import io.atomix.protocols.raft.protocol.MetadataResponse;
import io.atomix.protocols.raft.protocol.OpenSessionRequest;
import io.atomix.protocols.raft.protocol.OpenSessionResponse;
import io.atomix.protocols.raft.protocol.PollRequest;
import io.atomix.protocols.raft.protocol.PollResponse;
import io.atomix.protocols.raft.protocol.PublishRequest;
import io.atomix.protocols.raft.protocol.QueryRequest;
import io.atomix.protocols.raft.protocol.QueryResponse;
import io.atomix.protocols.raft.protocol.RaftResponse;
import io.atomix.protocols.raft.protocol.ReconfigureRequest;
import io.atomix.protocols.raft.protocol.ReconfigureResponse;
import io.atomix.protocols.raft.protocol.ResetRequest;
import io.atomix.protocols.raft.protocol.VoteRequest;
import io.atomix.protocols.raft.protocol.VoteResponse;
import io.atomix.protocols.raft.proxy.CommunicationStrategy;
import io.atomix.protocols.raft.proxy.RaftProxy;
import io.atomix.protocols.raft.service.RaftService;
import io.atomix.protocols.raft.session.SessionId;
import io.atomix.protocols.raft.storage.RaftStorage;
import io.atomix.protocols.raft.storage.log.entry.CloseSessionEntry;
import io.atomix.protocols.raft.storage.log.entry.CommandEntry;
import io.atomix.protocols.raft.storage.log.entry.ConfigurationEntry;
import io.atomix.protocols.raft.storage.log.entry.InitializeEntry;
import io.atomix.protocols.raft.storage.log.entry.KeepAliveEntry;
import io.atomix.protocols.raft.storage.log.entry.MetadataEntry;
import io.atomix.protocols.raft.storage.log.entry.OpenSessionEntry;
import io.atomix.protocols.raft.storage.log.entry.QueryEntry;
import io.atomix.protocols.raft.storage.system.Configuration;
import io.atomix.storage.StorageLevel;
import org.junit.After;
import org.junit.Before;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.primitives.impl.RaftClientCommunicator;
import org.onosproject.store.primitives.impl.RaftServerCommunicator;
import org.onosproject.store.service.Serializer;

/**
 * Base class for various Atomix tests.
 *
 * @param <T> the Raft primitive type being tested
 */
public abstract class AtomixTestBase<T extends AbstractRaftPrimitive> {

    private static final Serializer PROTOCOL_SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
            .register(OpenSessionRequest.class)
            .register(OpenSessionResponse.class)
            .register(CloseSessionRequest.class)
            .register(CloseSessionResponse.class)
            .register(KeepAliveRequest.class)
            .register(KeepAliveResponse.class)
            .register(QueryRequest.class)
            .register(QueryResponse.class)
            .register(CommandRequest.class)
            .register(CommandResponse.class)
            .register(MetadataRequest.class)
            .register(MetadataResponse.class)
            .register(JoinRequest.class)
            .register(JoinResponse.class)
            .register(LeaveRequest.class)
            .register(LeaveResponse.class)
            .register(ConfigureRequest.class)
            .register(ConfigureResponse.class)
            .register(ReconfigureRequest.class)
            .register(ReconfigureResponse.class)
            .register(InstallRequest.class)
            .register(InstallResponse.class)
            .register(PollRequest.class)
            .register(PollResponse.class)
            .register(VoteRequest.class)
            .register(VoteResponse.class)
            .register(AppendRequest.class)
            .register(AppendResponse.class)
            .register(PublishRequest.class)
            .register(ResetRequest.class)
            .register(RaftResponse.Status.class)
            .register(RaftError.class)
            .register(RaftError.Type.class)
            .register(ReadConsistency.class)
            .register(byte[].class)
            .register(long[].class)
            .register(CloseSessionEntry.class)
            .register(CommandEntry.class)
            .register(ConfigurationEntry.class)
            .register(InitializeEntry.class)
            .register(KeepAliveEntry.class)
            .register(MetadataEntry.class)
            .register(OpenSessionEntry.class)
            .register(QueryEntry.class)
            .register(RaftOperation.class)
            .register(RaftEvent.class)
            .register(DefaultEventType.class)
            .register(DefaultOperationId.class)
            .register(OperationType.class)
            .register(ReadConsistency.class)
            .register(ArrayList.class)
            .register(LinkedList.class)
            .register(Collections.emptyList().getClass())
            .register(HashSet.class)
            .register(DefaultRaftMember.class)
            .register(MemberId.class)
            .register(SessionId.class)
            .register(RaftMember.Type.class)
            .register(Instant.class)
            .register(Configuration.class)
            .register(AtomixAtomicCounterMapOperations.class)
            .register(AtomixConsistentMapEvents.class)
            .register(AtomixConsistentMapOperations.class)
            .register(AtomixConsistentSetMultimapOperations.class)
            .register(AtomixConsistentSetMultimapEvents.class)
            .register(AtomixConsistentTreeMapOperations.class)
            .register(AtomixCounterOperations.class)
            .register(AtomixDocumentTreeEvents.class)
            .register(AtomixDocumentTreeOperations.class)
            .register(AtomixLeaderElectorEvents.class)
            .register(AtomixLeaderElectorOperations.class)
            .register(AtomixWorkQueueEvents.class)
            .register(AtomixWorkQueueOperations.class)
            .build());

    private static final Serializer STORAGE_SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
            .register(CloseSessionEntry.class)
            .register(CommandEntry.class)
            .register(ConfigurationEntry.class)
            .register(InitializeEntry.class)
            .register(KeepAliveEntry.class)
            .register(MetadataEntry.class)
            .register(OpenSessionEntry.class)
            .register(QueryEntry.class)
            .register(RaftOperation.class)
            .register(ReadConsistency.class)
            .register(AtomixAtomicCounterMapOperations.class)
            .register(AtomixConsistentMapOperations.class)
            .register(AtomixConsistentSetMultimapOperations.class)
            .register(AtomixConsistentTreeMapOperations.class)
            .register(AtomixCounterOperations.class)
            .register(AtomixDocumentTreeOperations.class)
            .register(AtomixLeaderElectorOperations.class)
            .register(AtomixWorkQueueOperations.class)
            .register(ArrayList.class)
            .register(HashSet.class)
            .register(DefaultRaftMember.class)
            .register(MemberId.class)
            .register(RaftMember.Type.class)
            .register(Instant.class)
            .register(Configuration.class)
            .register(byte[].class)
            .register(long[].class)
            .build());

    protected TestClusterCommunicationServiceFactory communicationServiceFactory;
    protected List<RaftMember> members = Lists.newCopyOnWriteArrayList();
    protected List<RaftClient> clients = Lists.newCopyOnWriteArrayList();
    protected List<RaftServer> servers = Lists.newCopyOnWriteArrayList();
    protected int nextId;

    /**
     * Creates the primitive service.
     *
     * @return the primitive service
     */
    protected abstract RaftService createService();

    /**
     * Creates a new primitive.
     *
     * @param name the primitive name
     * @return the primitive instance
     */
    protected T newPrimitive(String name) {
        RaftClient client = createClient();
        RaftProxy proxy = client.newProxyBuilder()
                .withName(name)
                .withServiceType("test")
                .withReadConsistency(readConsistency())
                .withCommunicationStrategy(communicationStrategy())
                .build()
                .open()
                .join();
        return createPrimitive(proxy);
    }

    /**
     * Creates a new primitive instance.
     *
     * @param proxy the primitive proxy
     * @return the primitive instance
     */
    protected abstract T createPrimitive(RaftProxy proxy);

    /**
     * Returns the proxy read consistency.
     *
     * @return the primitive read consistency
     */
    protected ReadConsistency readConsistency() {
        return ReadConsistency.LINEARIZABLE;
    }

    /**
     * Returns the proxy communication strategy.
     *
     * @return the primitive communication strategy
     */
    protected CommunicationStrategy communicationStrategy() {
        return CommunicationStrategy.LEADER;
    }

    @Before
    public void prepare() {
        members.clear();
        clients.clear();
        servers.clear();
        communicationServiceFactory = new TestClusterCommunicationServiceFactory();
        createServers(3);
    }

    @After
    public void cleanup() {
        shutdown();
    }

    /**
     * Shuts down clients and servers.
     */
    private void shutdown() {
        clients.forEach(c -> {
            try {
                c.close().get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
            }
        });

        servers.forEach(s -> {
            try {
                if (s.isRunning()) {
                    s.shutdown().get(10, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
            }
        });

        Path directory = Paths.get("target/primitives/");
        if (Files.exists(directory)) {
            try {
                Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
            }
        }
    }

    /**
     * Returns the next unique member identifier.
     *
     * @return The next unique member identifier.
     */
    private MemberId nextMemberId() {
        return MemberId.from(String.valueOf(++nextId));
    }

    /**
     * Returns the next server address.
     *
     * @param type The startup member type.
     * @return The next server address.
     */
    private RaftMember nextMember(RaftMember.Type type) {
        return new TestMember(nextMemberId(), type);
    }

    /**
     * Creates a set of Raft servers.
     */
    protected List<RaftServer> createServers(int nodes) {
        List<RaftServer> servers = new ArrayList<>();

        for (int i = 0; i < nodes; i++) {
            members.add(nextMember(RaftMember.Type.ACTIVE));
        }

        CountDownLatch latch = new CountDownLatch(nodes);
        for (int i = 0; i < nodes; i++) {
            RaftServer server = createServer(members.get(i));
            server.bootstrap(members.stream().map(RaftMember::memberId).collect(Collectors.toList()))
                    .thenRun(latch::countDown);
            servers.add(server);
        }

        try {
            latch.await(30000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return servers;
    }

    /**
     * Creates a Raft server.
     */
    private RaftServer createServer(RaftMember member) {
        RaftServer.Builder builder = RaftServer.newBuilder(member.memberId())
                .withType(member.getType())
                .withProtocol(new RaftServerCommunicator(
                        "partition-1",
                        PROTOCOL_SERIALIZER,
                        communicationServiceFactory.newCommunicationService(NodeId.nodeId(member.memberId().id()))))
                .withStorage(RaftStorage.newBuilder()
                        .withStorageLevel(StorageLevel.MEMORY)
                        .withDirectory(new File(String.format("target/primitives/%s", member.memberId())))
                        .withSerializer(new AtomixSerializerAdapter(STORAGE_SERIALIZER))
                        .withMaxSegmentSize(1024 * 1024)
                        .build())
                .addService("test", this::createService);

        RaftServer server = builder.build();
        servers.add(server);
        return server;
    }

    /**
     * Creates a Raft client.
     */
    private RaftClient createClient() {
        MemberId memberId = nextMemberId();
        RaftClient client = RaftClient.newBuilder()
                .withMemberId(memberId)
                .withProtocol(new RaftClientCommunicator(
                        "partition-1",
                        PROTOCOL_SERIALIZER,
                        communicationServiceFactory.newCommunicationService(NodeId.nodeId(memberId.id()))))
                .build();

        client.connect(members.stream().map(RaftMember::memberId).collect(Collectors.toList())).join();
        clients.add(client);
        return client;
    }

    /**
     * Test member.
     */
    public static class TestMember implements RaftMember {
        private final MemberId memberId;
        private final Type type;

        public TestMember(MemberId memberId, Type type) {
            this.memberId = memberId;
            this.type = type;
        }

        @Override
        public MemberId memberId() {
            return memberId;
        }

        @Override
        public int hash() {
            return memberId.hashCode();
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public void addTypeChangeListener(Consumer<Type> listener) {

        }

        @Override
        public void removeTypeChangeListener(Consumer<Type> listener) {

        }

        @Override
        public Instant getLastUpdated() {
            return Instant.now();
        }
        @Override
        public CompletableFuture<Void> promote() {
            return null;
        }

        @Override
        public CompletableFuture<Void> promote(Type type) {
            return null;
        }

        @Override
        public CompletableFuture<Void> demote() {
            return null;
        }

        @Override
        public CompletableFuture<Void> demote(Type type) {
            return null;
        }

        @Override
        public CompletableFuture<Void> remove() {
            return null;
        }
    }
}
