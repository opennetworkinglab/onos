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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.openstacktelemetry.api.DefaultTelemetryConfig;
import org.onosproject.openstacktelemetry.api.TelemetryConfigEvent;
import org.onosproject.openstacktelemetry.api.TelemetryConfigProvider;
import org.onosproject.openstacktelemetry.api.TelemetryConfigStore;
import org.onosproject.openstacktelemetry.api.TelemetryConfigStoreDelegate;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType;
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

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacktelemetry.api.Constants.OPENSTACK_TELEMETRY_APP_ID;
import static org.onosproject.openstacktelemetry.api.TelemetryConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.openstacktelemetry.api.TelemetryConfigEvent.Type.CONFIG_DELETED;
import static org.onosproject.openstacktelemetry.api.TelemetryConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.openstacktelemetry.api.TelemetryConfigEvent.Type.SERVICE_DISABLED;
import static org.onosproject.openstacktelemetry.api.TelemetryConfigEvent.Type.SERVICE_ENABLED;
import static org.onosproject.openstacktelemetry.api.TelemetryConfigEvent.Type.SERVICE_PENDING;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.DISABLED;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.ENABLED;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.PENDING;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the inventory of telemetry configurations using a {@code ConsistentMap}.
 */
@Component(immediate = true, service = TelemetryConfigStore.class)
public class DistributedTelemetryConfigStore
        extends AbstractStore<TelemetryConfigEvent, TelemetryConfigStoreDelegate>
        implements TelemetryConfigStore {

    protected final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";

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

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, TelemetryConfig>
            telemetryConfigMapListener = new TelemetryConfigMapListener();

    private ConsistentMap<String, TelemetryConfig> telemetryConfigStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(OPENSTACK_TELEMETRY_APP_ID);

        telemetryConfigStore = storageService.<String, TelemetryConfig>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_TELEMETRY_CONFIG))
                .withName("telemetry-config-store")
                .withApplicationId(appId)
                .build();
        telemetryConfigStore.addListener(telemetryConfigMapListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        telemetryConfigStore.removeListener(telemetryConfigMapListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void createTelemetryConfig(TelemetryConfig config) {
        telemetryConfigStore.compute(config.name(), (name, existing) -> {
            final String error = config.name() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return config;
        });
    }

    @Override
    public void updateTelemetryConfig(TelemetryConfig config) {
        telemetryConfigStore.compute(config.name(), (name, existing) -> {
            final String error = config.name() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return config;
        });
    }

    @Override
    public TelemetryConfig removeTelemetryConfig(String name) {
        Versioned<TelemetryConfig> config = telemetryConfigStore.remove(name);
        return config == null ? null : config.value();
    }

    @Override
    public TelemetryConfig telemetryConfig(String name) {
        return telemetryConfigStore.asJavaMap().get(name);
    }

    @Override
    public Set<TelemetryConfig> telemetryConfigs() {
        return ImmutableSet.copyOf(telemetryConfigStore.asJavaMap().values());
    }

    @Override
    public Set<TelemetryConfig> telemetryConfigsByType(ConfigType type) {
        return ImmutableSet.copyOf(telemetryConfigStore.asJavaMap().values()
                .stream().filter(c -> c.type() == type).collect(Collectors.toSet()));
    }

    @Override
    public void clear() {
        telemetryConfigStore.clear();
    }

    private class TelemetryConfigMapListener
                        implements MapEventListener<String, TelemetryConfig> {

        @Override
        public void event(MapEvent<String, TelemetryConfig> event) {
            switch (event.type()) {
                case INSERT:
                    eventExecutor.execute(() -> processTelemetryConfigMapInsertion(event));
                    break;
                case UPDATE:
                    eventExecutor.execute(() -> processTelemetryConfigMapUpdate(event));
                    break;
                case REMOVE:
                    eventExecutor.execute(() -> processTelemetryConfigMapRemoval(event));
                    break;
                default:
                    log.error("Unsupported telemetry config event type");
                    break;
            }
        }

        private void processTelemetryConfigMapInsertion(MapEvent<String,
                                                        TelemetryConfig> event) {
            log.debug("Telemetry config created");
            notifyDelegate(new TelemetryConfigEvent(
                    CONFIG_ADDED, event.newValue().value()));
        }

        private void processTelemetryConfigMapUpdate(MapEvent<String,
                                                     TelemetryConfig> event) {
            log.debug("Telemetry config updated");

            processTelemetryServiceStatusChange(event);

            notifyDelegate(new TelemetryConfigEvent(
                    CONFIG_UPDATED, event.newValue().value()));
        }

        private void processTelemetryConfigMapRemoval(MapEvent<String,
                                                      TelemetryConfig> event) {
            log.debug("Telemetry config removed");
            notifyDelegate(new TelemetryConfigEvent(
                    CONFIG_DELETED, event.oldValue().value()));
        }

        private void processTelemetryServiceStatusChange(
                                    MapEvent<String, TelemetryConfig> event) {
            TelemetryConfig oldValue = event.oldValue().value();
            TelemetryConfig newValue = event.newValue().value();

            if (oldValue.status() != DISABLED && newValue.status() == DISABLED) {
                log.debug("Telemetry service {} has been disabled!", newValue.name());
                notifyDelegate(new TelemetryConfigEvent(SERVICE_DISABLED, newValue));
            }

            if (oldValue.status() != ENABLED && newValue.status() == ENABLED) {
                log.debug("Telemetry service {} has been enabled!", newValue.name());
                notifyDelegate(new TelemetryConfigEvent(SERVICE_ENABLED, newValue));
            }

            if (oldValue.status() != PENDING && newValue.status() == PENDING) {
                log.debug("Telemetry service {} was pended!", newValue.name());
                notifyDelegate(new TelemetryConfigEvent(SERVICE_PENDING, newValue));
            }
        }
    }
}
