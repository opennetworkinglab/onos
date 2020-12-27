/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnode.api.DefaultKubevirtNode;
import org.onosproject.kubevirtnode.api.DefaultKubevirtPhyInterface;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeEvent;
import org.onosproject.kubevirtnode.api.KubevirtNodeState;
import org.onosproject.kubevirtnode.api.KubevirtNodeStore;
import org.onosproject.kubevirtnode.api.KubevirtNodeStoreDelegate;
import org.onosproject.kubevirtnode.api.KubevirtPhyInterface;
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
import static org.onosproject.kubevirtnode.api.KubevirtNodeEvent.Type.KUBEVIRT_NODE_COMPLETE;
import static org.onosproject.kubevirtnode.api.KubevirtNodeEvent.Type.KUBEVIRT_NODE_CREATED;
import static org.onosproject.kubevirtnode.api.KubevirtNodeEvent.Type.KUBEVIRT_NODE_INCOMPLETE;
import static org.onosproject.kubevirtnode.api.KubevirtNodeEvent.Type.KUBEVIRT_NODE_REMOVED;
import static org.onosproject.kubevirtnode.api.KubevirtNodeEvent.Type.KUBEVIRT_NODE_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubevirt node store using consistent map.
 */
@Component(immediate = true, service = KubevirtNodeStore.class)
public class DistributedKubevirtNodeStore
        extends AbstractStore<KubevirtNodeEvent, KubevirtNodeStoreDelegate>
        implements KubevirtNodeStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.kubevirtnode";

    private static final KryoNamespace
            SERIALIZER_KUBEVIRT_NODE = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(KubevirtNode.class)
            .register(DefaultKubevirtNode.class)
            .register(KubevirtPhyInterface.class)
            .register(DefaultKubevirtPhyInterface.class)
            .register(KubevirtNode.Type.class)
            .register(KubevirtNodeState.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, KubevirtNode> nodeMapEventListener =
            new KubevirtNodeMapListener();

    private ConsistentMap<String, KubevirtNode> nodeStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        nodeStore = storageService.<String, KubevirtNode>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_KUBEVIRT_NODE))
                .withName("kubevirt-nodestore")
                .withApplicationId(appId)
                .build();
        nodeStore.addListener(nodeMapEventListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        nodeStore.removeListener(nodeMapEventListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createNode(KubevirtNode node) {
        nodeStore.compute(node.hostname(), (hostname, existing) -> {
            final String error = node.hostname() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return node;
        });
    }

    @Override
    public void updateNode(KubevirtNode node) {
        nodeStore.compute(node.hostname(), (hostname, existing) -> {
            final String error = node.hostname() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return node;
        });
    }

    @Override
    public KubevirtNode removeNode(String hostname) {
        Versioned<KubevirtNode> node = nodeStore.remove(hostname);
        if (node == null) {
            final String error = hostname + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return node.value();
    }

    @Override
    public Set<KubevirtNode> nodes() {
        return ImmutableSet.copyOf(nodeStore.asJavaMap().values());
    }

    @Override
    public KubevirtNode node(String hostname) {
        return nodeStore.asJavaMap().get(hostname);
    }

    private class KubevirtNodeMapListener
            implements MapEventListener<String, KubevirtNode> {

        @Override
        public void event(MapEvent<String, KubevirtNode> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("Kubevirt node created {}", event.newValue());
                    eventExecutor.execute(() -> processNodeCreation(event));
                    break;
                case UPDATE:
                    log.debug("Kubevirt node updated {}", event.newValue());
                    eventExecutor.execute(() -> processNodeUpdate(event));
                    break;
                case REMOVE:
                    log.debug("Kubevirt node removed {}", event.oldValue());
                    eventExecutor.execute(() -> processNodeRemoval(event));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processNodeCreation(MapEvent<String, KubevirtNode> event) {
            notifyDelegate(new KubevirtNodeEvent(
                    KUBEVIRT_NODE_CREATED, event.newValue().value()));
        }

        private void processNodeUpdate(MapEvent<String, KubevirtNode> event) {
            notifyDelegate(new KubevirtNodeEvent(
                    KUBEVIRT_NODE_UPDATED, event.newValue().value()));

            if (event.newValue().value().state() == KubevirtNodeState.COMPLETE) {
                notifyDelegate(new KubevirtNodeEvent(
                        KUBEVIRT_NODE_COMPLETE, event.newValue().value()));
            } else if (event.newValue().value().state() == KubevirtNodeState.INCOMPLETE) {
                notifyDelegate(new KubevirtNodeEvent(
                        KUBEVIRT_NODE_INCOMPLETE, event.newValue().value()));
            }
        }

        private void processNodeRemoval(MapEvent<String, KubevirtNode> event) {
            notifyDelegate(new KubevirtNodeEvent(
                    KUBEVIRT_NODE_REMOVED, event.oldValue().value()));
        }
    }
}
