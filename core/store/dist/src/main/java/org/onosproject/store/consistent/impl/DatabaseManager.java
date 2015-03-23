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

package org.onosproject.store.consistent.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.kuujo.copycat.CopycatConfig;
import net.kuujo.copycat.cluster.ClusterConfig;
import net.kuujo.copycat.cluster.Member;
import net.kuujo.copycat.cluster.Member.Type;
import net.kuujo.copycat.cluster.internal.coordinator.ClusterCoordinator;
import net.kuujo.copycat.cluster.internal.coordinator.DefaultClusterCoordinator;
import net.kuujo.copycat.log.BufferedLog;
import net.kuujo.copycat.log.FileLog;
import net.kuujo.copycat.log.Log;
import net.kuujo.copycat.netty.NettyTcpProtocol;
import net.kuujo.copycat.protocol.Consistency;
import net.kuujo.copycat.protocol.Protocol;
import net.kuujo.copycat.util.concurrent.NamedThreadFactory;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.store.cluster.impl.NodeInfo;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.ecmap.EventuallyConsistentMapBuilderImpl;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.PartitionInfo;
import org.onosproject.store.service.StorageAdminService;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TransactionContext;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Database manager.
 */
@Component(immediate = true, enabled = true)
@Service
public class DatabaseManager implements StorageService, StorageAdminService {

    private final Logger log = getLogger(getClass());
    private ClusterCoordinator coordinator;
    private PartitionedDatabase partitionedDatabase;
    private Database inMemoryDatabase;
    public static final int COPYCAT_TCP_PORT = 7238; //  7238 = RAFT
    private static final String CONFIG_DIR = "../config";
    private static final String PARTITION_DEFINITION_FILE = "tablets.json";
    private static final int DATABASE_STARTUP_TIMEOUT_SEC = 60;
    public static final String BASE_PARTITION_NAME = "p0";
    private static final int RAFT_ELECTION_TIMEOUT = 3000;
    private static final int RAFT_HEARTBEAT_TIMEOUT = 1500;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    protected String nodeToUri(NodeInfo node) {
        return String.format("tcp://%s:%d", node.getIp(), COPYCAT_TCP_PORT);
    }

