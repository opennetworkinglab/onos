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
package org.onosproject.k8snode.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snode.api.DefaultK8sHost;
import org.onosproject.k8snode.api.K8sBridge;
import org.onosproject.k8snode.api.K8sHost;
import org.onosproject.k8snode.api.K8sHostEvent;
import org.onosproject.k8snode.api.K8sHostState;
import org.onosproject.k8snode.api.K8sHostStore;
import org.onosproject.k8snode.api.K8sHostStoreDelegate;
import org.onosproject.k8snode.api.K8sRouterBridge;
import org.onosproject.k8snode.api.K8sTunnelBridge;
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snode.api.K8sHostEvent.Type.K8S_HOST_COMPLETE;
import static org.onosproject.k8snode.api.K8sHostEvent.Type.K8S_HOST_CREATED;
import static org.onosproject.k8snode.api.K8sHostEvent.Type.K8S_HOST_INCOMPLETE;
import static org.onosproject.k8snode.api.K8sHostEvent.Type.K8S_HOST_REMOVED;
import static org.onosproject.k8snode.api.K8sHostEvent.Type.K8S_HOST_UPDATED;
import static org.onosproject.k8snode.api.K8sHostEvent.Type.K8S_NODES_ADDED;
import static org.onosproject.k8snode.api.K8sHostEvent.Type.K8S_NODES_REMOVED;
import static org.onosproject.k8snode.api.K8sHostState.COMPLETE;
import static org.onosproject.k8snode.api.K8sHostState.INCOMPLETE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubernetes host store using consistent map.
 */
@Component(immediate = true, service = K8sHostStore.class)
public class DistributedK8sHostStore
        extends AbstractStore<K8sHostEvent, K8sHostStoreDelegate>
        implements K8sHostStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.k8snode";

    private static final KryoNamespace
            SERIALIZER_K8S_HOST = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(K8sHost.class)
            .register(DefaultK8sHost.class)
            .register(K8sHostState.class)
            .register(K8sBridge.class)
            .register(K8sTunnelBridge.class)
            .register(K8sRouterBridge.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, K8sHost> hostMapListener =
            new K8sHostMapListener();
    private ConsistentMap<String, K8sHost> hostStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        hostStore = storageService.<String, K8sHost>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_HOST))
                .withName("k8s-hoststore")
                .withApplicationId(appId)
                .build();
        hostStore.addListener(hostMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostStore.removeListener(hostMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createHost(K8sHost host) {
        hostStore.compute(host.hostIp().toString(), (hostIp, existing) -> {
            final String error = host.hostIp().toString() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return host;
        });
    }

    @Override
    public void updateHost(K8sHost host) {
        hostStore.compute(host.hostIp().toString(), (hostIp, existing) -> {
            final String error = host.hostIp().toString() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return host;
        });
    }

    @Override
    public K8sHost removeHost(IpAddress hostIp) {
        Versioned<K8sHost> host = hostStore.remove(hostIp.toString());
        if (host == null) {
            final String error = hostIp.toString() + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return host.value();
    }

    @Override
    public Set<K8sHost> hosts() {
        return ImmutableSet.copyOf(hostStore.asJavaMap().values());
    }

    @Override
    public K8sHost host(IpAddress hostIp) {
        return hostStore.asJavaMap().get(hostIp.toString());
    }

    private class K8sHostMapListener
            implements MapEventListener<String, K8sHost> {

        @Override
        public void event(MapEvent<String, K8sHost> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("Kubernetes host created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sHostEvent(
                                    K8S_HOST_CREATED, event.newValue().value()
                            )));
                    break;
                case UPDATE:
                    log.debug("Kubernetes host updated {}", event.newValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new K8sHostEvent(
                                K8S_HOST_UPDATED, event.newValue().value()
                        ));

                        if (event.newValue().value().state() == COMPLETE) {
                            notifyDelegate(new K8sHostEvent(
                                    K8S_HOST_COMPLETE,
                                    event.newValue().value()
                            ));
                        } else if (event.newValue().value().state() == INCOMPLETE) {
                            notifyDelegate(new K8sHostEvent(
                                    K8S_HOST_INCOMPLETE,
                                    event.newValue().value()
                            ));
                        }

                        K8sHost origHost = event.newValue().value();
                        Set<String> oldNodes = event.oldValue().value().nodeNames();
                        Set<String> newNodes = event.newValue().value().nodeNames();

                        Set<String> addedNodes = new HashSet<>(newNodes);
                        Set<String> removedNodes = new HashSet<>(oldNodes);

                        addedNodes.removeAll(oldNodes);
                        removedNodes.removeAll(newNodes);

                        if (addedNodes.size() > 0) {
                            K8sHost addedHost = DefaultK8sHost.builder()
                                    .hostIp(origHost.hostIp())
                                    .state(origHost.state())
                                    .nodeNames(addedNodes)
                                    .build();
                            notifyDelegate(new K8sHostEvent(K8S_NODES_ADDED, addedHost));
                        }

                        if (removedNodes.size() > 0) {
                            K8sHost removedHost = DefaultK8sHost.builder()
                                    .hostIp(origHost.hostIp())
                                    .state(origHost.state())
                                    .nodeNames(removedNodes)
                                    .build();
                            notifyDelegate(new K8sHostEvent(K8S_NODES_REMOVED, removedHost));
                        }
                    });
                    break;
                case REMOVE:
                    log.debug("Kubernetes host removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sHostEvent(
                                    K8S_HOST_REMOVED, event.oldValue().value()
                            )));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }
}
