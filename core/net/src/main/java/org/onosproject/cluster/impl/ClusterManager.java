/*
 * Copyright 2014-present Open Networking Laboratory
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

import com.google.common.collect.Sets;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.system.SystemService;
import org.joda.time.DateTime;
import org.onlab.packet.IpAddress;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterMetadata;
import org.onosproject.cluster.ClusterMetadataAdminService;
import org.onosproject.cluster.ClusterMetadataDiff;
import org.onosproject.cluster.ClusterMetadataEvent;
import org.onosproject.cluster.ClusterMetadataEventListener;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterStore;
import org.onosproject.cluster.ClusterStoreDelegate;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultPartition;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.Partition;
import org.onosproject.cluster.PartitionId;
import org.onosproject.event.AbstractListenerManager;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

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
    protected ClusterMetadataService clusterMetadataService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataAdminService clusterMetadataAdminService;

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
        clusterMetadataService.addListener(metadataListener);
        processMetadata(clusterMetadataService.getClusterMetadata());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        clusterMetadataService.removeListener(metadataListener);
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
    public void markFullyStarted(boolean started) {
        store.markFullyStarted(started);
    }

    @Override
    public DateTime getLastUpdated(NodeId nodeId) {
        checkPermission(CLUSTER_READ);
        return store.getLastUpdated(nodeId);
    }

    @Override
    public void formCluster(Set<ControllerNode> nodes) {
        formCluster(nodes, DEFAULT_PARTITION_SIZE);
    }

    @Override
    public void formCluster(Set<ControllerNode> nodes, int partitionSize) {
        checkNotNull(nodes, "Nodes cannot be null");
        checkArgument(!nodes.isEmpty(), "Nodes cannot be empty");

        ClusterMetadata metadata = new ClusterMetadata("default", nodes, buildDefaultPartitions(nodes, partitionSize));
        clusterMetadataAdminService.setClusterMetadata(metadata);
        try {
            log.warn("Shutting down container for cluster reconfiguration!");
            // Clean up persistent state associated with previous cluster configuration.
            Tools.removeDirectory(System.getProperty("karaf.data") + "/partitions");
            systemService.reboot("now", SystemService.Swipe.NONE);
        } catch (Exception e) {
            log.error("Unable to reboot container", e);
        }
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

    private static Set<Partition> buildDefaultPartitions(Collection<ControllerNode> nodes, int partitionSize) {
        List<ControllerNode> sorted = new ArrayList<>(nodes);
        Collections.sort(sorted, (o1, o2) -> o1.id().toString().compareTo(o2.id().toString()));
        Set<Partition> partitions = Sets.newHashSet();
        // add partitions
        int length = nodes.size();
        int count = Math.min(partitionSize, length);
        for (int i = 0; i < length; i++) {
            int index = i;
            Set<NodeId> set = new HashSet<>(count);
            for (int j = 0; j < count; j++) {
                set.add(sorted.get((i + j) % length).id());
            }
            partitions.add(new DefaultPartition(PartitionId.from((index + 1)), set));
        }
        return partitions;
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
