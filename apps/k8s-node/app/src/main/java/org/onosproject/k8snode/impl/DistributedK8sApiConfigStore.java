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
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snode.api.DefaultHostNodesInfo;
import org.onosproject.k8snode.api.DefaultK8sApiConfig;
import org.onosproject.k8snode.api.HostNodesInfo;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfigEvent;
import org.onosproject.k8snode.api.K8sApiConfigStore;
import org.onosproject.k8snode.api.K8sApiConfigStoreDelegate;
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
import static org.onosproject.k8snode.api.K8sApiConfigEvent.Type.K8S_API_CONFIG_CREATED;
import static org.onosproject.k8snode.api.K8sApiConfigEvent.Type.K8S_API_CONFIG_REMOVED;
import static org.onosproject.k8snode.api.K8sApiConfigEvent.Type.K8S_API_CONFIG_UPDATED;
import static org.onosproject.k8snode.util.K8sNodeUtil.endpoint;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubernetes API config store using consistent map.
 */
@Component(immediate = true, service = K8sApiConfigStore.class)
public class DistributedK8sApiConfigStore
        extends AbstractStore<K8sApiConfigEvent, K8sApiConfigStoreDelegate>
        implements K8sApiConfigStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.k8snode";

    private static final KryoNamespace
            SERIALIZER_K8S_API_CONFIG = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(K8sApiConfig.class)
            .register(DefaultK8sApiConfig.class)
            .register(K8sApiConfig.Mode.class)
            .register(K8sApiConfig.Scheme.class)
            .register(K8sApiConfig.State.class)
            .register(HostNodesInfo.class)
            .register(DefaultHostNodesInfo.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, K8sApiConfig> apiConfigMapListener =
            new K8sApiConfigMapListener();
    private ConsistentMap<String, K8sApiConfig> apiConfigStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        apiConfigStore = storageService.<String, K8sApiConfig>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_API_CONFIG))
                .withName("k8s-apiconfig-store")
                .withApplicationId(appId)
                .build();
        apiConfigStore.addListener(apiConfigMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        apiConfigStore.removeListener(apiConfigMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createApiConfig(K8sApiConfig config) {
        String key = endpoint(config);
        apiConfigStore.compute(key, (endpoint, existing) -> {
            final String error = key + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return config;
        });
    }

    @Override
    public void updateApiConfig(K8sApiConfig config) {
        String key = endpoint(config);
        apiConfigStore.compute(key, (endpoint, existing) -> {
            final String error = key + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return config;
        });
    }

    @Override
    public K8sApiConfig removeApiConfig(String endpoint) {
        Versioned<K8sApiConfig> apiConfig = apiConfigStore.remove(endpoint);
        if (apiConfig == null) {
            final String error = endpoint + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return apiConfig.value();
    }

    @Override
    public K8sApiConfig removeApiConfig(K8sApiConfig.Scheme scheme,
                                        IpAddress ipAddress, int port) {
        String key = endpoint(scheme, ipAddress, port);
        return removeApiConfig(key);
    }

    @Override
    public Set<K8sApiConfig> apiConfigs() {
        return ImmutableSet.copyOf(apiConfigStore.asJavaMap().values());
    }

    @Override
    public K8sApiConfig apiConfig(String endpoint) {
        return apiConfigStore.asJavaMap().get(endpoint);
    }

    @Override
    public K8sApiConfig apiConfig(K8sApiConfig.Scheme scheme,
                                  IpAddress ipAddress, int port) {
        String key = endpoint(scheme, ipAddress, port);
        return apiConfig(key);
    }

    private class K8sApiConfigMapListener
            implements MapEventListener<String, K8sApiConfig> {

        @Override
        public void event(MapEvent<String, K8sApiConfig> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("Kubernetes API config created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sApiConfigEvent(
                                    K8S_API_CONFIG_CREATED, event.newValue().value()
                    )));
                    break;
                case UPDATE:
                    log.debug("Kubernetes API config updated {}", event.newValue());
                    eventExecutor.execute(() ->
                        notifyDelegate(new K8sApiConfigEvent(
                                K8S_API_CONFIG_UPDATED, event.newValue().value()
                    )));
                    break;
                case REMOVE:
                    log.debug("Kubernetes API config removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sApiConfigEvent(
                                    K8S_API_CONFIG_REMOVED, event.oldValue().value()
                    )));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }
}
