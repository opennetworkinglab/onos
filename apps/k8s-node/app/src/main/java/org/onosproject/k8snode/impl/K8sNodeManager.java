/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snode.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNode.Type;
import org.onosproject.k8snode.api.K8sNodeAdminService;
import org.onosproject.k8snode.api.K8sNodeEvent;
import org.onosproject.k8snode.api.K8sNodeListener;
import org.onosproject.k8snode.api.K8sNodeService;
import org.onosproject.k8snode.api.K8sNodeStore;
import org.onosproject.k8snode.api.K8sNodeStoreDelegate;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snode.api.K8sNodeState.COMPLETE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service administering the inventory of kubernetes nodes.
 */
@Service
@Component(immediate = true)
public class K8sNodeManager
        extends ListenerRegistry<K8sNodeEvent, K8sNodeListener>
        implements K8sNodeService, K8sNodeAdminService {

    private final Logger log = getLogger(getClass());

    private static final String MSG_NODE = "Kubernetes node %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";
    private static final String OVSDB_PORT = "ovsdbPortNum";
    private static final int DEFAULT_OVSDB_PORT = 6640;

    private static final String ERR_NULL_NODE = "Kubernetes node cannot be null";
    private static final String ERR_NULL_HOSTNAME = "Kubernetes node hostname cannot be null";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected K8sNodeStore nodeStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Property(name = OVSDB_PORT, intValue = DEFAULT_OVSDB_PORT,
            label = "OVSDB server listen port")
    private int ovsdbPort = DEFAULT_OVSDB_PORT;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final K8sNodeStoreDelegate delegate = new K8sNodeManager.InternalNodeStoreDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);
        nodeStore.setDelegate(delegate);

        leadershipService.runForLeadership(appId.name());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        nodeStore.unsetDelegate(delegate);

        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        int updatedOvsdbPort = Tools.getIntegerProperty(properties, OVSDB_PORT);
        if (!Objects.equals(updatedOvsdbPort, ovsdbPort)) {
            ovsdbPort = updatedOvsdbPort;
        }

        log.info("Modified");
    }

    @Override
    public void createNode(K8sNode node) {
        checkNotNull(node, ERR_NULL_NODE);
        nodeStore.createNode(node);
        log.info(String.format(MSG_NODE, node.hostname(), MSG_CREATED));
    }

    @Override
    public void updateNode(K8sNode node) {
        checkNotNull(node, ERR_NULL_NODE);
        nodeStore.updateNode(node);
        log.info(String.format(MSG_NODE, node.hostname(), MSG_UPDATED));
    }

    @Override
    public K8sNode removeNode(String hostname) {
        checkArgument(!Strings.isNullOrEmpty(hostname), ERR_NULL_HOSTNAME);
        K8sNode node = nodeStore.removeNode(hostname);
        log.info(String.format(MSG_NODE, hostname, MSG_REMOVED));
        return node;
    }

    @Override
    public Set<K8sNode> nodes() {
        return nodeStore.nodes();
    }

    @Override
    public Set<K8sNode> nodes(Type type) {
        Set<K8sNode> nodes = nodeStore.nodes().stream()
                .filter(node -> Objects.equals(node.type(), type))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(nodes);
    }

    @Override
    public Set<K8sNode> completeNodes() {
        Set<K8sNode> nodes = nodeStore.nodes().stream()
                .filter(node -> node.state() == COMPLETE)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(nodes);
    }

    @Override
    public Set<K8sNode> completeNodes(Type type) {
        Set<K8sNode> nodes = nodeStore.nodes().stream()
                .filter(node -> node.type() == type &&
                        node.state() == COMPLETE)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(nodes);
    }

    @Override
    public K8sNode node(String hostname) {
        return nodeStore.node(hostname);
    }

    @Override
    public K8sNode node(DeviceId deviceId) {
        return nodeStore.nodes().stream()
                .filter(node -> Objects.equals(node.intgBridge(), deviceId) ||
                        Objects.equals(node.ovsdb(), deviceId))
                .findFirst().orElse(null);
    }

    private class InternalNodeStoreDelegate implements K8sNodeStoreDelegate {

        @Override
        public void notify(K8sNodeEvent event) {
            if (event != null) {
                log.trace("send kubernetes node event {}", event);
                process(event);
            }
        }
    }
}
