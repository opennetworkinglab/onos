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
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
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

    private static final long CONNECTION_CUSTODIAN_DELAY = 100L;
    private static final long CONNECTION_CUSTODIAN_FREQUENCY = 5000;

    private static final long SELECT_TIMEOUT = 50;
    private static final int WORKERS = 3;
    private static final int INITIATORS = 2;
    private static final int COMM_BUFFER_SIZE = 16 * 1024;
    private static final int COMM_IDLE_TIME = 500;

    private static final boolean SO_NO_DELAY = false;
    private static final int SO_SEND_BUFFER_SIZE = 128 * 1024;
    private static final int SO_RCV_BUFFER_SIZE = 128 * 1024;

    private DefaultControllerNode self;
    private final Map<NodeId, DefaultControllerNode> nodes = new ConcurrentHashMap<>();
    private final Map<NodeId, State> states = new ConcurrentHashMap<>();
    private final Map<NodeId, TLVMessageStream> streams = new ConcurrentHashMap<>();
    private final Map<SocketChannel, DefaultControllerNode> nodesByChannel = new ConcurrentHashMap<>();

    private final ExecutorService listenExecutor =
            Executors.newSingleThreadExecutor(namedThreads("onos-comm-listen"));
    private final ExecutorService commExecutors =
            Executors.newFixedThreadPool(WORKERS, namedThreads("onos-comm-cluster"));
    private final ExecutorService heartbeatExecutor =
            Executors.newSingleThreadExecutor(namedThreads("onos-comm-heartbeat"));
    private final ExecutorService initiatorExecutors =
            Executors.newFixedThreadPool(INITIATORS, namedThreads("onos-comm-initiator"));

    private final Timer timer = new Timer();
    private final TimerTask connectionCustodian = new ConnectionCustodian();

    private ListenLoop listenLoop;
    private List<CommLoop> commLoops = new ArrayList<>(WORKERS);

    @Activate
    public void activate() {
        establishIdentity();
        startCommunications();
        startListening();
        startInitiating();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        listenLoop.shutdown();
        for (CommLoop loop : commLoops) {
            loop.shutdown();
        }
        log.info("Stopped");
    }


    // Establishes the controller's own identity.
    private void establishIdentity() {
        IpPrefix ip = valueOf(System.getProperty("onos.ip", "127.0.1.1"));
        self = new DefaultControllerNode(new NodeId(ip.toString()), ip);
        nodes.put(self.id(), self);
    }

    // Kicks off the IO loops.
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

    /**
     * Initiates open connection request and registers the pending socket
     * channel with the given IO loop.
     *
     * @param loop loop with which the channel should be registered
     * @throws java.io.IOException if the socket could not be open or connected
     */
    private void openConnection(DefaultControllerNode node, CommLoop loop) throws IOException {
        SocketAddress sa = new InetSocketAddress(getByAddress(node.ip().toOctets()), node.tcpPort());
        SocketChannel ch = SocketChannel.open();
        nodesByChannel.put(ch, node);
        ch.configureBlocking(false);
        loop.connectStream(ch);
        ch.connect(sa);
    }


    // Attempts to connect to any nodes that do not have an associated connection.
    private void startInitiating() {
        timer.schedule(connectionCustodian, CONNECTION_CUSTODIAN_DELAY, CONNECTION_CUSTODIAN_FREQUENCY);
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
            SocketChannel sc = channel.accept();
            sc.configureBlocking(false);

            Socket so = sc.socket();
            so.setTcpNoDelay(SO_NO_DELAY);
            so.setReceiveBufferSize(SO_RCV_BUFFER_SIZE);
            so.setSendBufferSize(SO_SEND_BUFFER_SIZE);

            findLeastUtilizedLoop().acceptStream(sc);
            log.info("Connected client");
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

        @Override
        public TLVMessageStream acceptStream(SocketChannel channel) {
            TLVMessageStream stream = super.acceptStream(channel);
            try {
                InetSocketAddress sa = (InetSocketAddress) channel.getRemoteAddress();
                log.info("Accepted a new connection from {}", IpPrefix.valueOf(sa.getAddress().getAddress()));
            } catch (IOException e) {
                log.warn("Unable to accept connection from an unknown end-point", e);
            }
            return stream;
        }

        @Override
        public TLVMessageStream connectStream(SocketChannel channel) {
            TLVMessageStream stream = super.connectStream(channel);
            DefaultControllerNode node = nodesByChannel.get(channel);
            if (node != null) {
                log.info("Opened connection to {}", node.id());
                streams.put(node.id(), stream);
            }
            return stream;
        }
    }


    // Sweeps through all controller nodes and attempts to open connection to
    // those that presently do not have one.
    private class ConnectionCustodian extends TimerTask {
        @Override
        public void run() {
            for (DefaultControllerNode node : nodes.values()) {
                if (node != self && !streams.containsKey(node.id())) {
                    try {
                        openConnection(node, findLeastUtilizedLoop());
                    } catch (IOException e) {
                        log.warn("Unable to connect", e);
                    }
                }
            }
        }
    }

    // Finds the least utilities IO loop.
    private CommLoop findLeastUtilizedLoop() {
        CommLoop leastUtilized = null;
        int minCount = Integer.MAX_VALUE;
        for (CommLoop loop : commLoops) {
            int count = loop.streamCount();
            if (count == 0) {
                return loop;
            }

            if (count < minCount) {
                leastUtilized = loop;
                minCount = count;
            }
        }
        return leastUtilized;
    }
}
