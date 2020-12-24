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
import org.onosproject.kubevirtnode.api.DefaultKubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigStore;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigStoreDelegate;
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
import static org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent.Type.KUBEVIRT_API_CONFIG_CREATED;
import static org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent.Type.KUBEVIRT_API_CONFIG_REMOVED;
import static org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent.Type.KUBEVIRT_API_CONFIG_UPDATED;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.endpoint;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of KubeVirt API config store using consistent map.
 */
@Component(immediate = true, service = KubevirtApiConfigStore.class)
public class DistributedKubevirtApiConfigStore
        extends AbstractStore<KubevirtApiConfigEvent, KubevirtApiConfigStoreDelegate>
        implements KubevirtApiConfigStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.kubevirtnode";

    private static final KryoNamespace
            SERIALIZER_KUBEVIRT_API_CONFIG = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(KubevirtApiConfig.class)
            .register(DefaultKubevirtApiConfig.class)
            .register(KubevirtApiConfig.Scheme.class)
            .register(KubevirtApiConfig.State.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, KubevirtApiConfig> apiConfigMapListener =
            new KubevirtApiConfigMapListener();
    private ConsistentMap<String, KubevirtApiConfig> apiConfigStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        apiConfigStore = storageService.<String, KubevirtApiConfig>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_KUBEVIRT_API_CONFIG))
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
    public void createApiConfig(KubevirtApiConfig config) {
        String key = endpoint(config);
        apiConfigStore.compute(key, (endpoint, existing) -> {
            final String error = key + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return config;
        });
    }

    @Override
    public void updateApiConfig(KubevirtApiConfig config) {
        String key = endpoint(config);
        apiConfigStore.compute(key, (endpoint, existing) -> {
            final String error = key + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return config;
        });
    }

    @Override
    public KubevirtApiConfig removeApiConfig(String endpoint) {
        Versioned<KubevirtApiConfig> apiConfig = apiConfigStore.remove(endpoint);
        if (apiConfig == null) {
            final String error = endpoint + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return apiConfig.value();
    }

    @Override
    public Set<KubevirtApiConfig> apiConfigs() {
        return ImmutableSet.copyOf(apiConfigStore.asJavaMap().values());
    }

    @Override
    public KubevirtApiConfig apiConfig(String endpoint) {
        return apiConfigStore.asJavaMap().get(endpoint);
    }

    private class KubevirtApiConfigMapListener
            implements MapEventListener<String, KubevirtApiConfig> {

        @Override
        public void event(MapEvent<String, KubevirtApiConfig> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("Kubevirt API config created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtApiConfigEvent(
                                    KUBEVIRT_API_CONFIG_CREATED, event.newValue().value()
                            )));
                    break;
                case UPDATE:
                    log.debug("Kubevirt API config updated {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtApiConfigEvent(
                                    KUBEVIRT_API_CONFIG_UPDATED, event.newValue().value()
                            )));
                    break;
                case REMOVE:
                    log.debug("Kubevirt API config removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtApiConfigEvent(
                                    KUBEVIRT_API_CONFIG_REMOVED, event.oldValue().value()
                            )));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }
}
