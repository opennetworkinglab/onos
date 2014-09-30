package org.onlab.onos.store.cluster.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.GoodbyeMessage;
import org.onlab.onos.store.cluster.messaging.HelloMessage;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.onlab.onos.store.cluster.messaging.MessageSubscriber;
import org.onlab.onos.store.cluster.messaging.SerializationService;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.net.InetAddress.getByAddress;
import static org.onlab.util.Tools.namedThreads;

/**
 * Implements the cluster communication services to use by other stores.
 */
@Component(immediate = true)
@Service
public class ClusterCommunicationManager
        implements ClusterCommunicationService, ClusterCommunicationAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final long CONNECTION_CUSTODIAN_DELAY = 100L;
    private static final long CONNECTION_CUSTODIAN_FREQUENCY = 2000;

    private static final long START_TIMEOUT = 1000;
    private static final int WORKERS = 3;

    private ClusterConnectionListener connectionListener;
    private List<ClusterIOWorker> workers = new ArrayList<>(WORKERS);

    private DefaultControllerNode localNode;
    private ClusterNodesDelegate nodesDelegate;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SerializationService serializationService;

    // Nodes to be monitored to make sure they have a connection.
    private final Set<DefaultControllerNode> nodes = new HashSet<>();

    // Means to track message streams to other nodes.
    private final Map<NodeId, ClusterMessageStream> streams = new ConcurrentHashMap<>();

    // TODO: use something different that won't require synchronization
    private Multimap<MessageSubject, MessageSubscriber> subscribers = HashMultimap.create();

    // Executor pools for listening and managing connections to other nodes.
    private final ExecutorService listenExecutor =
            Executors.newSingleThreadExecutor(namedThreads("onos-comm-listen"));
    private final ExecutorService commExecutors =
            Executors.newFixedThreadPool(WORKERS, namedThreads("onos-comm-cluster"));
    private final ExecutorService heartbeatExecutor =
            Executors.newSingleThreadExecutor(namedThreads("onos-comm-heartbeat"));

    private final Timer timer = new Timer("onos-comm-initiator");
    private final TimerTask connectionCustodian = new ConnectionCustodian();
    private GoodbyeSubscriber goodbyeSubscriber = new GoodbyeSubscriber();

    @Activate
    public void activate() {
        addSubscriber(MessageSubject.GOODBYE, goodbyeSubscriber);
        log.info("Activated but waiting for delegate");
    }

    @Deactivate
    public void deactivate() {
        connectionCustodian.cancel();
        if (connectionListener != null) {
            connectionListener.shutdown();
            for (ClusterIOWorker worker : workers) {
                worker.shutdown();
            }
        }
        log.info("Stopped");
    }

    @Override
    public boolean send(ClusterMessage message) {
        boolean ok = true;
        for (DefaultControllerNode node : nodes) {
            if (!node.equals(localNode)) {
                ok = send(message, node.id()) && ok;
            }
        }
        return ok;
    }

    @Override
    public boolean send(ClusterMessage message, NodeId toNodeId) {
        ClusterMessageStream stream = streams.get(toNodeId);
        if (stream != null && !toNodeId.equals(localNode.id())) {
            try {
                stream.write(message);
                return true;
            } catch (IOException e) {
                log.warn("Unable to send message {} to node {}",
                         message.subject(), toNodeId);
            }
        }
        return false;
    }

    @Override
    public synchronized void addSubscriber(MessageSubject subject,
                                           MessageSubscriber subscriber) {
        subscribers.put(subject, subscriber);
    }

    @Override
    public synchronized void removeSubscriber(MessageSubject subject,
                                              MessageSubscriber subscriber) {
        subscribers.remove(subject, subscriber);
    }

    @Override
    public Set<MessageSubscriber> getSubscribers(MessageSubject subject) {
        return ImmutableSet.copyOf(subscribers.get(subject));
    }

    @Override
    public void addNode(DefaultControllerNode node) {
        nodes.add(node);
    }

    @Override
    public void removeNode(DefaultControllerNode node) {
        send(new GoodbyeMessage(node.id()));
        nodes.remove(node);
        ClusterMessageStream stream = streams.remove(node.id());
        if (stream != null) {
            stream.close();
        }
    }

    @Override
    public void startUp(DefaultControllerNode localNode,
                        ClusterNodesDelegate delegate) {
        this.localNode = localNode;
        this.nodesDelegate = delegate;

        startCommunications();
        startListening();
        startInitiatingConnections();
        log.info("Started");
    }

    @Override
    public void clearAllNodesAndStreams() {
        nodes.clear();
        send(new GoodbyeMessage(localNode.id()));
        for (ClusterMessageStream stream : streams.values()) {
            stream.close();
        }
        streams.clear();
    }

    /**
     * Dispatches the specified message to all subscribers to its subject.
     *
     * @param message message to dispatch
     * @param fromNodeId node from which the message was received
     */
    void dispatch(ClusterMessage message, NodeId fromNodeId) {
        Set<MessageSubscriber> set = getSubscribers(message.subject());
        if (set != null) {
            for (MessageSubscriber subscriber : set) {
                subscriber.receive(message, fromNodeId);
            }
        }
    }

    /**
     * Removes the stream associated with the specified node.
     *
     * @param nodeId  newly detected cluster node id
     * @param ip      node IP listen address
     * @param tcpPort node TCP listen port
     * @return controller node bound to the stream
     */
    DefaultControllerNode addNodeStream(NodeId nodeId, IpPrefix ip, int tcpPort,
                                        ClusterMessageStream stream) {
        DefaultControllerNode node = nodesDelegate.nodeDetected(nodeId, ip, tcpPort);
        stream.setNode(node);
        streams.put(node.id(), stream);
        return node;
    }

    /**
     * Removes the stream associated with the specified node.
     *
     * @param node node whose stream to remove
     */
    void removeNodeStream(DefaultControllerNode node) {
        nodesDelegate.nodeVanished(node.id());
        streams.remove(node.id());
    }

    /**
     * Finds the least utilized IO worker.
     *
     * @return IO worker
     */
    ClusterIOWorker findWorker() {
        ClusterIOWorker leastUtilized = null;
        int minCount = Integer.MAX_VALUE;
        for (ClusterIOWorker worker : workers) {
            int count = worker.streamCount();
            if (count == 0) {
                return worker;
            }

            if (count < minCount) {
                leastUtilized = worker;
                minCount = count;
            }
        }
        return leastUtilized;
    }

    /**
     * Kicks off the IO loops and waits for them to startup.
     */
    private void startCommunications() {
        HelloMessage hello = new HelloMessage(localNode.id(), localNode.ip(),
                                              localNode.tcpPort());
        for (int i = 0; i < WORKERS; i++) {
            try {
                ClusterIOWorker worker =
                        new ClusterIOWorker(this, serializationService, hello);
                workers.add(worker);
                commExecutors.execute(worker);
            } catch (IOException e) {
                log.warn("Unable to start communication worker", e);
            }
        }

        // Wait for the IO loops to start
        for (ClusterIOWorker loop : workers) {
            if (!loop.awaitStart(START_TIMEOUT)) {
                log.warn("Comm loop did not start on-time; moving on...");
            }
        }
    }

    /**
     * Starts listening for connections from peer cluster members.
     */
    private void startListening() {
        try {
            connectionListener =
                    new ClusterConnectionListener(this, localNode.ip(), localNode.tcpPort());
            listenExecutor.execute(connectionListener);
            if (!connectionListener.awaitStart(START_TIMEOUT)) {
                log.warn("Listener did not start on-time; moving on...");
            }
        } catch (IOException e) {
            log.error("Unable to listen for cluster connections", e);
        }
    }

    /**
     * Attempts to connect to any nodes that do not have an associated connection.
     */
    private void startInitiatingConnections() {
        timer.schedule(connectionCustodian, CONNECTION_CUSTODIAN_DELAY,
                       CONNECTION_CUSTODIAN_FREQUENCY);
    }

    /**
     * Initiates open connection request and registers the pending socket
     * channel with the given IO worker.
     *
     * @param worker loop with which the channel should be registered
     * @throws java.io.IOException if the socket could not be open or connected
     */
    private void initiateConnection(DefaultControllerNode node,
                                    ClusterIOWorker worker) throws IOException {
        SocketAddress sa = new InetSocketAddress(getByAddress(node.ip().toOctets()), node.tcpPort());
        SocketChannel ch = SocketChannel.open();
        ch.configureBlocking(false);
        ch.connect(sa);
        worker.connectStream(ch);
    }

    // Sweeps through all controller nodes and attempts to open connection to
    // those that presently do not have one.
    private class ConnectionCustodian extends TimerTask {
        @Override
        public void run() {
            for (DefaultControllerNode node : nodes) {
                if (!node.id().equals(localNode.id()) && !streams.containsKey(node.id())) {
                    try {
                        initiateConnection(node, findWorker());
                    } catch (IOException e) {
                        log.debug("Unable to connect", e);
                    }
                }
            }
        }
    }

    private class GoodbyeSubscriber implements MessageSubscriber {
        @Override
        public void receive(ClusterMessage message, NodeId fromNodeId) {
            log.info("Received goodbye message from {}", fromNodeId);
            nodesDelegate.nodeRemoved(fromNodeId);
        }
    }
}
