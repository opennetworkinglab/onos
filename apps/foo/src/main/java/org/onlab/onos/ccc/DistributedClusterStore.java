package org.onlab.onos.ccc;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.nio.AcceptorLoop;
import org.onlab.nio.IOLoop;
import org.onlab.nio.MessageStream;
import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterStore;
import org.onlab.onos.cluster.ClusterStoreDelegate;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.AbstractStore;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.net.InetAddress.getByAddress;
import static org.onlab.onos.cluster.ControllerNode.State;
import static org.onlab.packet.IpPrefix.valueOf;
import static org.onlab.util.Tools.namedThreads;

/**
 * Distributed implementation of the cluster nodes store.
 */
@Component(immediate = true)
@Service
public class DistributedClusterStore
        extends AbstractStore<ClusterEvent, ClusterStoreDelegate>
        implements ClusterStore {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final long SELECT_TIMEOUT = 50;
    private static final int WORKERS = 3;
    private static final int COMM_BUFFER_SIZE = 16 * 1024;
    private static final int COMM_IDLE_TIME = 500;

    private DefaultControllerNode self;
    private final Map<NodeId, DefaultControllerNode> nodes = new ConcurrentHashMap<>();
    private final Map<NodeId, State> states = new ConcurrentHashMap<>();

    private final ExecutorService listenExecutor =
            Executors.newSingleThreadExecutor(namedThreads("onos-listen"));
    private final ExecutorService commExecutors =
            Executors.newFixedThreadPool(WORKERS, namedThreads("onos-cluster"));
    private final ExecutorService heartbeatExecutor =
            Executors.newSingleThreadExecutor(namedThreads("onos-heartbeat"));

    private ListenLoop listenLoop;
    private List<CommLoop> commLoops = new ArrayList<>(WORKERS);

    @Activate
    public void activate() {
        establishIdentity();
        startCommunications();
        startListening();
        log.info("Started");
    }

    private void startCommunications() {
        for (int i = 0; i < WORKERS; i++) {
            try {
                CommLoop loop = new CommLoop();
                commLoops.add(loop);
                commExecutors.execute(loop);
            } catch (IOException e) {
                log.warn("Unable to start comm IO loop", e);
            }
        }
    }

    // Starts listening for connections from peer cluster members.
    private void startListening() {
        try {
            listenLoop = new ListenLoop(self.ip(), self.tcpPort());
            listenExecutor.execute(listenLoop);
        } catch (IOException e) {
            log.error("Unable to listen for cluster connections", e);
        }
    }

    // Establishes the controller's own identity.
    private void establishIdentity() {
        // For now rely on env. variable.
        IpPrefix ip = valueOf(System.getenv("ONOS_NIC"));
        self = new DefaultControllerNode(new NodeId(ip.toString()), ip);
    }

    @Deactivate
    public void deactivate() {
        listenLoop.shutdown();
        for (CommLoop loop : commLoops) {
            loop.shutdown();
        }
        log.info("Stopped");
    }

    @Override
    public ControllerNode getLocalNode() {
        return self;
    }

    @Override
    public Set<ControllerNode> getNodes() {
        ImmutableSet.Builder<ControllerNode> builder = ImmutableSet.builder();
        return builder.addAll(nodes.values()).build();
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        return nodes.get(nodeId);
    }

    @Override
    public State getState(NodeId nodeId) {
        State state = states.get(nodeId);
        return state == null ? State.INACTIVE : state;
    }

    @Override
    public ControllerNode addNode(NodeId nodeId, IpPrefix ip, int tcpPort) {
        DefaultControllerNode node = new DefaultControllerNode(nodeId, ip, tcpPort);
        nodes.put(nodeId, node);
        return node;
    }

    @Override
    public void removeNode(NodeId nodeId) {
        nodes.remove(nodeId);
    }

    // Listens and accepts inbound connections from other cluster nodes.
    private class ListenLoop extends AcceptorLoop {
        ListenLoop(IpPrefix ip, int tcpPort) throws IOException {
            super(SELECT_TIMEOUT, new InetSocketAddress(getByAddress(ip.toOctets()), tcpPort));
        }

        @Override
        protected void acceptConnection(ServerSocketChannel channel) throws IOException {

        }
    }

    private class CommLoop extends IOLoop<TLVMessage, TLVMessageStream> {
        CommLoop() throws IOException {
            super(SELECT_TIMEOUT);
        }

        @Override
        protected TLVMessageStream createStream(ByteChannel byteChannel) {
            return new TLVMessageStream(this, byteChannel, COMM_BUFFER_SIZE, COMM_IDLE_TIME);
        }

        @Override
        protected void processMessages(List<TLVMessage> messages, MessageStream<TLVMessage> stream) {

        }
    }
}
