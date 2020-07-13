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
package org.onosproject.openstacknetworking.impl;

import com.google.common.base.Strings;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.OpenstackNetwork.Type;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.Network;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknetworking.api.Constants.ANNOTATION_CREATE_TIME;
import static org.onosproject.openstacknetworking.api.Constants.ANNOTATION_NETWORK_ID;
import static org.onosproject.openstacknetworking.api.Constants.ANNOTATION_PORT_ID;
import static org.onosproject.openstacknetworking.api.Constants.ANNOTATION_SEGMENT_ID;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PORT_NAME_PREFIX_VM;
import static org.onosproject.openstacknetworking.api.Constants.PORT_NAME_VHOST_USER_PREFIX_VM;
import static org.onosproject.openstacknetworking.api.Constants.TUNNEL_TYPE;
import static org.onosproject.openstacknetworking.api.Constants.portNamePrefixMap;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.FLAT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.vnicType;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.CONTROLLER;

@Component(immediate = true, service = HostProvider.class)
public class OpenstackSwitchingHostProvider
        extends AbstractProvider implements HostProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ERR_ADD_HOST = "Failed to add host: ";
    private static final String SONA_HOST_SCHEME = "sona";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostProviderRegistry hostProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    private HostProviderService hostProviderService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "device-event"));
    private final InternalDeviceListener internalDeviceListener =
            new InternalDeviceListener();
    private final InternalNetworkListener internalNetworkListener =
            new InternalNetworkListener();
    private final InternalOpenstackNodeListener internalNodeListener =
            new InternalOpenstackNodeListener();

    /**
     * Creates OpenStack switching host provider.
     */
    public OpenstackSwitchingHostProvider() {
        super(new ProviderId(SONA_HOST_SCHEME, OPENSTACK_NETWORKING_APP_ID));
    }

    @Activate
    protected void activate() {
        coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        deviceService.addListener(internalDeviceListener);
        osNodeService.addListener(internalNodeListener);
        osNetworkService.addListener(internalNetworkListener);
        hostProviderService = hostProviderRegistry.register(this);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostProviderRegistry.unregister(this);
        osNodeService.removeListener(internalNodeListener);
        osNetworkService.removeListener(internalNetworkListener);
        deviceService.removeListener(internalDeviceListener);

        executor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void triggerProbe(Host host) {
        // no probe is required
    }

    /**
     * A helper method which logs the port addition event and performs port
     * addition action.
     *
     * @param event device event
     */
    void portAddedHelper(DeviceEvent event) {
        log.debug("Instance port {} is detected from {}",
                event.port().annotations().value(PORT_NAME),
                event.subject().id());
        // we check the existence of openstack port, in case VM creation
        // event comes before port creation
        if (osNetworkService.port(event.port()) != null) {
            processPortAdded(event.port());
        }
    }

    /**
     * A helper method which logs the port removal event and performs port
     * removal action.
     *
     * @param event device event
     */
    void portRemovedHelper(DeviceEvent event) {
        log.debug("Instance port {} is removed from {}",
                event.port().annotations().value(PORT_NAME),
                event.subject().id());
        // we check the existence of openstack port, will not remove any hosts
        // or instance ports with non-exist openstack port
        if (osNetworkService.port(event.port()) != null) {
            processPortRemoved(event.port());
        }
    }

    /**
     * Processes port addition event.
     * Once a port addition event is detected, it tries to create a host instance
     * with openstack augmented host information such as networkId, portId,
     * createTime, segmentId and notifies to host provider.
     *
     * @param port port object used in ONOS
     */
    void processPortAdded(Port port) {
        // TODO check the node state is COMPLETE
        org.openstack4j.model.network.Port osPort = osNetworkService.port(port);
        if (osPort == null) {
            log.warn(ERR_ADD_HOST + "OpenStack port for {} not found", port);
            return;
        }

        Network osNet = osNetworkService.network(osPort.getNetworkId());
        if (osNet == null) {
            log.warn(ERR_ADD_HOST + "OpenStack network {} not found",
                    osPort.getNetworkId());
            return;
        }

        if (osPort.getFixedIps().isEmpty()) {
            log.warn(ERR_ADD_HOST + "no fixed IP for port {}", osPort.getId());
            return;
        }

        MacAddress mac = MacAddress.valueOf(osPort.getMacAddress());
        HostId hostId = HostId.hostId(mac);

        /* typically one openstack port should only be bound to one fix IP address;
           however, openstack4j binds multiple fixed IPs to one port, this might
           be a defect of openstack4j implementation */

        // TODO: we need to find a way to bind multiple ports from multiple
        // openstack networks into one host sooner or later
        Set<IpAddress> fixedIps = osPort.getFixedIps().stream()
                .map(ip -> IpAddress.valueOf(ip.getIpAddress()))
                .collect(Collectors.toSet());

        // connect point is the combination of switch ID with port number where
        // the host is attached to
        ConnectPoint connectPoint = new ConnectPoint(port.element().id(), port.number());

        long createTime = System.currentTimeMillis();

        // we check whether the host already attached to same locations
        Host host = hostService.getHost(hostId);

        // build host annotations to include a set of meta info from neutron
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                .set(ANNOTATION_NETWORK_ID, osPort.getNetworkId())
                .set(ANNOTATION_PORT_ID, osPort.getId())
                .set(ANNOTATION_CREATE_TIME, String.valueOf(createTime));

        // FLAT typed network does not require segment ID
        Type netType = osNetworkService.networkType(osNet.getId());
        if (netType != FLAT) {
            annotations.set(ANNOTATION_SEGMENT_ID, osNet.getProviderSegID());
        }

        // build host description object
        HostDescription hostDesc = new DefaultHostDescription(
                mac,
                VlanId.NONE,
                new HostLocation(connectPoint, createTime),
                fixedIps,
                annotations.build());

        if (host != null) {
            Set<HostLocation> locations = host.locations().stream()
                    .filter(l -> l.deviceId().equals(connectPoint.deviceId()))
                    .filter(l -> l.port().equals(connectPoint.port()))
                    .collect(Collectors.toSet());

            // newly added location is not in the existing location list,
            // therefore, we simply add this into the location list
            if (locations.isEmpty()) {
                hostProviderService.addLocationToHost(hostId,
                        new HostLocation(connectPoint, createTime));
            }

            // newly added location is in the existing location list,
            // the hostDetected method invocation in turn triggers host Update event
            if (locations.size() == 1) {
                hostProviderService.hostDetected(hostId, hostDesc, false);
            }
        } else {
            hostProviderService.hostDetected(hostId, hostDesc, false);
        }
    }

    /**
     * Processes port removal event.
     * Once a port removal event is detected, it tries to look for a host
     * instance through host provider by giving connect point information,
     * and vanishes it.
     *
     * @param port ONOS port
     */
    private void processPortRemoved(Port port) {
        ConnectPoint connectPoint = new ConnectPoint(port.element().id(), port.number());

        Set<Host> hosts = hostService.getConnectedHosts(connectPoint);

        hosts.forEach(h -> {
            Optional<HostLocation> hostLocation = h.locations().stream()
                    .filter(l -> l.deviceId().equals(port.element().id()))
                    .filter(l -> l.port().equals(port.number())).findAny();

            // if the host contains only one filtered location, we remove the host
            if (h.locations().size() == 1) {
                hostProviderService.hostVanished(h.id());
            }

            // if the host contains multiple locations, we simply remove the
            // host location
            if (h.locations().size() > 1 && hostLocation.isPresent()) {
                hostProviderService.removeLocationFromHost(h.id(), hostLocation.get());
            }
        });
    }

    /**
     * An internal device listener which listens the port events generated from
     * OVS integration bridge.
     */
    private class InternalDeviceListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            Port port = event.port();
            if (port == null) {
                return false;
            }

            String portName = port.annotations().value(PORT_NAME);

            return !Strings.isNullOrEmpty(portName) &&
                    (portName.startsWith(PORT_NAME_PREFIX_VM) ||
                            isDirectPort(portName) ||
                            portName.startsWith(PORT_NAME_VHOST_USER_PREFIX_VM));
        }

        private boolean isRelevantHelper(DeviceEvent event) {
            return mastershipService.isLocalMaster(event.subject().id());
        }

        private boolean isDirectPort(String portName) {
            return portNamePrefixMap().values().stream().anyMatch(portName::startsWith);
        }

        @Override
        public void event(DeviceEvent event) {
            log.info("Device event occurred with type {}", event.type());
            switch (event.type()) {
                case PORT_UPDATED:
                    executor.execute(() -> processPortUpdate(event));
                    break;
                case PORT_ADDED:
                    executor.execute(() -> processPortAddition(event));
                    break;
                case PORT_REMOVED:
                    executor.execute(() -> processPortRemoval(event));
                    break;
                default:
                    break;
            }
        }

        private void processPortUpdate(DeviceEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            if (!event.port().isEnabled()) {
                portRemovedHelper(event);
            } else if (event.port().isEnabled()) {
                portAddedHelper(event);
            }
        }

        private void processPortAddition(DeviceEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            portAddedHelper(event);
        }

        private void processPortRemoval(DeviceEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            portRemovedHelper(event);
        }
    }

    private class InternalOpenstackNodeListener implements OpenstackNodeListener {

        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {
            return event.subject().type() != CONTROLLER;
        }

        private boolean isRelevantHelper(OpenstackNodeEvent event) {
            // do not allow to proceed without mastership
            Device device = deviceService.getDevice(event.subject().intgBridge());
            if (device == null) {
                return false;
            }
            return mastershipService.isLocalMaster(device.id());
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode osNode = event.subject();

            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                    executor.execute(() -> processCompleteNode(event, event.subject()));
                    break;
                case OPENSTACK_NODE_INCOMPLETE:
                    log.warn("{} is changed to INCOMPLETE state", osNode);
                    break;
                case OPENSTACK_NODE_CREATED:
                case OPENSTACK_NODE_UPDATED:
                case OPENSTACK_NODE_REMOVED:
                default:
                    break;
            }
        }

        private void processCompleteNode(OpenstackNodeEvent event,
                                         OpenstackNode osNode) {

            if (!isRelevantHelper(event)) {
                return;
            }

            log.info("COMPLETE node {} is detected", osNode.hostname());

            deviceService.getPorts(osNode.intgBridge()).stream()
                    .filter(port -> vnicType(port.annotations().value(PORT_NAME))
                                    .equals(Constants.VnicType.NORMAL) ||
                            vnicType(port.annotations().value(PORT_NAME))
                                    .equals(Constants.VnicType.DIRECT))
                    .filter(Port::isEnabled)
                    .forEach(port -> {
                        log.debug("Instance port {} is detected from {}",
                                port.annotations().value(PORT_NAME),
                                osNode.hostname());
                        processPortAdded(port);
                    });

            Tools.stream(hostService.getHosts())
                    .filter(host -> deviceService.getPort(
                            host.location().deviceId(),
                            host.location().port()) == null)
                    .forEach(host -> {
                        log.info("Remove stale host {}", host.id());
                        hostProviderService.hostVanished(host.id());
                    });
        }
    }

    private class InternalNetworkListener implements OpenstackNetworkListener {

        @Override
        public void event(OpenstackNetworkEvent event) {
            switch (event.type()) {
                case OPENSTACK_PORT_CREATED:
                    executor.execute(() -> processOpenstackPortAddition(event));
                    break;
                default:
                    break;
            }
        }

        private void processOpenstackPortAddition(OpenstackNetworkEvent event) {
            String portId = event.port().getId();
            deviceService.getDevices().forEach(device -> {
                deviceService.getPorts(device.id()).stream()
                        .filter(Port::isEnabled)
                        .filter(p -> p.annotations().value(PORT_NAME) != null)
                        .filter(p -> portId.contains(p.annotations().value(PORT_NAME).substring(3)))
                        .filter(p -> !TUNNEL_TYPE.contains(p.annotations().value(PORT_NAME).toUpperCase()))
                        .findAny().ifPresent(OpenstackSwitchingHostProvider.this::processPortAdded);
            });
        }
    }
}
