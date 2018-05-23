/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.cluster.impl;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.system.SystemService;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterMetadata;
import org.onosproject.cluster.ClusterMetadataDiff;
import org.onosproject.cluster.ClusterMetadataEvent;
import org.onosproject.cluster.ClusterMetadataEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterStore;
import org.onosproject.cluster.ClusterStoreDelegate;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.Node;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.Version;
import org.onosproject.event.AbstractListenerManager;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.CLUSTER_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the cluster service.
 */
@Component(immediate = true)
@Service
public class ClusterManager
        extends AbstractListenerManager<ClusterEvent, ClusterEventListener>
        implements ClusterService, ClusterAdminService {

    public static final String INSTANCE_ID_NULL = "Instance ID cannot be null";
    private static final int DEFAULT_PARTITION_SIZE = 3;
    private final Logger log = getLogger(getClass());

    private ClusterStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SystemService systemService;

    private final AtomicReference<ClusterMetadata> currentMetadata = new AtomicReference<>();
    private final InternalClusterMetadataListener metadataListener = new InternalClusterMetadataListener();

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(ClusterEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(ClusterEvent.class);
        log.info("Stopped");
    }

    @Override
    public ControllerNode getLocalNode() {
        checkPermission(CLUSTER_READ);
        return store.getLocalNode();
    }

    @Override
    public Set<Node> getConsensusNodes() {
        checkPermission(CLUSTER_READ);
        return store.getStorageNodes();
    }

    @Override
    public Set<ControllerNode> getNodes() {
        checkPermission(CLUSTER_READ);
        return store.getNodes();
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        checkPermission(CLUSTER_READ);
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return store.getNode(nodeId);
    }

    @Override
    public ControllerNode.State getState(NodeId nodeId) {
        checkPermission(CLUSTER_READ);
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return store.getState(nodeId);
    }

    @Override
    public Version getVersion(NodeId nodeId) {
        checkPermission(CLUSTER_READ);
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return store.getVersion(nodeId);
    }

    @Override
    public void markFullyStarted(boolean started) {
        store.markFullyStarted(started);
    }

    @Override
    public Instant getLastUpdatedInstant(NodeId nodeId) {
        checkPermission(CLUSTER_READ);
        return store.getLastUpdatedInstant(nodeId);
    }

    @Override
    public void formCluster(Set<ControllerNode> nodes) {
        formCluster(nodes, DEFAULT_PARTITION_SIZE);
    }

    @Override
    public void formCluster(Set<ControllerNode> nodes, int partitionSize) {
        log.warn("formCluster is deprecated");
    }

    @Override
    public ControllerNode addNode(NodeId nodeId, IpAddress ip, int tcpPort) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        checkNotNull(ip, "IP address cannot be null");
        checkArgument(tcpPort > 5000, "TCP port must be > 5000");
        return store.addNode(nodeId, ip, tcpPort);
    }

    @Override
    public void removeNode(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        store.removeNode(nodeId);
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements ClusterStoreDelegate {
        @Override
        public void notify(ClusterEvent event) {
            post(event);
        }
    }

    /**
     * Processes metadata by adding and removing nodes from the cluster.
     */
    private synchronized void processMetadata(ClusterMetadata metadata) {
        try {
            ClusterMetadataDiff examiner =
                    new ClusterMetadataDiff(currentMetadata.get(), metadata);
            examiner.nodesAdded().forEach(node -> addNode(node.id(), node.ip(), node.tcpPort()));
            examiner.nodesRemoved().forEach(this::removeNode);
        } finally {
            currentMetadata.set(metadata);
        }
    }

    private class InternalClusterMetadataListener implements ClusterMetadataEventListener {
        @Override
        public void event(ClusterMetadataEvent event) {
            processMetadata(event.subject());
        }
    }
}
