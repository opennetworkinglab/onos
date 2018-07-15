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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.Host;
import org.onosproject.net.HostLocation;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortAdminService;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.InstancePortStore;
import org.onosproject.openstacknetworking.api.InstancePortStoreDelegate;
import org.slf4j.Logger;

import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.openstacknetworking.api.Constants.ANNOTATION_NETWORK_ID;
import static org.onosproject.openstacknetworking.api.Constants.ANNOTATION_PORT_ID;
import static org.onosproject.openstacknetworking.api.InstancePort.State.ACTIVE;
import static org.onosproject.openstacknetworking.api.InstancePort.State.INACTIVE;
import static org.onosproject.openstacknetworking.api.InstancePort.State.MIGRATED;
import static org.onosproject.openstacknetworking.api.InstancePort.State.MIGRATING;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing instance ports.
 * It also provides instance port events for the hosts mapped to OpenStack VM interface.
 */
@Service
@Component(immediate = true)
public class InstancePortManager
        extends ListenerRegistry<InstancePortEvent, InstancePortListener>
        implements InstancePortService, InstancePortAdminService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_INSTANCE_PORT = "Instance port %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_INSTANCE_PORT = "Instance port cannot be null";
    private static final String ERR_NULL_INSTANCE_PORT_ID = "Instance port ID cannot be null";
    private static final String ERR_NULL_MAC_ADDRESS = "MAC address cannot be null";
    private static final String ERR_NULL_IP_ADDRESS = "IP address cannot be null";
    private static final String ERR_NULL_NETWORK_ID = "Network ID cannot be null";

    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortStore instancePortStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private final InstancePortStoreDelegate
                            delegate = new InternalInstancePortStoreDelegate();
    private final InternalHostListener
                            hostListener = new InternalHostListener();

    @Activate
    protected void activate() {
        coreService.registerApplication(Constants.OPENSTACK_NETWORKING_APP_ID);
        instancePortStore.setDelegate(delegate);
        hostService.addListener(hostListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostService.removeListener(hostListener);
        instancePortStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createInstancePort(InstancePort instancePort) {
        checkNotNull(instancePort, ERR_NULL_INSTANCE_PORT);
        checkArgument(!Strings.isNullOrEmpty(instancePort.portId()),
                                                    ERR_NULL_INSTANCE_PORT_ID);

        instancePortStore.createInstancePort(instancePort);
        log.info(String.format(MSG_INSTANCE_PORT, instancePort.portId(),
                                                    MSG_CREATED));
    }

    @Override
    public void updateInstancePort(InstancePort instancePort) {
        checkNotNull(instancePort, ERR_NULL_INSTANCE_PORT);
        checkArgument(!Strings.isNullOrEmpty(instancePort.portId()),
                                                    ERR_NULL_INSTANCE_PORT_ID);

        instancePortStore.updateInstancePort(instancePort);
        log.info(String.format(MSG_INSTANCE_PORT, instancePort.portId(), MSG_UPDATED));
    }

    @Override
    public void removeInstancePort(String portId) {
        checkArgument(!Strings.isNullOrEmpty(portId), ERR_NULL_INSTANCE_PORT_ID);

        synchronized (this) {
            if (isInstancePortInUse(portId)) {
                final String error =
                            String.format(MSG_INSTANCE_PORT, portId, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            InstancePort instancePort = instancePortStore.removeInstancePort(portId);
            if (instancePort != null) {
                log.info(String.format(MSG_INSTANCE_PORT, instancePort.portId(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void clear() {
        instancePortStore.clear();
    }

    @Override
    public InstancePort instancePort(MacAddress macAddress) {
        checkNotNull(macAddress, ERR_NULL_MAC_ADDRESS);

        return instancePortStore.instancePorts().stream()
                .filter(port -> port.macAddress().equals(macAddress))
                .findFirst().orElse(null);
    }

    @Override
    public InstancePort instancePort(IpAddress ipAddress, String osNetId) {
        checkNotNull(ipAddress, ERR_NULL_IP_ADDRESS);
        checkNotNull(osNetId, ERR_NULL_NETWORK_ID);

        return instancePortStore.instancePorts().stream()
                .filter(port -> port.networkId().equals(osNetId))
                .filter(port -> port.ipAddress().equals(ipAddress))
                .findFirst().orElse(null);
    }

    @Override
    public InstancePort instancePort(String portId) {
        checkArgument(!Strings.isNullOrEmpty(portId), ERR_NULL_INSTANCE_PORT_ID);

        return instancePortStore.instancePort(portId);
    }

    @Override
    public Set<InstancePort> instancePorts() {
        Set<InstancePort> ports = instancePortStore.instancePorts();

        return ImmutableSet.copyOf(ports);
    }

    @Override
    public Set<InstancePort> instancePorts(String osNetId) {
        checkNotNull(osNetId, ERR_NULL_NETWORK_ID);

        Set<InstancePort> ports = instancePortStore.instancePorts().stream()
                                    .filter(port -> port.networkId().equals(osNetId))
                                    .collect(Collectors.toSet());

        return ImmutableSet.copyOf(ports);
    }

    private boolean isInstancePortInUse(String portId) {
        // TODO add checking logic
        return false;
    }

    private class InternalInstancePortStoreDelegate implements InstancePortStoreDelegate {

        @Override
        public void notify(InstancePortEvent event) {
            if (event != null) {
                log.trace("send instance port event {}", event);
                process(event);
            }
        }
    }

    /**
     * An internal listener that listens host event generated by HostLocationTracker
     * in DistributedHostStore. The role of this listener is to convert host event
     * to instance port event and post to the subscribers that have interested on
     * this type of event.
     */
    private class InternalHostListener implements HostListener {

        @Override
        public boolean isRelevant(HostEvent event) {
            Host host = event.subject();
            if (!isValidHost(host)) {
                log.debug("Invalid host detected, ignore it {}", host);
                return false;
            }
            return true;
        }

        @Override
        public void event(HostEvent event) {
            InstancePort instPort = DefaultInstancePort.from(event.subject(), ACTIVE);

            switch (event.type()) {
                case HOST_UPDATED:
                    updateInstancePort(instPort);
                    break;
                case HOST_ADDED:
                    InstancePort existingPort = instancePort(instPort.portId());
                    if (existingPort == null) {
                        // first time to add instance
                        createInstancePort(instPort);
                    } else {
                        // the instance was restarted
                        if (existingPort.state() == INACTIVE) {
                            updateInstancePort(instPort);
                        }
                    }
                    break;
                case HOST_REMOVED:
                    // we will remove instance port from persistent store,
                    // only if we receive port removal signal from neutron
                    // by default, we update the instance port state to INACTIVE
                    // to indicate the instance is terminated
                    updateInstancePort(instPort.updateState(INACTIVE));
                    break;
                case HOST_MOVED:
                    Host oldHost = event.prevSubject();
                    Host currHost = event.subject();

                    // in the middle of VM migration
                    if (oldHost.locations().size() < currHost.locations().size()) {
                        updateInstancePort(instPort.updateState(MIGRATING));
                    }

                    // finish of VM migration
                    if (oldHost.locations().size() > currHost.locations().size()) {
                        Set<HostLocation> diff =
                                Sets.difference(oldHost.locations(), currHost.locations());
                        HostLocation location = diff.stream().findFirst().orElse(null);

                        if (location != null) {
                            InstancePort updated = instPort.updateState(MIGRATED);
                            updateInstancePort(updated.updatePrevLocation(
                                        location.deviceId(), location.port()));
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        private boolean isValidHost(Host host) {
            return !host.ipAddresses().isEmpty() &&
                    host.annotations().value(ANNOTATION_NETWORK_ID) != null &&
                    host.annotations().value(ANNOTATION_PORT_ID) != null;
        }
    }
}
