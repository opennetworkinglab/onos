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

import com.google.common.base.Strings;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigAdminService;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigEvent;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigListener;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigService;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigStore;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigStoreDelegate;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.endpoint;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.resolveHostname;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service administering the inventory of KubeVirt API configs.
 */
@Component(
        immediate = true,
        service = {KubevirtApiConfigService.class, KubevirtApiConfigAdminService.class }
)
public class KubevirtApiConfigManager
        extends ListenerRegistry<KubevirtApiConfigEvent, KubevirtApiConfigListener>
        implements KubevirtApiConfigService, KubevirtApiConfigAdminService {

    private final Logger log = getLogger(getClass());

    private static final int API_SERVER_PORT = 443;

    private static final String MSG_CONFIG = "KubeVirt API config %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_CONFIG = "KubeVirt API config cannot be null";
    private static final String ERR_NULL_ENDPOINT = "KubeVirt API endpoint cannot be null";
    private static final String ERR_UNIQUE_CONFIG = "KubeVirt API config should be unique";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtApiConfigStore configStore;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final KubevirtApiConfigStoreDelegate delegate = new InternalApiConfigStoreDelegate();

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
    public void createApiConfig(KubevirtApiConfig config) {
        checkNotNull(config, ERR_NULL_CONFIG);
        checkArgument(configStore.apiConfigs().size() == 0, ERR_UNIQUE_CONFIG);

        KubevirtApiConfig newConfig = config;
        if (config.apiServerFqdn() != null) {
            IpAddress apiServerIp = resolveHostname(config.apiServerFqdn());
            if (apiServerIp != null) {
                newConfig = config.updateIpAddress(apiServerIp);
                newConfig = newConfig.updatePort(API_SERVER_PORT);
            } else {
                log.warn("API server IP is not resolved for host {}", config.apiServerFqdn());
            }
        }

        configStore.createApiConfig(newConfig);
        log.info(String.format(MSG_CONFIG, endpoint(newConfig), MSG_CREATED));
    }

    @Override
    public void updateApiConfig(KubevirtApiConfig config) {
        checkNotNull(config, ERR_NULL_CONFIG);
        configStore.updateApiConfig(config);
        log.info(String.format(MSG_CONFIG, endpoint(config), MSG_UPDATED));
    }

    @Override
    public KubevirtApiConfig removeApiConfig(String endpoint) {
        checkArgument(!Strings.isNullOrEmpty(endpoint), ERR_NULL_ENDPOINT);
        KubevirtApiConfig config = configStore.removeApiConfig(endpoint);
        log.info(String.format(MSG_CONFIG, endpoint, MSG_REMOVED));
        return config;
    }

    @Override
    public KubevirtApiConfig apiConfig() {
        return configStore.apiConfigs().stream().findAny().orElse(null);
    }

    private class InternalApiConfigStoreDelegate implements KubevirtApiConfigStoreDelegate {

        @Override
        public void notify(KubevirtApiConfigEvent event) {
            if (event != null) {
                log.trace("send KubeVirt API config event {}", event);
                process(event);
            }
        }
    }
}
