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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;

import net.kuujo.copycat.CopycatConfig;
import net.kuujo.copycat.cluster.ClusterConfig;
import net.kuujo.copycat.cluster.Member;
import net.kuujo.copycat.cluster.Member.Type;
import net.kuujo.copycat.cluster.internal.coordinator.ClusterCoordinator;
import net.kuujo.copycat.cluster.internal.coordinator.DefaultClusterCoordinator;
import net.kuujo.copycat.log.BufferedLog;
import net.kuujo.copycat.log.FileLog;
import net.kuujo.copycat.log.Log;
import net.kuujo.copycat.protocol.Consistency;
import net.kuujo.copycat.protocol.Protocol;
import net.kuujo.copycat.util.concurrent.NamedThreadFactory;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.app.ApplicationEvent;
import org.onosproject.app.ApplicationListener;
import org.onosproject.app.ApplicationService;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.persistence.PersistenceService;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.ecmap.EventuallyConsistentMapBuilderImpl;
import org.onosproject.store.service.AtomicCounterBuilder;
import org.onosproject.store.service.AtomicValueBuilder;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.DistributedQueueBuilder;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.MapInfo;
import org.onosproject.store.service.PartitionInfo;
import org.onosproject.store.service.DistributedSetBuilder;
import org.onosproject.store.service.StorageAdminService;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Transaction;
import org.onosproject.store.service.TransactionContextBuilder;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.app.ApplicationEvent.Type.APP_UNINSTALLED;
import static org.onosproject.app.ApplicationEvent.Type.APP_DEACTIVATED;

/**
 * Database manager.
 */
@Component(immediate = true, enabled = true)
@Service
public class DatabaseManager implements StorageService, StorageAdminService {

    private final Logger log = getLogger(getClass());

    public static final String BASE_PARTITION_NAME = "p0";

    private static final int RAFT_ELECTION_TIMEOUT_MILLIS = 3000;
    private static final int DATABASE_OPERATION_TIMEOUT_MILLIS = 5000;

    private ClusterCoordinator coordinator;
    protected PartitionedDatabase partitionedDatabase;
    protected Database inMemoryDatabase;
    protected NodeId localNodeId;

    private TransactionManager transactionManager;
    private final IdGenerator transactionIdGenerator = () -> RandomUtils.nextLong();

    private ApplicationListener appListener = new InternalApplicationListener();

    private final Multimap<String, DefaultAsyncConsistentMap> maps =
            Multimaps.synchronizedMultimap(ArrayListMultimap.create());
    private final Multimap<ApplicationId, DefaultAsyncConsistentMap> mapsByApplication =
            Multimaps.synchronizedMultimap(ArrayListMultimap.create());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataService clusterMetadataService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    protected ApplicationService applicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PersistenceService persistenceService;

    protected String nodeIdToUri(NodeId nodeId) {
        ControllerNode node = clusterService.getNode(nodeId);
        return String.format("onos://%s:%d", node.ip(), node.tcpPort());
    }

    protected void bindApplicationService(ApplicationService service) {
        applicationService = service;
        applicationService.addListener(appListener);
    }

    protected void unbindApplicationService(ApplicationService service) {
        applicationService.removeListener(appListener);
        this.applicationService = null;
    }

    @Activate
    public void activate() {
        localNodeId = clusterService.getLocalNode().id();

        Map<String, Set<NodeId>> partitionMap = Maps.newHashMap();
        clusterMetadataService.getClusterMetadata().getPartitions().forEach(p -> {
            partitionMap.put(p.getName(), Sets.newHashSet(p.getMembers()));
        });


        String[] activeNodeUris = partitionMap.values()
                    .stream()
                    .reduce((s1, s2) -> Sets.union(s1, s2))
                    .get()
                    .stream()
                    .map(this::nodeIdToUri)
                    .toArray(String[]::new);

        String localNodeUri = nodeIdToUri(clusterMetadataService.getLocalNode().id());
        Protocol protocol = new CopycatCommunicationProtocol(clusterService, clusterCommunicator);

        ClusterConfig clusterConfig = new ClusterConfig()
            .withProtocol(protocol)
            .withElectionTimeout(electionTimeoutMillis(activeNodeUris))
            .withHeartbeatInterval(heartbeatTimeoutMillis(activeNodeUris))
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
                String[] replicas = entry.getValue().stream().map(this::nodeIdToUri).toArray(String[]::new);
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

        CompletableFuture<Void> status = coordinator.open()
            .thenCompose(v -> CompletableFuture.allOf(inMemoryDatabase.open(), partitionedDatabase.open())
            .whenComplete((db, error) -> {
                if (error != null) {
                    log.error("Failed to initialize database.", error);
                } else {
                    log.info("Successfully initialized database.");
                }
            }));

        Futures.getUnchecked(status);

        transactionManager = new TransactionManager(partitionedDatabase, consistentMapBuilder());
        partitionedDatabase.setTransactionManager(transactionManager);

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
        ImmutableList.copyOf(maps.values()).forEach(this::unregisterMap);
        if (applicationService != null) {
            applicationService.removeListener(appListener);
        }
        log.info("Stopped");
    }

