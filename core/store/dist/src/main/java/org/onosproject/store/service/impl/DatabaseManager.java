/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.service.impl;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.kuujo.copycat.Copycat;
import net.kuujo.copycat.CopycatConfig;
import net.kuujo.copycat.cluster.ClusterConfig;
import net.kuujo.copycat.cluster.Member;
import net.kuujo.copycat.cluster.TcpCluster;
import net.kuujo.copycat.cluster.TcpClusterConfig;
import net.kuujo.copycat.cluster.TcpMember;
import net.kuujo.copycat.event.EventHandler;
import net.kuujo.copycat.event.LeaderElectEvent;
import net.kuujo.copycat.log.Log;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.service.BatchReadRequest;
import org.onosproject.store.service.BatchReadResult;
import org.onosproject.store.service.BatchWriteRequest;
import org.onosproject.store.service.BatchWriteResult;
import org.onosproject.store.service.DatabaseAdminService;
import org.onosproject.store.service.DatabaseException;
import org.onosproject.store.service.DatabaseService;
import org.onosproject.store.service.ReadResult;
import org.onosproject.store.service.ReadStatus;
import org.onosproject.store.service.VersionedValue;
import org.onosproject.store.service.WriteResult;
import org.onosproject.store.service.WriteStatus;
import org.onlab.packet.IpAddress;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

/**
 * Strongly consistent and durable state management service based on
 * Copycat implementation of Raft consensus protocol.
 */
@Component(immediate = false)
@Service
public class DatabaseManager implements DatabaseService, DatabaseAdminService {

    private static final int RETRY_MS = 500;

    private static final int ACTIVATE_MAX_RETRIES = 100;

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DatabaseProtocolService copycatMessagingProtocol;

    public static final String LOG_FILE_PREFIX = "raft/onos-copy-cat-log_";

    // Current working dir seems to be /opt/onos/apache-karaf-3.0.2
    // TODO: Set the path to /opt/onos/config
    private static final String CONFIG_DIR = "../config";

    private static final String DEFAULT_MEMBER_FILE = "tablets.json";

    private static final String DEFAULT_TABLET = "default";

    // TODO: make this configurable
    // initial member configuration file path
    private String initialMemberConfig = DEFAULT_MEMBER_FILE;

    public static final MessageSubject RAFT_LEADER_ELECTION_EVENT =
            new MessageSubject("raft-leader-election-event");

    private Copycat copycat;
    private DatabaseClient client;

    // guarded by synchronized block
    private ClusterConfig<TcpMember> clusterConfig;

    private CountDownLatch clusterEventLatch;
    private ClusterEventListener clusterEventListener;

    private Map<String, Set<DefaultControllerNode>> tabletMembers;

    private boolean autoAddMember = false;

    private ScheduledExecutorService executor;

    private volatile LeaderElectEvent myLeaderEvent = null;

    // TODO make this configurable
    private int maxLogSizeBytes = 128 * (1024 * 1024);

    // TODO make this configurable
    private long electionTimeoutMs = 5000; // CopyCat default: 2000

