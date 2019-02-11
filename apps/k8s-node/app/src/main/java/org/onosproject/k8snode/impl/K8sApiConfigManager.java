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
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfig.Scheme;
import org.onosproject.k8snode.api.K8sApiConfigAdminService;
import org.onosproject.k8snode.api.K8sApiConfigEvent;
import org.onosproject.k8snode.api.K8sApiConfigListener;
import org.onosproject.k8snode.api.K8sApiConfigService;
import org.onosproject.k8snode.api.K8sApiConfigStore;
import org.onosproject.k8snode.api.K8sApiConfigStoreDelegate;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snode.util.K8sNodeUtil.endpoint;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service administering the inventory of kubernetes API configs.
 */
@Component(
        immediate = true,
        service = { K8sApiConfigService.class, K8sApiConfigAdminService.class }
)
public class K8sApiConfigManager
        extends ListenerRegistry<K8sApiConfigEvent, K8sApiConfigListener>
        implements K8sApiConfigService, K8sApiConfigAdminService {

    private final Logger log = getLogger(getClass());

    private static final String MSG_CONFIG = "Kubernetes API config %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_CONFIG = "Kubernetes API config cannot be null";
    private static final String ERR_NULL_ENDPOINT = "Kubernetes API endpoint cannot be null";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sApiConfigStore configStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final K8sApiConfigStoreDelegate delegate = new InternalApiConfigStoreDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);
        configStore.setDelegate(delegate);

        leadershipService.runForLeadership(appId.name());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        configStore.unsetDelegate(delegate);

        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void createApiConfig(K8sApiConfig config) {
        checkNotNull(config, ERR_NULL_CONFIG);
        configStore.createApiConfig(config);
        log.info(String.format(MSG_CONFIG, endpoint(config), MSG_CREATED));
    }

    @Override
    public void updateApiConfig(K8sApiConfig config) {
        checkNotNull(config, ERR_NULL_CONFIG);
        configStore.updateApiConfig(config);
        log.info(String.format(MSG_CONFIG, endpoint(config), MSG_UPDATED));
    }

    @Override
    public K8sApiConfig removeApiConfig(String endpoint) {
        checkArgument(!Strings.isNullOrEmpty(endpoint), ERR_NULL_ENDPOINT);
        K8sApiConfig config = configStore.removeApiConfig(endpoint);
        log.info(String.format(MSG_CONFIG, endpoint, MSG_REMOVED));
        return config;
    }

    @Override
    public K8sApiConfig removeApiConfig(Scheme scheme,
                                        IpAddress ipAddress, int port) {
        return removeApiConfig(endpoint(scheme, ipAddress, port));
    }

    @Override
    public Set<K8sApiConfig> apiConfigs() {
        return configStore.apiConfigs();
    }

    @Override
    public K8sApiConfig apiConfig(String endpoint) {
        return configStore.apiConfig(endpoint);
    }

    @Override
    public K8sApiConfig apiConfig(Scheme scheme, IpAddress ipAddress, int port) {
        return apiConfig(endpoint(scheme, ipAddress, port));
    }

    private class InternalApiConfigStoreDelegate implements K8sApiConfigStoreDelegate {

        @Override
        public void notify(K8sApiConfigEvent event) {
            if (event != null) {
                log.trace("send kubernetes API config event {}", event);
                process(event);
            }
        }
    }
}
