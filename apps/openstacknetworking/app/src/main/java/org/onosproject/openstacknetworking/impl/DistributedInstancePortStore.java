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
package org.onosproject.openstacknetworking.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortStore;
import org.onosproject.openstacknetworking.api.InstancePortStoreDelegate;
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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.InstancePort.State.ACTIVE;
import static org.onosproject.openstacknetworking.api.InstancePort.State.INACTIVE;
import static org.onosproject.openstacknetworking.api.InstancePort.State.MIGRATED;
import static org.onosproject.openstacknetworking.api.InstancePort.State.MIGRATING;
import static org.onosproject.openstacknetworking.api.InstancePort.State.REMOVE_PENDING;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_MIGRATION_ENDED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_MIGRATION_STARTED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_PORT_DETECTED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_PORT_UPDATED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_PORT_VANISHED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_RESTARTED;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_TERMINATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the inventory of openstack instance port using a {@code ConsistentMap}.
 */
@Component(immediate = true, service = InstancePortStore.class)
public class DistributedInstancePortStore
        extends AbstractStore<InstancePortEvent, InstancePortStoreDelegate>
        implements InstancePortStore {

    protected final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";

    private static final KryoNamespace SERIALIZER_INSTANCE_PORT = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(InstancePort.class)
            .register(DefaultInstancePort.class)
            .register(InstancePort.State.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, InstancePort>
            instancePortMapListener = new InstancePortMapListener();

    private ConsistentMap<String, InstancePort> instancePortStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);

        instancePortStore = storageService.<String, InstancePort>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_INSTANCE_PORT))
                .withName("openstack-instanceport-store")
                .withApplicationId(appId)
                .build();
        instancePortStore.addListener(instancePortMapListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        instancePortStore.removeListener(instancePortMapListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void createInstancePort(InstancePort port) {
        instancePortStore.compute(port.portId(), (id, existing) -> {
            final String error = port.portId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return port;
        });
    }

    @Override
    public void updateInstancePort(InstancePort port) {
        instancePortStore.compute(port.portId(), (id, existing) -> {
            final String error = port.portId() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return port;
        });
    }

    @Override
    public InstancePort removeInstancePort(String portId) {
        Versioned<InstancePort> port = instancePortStore.remove(portId);
        return port == null ? null : port.value();
    }

    @Override
    public InstancePort instancePort(String portId) {
        return instancePortStore.asJavaMap().get(portId);
    }

    @Override
    public Set<InstancePort> instancePorts() {
        return ImmutableSet.copyOf(instancePortStore.asJavaMap().values());
    }

    @Override
    public void clear() {
        instancePortStore.clear();
    }

    private class InstancePortMapListener implements MapEventListener<String, InstancePort> {

        @Override
        public void event(MapEvent<String, InstancePort> event) {
            switch (event.type()) {
                case INSERT:
                    eventExecutor.execute(() -> processInstancePortMapInsertion(event));
                    break;
                case UPDATE:
                    eventExecutor.execute(() -> processInstancePortMapUpdate(event));
                    break;
                case REMOVE:
                    eventExecutor.execute(() -> processInstancePortMapRemoval(event));
                    break;
                default:
                    log.error("Unsupported instance port event type");
                    break;
            }
        }

        private void processInstancePortMapUpdate(MapEvent<String, InstancePort> event) {
            log.debug("Instance port updated");
            processInstancePortUpdate(event);
        }

        private void processInstancePortMapInsertion(MapEvent<String, InstancePort> event) {
            log.debug("Instance port created");
            notifyDelegate(new InstancePortEvent(
                    OPENSTACK_INSTANCE_PORT_DETECTED,
                    event.newValue().value()));
        }

        private void processInstancePortMapRemoval(MapEvent<String, InstancePort> event) {
            log.debug("Instance port removed");
            notifyDelegate(new InstancePortEvent(
                    OPENSTACK_INSTANCE_PORT_VANISHED,
                    event.oldValue().value()));
        }

        private void processInstancePortUpdate(MapEvent<String, InstancePort> event) {
            InstancePort.State oldState = event.oldValue().value().state();
            InstancePort.State newState = event.newValue().value().state();

            if ((oldState == ACTIVE || oldState == INACTIVE) && newState == MIGRATING) {
                notifyDelegate(new InstancePortEvent(
                        OPENSTACK_INSTANCE_MIGRATION_STARTED,
                        event.newValue().value()));
                return;
            }

            if (oldState == MIGRATING && newState == MIGRATED) {
                notifyDelegate(new InstancePortEvent(
                        OPENSTACK_INSTANCE_MIGRATION_ENDED,
                        event.newValue().value()));
                updateInstancePort(event.newValue().value().updateState(ACTIVE));
                return;
            }

            if (oldState == ACTIVE && newState == INACTIVE) {
                notifyDelegate(new InstancePortEvent(
                        OPENSTACK_INSTANCE_TERMINATED,
                        event.newValue().value()));
                return;
            }

            if (oldState == INACTIVE && newState == ACTIVE) {
                notifyDelegate(new InstancePortEvent(
                        OPENSTACK_INSTANCE_RESTARTED,
                        event.newValue().value()));
                return;
            }

            // this should be auto-transition
            if (oldState == MIGRATED && newState == ACTIVE) {
                return;
            }

            // we do not trigger instance port update for pending state transition
            if (newState == REMOVE_PENDING) {
                return;
            }

            notifyDelegate(new InstancePortEvent(
                    OPENSTACK_INSTANCE_PORT_UPDATED,
                    event.newValue().value()));
        }
    }
}