    @Activate
    public void activate() throws InterruptedException, ExecutionException {

        // KARAF_DATA
        //  http://karaf.apache.org/manual/latest/users-guide/start-stop.html
        final String dataDir = System.getProperty("karaf.data", "./data");

        // load tablet configuration
        File file = new File(CONFIG_DIR, initialMemberConfig);
        log.info("Loading config: {}", file.getAbsolutePath());
        TabletDefinitionStore tabletDef = new TabletDefinitionStore(file);
        try {
            tabletMembers = tabletDef.read();
        } catch (IOException e) {
            log.error("Failed to load tablet config {}", file);
            throw new IllegalStateException("Failed to load tablet config", e);
        }

        // load default tablet configuration and start copycat
        clusterConfig = new TcpClusterConfig();
        Set<DefaultControllerNode> defaultMembers = tabletMembers.get(DEFAULT_TABLET);
        if (defaultMembers == null || defaultMembers.isEmpty()) {
            log.error("No members found in [{}] tablet configuration.",
                      DEFAULT_TABLET);
            throw new IllegalStateException("No member found in tablet configuration");

        }

        final ControllerNode localNode = clusterService.getLocalNode();
        for (ControllerNode member : defaultMembers) {
            final TcpMember tcpMember = new TcpMember(member.ip().toString(),
                                                      member.tcpPort());
            if (localNode.equals(member)) {
                clusterConfig.setLocalMember(tcpMember);
            } else {
                clusterConfig.addRemoteMember(tcpMember);
            }
        }

        if (clusterConfig.getLocalMember() != null) {

            // Wait for a minimum viable Raft cluster to boot up.
            waitForClusterQuorum();

            final TcpCluster cluster;
            synchronized (clusterConfig) {
                // Create the cluster.
                cluster = new TcpCluster(clusterConfig);
            }
            log.info("Starting cluster: {}", cluster);

            DatabaseEntryExpirationTracker expirationTracker =
                    new DatabaseEntryExpirationTracker(
                            clusterConfig.getLocalMember(),
                            clusterService.getLocalNode(),
                            clusterCommunicator,
                            this);

            DatabaseStateMachine stateMachine = new DatabaseStateMachine();
            stateMachine.addEventListener(expirationTracker);
            Log consensusLog = new MapDBLog(dataDir + "/" + LOG_FILE_PREFIX + localNode.id(),
                    ClusterMessagingProtocol.DB_SERIALIZER);

            CopycatConfig ccConfig = new CopycatConfig();
            ccConfig.setMaxLogSize(maxLogSizeBytes);
            ccConfig.setElectionTimeout(electionTimeoutMs);

            copycat = new Copycat(stateMachine, consensusLog, cluster, copycatMessagingProtocol, ccConfig);
            copycat.event(LeaderElectEvent.class).registerHandler(new RaftLeaderElectionMonitor());
            copycat.event(LeaderElectEvent.class).registerHandler(expirationTracker);
        }

        client = new DatabaseClient(copycatMessagingProtocol);
        clusterCommunicator.addSubscriber(RAFT_LEADER_ELECTION_EVENT, client);

        // Starts copycat if this node is a participant
        // of the Raft cluster.
        if (copycat != null) {
            copycat.start().get();

            executor =
                    newSingleThreadScheduledExecutor(namedThreads("db-heartbeat-%d"));
            executor.scheduleWithFixedDelay(new LeaderAdvertiser(), 5, 2, TimeUnit.SECONDS);

        }

        client.waitForLeader();

        // Try and list the tables to verify database manager is
        // in a state where it can serve requests.
        tryTableListing();

        log.info("Started.");
    }

    @Deactivate
    public void deactivate() {
        if (executor != null) {
            executor.shutdownNow();
        }
        clusterService.removeListener(clusterEventListener);
        // TODO: ClusterCommunicationService must support more than one
        // handler per message subject.
        clusterCommunicator.removeSubscriber(RAFT_LEADER_ELECTION_EVENT);
        if (copycat != null) {
            copycat.stop();
        }
        log.info("Stopped.");
    }

    private void waitForClusterQuorum() {
        // note: from this point beyond, clusterConfig requires synchronization
        clusterEventLatch = new CountDownLatch(1);
        clusterEventListener = new InternalClusterEventListener();
        clusterService.addListener(clusterEventListener);

        final int raftClusterSize = clusterConfig.getMembers().size();
        final int raftClusterQuorumSize = (int) (Math.floor(raftClusterSize / 2)) + 1;
        if (clusterService.getNodes().size() < raftClusterQuorumSize) {
            // current cluster size smaller then expected
            try {
                final int waitTimeSec = 120;
                log.info("Waiting for a maximum of {}s for raft cluster quorum to boot up...", waitTimeSec);
                if (!clusterEventLatch.await(waitTimeSec, TimeUnit.SECONDS)) {
                    log.info("Starting with {}/{} nodes cluster",
                             clusterService.getNodes().size(),
                             raftClusterSize);
                }
            } catch (InterruptedException e) {
                log.info("Interrupted waiting for raft quorum.", e);
            }
        }
    }

