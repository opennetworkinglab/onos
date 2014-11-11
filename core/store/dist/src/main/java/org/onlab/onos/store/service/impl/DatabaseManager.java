package org.onlab.onos.store.service.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.kuujo.copycat.Copycat;
import net.kuujo.copycat.StateMachine;
import net.kuujo.copycat.cluster.ClusterConfig;
import net.kuujo.copycat.cluster.Member;
import net.kuujo.copycat.cluster.TcpCluster;
import net.kuujo.copycat.cluster.TcpClusterConfig;
import net.kuujo.copycat.cluster.TcpMember;
import net.kuujo.copycat.log.InMemoryLog;
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
import org.onlab.onos.store.service.DatabaseAdminService;
import org.onlab.onos.store.service.DatabaseException;
import org.onlab.onos.store.service.DatabaseService;
import org.onlab.onos.store.service.NoSuchTableException;
import org.onlab.onos.store.service.OptimisticLockException;
import org.onlab.onos.store.service.OptionalResult;
import org.onlab.onos.store.service.ReadRequest;
import org.onlab.onos.store.service.ReadResult;
import org.onlab.onos.store.service.WriteAborted;
import org.onlab.onos.store.service.WriteRequest;
import org.onlab.onos.store.service.WriteResult;
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
    protected DatabaseProtocolService copycatMessagingProtocol;

    public static final String LOG_FILE_PREFIX = "/tmp/onos-copy-cat-log_";

    // Current working dir seems to be /opt/onos/apache-karaf-3.0.2
    // TODO: Get the path to /opt/onos/config
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


        StateMachine stateMachine = new DatabaseStateMachine();
        Log consensusLog = new MapDBLog(LOG_FILE_PREFIX + localNode.id(),
                                        ClusterMessagingProtocol.SERIALIZER);

        copycat = new Copycat(stateMachine, consensusLog, cluster, copycatMessagingProtocol);
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
    public void dropTable(String name) {
        client.dropTable(name);
    }

    @Override
    public void dropAllTables() {
        client.dropAllTables();
    }

    @Override
    public List<String> listTables() {
        return client.listTables();
    }

    @Override
    public ReadResult read(ReadRequest request) {
        return batchRead(Arrays.asList(request)).get(0).get();
    }

    @Override
    public List<OptionalResult<ReadResult, DatabaseException>> batchRead(
            List<ReadRequest> batch) {
        List<OptionalResult<ReadResult, DatabaseException>> readResults = new ArrayList<>(batch.size());
        for (InternalReadResult internalReadResult : client.batchRead(batch)) {
            if (internalReadResult.status() == InternalReadResult.Status.NO_SUCH_TABLE) {
                readResults.add(new DatabaseOperationResult<ReadResult, DatabaseException>(
                        new NoSuchTableException()));
            } else {
                readResults.add(new DatabaseOperationResult<ReadResult, DatabaseException>(
                        internalReadResult.result()));
            }
        }
        return readResults;
    }

    @Override
    public OptionalResult<WriteResult, DatabaseException> writeNothrow(WriteRequest request) {
        return batchWrite(Arrays.asList(request)).get(0);
    }

    @Override
    public WriteResult write(WriteRequest request) {
//            throws OptimisticLockException, PreconditionFailedException {
        return writeNothrow(request).get();
    }

    @Override
    public List<OptionalResult<WriteResult, DatabaseException>> batchWrite(
            List<WriteRequest> batch) {
        List<OptionalResult<WriteResult, DatabaseException>> writeResults = new ArrayList<>(batch.size());
        for (InternalWriteResult internalWriteResult : client.batchWrite(batch)) {
            if (internalWriteResult.status() == InternalWriteResult.Status.NO_SUCH_TABLE) {
                writeResults.add(new DatabaseOperationResult<WriteResult, DatabaseException>(
                        new NoSuchTableException()));
            } else if (internalWriteResult.status() == InternalWriteResult.Status.PREVIOUS_VERSION_MISMATCH) {
                writeResults.add(new DatabaseOperationResult<WriteResult, DatabaseException>(
                        new OptimisticLockException()));
            } else if (internalWriteResult.status() == InternalWriteResult.Status.PREVIOUS_VALUE_MISMATCH) {
                // TODO: throw a different exception?
                writeResults.add(new DatabaseOperationResult<WriteResult, DatabaseException>(
                        new OptimisticLockException()));
            } else if (internalWriteResult.status() == InternalWriteResult.Status.ABORTED) {
                writeResults.add(new DatabaseOperationResult<WriteResult, DatabaseException>(
                        new WriteAborted()));
            } else {
                writeResults.add(new DatabaseOperationResult<WriteResult, DatabaseException>(
                        internalWriteResult.result()));
            }
        }
        return writeResults;

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

    public static final class KryoRegisteredInMemoryLog extends InMemoryLog {
        public KryoRegisteredInMemoryLog() {
            super();
            // required to deserialize object across bundles
            super.kryo.register(TcpMember.class, new TcpMemberSerializer());
            super.kryo.register(TcpClusterConfig.class, new TcpClusterConfigSerializer());
        }
    }

    private class DatabaseOperationResult<R, E extends DatabaseException> implements OptionalResult<R, E> {

        private final R result;
        private final DatabaseException exception;

        public DatabaseOperationResult(R result) {
            this.result = result;
            this.exception = null;
        }

        public DatabaseOperationResult(DatabaseException exception) {
            this.result = null;
            this.exception = exception;
        }

        @Override
        public R get() {
            if (result != null) {
                return result;
            }
            throw exception;
        }

        @Override
        public boolean hasValidResult() {
            return result != null;
        }

        @Override
        public String toString() {
            if (result != null) {
                return result.toString();
            } else {
                return exception.toString();
            }
        }
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
