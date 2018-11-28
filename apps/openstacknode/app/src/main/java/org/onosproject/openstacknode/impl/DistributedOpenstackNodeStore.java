/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknode.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.openstacknode.api.DefaultDpdkConfig;
import org.onosproject.openstacknode.api.DefaultDpdkInterface;
import org.onosproject.openstacknode.api.DefaultKeystoneConfig;
import org.onosproject.openstacknode.api.DefaultNeutronConfig;
import org.onosproject.openstacknode.api.DefaultOpenstackAuth;
import org.onosproject.openstacknode.api.DefaultOpenstackNode;
import org.onosproject.openstacknode.api.DefaultOpenstackPhyInterface;
import org.onosproject.openstacknode.api.DefaultOpenstackSshAuth;
import org.onosproject.openstacknode.api.DpdkConfig;
import org.onosproject.openstacknode.api.DpdkInterface;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeStore;
import org.onosproject.openstacknode.api.OpenstackNodeStoreDelegate;
import org.onosproject.openstacknode.api.OpenstackPhyInterface;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknode.api.NodeState.COMPLETE;
import static org.onosproject.openstacknode.api.NodeState.INCOMPLETE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.CONTROLLER;
import static org.onosproject.openstacknode.api.OpenstackNodeEvent.Type.OPENSTACK_NODE_COMPLETE;
import static org.onosproject.openstacknode.api.OpenstackNodeEvent.Type.OPENSTACK_NODE_CREATED;
import static org.onosproject.openstacknode.api.OpenstackNodeEvent.Type.OPENSTACK_NODE_INCOMPLETE;
import static org.onosproject.openstacknode.api.OpenstackNodeEvent.Type.OPENSTACK_NODE_REMOVED;
import static org.onosproject.openstacknode.api.OpenstackNodeEvent.Type.OPENSTACK_NODE_UPDATED;
import static org.onosproject.store.service.MapEvent.Type.INSERT;
import static org.onosproject.store.service.MapEvent.Type.UPDATE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of openstack node store using consistent map.
 */
@Component(immediate = true, service = OpenstackNodeStore.class)
public class DistributedOpenstackNodeStore
        extends AbstractStore<OpenstackNodeEvent, OpenstackNodeStoreDelegate>
        implements OpenstackNodeStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.openstacknode";

    private static final KryoNamespace
            SERIALIZER_OPENSTACK_NODE = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(OpenstackNode.class)
            .register(DefaultOpenstackNode.class)
            .register(OpenstackNode.NodeType.class)
            .register(NodeState.class)
            .register(DpdkConfig.class)
            .register(DefaultDpdkConfig.class)
            .register(DpdkConfig.DatapathType.class)
            .register(OpenstackPhyInterface.class)
            .register(DefaultOpenstackPhyInterface.class)
            .register(DpdkInterface.class)
            .register(DefaultDpdkInterface.class)
            .register(DpdkInterface.Type.class)
            .register(ControllerInfo.class)
            .register(DefaultOpenstackAuth.class)
            .register(DefaultOpenstackAuth.Perspective.class)
            .register(DefaultOpenstackAuth.Protocol.class)
            .register(DefaultOpenstackSshAuth.class)
            .register(DefaultKeystoneConfig.class)
            .register(DefaultNeutronConfig.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, OpenstackNode> osNodeMapListener =
            new OpenstackNodeMapListener();
    private ConsistentMap<String, OpenstackNode> osNodeStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        osNodeStore = storageService.<String, OpenstackNode>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_OPENSTACK_NODE))
                .withName("openstack-nodestore")
                .withApplicationId(appId)
                .build();
        osNodeStore.addListener(osNodeMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osNodeStore.removeListener(osNodeMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createNode(OpenstackNode osNode) {
        osNodeStore.compute(osNode.hostname(), (hostname, existing) -> {
            final String error = osNode.hostname() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return osNode;
        });
    }

    @Override
    public void updateNode(OpenstackNode osNode) {
        osNodeStore.compute(osNode.hostname(), (hostname, existing) -> {
            final String error = osNode.hostname() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return osNode;
        });
    }

    @Override
    public OpenstackNode removeNode(String hostname) {
        Versioned<OpenstackNode> osNode = osNodeStore.remove(hostname);
        if (osNode == null) {
            final String error = hostname + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return osNode.value();
    }

    @Override
    public Set<OpenstackNode> nodes() {
        return ImmutableSet.copyOf(osNodeStore.asJavaMap().values());
    }

    @Override
    public OpenstackNode node(String hostname) {
        return osNodeStore.asJavaMap().get(hostname);
    }

    /**
     * An internal openstack node map listener.
     */
    private class OpenstackNodeMapListener
                            implements MapEventListener<String, OpenstackNode> {
        @Override
        public void event(MapEvent<String, OpenstackNode> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("OpenStack node created {}", event.newValue());
                    eventExecutor.execute(() -> processNodeCreation(event));
                    break;
                case UPDATE:
                    log.debug("OpenStack node updated {}", event.newValue());
                    eventExecutor.execute(() -> processNodeUpdate(event));
                    break;
                case REMOVE:
                    log.debug("OpenStack node removed {}", event.oldValue());
                    eventExecutor.execute(() -> processNodeRemoval(event));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processNodeCreation(MapEvent<String, OpenstackNode> event) {
            notifyDelegate(new OpenstackNodeEvent(
                    OPENSTACK_NODE_CREATED, event.newValue().value()));
        }

        private void processNodeUpdate(MapEvent<String, OpenstackNode> event) {
            notifyDelegate(new OpenstackNodeEvent(
                    OPENSTACK_NODE_UPDATED,
                    event.newValue().value()
            ));

            // if the event is about controller node, we will not
            // process COMPLETE and INCOMPLETE state
            if (isControllerNode(event)) {
                return;
            }

            if (event.newValue().value().state() == COMPLETE) {
                notifyDelegate(new OpenstackNodeEvent(
                        OPENSTACK_NODE_COMPLETE,
                        event.newValue().value()
                ));
            } else if (event.newValue().value().state() == INCOMPLETE) {
                notifyDelegate(new OpenstackNodeEvent(
                        OPENSTACK_NODE_INCOMPLETE,
                        event.newValue().value()
                ));
            }
        }

        private void processNodeRemoval(MapEvent<String, OpenstackNode> event) {
            notifyDelegate(new OpenstackNodeEvent(
                    OPENSTACK_NODE_REMOVED, event.oldValue().value()));
        }

        /**
         * Checks the openstack node whether a controller node or not with
         * the given MapEvent.
         *
         * @param event map event
         * @return controller node indicator flag
         */
        private boolean isControllerNode(MapEvent<String, OpenstackNode> event) {

            OpenstackNode node;

            if (event.type() == INSERT || event.type() == UPDATE) {
                node = event.newValue().value();
            } else {
                node = event.oldValue().value();
            }

            return node.type() == CONTROLLER;
        }
    }
}