    private void tryTableListing() throws InterruptedException {
        int retries = 0;
        do {
            try {
                listTables();
                return;
            } catch (DatabaseException.Timeout e) {
                log.debug("Failed to listTables. Will retry...", e);
            } catch (DatabaseException e) {
                log.debug("Failed to listTables. Will retry later...", e);
                Thread.sleep(RETRY_MS);
            }
            if (retries == ACTIVATE_MAX_RETRIES) {
                log.error("Failed to listTables after multiple attempts. Giving up.");
                // Exiting hoping things will be fixed by the time
                // others start using the service
                return;
            }
            retries++;
        } while (true);
    }

    @Override
    public boolean createTable(String name) {
        return client.createTable(name);
    }

    @Override
    public boolean createTable(String name, int ttlMillis) {
        return client.createTable(name, ttlMillis);
    }

    @Override
    public void dropTable(String name) {
        client.dropTable(name);
    }

    @Override
    public void dropAllTables() {
        client.dropAllTables();
    }

    @Override
    public Set<String> listTables() {
        return client.listTables();
    }

    @Override
    public VersionedValue get(String tableName, String key) {
        BatchReadRequest batchRequest = new BatchReadRequest.Builder().get(tableName, key).build();
        ReadResult readResult = batchRead(batchRequest).getAsList().get(0);
        if (readResult.status().equals(ReadStatus.OK)) {
            return readResult.value();
        }
        throw new DatabaseException("get failed due to status: " + readResult.status());
    }

    @Override
    public Map<String, VersionedValue> getAll(String tableName) {
        return client.getAll(tableName);
    }


    @Override
    public BatchReadResult batchRead(BatchReadRequest batchRequest) {
        return new BatchReadResult(client.batchRead(batchRequest));
    }

    @Override
    public BatchWriteResult batchWrite(BatchWriteRequest batchRequest) {
        return new BatchWriteResult(client.batchWrite(batchRequest));
    }

    @Override
    public VersionedValue put(String tableName, String key, byte[] value) {
        BatchWriteRequest batchRequest = new BatchWriteRequest.Builder().put(tableName, key, value).build();
        WriteResult writeResult = batchWrite(batchRequest).getAsList().get(0);
        if (writeResult.status().equals(WriteStatus.OK)) {
            return writeResult.previousValue();
        }
        throw new DatabaseException("put failed due to status: " + writeResult.status());
    }

    @Override
    public boolean putIfAbsent(String tableName, String key, byte[] value) {
        BatchWriteRequest batchRequest = new BatchWriteRequest.Builder()
                    .putIfAbsent(tableName, key, value).build();
        WriteResult writeResult = batchWrite(batchRequest).getAsList().get(0);
        if (writeResult.status().equals(WriteStatus.OK)) {
            return true;
        } else if (writeResult.status().equals(WriteStatus.PRECONDITION_VIOLATION)) {
            return false;
        }
        throw new DatabaseException("putIfAbsent failed due to status: "
                    + writeResult.status());
    }

    @Override
    public boolean putIfVersionMatches(String tableName, String key,
            byte[] value, long version) {
        BatchWriteRequest batchRequest =
                new BatchWriteRequest.Builder()
                    .putIfVersionMatches(tableName, key, value, version).build();
        WriteResult writeResult = batchWrite(batchRequest).getAsList().get(0);
        if (writeResult.status().equals(WriteStatus.OK)) {
            return true;
        } else if (writeResult.status().equals(WriteStatus.PRECONDITION_VIOLATION)) {
            return false;
        }
        throw new DatabaseException("putIfVersionMatches failed due to status: "
                    + writeResult.status());
    }