    @Override
    public TransactionContextBuilder transactionContextBuilder() {
        return new DefaultTransactionContextBuilder(this, transactionIdGenerator.getNewId());
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
            .withElectionTimeout(electionTimeoutMillis(replicas))
            .withHeartbeatInterval(heartbeatTimeoutMillis(replicas))
            .withConsistency(Consistency.DEFAULT)
            .withLog(log)
            .withDefaultSerializer(new DatabaseSerializer())
            .withReplicas(replicas);
    }

    private long electionTimeoutMillis(String[] replicas) {
        return replicas.length == 1 ? 10L : RAFT_ELECTION_TIMEOUT_MILLIS;
    }

    private long heartbeatTimeoutMillis(String[] replicas) {
        return electionTimeoutMillis(replicas) / 2;
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
                                                        clusterCommunicator,
                                                        persistenceService);
    }

    @Override
    public <K, V> ConsistentMapBuilder<K, V> consistentMapBuilder() {
        return new DefaultConsistentMapBuilder<>(this);
    }

    @Override
    public <E> DistributedSetBuilder<E> setBuilder() {
        return new DefaultDistributedSetBuilder<>(this);
    }


    @Override
    public <E> DistributedQueueBuilder<E> queueBuilder() {
        return new DefaultDistributedQueueBuilder<>(this);
    }

    @Override
    public AtomicCounterBuilder atomicCounterBuilder() {
        return new DefaultAtomicCounterBuilder(inMemoryDatabase, partitionedDatabase);
    }

    @Override
    public <V> AtomicValueBuilder<V> atomicValueBuilder() {
        return new DefaultAtomicValueBuilder<>(this);
    }

    @Override
    public List<MapInfo> getMapInfo() {
        List<MapInfo> maps = Lists.newArrayList();
        maps.addAll(getMapInfo(inMemoryDatabase));
        maps.addAll(getMapInfo(partitionedDatabase));
        return maps;
    }

    private List<MapInfo> getMapInfo(Database database) {
        return complete(database.maps())
            .stream()
            .map(name -> new MapInfo(name, complete(database.mapSize(name))))
            .filter(info -> info.size() > 0)
            .collect(Collectors.toList());
    }


    @Override
    public Map<String, Long> getCounters() {
        Map<String, Long> counters = Maps.newHashMap();
        counters.putAll(complete(inMemoryDatabase.counters()));
        counters.putAll(complete(partitionedDatabase.counters()));
        return counters;
    }

    @Override
    public Map<String, Long> getPartitionedDatabaseCounters() {
        Map<String, Long> counters = Maps.newHashMap();
        counters.putAll(complete(partitionedDatabase.counters()));
        return counters;
    }

    @Override
    public Map<String, Long> getInMemoryDatabaseCounters() {
        Map<String, Long> counters = Maps.newHashMap();
        counters.putAll(complete(inMemoryDatabase.counters()));
        return counters;
    }

    @Override
    public Collection<Transaction> getTransactions() {
        return complete(transactionManager.getTransactions());
    }

    private static <T> T complete(CompletableFuture<T> future) {
        try {
            return future.get(DATABASE_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConsistentMapException.Interrupted();
        } catch (TimeoutException e) {
            throw new ConsistentMapException.Timeout();
        } catch (ExecutionException e) {
            throw new ConsistentMapException(e.getCause());
        }
    }

    @Override
    public void redriveTransactions() {
        getTransactions().stream().forEach(transactionManager::execute);
    }

    protected <K, V> DefaultAsyncConsistentMap<K, V> registerMap(DefaultAsyncConsistentMap<K, V> map) {
        maps.put(map.name(), map);
        if (map.applicationId() != null) {
            mapsByApplication.put(map.applicationId(), map);
        }
        return map;
    }

    protected <K, V> void unregisterMap(DefaultAsyncConsistentMap<K, V> map) {
        maps.remove(map.name(), map);
        if (map.applicationId() != null) {
            mapsByApplication.remove(map.applicationId(), map);
        }
    }

    private class InternalApplicationListener implements ApplicationListener {
        @Override
        public void event(ApplicationEvent event) {
            if (event.type() == APP_UNINSTALLED || event.type() == APP_DEACTIVATED) {
                ApplicationId appId = event.subject().id();
                List<DefaultAsyncConsistentMap> mapsToRemove;
                synchronized (mapsByApplication) {
                    mapsToRemove = ImmutableList.copyOf(mapsByApplication.get(appId));
                }
                mapsToRemove.forEach(DatabaseManager.this::unregisterMap);
                if (event.type() == APP_UNINSTALLED) {
                    mapsToRemove.stream().filter(map -> map.purgeOnUninstall()).forEach(map -> map.clear());
                }
            }
        }
    }
}
