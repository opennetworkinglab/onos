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
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
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
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
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
@Component(
    immediate = true,
    service = { InstancePortService.class, InstancePortAdminService.class }
)
public class InstancePortManager
        extends ListenerRegistry<InstancePortEvent, InstancePortListener>
        implements InstancePortService, InstancePortAdminService {

    protected final Logger log = getLogger(getClass());

    private static final String OPENSTACK_PROVIDER = "org.onosproject.openstacknetworking";
    private static final String MSG_INSTANCE_PORT = "Instance port %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_INSTANCE_PORT = "Instance port cannot be null";
    private static final String ERR_NULL_INSTANCE_PORT_ID = "Instance port ID cannot be null";
    private static final String ERR_NULL_MAC_ADDRESS = "MAC address cannot be null";
    private static final String ERR_NULL_IP_ADDRESS = "IP address cannot be null";
    private static final String ERR_NULL_NETWORK_ID = "Network ID cannot be null";
    private static final String ERR_NULL_DEVICE_ID = "Device ID cannot be null";
    private static final String ERR_NULL_PORT_NUMBER = "Port number cannot be null";

    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InstancePortStore instancePortStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackRouterService routerService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));
    private final InstancePortStoreDelegate
                            delegate = new InternalInstancePortStoreDelegate();
    private final InternalHostListener
                            hostListener = new InternalHostListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(Constants.OPENSTACK_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        instancePortStore.setDelegate(delegate);
        hostService.addListener(hostListener);
        leadershipService.runForLeadership(appId.name());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostService.removeListener(hostListener);
        instancePortStore.unsetDelegate(delegate);
        leadershipService.withdraw(appId.name());

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

        // in case OpenStack removes the port prior to OVS, we will not update
        // the instance port as it does not exist in the store
        if (instancePortStore.instancePort(instancePort.portId()) == null) {
            log.warn("Unable to update instance port {}, as it does not exist",
                                                        instancePort.portId());
            return;
        }

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
    public InstancePort instancePort(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId, ERR_NULL_DEVICE_ID);
        checkNotNull(portNumber, ERR_NULL_PORT_NUMBER);

        return instancePortStore.instancePorts().stream()
                .filter(port -> port.deviceId().equals(deviceId))
                .filter(port -> port.portNumber().equals(portNumber))
                .findFirst().orElse(null);
    }

    @Override
    public Set<InstancePort> instancePort(DeviceId deviceId) {
        Set<InstancePort> ports = instancePortStore.instancePorts().stream()
                .filter(port -> port.deviceId().equals(deviceId))
                .collect(Collectors.toSet());

        return ImmutableSet.copyOf(ports);
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

    @Override
    public IpAddress floatingIp(String osPortId) {
        checkNotNull(osPortId, ERR_NULL_INSTANCE_PORT_ID);

        return routerService.floatingIps().stream()
                .filter(fip -> osPortId.equals(fip.getPortId()))
                .filter(fip -> fip.getFloatingIpAddress() != null)
                .map(fip -> IpAddress.valueOf(fip.getFloatingIpAddress()))
                .findFirst().orElse(null);
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

            boolean isProvider =
                    OPENSTACK_PROVIDER.equals(event.subject().providerId().id());

            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            return Objects.equals(localNodeId, leader) && isProvider;
        }

        @Override
        public void event(HostEvent event) {
            InstancePort instPort = DefaultInstancePort.from(event.subject(), ACTIVE);

            switch (event.type()) {
                case HOST_UPDATED:
                    eventExecutor.execute(() -> processHostUpdate(instPort));
                    break;
                case HOST_ADDED:
                    eventExecutor.execute(() -> processHostAddition(instPort));
                    break;
                case HOST_REMOVED:
                    eventExecutor.execute(() -> processHostRemoval(instPort));
                    break;
                case HOST_MOVED:
                    eventExecutor.execute(() -> processHostMove(event, instPort));
                    break;
                default:
                    break;
            }
        }

        private void processHostUpdate(InstancePort instPort) {
            InstancePort existingPort = instancePort(instPort.portId());
            if (existingPort == null) {
                createInstancePort(instPort);
            } else {
                updateInstancePort(instPort);
            }
        }

        private void processHostAddition(InstancePort instPort) {
            InstancePort existingPort = instancePort(instPort.portId());
            if (existingPort == null) {
                // first time to add instance
                createInstancePort(instPort);
            } else {
                if (existingPort.state() == INACTIVE) {

                    if (instPort.deviceId().equals(existingPort.deviceId())) {
                        // VM RESTART case
                        // if the ID of switch where VM is attached to is
                        // identical, we can assume that the VM was
                        // restarted in the same location;
                        // note that the switch port number where VM is
                        // attached can be varied per each restart
                        updateInstancePort(instPort);
                    } else {
                        // VM COLD MIGRATION case
                        // if the ID of switch where VM is attached to is
                        // varied, we can assume that the VM was migrated
                        // to a new location
                        updateInstancePort(instPort.updateState(MIGRATING));
                        InstancePort updated = instPort.updateState(MIGRATED);
                        updateInstancePort(updated.updatePrevLocation(
                                existingPort.deviceId(), existingPort.portNumber()));
                    }
                }
            }
        }

        private void processHostRemoval(InstancePort instPort) {
            /* in case the instance port cannot be found in the store,
               this indicates that the instance port was removed due to
               the removal of openstack port; in some cases, openstack
               port removal message arrives before ovs port removal message */
            if (instancePortStore.instancePort(instPort.portId()) == null) {
                log.debug("instance port was removed before ovs port removal");
                return;
            }

            /* we will remove instance port from persistent store,
               only if we receive port removal signal from neutron.
               by default, we update the instance port state to INACTIVE
               to indicate the instance is terminated */
            updateInstancePort(instPort.updateState(INACTIVE));
        }

        private void processHostMove(HostEvent event, InstancePort instPort) {
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
        }

        private boolean isValidHost(Host host) {
            return !host.ipAddresses().isEmpty() &&
                    host.annotations().value(ANNOTATION_NETWORK_ID) != null &&
                    host.annotations().value(ANNOTATION_PORT_ID) != null;
        }
    }
}