    @Override
    public boolean putIfValueMatches(String tableName, String key,
            byte[] oldValue, byte[] newValue) {
        BatchWriteRequest batchRequest = new BatchWriteRequest.Builder()
                    .putIfValueMatches(tableName, key, oldValue, newValue).build();
        WriteResult writeResult = batchWrite(batchRequest).getAsList().get(0);
        if (writeResult.status().equals(WriteStatus.OK)) {
            return true;
        } else if (writeResult.status().equals(WriteStatus.PRECONDITION_VIOLATION)) {
            return false;
        }
        throw new DatabaseException("putIfValueMatches failed due to status: "
                    + writeResult.status());
    }

    @Override
    public VersionedValue remove(String tableName, String key) {
        BatchWriteRequest batchRequest = new BatchWriteRequest.Builder()
                    .remove(tableName, key).build();
        WriteResult writeResult = batchWrite(batchRequest).getAsList().get(0);
        if (writeResult.status().equals(WriteStatus.OK)) {
            return writeResult.previousValue();
        }
        throw new DatabaseException("remove failed due to status: "
                    + writeResult.status());
    }

    @Override
    public boolean removeIfVersionMatches(String tableName, String key,
            long version) {
        BatchWriteRequest batchRequest = new BatchWriteRequest.Builder()
                    .removeIfVersionMatches(tableName, key, version).build();
        WriteResult writeResult = batchWrite(batchRequest).getAsList().get(0);
        if (writeResult.status().equals(WriteStatus.OK)) {
            return true;
        } else if (writeResult.status().equals(WriteStatus.PRECONDITION_VIOLATION)) {
            return false;
        }
        throw new DatabaseException("removeIfVersionMatches failed due to status: "
                    + writeResult.status());
    }

    @Override
    public boolean removeIfValueMatches(String tableName, String key,
            byte[] value) {
        BatchWriteRequest batchRequest = new BatchWriteRequest.Builder()
                    .removeIfValueMatches(tableName, key, value).build();
        WriteResult writeResult = batchWrite(batchRequest).getAsList().get(0);
        if (writeResult.status().equals(WriteStatus.OK)) {
            return true;
        } else if (writeResult.status().equals(WriteStatus.PRECONDITION_VIOLATION)) {
            return false;
        }
        throw new DatabaseException("removeIfValueMatches failed due to status: "
                    + writeResult.status());
    }

    @Override
    public void addMember(final ControllerNode node) {
        final TcpMember tcpMember = new TcpMember(node.ip().toString(),
                                                  node.tcpPort());
        log.info("{} was added to the cluster", tcpMember);
        synchronized (clusterConfig) {
            clusterConfig.addRemoteMember(tcpMember);
        }
    }

    @Override
    public Optional<ControllerNode> leader() {
        if (copycat != null) {
            if (copycat.isLeader()) {
                return Optional.of(clusterService.getLocalNode());
            }
            Member leader = copycat.cluster().remoteMember(copycat.leader());
            return Optional.ofNullable(getNodeIdFromMember(leader));
        }
        return Optional.ofNullable(getNodeIdFromMember(client.getCurrentLeader()));
    }

    private final class LeaderAdvertiser implements Runnable {

        @Override
        public void run() {
            try {
                LeaderElectEvent event = myLeaderEvent;
                if (event != null) {
                    log.trace("Broadcasting RAFT_LEADER_ELECTION_EVENT: {}", event);
                    // This node just became the leader.
                    clusterCommunicator.broadcastIncludeSelf(
                            new ClusterMessage(
                                    clusterService.getLocalNode().id(),
                                    RAFT_LEADER_ELECTION_EVENT,
                                    ClusterMessagingProtocol.DB_SERIALIZER.encode(event)));
                }
            } catch (Exception e) {
                log.debug("LeaderAdvertiser failed with exception", e);
            }
        }

    }

