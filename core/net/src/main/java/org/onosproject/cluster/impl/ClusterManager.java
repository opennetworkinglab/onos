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

import org.apache.karaf.system.SystemService;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterMetadata;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterStore;
import org.onosproject.cluster.ClusterStoreDelegate;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.Node;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.Version;
import org.onosproject.event.AbstractListenerManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.CLUSTER_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the cluster service.
 */
@Component(immediate = true, service = {ClusterService.class, ClusterAdminService.class})
public class ClusterManager
        extends AbstractListenerManager<ClusterEvent, ClusterEventListener>
        implements ClusterService, ClusterAdminService {

    public static final String INSTANCE_ID_NULL = "Instance ID cannot be null";
    private static final int DEFAULT_PARTITION_SIZE = 3;
    private final Logger log = getLogger(getClass());

    private ClusterStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected SystemService systemService;

    private final AtomicReference<ClusterMetadata> currentMetadata = new AtomicReference<>();

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

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements ClusterStoreDelegate {
        @Override
        public void notify(ClusterEvent event) {
            post(event);
        }
    }
}
