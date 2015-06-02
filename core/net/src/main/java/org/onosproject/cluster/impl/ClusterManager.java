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
package org.onosproject.cluster.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.system.SystemService;
import org.joda.time.DateTime;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.ClusterDefinitionService;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterStore;
import org.onosproject.cluster.ClusterStoreDelegate;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.Permission;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppGuard.checkPermission;


/**
 * Implementation of the cluster service.
 */
@Component(immediate = true)
@Service
public class ClusterManager implements ClusterService, ClusterAdminService {

    public static final String INSTANCE_ID_NULL = "Instance ID cannot be null";
    private final Logger log = getLogger(getClass());

    private ClusterStoreDelegate delegate = new InternalStoreDelegate();

    protected final ListenerRegistry<ClusterEvent, ClusterEventListener>
            listenerRegistry = new ListenerRegistry<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterDefinitionService clusterDefinitionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SystemService systemService;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(ClusterEvent.class, listenerRegistry);
        clusterDefinitionService.seedNodes()
                                .forEach(node -> store.addNode(node.id(), node.ip(), node.tcpPort()));
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
        checkPermission(Permission.CLUSTER_READ);
        return store.getLocalNode();
    }

    @Override
    public Set<ControllerNode> getNodes() {
        checkPermission(Permission.CLUSTER_READ);
        return store.getNodes();
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        checkPermission(Permission.CLUSTER_READ);
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return store.getNode(nodeId);
    }

    @Override
    public ControllerNode.State getState(NodeId nodeId) {
        checkPermission(Permission.CLUSTER_READ);
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return store.getState(nodeId);
    }


    @Override
    public DateTime getLastUpdated(NodeId nodeId) {
        checkPermission(Permission.CLUSTER_READ);
        return store.getLastUpdated(nodeId);
    }

    @Override
    public void formCluster(Set<ControllerNode> nodes, String ipPrefix) {
        checkNotNull(nodes, "Nodes cannot be null");
        checkArgument(!nodes.isEmpty(), "Nodes cannot be empty");
        checkNotNull(ipPrefix, "IP prefix cannot be null");
        clusterDefinitionService.formCluster(nodes, ipPrefix);
        try {
            log.warn("Shutting down container for cluster reconfiguration!");
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

    @Override
    public void addListener(ClusterEventListener listener) {
        checkPermission(Permission.CLUSTER_EVENT);
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(ClusterEventListener listener) {
        checkPermission(Permission.CLUSTER_EVENT);
        listenerRegistry.removeListener(listener);
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements ClusterStoreDelegate {
        @Override
        public void notify(ClusterEvent event) {
            checkNotNull(event, "Event cannot be null");
            eventDispatcher.post(event);
        }
    }
}
