/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.intent.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointDescription;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointState;
import org.onosproject.net.behaviour.protection.ProtectionConfig;
import org.onosproject.net.behaviour.protection.ProtectionConfigBehaviour;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.config.basics.SubjectFactories.DEVICE_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

// TODO In theory just @Component should be sufficient,
//      but won't work without @Service. Need investigation.
/**
 * Component to monitor {@link ProtectionConfig} changes.
 */
@Component(immediate = true, service = ProtectionConfigMonitor.class)
public class ProtectionConfigMonitor {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgRegistry;

    private final List<ConfigFactory<?, ?>> factories = ImmutableList.of(
         new ConfigFactory<DeviceId, ProtectionConfig>(DEVICE_SUBJECT_FACTORY,
                 ProtectionConfig.class, ProtectionConfig.CONFIG_KEY) {
             @Override
             public ProtectionConfig createConfig() {
                 return new ProtectionConfig();
             }
         });


    private final ProtectionConfigListener listener = new ProtectionConfigListener();

    private ExecutorService worker;


    @Activate
    public void activate() {
        worker = newSingleThreadExecutor(groupedThreads("onos/protection",
                                                        "installer",
                                                        log));
        networkConfigService.addListener(listener);

        factories.forEach(cfgRegistry::registerConfigFactory);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        networkConfigService.removeListener(listener);

        worker.shutdown();
        try {
            worker.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Interrupted.", e);
            Thread.currentThread().interrupt();
        }
        factories.forEach(cfgRegistry::unregisterConfigFactory);

        log.info("Stopped");
    }

    /**
     * Retrieves {@link ProtectionConfigBehaviour} for the Device.
     *
     * @param did {@link DeviceId} of the Device to fetch
     * @return {@link ProtectionConfigBehaviour}
     *   or throws {@link UnsupportedOperationException} on error.
     */
    private ProtectionConfigBehaviour getBehaviour(DeviceId did) {
        DriverHandler handler = driverService.createHandler(did);
        if (!handler.hasBehaviour(ProtectionConfigBehaviour.class)) {
            log.error("{} does not support protection", did);
            throw new UnsupportedOperationException(did + " does not support protection");
        }

        return handler.behaviour(ProtectionConfigBehaviour.class);
    }

    /**
     * Retrieves first virtual Port with specified fingerprint.
     *
     * @param behaviour to use to query the Device
     * @param fingerprint to look for
     * @return virtual Port {@link ConnectPoint} if found.
     */
    private Optional<ConnectPoint> findFirstVirtualPort(ProtectionConfigBehaviour behaviour,
                                                        String fingerprint) {

        CompletableFuture<Map<ConnectPoint, ProtectedTransportEndpointState>>
        states = behaviour.getProtectionEndpointStates();

        Map<ConnectPoint, ProtectedTransportEndpointState> map;
        try {
            map = states.get();
        } catch (InterruptedException e1) {
            log.error("Interrupted.", e1);
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e1) {
            log.error("Exception caught.", e1);
            return Optional.empty();
        }

        // TODO this is not clean, should add utility method to API?
        return map.entrySet().stream()
            .filter(e -> fingerprint.equals(e.getValue().description().fingerprint()))
            .map(Entry::getKey)
            .findFirst();
    }

    private void addProtection(DeviceId did, ProtectionConfig added) {
        ProtectedTransportEndpointDescription description = added.asDescription();
        log.info("adding protection {}-{}", did, description);

        ProtectionConfigBehaviour behaviour = getBehaviour(did);


        CompletableFuture<ConnectPoint> result;
        result = behaviour.createProtectionEndpoint(description);
        result.handle((vPort, e) -> {
            if (vPort != null) {
                log.info("Virtual Port {} created for {}", vPort, description);
                log.debug("{}", deviceService.getPort(vPort));
            } else {
                log.error("Protection {} exceptionally failed.", added, e);
            }
            return vPort;
        });
    }

