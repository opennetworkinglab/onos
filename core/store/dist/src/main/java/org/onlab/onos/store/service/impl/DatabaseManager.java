package org.onlab.onos.store.service.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.kuujo.copycat.Copycat;
import net.kuujo.copycat.cluster.ClusterConfig;
import net.kuujo.copycat.cluster.Member;
import net.kuujo.copycat.cluster.TcpCluster;
import net.kuujo.copycat.cluster.TcpClusterConfig;
import net.kuujo.copycat.cluster.TcpMember;
import net.kuujo.copycat.event.LeaderElectEvent;
import net.kuujo.copycat.log.Log;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterEventListener;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.service.BatchReadRequest;
import org.onlab.onos.store.service.BatchReadResult;
import org.onlab.onos.store.service.BatchWriteRequest;
import org.onlab.onos.store.service.BatchWriteResult;
import org.onlab.onos.store.service.DatabaseAdminService;
import org.onlab.onos.store.service.DatabaseException;
import org.onlab.onos.store.service.DatabaseService;
import org.onlab.onos.store.service.ReadResult;
import org.onlab.onos.store.service.ReadStatus;
import org.onlab.onos.store.service.VersionedValue;
import org.onlab.onos.store.service.WriteResult;
import org.onlab.onos.store.service.WriteStatus;
import org.onlab.packet.IpAddress;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

/**
 * Strongly consistent and durable state management service based on
 * Copycat implementation of Raft consensus protocol.
 */
@Component(immediate = true)
@Service
public class DatabaseManager implements DatabaseService, DatabaseAdminService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DatabaseProtocolService copycatMessagingProtocol;

    // FIXME: point to appropriate path
    public static final String LOG_FILE_PREFIX = "/tmp/onos-copy-cat-log_";

    // Current working dir seems to be /opt/onos/apache-karaf-3.0.2
    // TODO: Set the path to /opt/onos/config
    private static final String CONFIG_DIR = "../config";

    private static final String DEFAULT_MEMBER_FILE = "tablets.json";

    private static final String DEFAULT_TABLET = "default";

    // TODO: make this configurable
    // initial member configuration file path
    private String initialMemberConfig = DEFAULT_MEMBER_FILE;

    private Copycat copycat;
    private DatabaseClient client;

    // guarded by synchronized block
    private ClusterConfig<TcpMember> clusterConfig;

    private CountDownLatch clusterEventLatch;
    private ClusterEventListener clusterEventListener;

    private Map<String, Set<DefaultControllerNode>> tabletMembers;

    private boolean autoAddMember = false;

    @Activate
    public void activate() {

        // TODO: Not every node should be part of the consensus ring.

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
        Set<DefaultControllerNode> defaultMember = tabletMembers.get(DEFAULT_TABLET);
        if (defaultMember == null || defaultMember.isEmpty()) {
            log.error("No member found in [{}] tablet configuration.",
                      DEFAULT_TABLET);
            throw new IllegalStateException("No member found in tablet configuration");

        }

        final ControllerNode localNode = clusterService.getLocalNode();
        for (ControllerNode member : defaultMember) {
            final TcpMember tcpMember = new TcpMember(member.ip().toString(),
                                                      member.tcpPort());
            if (localNode.equals(member)) {
                clusterConfig.setLocalMember(tcpMember);
            } else {
                clusterConfig.addRemoteMember(tcpMember);
            }
        }

        // note: from this point beyond, clusterConfig requires synchronization
        clusterEventLatch = new CountDownLatch(1);
        clusterEventListener = new InternalClusterEventListener();
        clusterService.addListener(clusterEventListener);

        if (clusterService.getNodes().size() < clusterConfig.getMembers().size()) {
            // current cluster size smaller then expected
            try {
                if (!clusterEventLatch.await(120, TimeUnit.SECONDS)) {
                    log.info("Starting with {}/{} nodes cluster",
                             clusterService.getNodes().size(),
                             clusterConfig.getMembers().size());
                }
            } catch (InterruptedException e) {
                log.info("Interrupted waiting for others", e);
            }
        }

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
        Log consensusLog = new MapDBLog(LOG_FILE_PREFIX + localNode.id(),
                                        ClusterMessagingProtocol.SERIALIZER);

        copycat = new Copycat(stateMachine, consensusLog, cluster, copycatMessagingProtocol);

        copycat.event(LeaderElectEvent.class).registerHandler(expirationTracker);

        copycat.start();

        client = new DatabaseClient(copycat);

        log.info("Started.");
    }

    @Deactivate
    public void deactivate() {
        clusterService.removeListener(clusterEventListener);
        copycat.stop();
        log.info("Stopped.");
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
            if (member instanceof TcpMember) {
                final TcpMember tcpMember = (TcpMember) member;
                // TODO assuming tcpMember#host to be IP address,
                // but if not lookup DNS, etc. first
                IpAddress ip = IpAddress.valueOf(tcpMember.host());
                int tcpPort = tcpMember.port();
                NodeId id = getNodeIdFromIp(ip, tcpPort);
                if (id == null) {
                    log.info("No NodeId found for {}:{}", ip, tcpPort);
                    continue;
                }
                members.add(new DefaultControllerNode(id, ip, tcpPort));
            }
        }
        return members;
    }

    private NodeId getNodeIdFromIp(IpAddress ip, int tcpPort) {
        for (ControllerNode node : clusterService.getNodes()) {
            if (node.ip().equals(ip) &&
                node.tcpPort() == tcpPort) {
                return node.id();
            }
        }
        return null;
    }
}
