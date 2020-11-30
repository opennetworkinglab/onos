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

import com.google.common.collect.ImmutableSet;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snode.api.DefaultK8sExternalNetwork;
import org.onosproject.k8snode.api.DefaultK8sNode;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sExternalNetwork;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeEvent;
import org.onosproject.k8snode.api.K8sNodeInfo;
import org.onosproject.k8snode.api.K8sNodeState;
import org.onosproject.k8snode.api.K8sNodeStore;
import org.onosproject.k8snode.api.K8sNodeStoreDelegate;
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
import static org.onosproject.k8snode.api.K8sNodeEvent.Type.*;
import static org.onosproject.k8snode.api.K8sNodeState.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubernetes node store using consistent map.
 */
@Component(immediate = true, service = K8sNodeStore.class)
public class DistributedK8sNodeStore
        extends AbstractStore<K8sNodeEvent, K8sNodeStoreDelegate>
        implements K8sNodeStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.k8snode";

    private static final KryoNamespace
            SERIALIZER_K8S_NODE = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(K8sNode.class)
            .register(DefaultK8sNode.class)
            .register(K8sNode.Type.class)
            .register(K8sNodeInfo.class)
            .register(K8sNodeState.class)
            .register(K8sApiConfig.Mode.class)
            .register(K8sExternalNetwork.class)
            .register(DefaultK8sExternalNetwork.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, K8sNode> nodeMapListener =
            new K8sNodeMapListener();
    private ConsistentMap<String, K8sNode> nodeStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        nodeStore = storageService.<String, K8sNode>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_NODE))
                .withName("k8s-nodestore")
                .withApplicationId(appId)
                .build();
        nodeStore.addListener(nodeMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        nodeStore.removeListener(nodeMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createNode(K8sNode node) {
        nodeStore.compute(node.hostname(), (hostname, existing) -> {
            final String error = node.hostname() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return node;
        });
    }

    @Override
    public void updateNode(K8sNode node) {
        nodeStore.compute(node.hostname(), (hostname, existing) -> {
            final String error = node.hostname() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return node;
        });
    }

    @Override
    public K8sNode removeNode(String hostname) {
        Versioned<K8sNode> node = nodeStore.remove(hostname);
        if (node == null) {
            final String error = hostname + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return node.value();
    }

    @Override
    public Set<K8sNode> nodes() {
        return ImmutableSet.copyOf(nodeStore.asJavaMap().values());
    }

    @Override
    public K8sNode node(String hostname) {
        return nodeStore.asJavaMap().get(hostname);
    }

    private class K8sNodeMapListener
            implements MapEventListener<String, K8sNode> {

        @Override
        public void event(MapEvent<String, K8sNode> event) {

            switch (event.type()) {
                case INSERT:
                    log.debug("Kubernetes node created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sNodeEvent(
                                    K8S_NODE_CREATED, event.newValue().value()
                    )));
                    break;
                case UPDATE:
                    log.debug("Kubernetes node updated {}", event.newValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new K8sNodeEvent(
                                K8S_NODE_UPDATED,
                                event.newValue().value()
                        ));

                        if (event.newValue().value().state() == COMPLETE) {
                            notifyDelegate(new K8sNodeEvent(
                                    K8S_NODE_COMPLETE,
                                    event.newValue().value()
                            ));
                        } else if (event.newValue().value().state() == INCOMPLETE) {
                            notifyDelegate(new K8sNodeEvent(
                                    K8S_NODE_INCOMPLETE,
                                    event.newValue().value()
                            ));
                        }
                    });
                    break;
                case REMOVE:
                    log.debug("Kubernetes node removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sNodeEvent(
                                    K8S_NODE_REMOVED, event.oldValue().value()
                            )));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }
}