    private void updateProtection(DeviceId did, ProtectionConfig before, ProtectionConfig after) {
        ProtectedTransportEndpointDescription description = after.asDescription();
        log.info("updating protection {}-{}", did, description);

        ProtectionConfigBehaviour behaviour = getBehaviour(did);

        Optional<ConnectPoint> existing = findFirstVirtualPort(behaviour, after.fingerprint());
        if (!existing.isPresent()) {
            log.warn("Update requested, but not found, falling back as add");
            addProtection(did, after);
            return;
        }
        ConnectPoint vPort = existing.get();
        log.info("updating protection virtual Port {} : {}", vPort, description);
        behaviour.updateProtectionEndpoint(vPort, description)
            .handle((vPortNew, e) -> {
            if (vPort != null) {
                log.info("Virtual Port {} updated for {}", vPort, description);
                log.debug("{}", deviceService.getPort(vPort));
            } else {
                log.error("Protection {} -> {} exceptionally failed.",
                          before, after, e);
            }
            return vPort;
        });
    }

    private void removeProtection(DeviceId did, ProtectionConfig removed) {
        ProtectedTransportEndpointDescription description = removed.asDescription();
        log.info("removing protection {}-{}", did, description);

        ProtectionConfigBehaviour behaviour = getBehaviour(did);

        Optional<ConnectPoint> existing = findFirstVirtualPort(behaviour, removed.fingerprint());
        if (!existing.isPresent()) {
            log.warn("Remove requested, but not found, ignoring");
            return;
        }
        ConnectPoint vPort = existing.get();

        log.info("removing protection virtual port {} : {}", vPort, description);
        behaviour.deleteProtectionEndpoint(vPort)
        .handle((result, ex) -> {
            if (ex != null) {
                log.info("removed protection {} : {}", vPort, result);
            } else {
                log.warn("removed protection {} failed.", vPort, ex);
            }
            return result;
        });
    }

    /**
     * Listens for new {@link ProtectionConfig} to install/remove.
     */
    public class ProtectionConfigListener
            implements NetworkConfigListener {

        /**
         * Relevant {@link NetworkConfigEvent} type.
         */
        private final Set<NetworkConfigEvent.Type> relevant
            = ImmutableSet.copyOf(EnumSet.of(
                         NetworkConfigEvent.Type.CONFIG_ADDED,
                         NetworkConfigEvent.Type.CONFIG_UPDATED,
                         NetworkConfigEvent.Type.CONFIG_REMOVED));

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass() == ProtectionConfig.class &&
                    relevant.contains(event.type());
        }

        @Override
        public void event(NetworkConfigEvent event) {
            worker.execute(() -> processEvent(event));
        }

        /**
         * Process {@link ProtectionConfig} add/update/remove event.
         * <p>
         * Note: will be executed in the worker thread.
         *
         * @param event {@link ProtectionConfig} add/update/remove event
         */
        protected void processEvent(NetworkConfigEvent event) {

            final DeviceId did = (DeviceId) event.subject();
            log.debug("{} to {}: {}", event.type(), did, event);

            if (deviceService.getRole(did) != MastershipRole.MASTER) {
                log.debug("Not the master, ignoring. {}", event);
                return;
            }

            switch (event.type()) {
            case CONFIG_ADDED:
                addProtection(did,
                              (ProtectionConfig) event.config().get());
                break;
            case CONFIG_UPDATED:
                updateProtection(did,
                                 (ProtectionConfig) event.prevConfig().get(),
                                 (ProtectionConfig) event.config().get());
                break;
            case CONFIG_REMOVED:
                removeProtection(did,
                                 (ProtectionConfig) event.prevConfig().get());
                break;

            case CONFIG_REGISTERED:
            case CONFIG_UNREGISTERED:
            default:
                log.warn("Ignoring unexpected event: {}", event);
                break;
            }
        }
    }

}