    @Activate
    public void activate() {

        // load database configuration
        File file = new File(CONFIG_DIR, PARTITION_DEFINITION_FILE);
        log.info("Loading database definition: {}", file.getAbsolutePath());

        Map<String, Set<NodeInfo>> partitionMap;
        try {
            DatabaseDefinitionStore databaseDef = new DatabaseDefinitionStore(file);
            partitionMap = databaseDef.read().getPartitions();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load database config", e);
        }

        String[] activeNodeUris = partitionMap.values()
                    .stream()
                    .reduce((s1, s2) -> Sets.union(s1, s2))
                    .get()
                    .stream()
                    .map(this::nodeToUri)
                    .toArray(String[]::new);

        String localNodeUri = nodeToUri(NodeInfo.of(clusterService.getLocalNode()));

        ClusterConfig clusterConfig = new ClusterConfig()
            .withProtocol(newNettyProtocol())
            .withElectionTimeout(RAFT_ELECTION_TIMEOUT)
            .withHeartbeatInterval(RAFT_HEARTBEAT_TIMEOUT)
            .withMembers(activeNodeUris)
            .withLocalMember(localNodeUri);

        CopycatConfig copycatConfig = new CopycatConfig()
            .withName("onos")
            .withClusterConfig(clusterConfig)
            .withDefaultSerializer(new DatabaseSerializer())
            .withDefaultExecutor(Executors.newSingleThreadExecutor(new NamedThreadFactory("copycat-coordinator-%d")));

        coordinator = new DefaultClusterCoordinator(copycatConfig.resolve());

        DatabaseConfig inMemoryDatabaseConfig =
                newDatabaseConfig(BASE_PARTITION_NAME, newInMemoryLog(), activeNodeUris);
        inMemoryDatabase = coordinator
                .getResource(inMemoryDatabaseConfig.getName(), inMemoryDatabaseConfig.resolve(clusterConfig)
                .withSerializer(copycatConfig.getDefaultSerializer())
                .withDefaultExecutor(copycatConfig.getDefaultExecutor()));

        List<Database> partitions = partitionMap.entrySet()
            .stream()
            .map(entry -> {
                String[] replicas = entry.getValue().stream().map(this::nodeToUri).toArray(String[]::new);
                return newDatabaseConfig(entry.getKey(), newPersistentLog(), replicas);
                })
            .map(config -> {
                Database db = coordinator.getResource(config.getName(), config.resolve(clusterConfig)
                        .withSerializer(copycatConfig.getDefaultSerializer())
                        .withDefaultExecutor(copycatConfig.getDefaultExecutor()));
                return db;
            })
            .collect(Collectors.toList());

        partitionedDatabase = new PartitionedDatabase("onos-store", partitions);

        CountDownLatch latch = new CountDownLatch(1);

        coordinator.open()
            .thenCompose(v -> CompletableFuture.allOf(inMemoryDatabase.open(), partitionedDatabase.open())
            .whenComplete((db, error) -> {
                if (error != null) {
                    log.warn("Failed to create databases.", error);
                } else {
                    latch.countDown();
                    log.info("Successfully created databases.");
                }
            }));

        try {
            if (!latch.await(DATABASE_STARTUP_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                log.warn("Timed out waiting for database to initialize.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Failed to complete database initialization.");
        }
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        CompletableFuture.allOf(inMemoryDatabase.close(), partitionedDatabase.close())
            .thenCompose(v -> coordinator.close())
            .whenComplete((result, error) -> {
                if (error != null) {
                    log.warn("Failed to cleanly close databases.", error);
                } else {
                    log.info("Successfully closed databases.");
                }
            });
        log.info("Stopped");
    }

    @Override
    public TransactionContext createTransactionContext() {
        return new DefaultTransactionContext(partitionedDatabase);
    }

    @Override
    public List<PartitionInfo> getPartitionInfo() {
        return Lists.asList(
                    inMemoryDatabase,
                    partitionedDatabase.getPartitions().toArray(new Database[]{}))
                .stream()
                .map(DatabaseManager::toPartitionInfo)
                .collect(Collectors.toList());
    }

    private Protocol newNettyProtocol() {
        return new NettyTcpProtocol()
            .withSsl(false)
            .withConnectTimeout(60000)
            .withAcceptBacklog(1024)
            .withTrafficClass(-1)
            .withSoLinger(-1)
            .withReceiveBufferSize(32768)
            .withSendBufferSize(8192)
            .withThreads(1);
    }

    private Log newPersistentLog() {
        String logDir = System.getProperty("karaf.data", "./data");
        return new FileLog()
            .withDirectory(logDir)
            .withSegmentSize(1073741824) // 1GB
            .withFlushOnWrite(true)
            .withSegmentInterval(Long.MAX_VALUE);
    }

    private Log newInMemoryLog() {
        return new BufferedLog()
            .withFlushOnWrite(false)
            .withFlushInterval(Long.MAX_VALUE)
            .withSegmentSize(10485760) // 10MB
            .withSegmentInterval(Long.MAX_VALUE);
    }

    private DatabaseConfig newDatabaseConfig(String name, Log log, String[] replicas) {
        return new DatabaseConfig()
            .withName(name)
            .withElectionTimeout(RAFT_ELECTION_TIMEOUT)
            .withHeartbeatInterval(RAFT_HEARTBEAT_TIMEOUT)
            .withConsistency(Consistency.STRONG)
            .withLog(log)
            .withDefaultSerializer(new DatabaseSerializer())
            .withReplicas(replicas);
    }

    /**
     * Maps a Raft Database object to a PartitionInfo object.
     *
     * @param database database containing input data
     * @return PartitionInfo object
     */
    private static PartitionInfo toPartitionInfo(Database database) {
        return new PartitionInfo(database.name(),
                          database.cluster().term(),
                          database.cluster().members()
                                  .stream()
                                  .filter(member -> Type.ACTIVE.equals(member.type()))
                                  .map(Member::uri)
                                  .sorted()
                                  .collect(Collectors.toList()),
                          database.cluster().leader() != null ?
                                  database.cluster().leader().uri() : null);
    }


    @Override
    public <K, V> EventuallyConsistentMapBuilder<K, V> eventuallyConsistentMapBuilder() {
        return new EventuallyConsistentMapBuilderImpl<>(clusterService,
                                                        clusterCommunicator);
    }

    @Override
    public <K, V> ConsistentMapBuilder<K, V> consistentMapBuilder() {
        return new DefaultConsistentMapBuilder<>(inMemoryDatabase, partitionedDatabase);
    }
}