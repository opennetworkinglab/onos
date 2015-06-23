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
package org.onosproject.store.trivial;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.joda.time.DateTime;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterStore;
import org.onosproject.cluster.ClusterStoreDelegate;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PartitionEvent;
import org.onosproject.net.intent.PartitionEventListener;
import org.onosproject.net.intent.PartitionService;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of infrastructure devices using trivial in-memory
 * structures implementation.
 */
@Component(immediate = true)
@Service
public class SimpleClusterStore
        extends AbstractStore<ClusterEvent, ClusterStoreDelegate>
        implements ClusterStore, PartitionService {

    public static final IpAddress LOCALHOST = IpAddress.valueOf("127.0.0.1");

    private final Logger log = getLogger(getClass());

    private ControllerNode instance;

    private final DateTime creationTime = DateTime.now();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    private ListenerRegistry<PartitionEvent, PartitionEventListener> listenerRegistry;

    @Activate
    public void activate() {
        instance = new DefaultControllerNode(new NodeId("local"), LOCALHOST);

        listenerRegistry = new ListenerRegistry<>();
        eventDispatcher.addSink(PartitionEvent.class, listenerRegistry);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(PartitionEvent.class);
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
    public ControllerNode getNode(NodeId nodeId) {
        return instance.id().equals(nodeId) ? instance : null;
    }

    @Override
    public ControllerNode.State getState(NodeId nodeId) {
        return ControllerNode.State.ACTIVE;
    }

    @Override
    public DateTime getLastUpdated(NodeId nodeId) {
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
    public boolean isMine(Key intentKey) {
        return true;
    }

    @Override
    public NodeId getLeader(Key intentKey) {
        return instance.id();
    }

    @Override
    public void addListener(PartitionEventListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(PartitionEventListener listener) {
        listenerRegistry.removeListener(listener);
    }
}
