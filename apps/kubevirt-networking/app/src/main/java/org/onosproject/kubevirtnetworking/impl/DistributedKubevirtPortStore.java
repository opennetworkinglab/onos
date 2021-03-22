/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtPortStore;
import org.onosproject.kubevirtnetworking.api.KubevirtPortStoreDelegate;
import org.onosproject.net.DeviceId;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.KubevirtPortEvent.Type.KUBEVIRT_PORT_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtPortEvent.Type.KUBEVIRT_PORT_DEVICE_ADDED;
import static org.onosproject.kubevirtnetworking.api.KubevirtPortEvent.Type.KUBEVIRT_PORT_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtPortEvent.Type.KUBEVIRT_PORT_SECURITY_GROUP_ADDED;
import static org.onosproject.kubevirtnetworking.api.KubevirtPortEvent.Type.KUBEVIRT_PORT_SECURITY_GROUP_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtPortEvent.Type.KUBEVIRT_PORT_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubevirt pod store using consistent map.
 */
@Component(immediate = true, service = KubevirtPortStore.class)
public class DistributedKubevirtPortStore
        extends AbstractStore<KubevirtPortEvent, KubevirtPortStoreDelegate>
        implements KubevirtPortStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.kubevirtnetwork";

    private static final KryoNamespace
            SERIALIZER_KUBEVIRT_PORT = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(KubevirtPort.class)
            .register(DefaultKubevirtPort.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, KubevirtPort> portMapListener =
            new KubevirtPortMapListener();

    private ConsistentMap<String, KubevirtPort> portStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        portStore = storageService.<String, KubevirtPort>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_KUBEVIRT_PORT))
                .withName("kubevirt-portstore")
                .withApplicationId(appId)
                .build();
        portStore.addListener(portMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        portStore.removeListener(portMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createPort(KubevirtPort port) {
        portStore.compute(port.macAddress().toString(), (mac, existing) -> {
            final String error = port.macAddress().toString() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return port;
        });
    }

    @Override
    public void updatePort(KubevirtPort port) {
        portStore.compute(port.macAddress().toString(), (mac, existing) -> {
            final String error = port.macAddress().toString() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return port;
        });
    }

    @Override
    public KubevirtPort removePort(MacAddress mac) {
        Versioned<KubevirtPort> port = portStore.remove(mac.toString());
        if (port == null) {
            final String error = mac.toString() + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return port.value();
    }

    @Override
    public KubevirtPort port(MacAddress mac) {
        return portStore.asJavaMap().get(mac.toString());
    }

    @Override
    public Set<KubevirtPort> ports() {
        return ImmutableSet.copyOf(portStore.asJavaMap().values());
    }

    @Override
    public void clear() {
        portStore.clear();
    }

    private class KubevirtPortMapListener implements MapEventListener<String, KubevirtPort> {

        @Override
        public void event(MapEvent<String, KubevirtPort> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("Kubevirt port created");
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtPortEvent(
                                    KUBEVIRT_PORT_CREATED, event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubevirt port updated");
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtPortEvent(
                                    KUBEVIRT_PORT_UPDATED, event.newValue().value())));
                    processSecurityGroupEvent(event.oldValue().value(), event.newValue().value());
                    processDeviceEvent(event.oldValue().value(), event.newValue().value());
                    break;
                case REMOVE:
                    log.debug("Kubevirt port removed");
                    // if the event object has invalid port value, we do not
                    // propagate KUBEVIRT_PORT_REMOVED event.
                    if (event.oldValue() != null && event.oldValue().value() != null) {
                        eventExecutor.execute(() ->
                                notifyDelegate(new KubevirtPortEvent(
                                        KUBEVIRT_PORT_REMOVED, event.oldValue().value())));
                    }
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processSecurityGroupEvent(KubevirtPort oldPort, KubevirtPort newPort) {
            Set<String> oldSecurityGroups = oldPort.securityGroups() == null ?
                    ImmutableSet.of() : oldPort.securityGroups();
            Set<String> newSecurityGroups = newPort.securityGroups() == null ?
                    ImmutableSet.of() : newPort.securityGroups();

            oldSecurityGroups.stream()
                    .filter(sgId -> !Objects.requireNonNull(
                            newPort.securityGroups()).contains(sgId))
                    .forEach(sgId -> notifyDelegate(new KubevirtPortEvent(
                            KUBEVIRT_PORT_SECURITY_GROUP_REMOVED, newPort, sgId
                    )));

            newSecurityGroups.stream()
                    .filter(sgId -> !oldPort.securityGroups().contains(sgId))
                    .forEach(sgId -> notifyDelegate(new KubevirtPortEvent(
                            KUBEVIRT_PORT_SECURITY_GROUP_ADDED, newPort, sgId
                    )));
        }

        private void processDeviceEvent(KubevirtPort oldPort, KubevirtPort newPort) {
            DeviceId oldDeviceId = oldPort.deviceId();
            DeviceId newDeviceId = newPort.deviceId();

            if (oldDeviceId == null && newDeviceId != null) {
                notifyDelegate(new KubevirtPortEvent(
                        KUBEVIRT_PORT_DEVICE_ADDED, newPort, newDeviceId
                ));
            }
        }
    }
}
