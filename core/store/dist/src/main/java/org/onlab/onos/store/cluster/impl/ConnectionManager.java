package org.onlab.onos.store.cluster.impl;

import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.ClusterMessageStream;
import org.onlab.onos.store.cluster.messaging.HelloMessage;
import org.onlab.onos.store.cluster.messaging.SerializationService;
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
 * Manages connections to other controller cluster nodes.
 */
public class ConnectionManager implements MessageSender {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final long CONNECTION_CUSTODIAN_DELAY = 1000L;
    private static final long CONNECTION_CUSTODIAN_FREQUENCY = 5000;

    private static final long START_TIMEOUT = 1000;
    private static final int WORKERS = 3;

    private ClusterConnectionListener connectionListener;
    private List<ClusterIOWorker> workers = new ArrayList<>(WORKERS);

    private final DefaultControllerNode localNode;
    private final ClusterNodesDelegate nodesDelegate;
    private final CommunicationsDelegate commsDelegate;
    private final SerializationService serializationService;

    // Nodes to be monitored to make sure they have a connection.
    private final Set<DefaultControllerNode> nodes = new HashSet<>();

    // Means to track message streams to other nodes.
    private final Map<NodeId, ClusterMessageStream> streams = new ConcurrentHashMap<>();

    // Executor pools for listening and managing connections to other nodes.
    private final ExecutorService listenExecutor =
            Executors.newSingleThreadExecutor(namedThreads("onos-comm-listen"));
    private final ExecutorService commExecutors =
            Executors.newFixedThreadPool(WORKERS, namedThreads("onos-comm-cluster"));
    private final ExecutorService heartbeatExecutor =
            Executors.newSingleThreadExecutor(namedThreads("onos-comm-heartbeat"));

    private final Timer timer = new Timer("onos-comm-initiator");
    private final TimerTask connectionCustodian = new ConnectionCustodian();

    private final WorkerFinder workerFinder = new LeastUtilitiedWorkerFinder();


    /**
     * Creates a new connection manager.
     */
    ConnectionManager(DefaultControllerNode localNode,
                      ClusterNodesDelegate nodesDelegate,
                      CommunicationsDelegate commsDelegate,
                      SerializationService serializationService) {
        this.localNode = localNode;
        this.nodesDelegate = nodesDelegate;
        this.commsDelegate = commsDelegate;
        this.serializationService = serializationService;

        commsDelegate.setSender(this);
        startCommunications();
        startListening();
        startInitiating();
        log.info("Started");
    }

    /**
     * Shuts down the connection manager.
     */
    void shutdown() {
        connectionListener.shutdown();
        for (ClusterIOWorker worker : workers) {
            worker.shutdown();
        }
        log.info("Stopped");
    }

    /**
     * Adds the node to the list of monitored nodes.
     *
     * @param node node to be added
     */
    void addNode(DefaultControllerNode node) {
        nodes.add(node);
    }

    /**
     * Removes the node from the list of monitored nodes.
     *
     * @param node node to be removed
     */
    void removeNode(DefaultControllerNode node) {
        nodes.remove(node);
        ClusterMessageStream stream = streams.remove(node.id());
        if (stream != null) {
            stream.close();
        }
    }

    /**
     * Removes the stream associated with the specified node.
     *
     * @param node node whose stream to remove
     */
    void removeNodeStream(DefaultControllerNode node) {
        nodesDelegate.nodeVanished(node);
        streams.remove(node.id());
    }

    @Override
    public boolean send(NodeId nodeId, ClusterMessage message) {
        ClusterMessageStream stream = streams.get(nodeId);
        if (stream != null) {
            try {
                stream.write(message);
                return true;
            } catch (IOException e) {
                log.warn("Unable to send a message about {} to node {}",
                         message.subject(), nodeId);
            }
        }
        return false;
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
                        new ClusterIOWorker(this, commsDelegate,
                                            serializationService, hello);
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
                    new ClusterConnectionListener(localNode.ip(), localNode.tcpPort(),
                                                  workerFinder);
            listenExecutor.execute(connectionListener);
            if (!connectionListener.awaitStart(START_TIMEOUT)) {
                log.warn("Listener did not start on-time; moving on...");
            }
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
    private void initiateConnection(DefaultControllerNode node,
                                    ClusterIOWorker loop) throws IOException {
        SocketAddress sa = new InetSocketAddress(getByAddress(node.ip().toOctets()), node.tcpPort());
        SocketChannel ch = SocketChannel.open();
        ch.configureBlocking(false);
        ch.connect(sa);
        loop.connectStream(ch);
    }


    /**
     * Attempts to connect to any nodes that do not have an associated connection.
     */
    private void startInitiating() {
        timer.schedule(connectionCustodian, CONNECTION_CUSTODIAN_DELAY,
                       CONNECTION_CUSTODIAN_FREQUENCY);
    }

    // Sweeps through all controller nodes and attempts to open connection to
    // those that presently do not have one.
    private class ConnectionCustodian extends TimerTask {
        @Override
        public void run() {
            for (DefaultControllerNode node : nodes) {
                if (node != localNode && !streams.containsKey(node.id())) {
                    try {
                        initiateConnection(node, workerFinder.findWorker());
                    } catch (IOException e) {
                        log.debug("Unable to connect", e);
                    }
                }
            }
        }
    }

    // Finds the least utilitied IO loop.
    private class LeastUtilitiedWorkerFinder implements WorkerFinder {

        @Override
        public ClusterIOWorker findWorker() {
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
    }

}