    private final class RaftLeaderElectionMonitor implements EventHandler<LeaderElectEvent> {
        @Override
        public void handle(LeaderElectEvent event) {
            try {
                log.debug("Received LeaderElectEvent: {}", event);
                if (clusterConfig.getLocalMember() != null && event.leader().equals(clusterConfig.getLocalMember())) {
                    log.debug("Broadcasting RAFT_LEADER_ELECTION_EVENT");
                    myLeaderEvent = event;
                    // This node just became the leader.
                    clusterCommunicator.broadcastIncludeSelf(
                            new ClusterMessage(
                                    clusterService.getLocalNode().id(),
                                    RAFT_LEADER_ELECTION_EVENT,
                                    ClusterMessagingProtocol.DB_SERIALIZER.encode(event)));
                } else {
                    if (myLeaderEvent != null) {
                        log.debug("This node is no longer the Leader");
                    }
                    myLeaderEvent = null;
                }
            } catch (IOException e) {
                log.error("Failed to broadcast raft leadership change event", e);
            }
        }
    }

    private final class InternalClusterEventListener
    implements ClusterEventListener {

        @Override
        public void event(ClusterEvent event) {
            // TODO: Not every node should be part of the consensus ring.

            final ControllerNode node = event.subject();
            final TcpMember tcpMember = new TcpMember(node.ip().toString(),
                    node.tcpPort());

            switch (event.type()) {
            case INSTANCE_ACTIVATED:
            case INSTANCE_ADDED:
                if (autoAddMember) {
                    synchronized (clusterConfig) {
                        if (!clusterConfig.getMembers().contains(tcpMember)) {
                            log.info("{} was automatically added to the cluster", tcpMember);
                            clusterConfig.addRemoteMember(tcpMember);
                        }
                    }
                }
                break;
            case INSTANCE_DEACTIVATED:
            case INSTANCE_REMOVED:
                if (autoAddMember) {
                    Set<DefaultControllerNode> members
                    = tabletMembers.getOrDefault(DEFAULT_TABLET,
                            Collections.emptySet());
                    // remove only if not the initial members
                    if (!members.contains(node)) {
                        synchronized (clusterConfig) {
                            if (clusterConfig.getMembers().contains(tcpMember)) {
                                log.info("{} was automatically removed from the cluster", tcpMember);
                                clusterConfig.removeRemoteMember(tcpMember);
                            }
                        }
                    }
                }
                break;
            default:
                break;
            }
            if (copycat != null) {
                log.debug("Current cluster: {}", copycat.cluster());
            }
            clusterEventLatch.countDown();
        }

    }

    @Override
    public void removeMember(final ControllerNode node) {
        final TcpMember tcpMember = new TcpMember(node.ip().toString(),
                                                  node.tcpPort());
      log.info("{} was removed from the cluster", tcpMember);
      synchronized (clusterConfig) {
          clusterConfig.removeRemoteMember(tcpMember);
      }
    }

    @Override
    public Collection<ControllerNode> listMembers() {
        if (copycat == null) {
            return ImmutableList.of();
        }
        Set<ControllerNode> members = new HashSet<>();
        for (Member member : copycat.cluster().members()) {
            ControllerNode node = getNodeIdFromMember(member);
            if (node == null) {
                log.info("No Node found for {}", member);
                continue;
            }
            members.add(node);
        }
        return members;
    }

    private ControllerNode getNodeIdFromMember(Member member) {
        if (member instanceof TcpMember) {
            final TcpMember tcpMember = (TcpMember) member;
            // TODO assuming tcpMember#host to be IP address,
            // but if not lookup DNS, etc. first
            IpAddress ip = IpAddress.valueOf(tcpMember.host());
            int tcpPort = tcpMember.port();
            for (ControllerNode node : clusterService.getNodes()) {
                if (node.ip().equals(ip) &&
                    node.tcpPort() == tcpPort) {
                    return node;
                }
            }
        }
        return null;
    }
}
