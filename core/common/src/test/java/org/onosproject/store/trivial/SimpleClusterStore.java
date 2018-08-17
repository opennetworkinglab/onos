/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.store.trivial;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterStore;
import org.onosproject.cluster.ClusterStoreDelegate;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.Node;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.Version;
import org.onosproject.core.VersionService;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.intent.WorkPartitionEvent;
import org.onosproject.net.intent.WorkPartitionEventListener;
import org.onosproject.net.intent.WorkPartitionService;
import org.onosproject.store.AbstractStore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.Set;
import java.util.function.Function;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.INTENT_EVENT;
import static org.onosproject.security.AppPermission.Type.INTENT_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of infrastructure devices using trivial in-memory
 * structures implementation.
 */
@Component(immediate = true, service = { ClusterStore.class, WorkPartitionService.class })
public class SimpleClusterStore
        extends AbstractStore<ClusterEvent, ClusterStoreDelegate>
        implements ClusterStore, WorkPartitionService {

    public static final IpAddress LOCALHOST = IpAddress.valueOf("127.0.0.1");

    private final Logger log = getLogger(getClass());

    private ControllerNode instance;

    private final Instant creationTime = Instant.now();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VersionService versionService;

    private ListenerRegistry<WorkPartitionEvent, WorkPartitionEventListener> listenerRegistry;
    private boolean started = false;

    @Activate
    public void activate() {
        instance = new DefaultControllerNode(new NodeId("local"), LOCALHOST);

        listenerRegistry = new ListenerRegistry<>();
        eventDispatcher.addSink(WorkPartitionEvent.class, listenerRegistry);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(WorkPartitionEvent.class);
        log.info("Stopped");
    }


    @Override
    public ControllerNode getLocalNode() {
        return instance;
    }

    @Override
    public Set<ControllerNode> getNodes() {
        return ImmutableSet.of(instance);
    }

    @Override
    public Set<Node> getStorageNodes() {
        return ImmutableSet.of();
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        return instance.id().equals(nodeId) ? instance : null;
    }

    @Override
    public ControllerNode.State getState(NodeId nodeId) {
        return ControllerNode.State.ACTIVE;
    }

    @Override
    public Version getVersion(NodeId nodeId) {
        return instance.id().equals(nodeId) ? versionService.version() : null;
    }

    @Override
    public void markFullyStarted(boolean started) {
        this.started = started;
    }

    @Override
    public Instant getLastUpdatedInstant(NodeId nodeId) {
        return creationTime;
    }

    @Override
    public ControllerNode addNode(NodeId nodeId, IpAddress ip, int tcpPort) {
        return null;
    }

    @Override
    public void removeNode(NodeId nodeId) {
    }

    @Override
    public <K> boolean isMine(K key, Function<K, Long> hasher) {
        checkPermission(INTENT_READ);
        return true;
    }

    @Override
    public <K> NodeId getLeader(K key, Function<K, Long> hasher) {
        checkPermission(INTENT_READ);
        return instance.id();
    }

    @Override
    public void addListener(WorkPartitionEventListener listener) {
        checkPermission(INTENT_EVENT);
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(WorkPartitionEventListener listener) {
        checkPermission(INTENT_EVENT);
        listenerRegistry.removeListener(listener);
    }
}
