/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.store.cluster.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hazelcast.util.AddressUtil;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.joda.time.DateTime;
import org.onlab.netty.NettyMessagingService;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterStore;
import org.onosproject.cluster.ClusterStoreDelegate;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.ControllerNode.State;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.consistent.impl.DatabaseDefinition;
import org.onosproject.store.consistent.impl.DatabaseDefinitionStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hazelcast.util.AddressUtil.matchInterface;
import static java.net.NetworkInterface.getNetworkInterfaces;
import static java.util.Collections.list;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.cluster.DefaultControllerNode.DEFAULT_PORT;
import static org.onosproject.store.consistent.impl.DatabaseManager.PARTITION_DEFINITION_FILE;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
@Service
/**
 * Distributed cluster nodes store that employs an accrual failure
 * detector to identify cluster member up/down status.
 */
public class DistributedClusterStore
        extends AbstractStore<ClusterEvent, ClusterStoreDelegate>
        implements ClusterStore {

    private static final Logger log = getLogger(DistributedClusterStore.class);

    public static final String CLUSTER_DEFINITION_FILE = "../config/cluster.json";
    public static final String HEARTBEAT_MESSAGE = "onos-cluster-heartbeat";

    // TODO: make these configurable.
    private static final int HEARTBEAT_FD_PORT = 2419;
    private static final int HEARTBEAT_INTERVAL_MS = 100;
    private static final int PHI_FAILURE_THRESHOLD = 10;

    private static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(KryoNamespaces.API)
                    .register(HeartbeatMessage.class)
                    .build()
                    .populate(1);
        }
    };

    private static final String INSTANCE_ID_NULL = "Instance ID cannot be null";
    private static final byte SITE_LOCAL_BYTE = (byte) 0xC0;
    private static final String ONOS_NIC = "ONOS_NIC";

    private ClusterDefinition clusterDefinition;

    private Set<ControllerNode> seedNodes;
    private final Map<NodeId, ControllerNode> allNodes = Maps.newConcurrentMap();
    private final Map<NodeId, State> nodeStates = Maps.newConcurrentMap();
    private final Map<NodeId, DateTime> nodeStateLastUpdatedTimes = Maps.newConcurrentMap();
    private NettyMessagingService messagingService;
    private ScheduledExecutorService heartBeatSender = Executors.newSingleThreadScheduledExecutor(
            groupedThreads("onos/cluster/membership", "heartbeat-sender"));
    private ExecutorService heartBeatMessageHandler = Executors.newSingleThreadExecutor(
            groupedThreads("onos/cluster/membership", "heartbeat-receiver"));

    private PhiAccrualFailureDetector failureDetector;

    private ControllerNode localNode;

    @Activate
    public void activate() {
        File clusterDefinitionFile = new File(CLUSTER_DEFINITION_FILE);
        ClusterDefinitionStore clusterDefinitionStore =
                new ClusterDefinitionStore(clusterDefinitionFile.getPath());

        if (!clusterDefinitionFile.exists()) {
            createDefaultClusterDefinition(clusterDefinitionStore);
        }

        try {
            clusterDefinition = clusterDefinitionStore.read();
            seedNodes = ImmutableSet
                    .copyOf(clusterDefinition.getNodes())
                    .stream()
                    .map(n -> new DefaultControllerNode(new NodeId(n.getId()),
                                                        IpAddress.valueOf(n.getIp()),
                                                        n.getTcpPort()))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read cluster definition.", e);
        }

        seedNodes.forEach(node -> {
            allNodes.put(node.id(), node);
            updateState(node.id(), State.INACTIVE);
        });

        establishSelfIdentity();

        messagingService = new NettyMessagingService(HEARTBEAT_FD_PORT);
        try {
            messagingService.activate();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Failed to cleanly initialize membership and"
                            + " failure detector communication channel.", e);
        }
        messagingService.registerHandler(HEARTBEAT_MESSAGE,
                                         new HeartbeatMessageHandler(), heartBeatMessageHandler);

        failureDetector = new PhiAccrualFailureDetector();

        heartBeatSender.scheduleWithFixedDelay(this::heartbeat, 0,
                                               HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);

        log.info("Started");
    }

    private void createDefaultClusterDefinition(ClusterDefinitionStore store) {
        // Assumes IPv4 is returned.
        String ip = DistributedClusterStore.getSiteLocalAddress();
        String ipPrefix = ip.replaceFirst("\\.[0-9]*$", ".*");
        NodeInfo node = NodeInfo.from(ip, ip, DEFAULT_PORT);
        try {
            store.write(ClusterDefinition.from(ImmutableSet.of(node), ipPrefix));
        } catch (IOException e) {
            log.warn("Unable to write default cluster definition", e);
        }
    }

    /**
     * Returns the address that matches the IP prefix given in ONOS_NIC
     * environment variable if one was specified, or the first site local
     * address if one can be found or the loopback address otherwise.
     *
     * @return site-local address in string form
     */
    public static String getSiteLocalAddress() {
        try {
            String ipPrefix = System.getenv(ONOS_NIC);
            for (NetworkInterface nif : list(getNetworkInterfaces())) {
                for (InetAddress address : list(nif.getInetAddresses())) {
                    IpAddress ip = IpAddress.valueOf(address);
                    if (ipPrefix == null && address.isSiteLocalAddress() ||
                            ipPrefix != null && matchInterface(ip.toString(), ipPrefix)) {
                        return ip.toString();
                    }
                }
            }

        } catch (SocketException e) {
            log.error("Unable to get network interfaces", e);
        }

        return IpAddress.valueOf(InetAddress.getLoopbackAddress()).toString();
    }

    @Deactivate
    public void deactivate() {
        try {
            messagingService.deactivate();
        } catch (Exception e) {
            log.trace("Failed to cleanly shutdown cluster membership messaging", e);
        }

        heartBeatSender.shutdownNow();
        heartBeatMessageHandler.shutdownNow();

        log.info("Stopped");
    }

    @Override
    public void setDelegate(ClusterStoreDelegate delegate) {
        checkNotNull(delegate, "Delegate cannot be null");
        this.delegate = delegate;
    }

    @Override
    public void unsetDelegate(ClusterStoreDelegate delegate) {
        this.delegate = null;
    }

    @Override
    public boolean hasDelegate() {
        return this.delegate != null;
    }

    @Override
    public ControllerNode getLocalNode() {
        return localNode;
    }

    @Override
    public Set<ControllerNode> getNodes() {
        return ImmutableSet.copyOf(allNodes.values());
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return allNodes.get(nodeId);
    }

    @Override
    public State getState(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return nodeStates.get(nodeId);
    }

    @Override
    public ControllerNode addNode(NodeId nodeId, IpAddress ip, int tcpPort) {
        ControllerNode node = new DefaultControllerNode(nodeId, ip, tcpPort);
        allNodes.put(node.id(), node);
        updateState(nodeId, State.INACTIVE);
        delegate.notify(new ClusterEvent(ClusterEvent.Type.INSTANCE_ADDED, node));
        return node;
    }

    @Override
    public void removeNode(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        ControllerNode node = allNodes.remove(nodeId);
        if (node != null) {
            nodeStates.remove(nodeId);
            delegate.notify(new ClusterEvent(ClusterEvent.Type.INSTANCE_REMOVED, node));
        }
    }

    @Override
    public void formCluster(Set<ControllerNode> nodes, String ipPrefix) {
        try {
            Set<NodeInfo> infos = Sets.newHashSet();
            nodes.forEach(n -> infos.add(NodeInfo.from(n.id().toString(),
                                                       n.ip().toString(),
                                                       n.tcpPort())));

            ClusterDefinition cdef = ClusterDefinition.from(infos, ipPrefix);
            new ClusterDefinitionStore(CLUSTER_DEFINITION_FILE).write(cdef);

            DatabaseDefinition ddef = DatabaseDefinition.from(infos);
            new DatabaseDefinitionStore(PARTITION_DEFINITION_FILE).write(ddef);
        } catch (IOException e) {
            log.error("Unable to form cluster", e);
        }
    }

    private void updateState(NodeId nodeId, State newState) {
        nodeStates.put(nodeId, newState);
        nodeStateLastUpdatedTimes.put(nodeId, DateTime.now());
    }

    private void establishSelfIdentity() {
        try {
            IpAddress ip = findLocalIp();
            localNode = new DefaultControllerNode(new NodeId(ip.toString()), ip);
            allNodes.put(localNode.id(), localNode);
            updateState(localNode.id(), State.ACTIVE);
            log.info("Local Node: {}", localNode);
        } catch (SocketException e) {
            throw new IllegalStateException("Cannot determine local IP", e);
        }
    }

    private void heartbeat() {
        try {
            Set<ControllerNode> peers = allNodes.values()
                    .stream()
                    .filter(node -> !(node.id().equals(localNode.id())))
                    .collect(Collectors.toSet());
            byte[] hbMessagePayload = SERIALIZER.encode(new HeartbeatMessage(localNode, peers));
            peers.forEach((node) -> {
                heartbeatToPeer(hbMessagePayload, node);
                State currentState = nodeStates.get(node.id());
                double phi = failureDetector.phi(node.id());
                if (phi >= PHI_FAILURE_THRESHOLD) {
                    if (currentState == State.ACTIVE) {
                        updateState(node.id(), State.INACTIVE);
                        notifyStateChange(node.id(), State.ACTIVE, State.INACTIVE);
                    }
                } else {
                    if (currentState == State.INACTIVE) {
                        updateState(node.id(), State.ACTIVE);
                        notifyStateChange(node.id(), State.INACTIVE, State.ACTIVE);
                    }
                }
            });
        } catch (Exception e) {
            log.debug("Failed to send heartbeat", e);
        }
    }

    private void notifyStateChange(NodeId nodeId, State oldState, State newState) {
        ControllerNode node = allNodes.get(nodeId);
        if (newState == State.ACTIVE) {
            delegate.notify(new ClusterEvent(ClusterEvent.Type.INSTANCE_ACTIVATED, node));
        } else {
            delegate.notify(new ClusterEvent(ClusterEvent.Type.INSTANCE_DEACTIVATED, node));
        }
    }

    private void heartbeatToPeer(byte[] messagePayload, ControllerNode peer) {
        Endpoint remoteEp = new Endpoint(peer.ip(), HEARTBEAT_FD_PORT);
        try {
            messagingService.sendAsync(remoteEp, HEARTBEAT_MESSAGE, messagePayload);
        } catch (IOException e) {
            log.trace("Sending heartbeat to {} failed", remoteEp, e);
        }
    }

    private IpAddress findLocalIp() throws SocketException {
        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                IpAddress ip = IpAddress.valueOf(inetAddresses.nextElement());
                if (AddressUtil.matchInterface(ip.toString(), clusterDefinition.getIpPrefix())) {
                    return ip;
                }
            }
        }
        throw new IllegalStateException("Unable to determine local ip");
    }

    private class HeartbeatMessageHandler implements Consumer<byte[]> {
        @Override
        public void accept(byte[] message) {
            HeartbeatMessage hb = SERIALIZER.decode(message);
            failureDetector.report(hb.source().id());
            hb.knownPeers().forEach(node -> {
                allNodes.put(node.id(), node);
            });
        }
    }

    private static class HeartbeatMessage {
        private ControllerNode source;
        private Set<ControllerNode> knownPeers;

        public HeartbeatMessage(ControllerNode source, Set<ControllerNode> members) {
            this.source = source;
            this.knownPeers = ImmutableSet.copyOf(members);
        }

        public ControllerNode source() {
            return source;
        }

        public Set<ControllerNode> knownPeers() {
            return knownPeers;
        }
    }

    @Override
    public DateTime getLastUpdated(NodeId nodeId) {
        return nodeStateLastUpdatedTimes.get(nodeId);
    }

}
