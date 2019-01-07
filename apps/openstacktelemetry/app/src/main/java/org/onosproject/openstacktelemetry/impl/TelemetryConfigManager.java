/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.openstacktelemetry.api.DefaultTelemetryConfig;
import org.onosproject.openstacktelemetry.api.TelemetryConfigAdminService;
import org.onosproject.openstacktelemetry.api.TelemetryConfigEvent;
import org.onosproject.openstacktelemetry.api.TelemetryConfigListener;
import org.onosproject.openstacktelemetry.api.TelemetryConfigProvider;
import org.onosproject.openstacktelemetry.api.TelemetryConfigService;
import org.onosproject.openstacktelemetry.api.TelemetryConfigStore;
import org.onosproject.openstacktelemetry.api.TelemetryConfigStoreDelegate;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.openstacktelemetry.api.Constants.OPENSTACK_TELEMETRY_APP_ID;

/**
 * Provides implementation of administering and interfacing telemetry configs.
 * It also provides telemetry config events for the various exporters or connectors.
 */
@Component(
    immediate = true,
    service = {
            TelemetryConfigService.class, TelemetryConfigAdminService.class
    }
)
public class TelemetryConfigManager
        extends ListenerRegistry<TelemetryConfigEvent, TelemetryConfigListener>
        implements TelemetryConfigService, TelemetryConfigAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MSG_TELEMETRY_CONFIG = "Telemetry config %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_CONFIG = "Telemetry config cannot be null";
    private static final String NO_CONFIG = "Telemetry config not found";
    private static final String ERR_NULL_CONFIG_NAME = "Telemetry config name cannot be null";

    private static final KryoNamespace SERIALIZER_TELEMETRY_CONFIG =
            KryoNamespace.newBuilder()
                    .register(KryoNamespaces.API)
                    .register(TelemetryConfigProvider.class)
                    .register(DefaultTelemetryConfigProvider.class)
                    .register(TelemetryConfig.class)
                    .register(TelemetryConfig.ConfigType.class)
                    .register(TelemetryConfig.Status.class)
                    .register(DefaultTelemetryConfig.class)
                    .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TelemetryConfigStore telemetryConfigStore;

    private final TelemetryConfigStoreDelegate
                        delegate = new InternalTelemetryConfigStoreDelegate();

    private ConsistentMap<String, TelemetryConfigProvider> providerMap;

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_TELEMETRY_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        telemetryConfigStore.setDelegate(delegate);
        leadershipService.runForLeadership(appId.name());

        providerMap = storageService.<String, TelemetryConfigProvider>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_TELEMETRY_CONFIG))
                .withName("openstack-telemetry-config-provider")
                .withApplicationId(appId)
                .build();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        telemetryConfigStore.unsetDelegate(delegate);
        leadershipService.withdraw(appId.name());
        providerMap.clear();

        log.info("Stopped");
    }

    @Override
    public Set<TelemetryConfigProvider> getProviders() {
        ImmutableSet.Builder<TelemetryConfigProvider> builder = ImmutableSet.builder();
        providerMap.asJavaMap().values().forEach(builder::add);
        return builder.build();
    }

    @Override
    public void registerProvider(TelemetryConfigProvider provider) {
        if (isLeader()) {
            StringBuilder nameBuilder = new StringBuilder();
            provider.getTelemetryConfigs().forEach(config -> {
                nameBuilder.append(config.name());
                telemetryConfigStore.createTelemetryConfig(config);
                log.info(String.format(MSG_TELEMETRY_CONFIG, config.name(), MSG_CREATED));
            });
            providerMap.put(nameBuilder.toString(), provider);
        }
    }

    @Override
    public void unregisterProvider(TelemetryConfigProvider provider) {
        if (isLeader()) {
            StringBuilder nameBuilder = new StringBuilder();
            provider.getTelemetryConfigs().forEach(config -> {
                nameBuilder.append(config.name());
                telemetryConfigStore.removeTelemetryConfig(config.name());
                log.info(String.format(MSG_TELEMETRY_CONFIG, config.name(), MSG_REMOVED));
            });
            providerMap.remove(nameBuilder.toString());
        }
    }

    @Override
    public void updateTelemetryConfig(TelemetryConfig config) {
        checkNotNull(config, ERR_NULL_CONFIG);

        telemetryConfigStore.updateTelemetryConfig(config);
        log.info(String.format(MSG_TELEMETRY_CONFIG, config.name(), MSG_UPDATED));
    }

    @Override
    public void removeTelemetryConfig(String name) {
        checkNotNull(name, ERR_NULL_CONFIG_NAME);

        telemetryConfigStore.removeTelemetryConfig(name);
        log.info(String.format(MSG_TELEMETRY_CONFIG, name, MSG_REMOVED));
    }

    @Override
    public TelemetryConfig getConfig(String name) {
        return nullIsNotFound(telemetryConfigStore.telemetryConfig(name), NO_CONFIG);
    }

    @Override
    public Set<TelemetryConfig> getConfigsByType(ConfigType type) {
        return ImmutableSet.copyOf(telemetryConfigStore.telemetryConfigsByType(type));
    }

    @Override
    public Set<TelemetryConfig> getConfigs() {
        return ImmutableSet.copyOf(telemetryConfigStore.telemetryConfigs());
    }

    private boolean isLeader() {
        return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
    }

    private class InternalTelemetryConfigStoreDelegate implements TelemetryConfigStoreDelegate {

        @Override
        public void notify(TelemetryConfigEvent event) {
            if (event != null) {
                log.trace("send telemetry config event {}", event);
                process(event);
            }
        }
    }
}
