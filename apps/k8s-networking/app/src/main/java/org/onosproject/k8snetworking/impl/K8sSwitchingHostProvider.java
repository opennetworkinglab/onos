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
package org.onosproject.k8snetworking.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkAdminService;
import org.onosproject.k8snetworking.api.K8sNetworkEvent;
import org.onosproject.k8snetworking.api.K8sNetworkListener;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.k8snode.api.K8sHostService;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeEvent;
import org.onosproject.k8snode.api.K8sNodeListener;
import org.onosproject.k8snode.api.K8sNodeService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.Constants.ANNOTATION_CREATE_TIME;
import static org.onosproject.k8snetworking.api.Constants.ANNOTATION_NETWORK_ID;
import static org.onosproject.k8snetworking.api.Constants.ANNOTATION_PORT_ID;
import static org.onosproject.k8snetworking.api.Constants.ANNOTATION_SEGMENT_ID;
import static org.onosproject.k8snetworking.api.Constants.GENEVE;
import static org.onosproject.k8snetworking.api.Constants.GRE;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.api.Constants.VXLAN;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.allK8sDevices;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.existingContainerPortByMac;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.existingContainerPortByName;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.isContainer;
import static org.onosproject.k8snode.api.K8sNodeState.INIT;
import static org.onosproject.net.AnnotationKeys.PORT_MAC;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;

/**
 * A provider used to feed host information for kubernetes.
 */
@Component(immediate = true, service = HostProvider.class)
public class K8sSwitchingHostProvider extends AbstractProvider implements HostProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ERR_ADD_HOST = "Failed to add host: ";
    private static final String SONA_HOST_SCHEME = "sona-k8s";

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
    protected K8sNetworkAdminService k8sNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeService k8sNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sHostService k8sHostService;

    private HostProviderService hostProviderService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "device-event"));
    private final InternalDeviceListener internalDeviceListener =
            new InternalDeviceListener();
    private final InternalK8sNodeListener internalK8sNodeListener =
            new InternalK8sNodeListener();
    private final InternalK8sNetworkListener internalK8sNetworkListener =
            new InternalK8sNetworkListener();

    /**
     * Creates kubernetes switching host provider.
     */
    public K8sSwitchingHostProvider() {
        super(new ProviderId(SONA_HOST_SCHEME, K8S_NETWORKING_APP_ID));
    }

    @Activate
    protected void activate() {
        coreService.registerApplication(K8S_NETWORKING_APP_ID);
        deviceService.addListener(internalDeviceListener);
        k8sNodeService.addListener(internalK8sNodeListener);
        k8sNetworkService.addListener(internalK8sNetworkListener);
        hostProviderService = hostProviderRegistry.register(this);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostProviderRegistry.unregister(this);
        k8sNetworkService.removeListener(internalK8sNetworkListener);
        k8sNodeService.removeListener(internalK8sNodeListener);
        deviceService.removeListener(internalDeviceListener);

        executor.shutdown();

        log.info("Stopped");
    }


    @Override
    public void triggerProbe(Host host) {
        // no probe is required
    }

    /**
     * Processes port addition event.
     *
     * @param port port object used in ONOS
     */
    private void processPortAdded(Port port) {
        K8sPort k8sPort = portToK8sPortByName(port);
        if (k8sPort == null) {
            k8sPort = portToK8sPortByMac(port);
            if (k8sPort == null) {
                log.warn(ERR_ADD_HOST + "Kubernetes port for {} not found", port);
                return;
            }
        }

        K8sNetwork k8sNet = k8sNetworkService.network(k8sPort.networkId());
        if (k8sNet == null) {
            log.warn(ERR_ADD_HOST + "Kubernetes network {} not found",
                    k8sPort.networkId());
            return;
        }

        MacAddress mac = k8sPort.macAddress();
        HostId hostId = HostId.hostId(mac);

        // connect point is the combination of switch ID with port number where
        // the host is attached to
        ConnectPoint connectPoint = new ConnectPoint(port.element().id(), port.number());

        long createTime = System.currentTimeMillis();

        // update k8s port number by referring to ONOS port number

        k8sNetworkService.updatePort(k8sPort.updatePortNumber(port.number())
                                            .updateState(K8sPort.State.ACTIVE));

        // we check whether the host already attached to same locations
        Host host = hostService.getHost(hostId);

        // build host annotations to include a set of meta info from neutron
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                .set(ANNOTATION_NETWORK_ID, k8sPort.networkId())
                .set(ANNOTATION_PORT_ID, k8sPort.portId())
                .set(ANNOTATION_CREATE_TIME, String.valueOf(createTime))
                .set(ANNOTATION_SEGMENT_ID, k8sNet.segmentId());

        HostDescription hostDesc = new DefaultHostDescription(
                mac,
                VlanId.NONE,
                new HostLocation(connectPoint, createTime),
                ImmutableSet.of(k8sPort.ipAddress()),
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
     *
     * @param port ONOS port
     */
    private void processPortRemoved(Port port) {
        ConnectPoint connectPoint = new ConnectPoint(port.element().id(), port.number());

        Set<Host> hosts = hostService.getConnectedHosts(connectPoint);

        hosts.forEach(h -> hostProviderService.hostVanished(h.id()));

        K8sPort k8sPort = portToK8sPortByName(port);

        if (k8sPort == null) {
            k8sPort = portToK8sPortByMac(port);
            if (k8sPort == null) {
                log.warn(ERR_ADD_HOST + "Kubernetes port for {} not found", port);
                return;
            }
        }

        k8sNetworkService.removePort(k8sPort.portId());
    }

    /**
     * Process port inactivate event.
     *
     * @param port ONOS port
     */
    private void processPortInactivated(Port port) {
        K8sPort k8sPort = portToK8sPortByName(port);

        if (k8sPort == null) {
            k8sPort = portToK8sPortByMac(port);
            if (k8sPort == null) {
                log.warn(ERR_ADD_HOST + "Kubernetes port for {} not found", port);
                return;
            }
        }

        k8sNetworkService.updatePort(k8sPort.updateState(K8sPort.State.INACTIVE));
    }

    /**
     * Converts ONOS port to kubernetes port.
     *
     * @param port ONOS port
     * @return mapped kubernetes port
     */
    private K8sPort portToK8sPortByName(Port port) {
        String portName = port.annotations().value(PORT_NAME);
        if (Strings.isNullOrEmpty(portName)) {
            return null;
        }

        if (isContainer(portName)) {
            return k8sNetworkService.ports().stream()
                    .filter(p -> existingContainerPortByName(p.portId(), portName))
                    .findAny().orElse(null);
        } else {
            return null;
        }
    }

    /**
     * Converts ONOS port to kubernetes port.
     *
     * @param port ONOS port
     * @return mapped kubernetes port
     */
    private K8sPort portToK8sPortByMac(Port port) {
        String portName = port.annotations().value(PORT_NAME);
        String portMac = port.annotations().value(PORT_MAC);
        if (Strings.isNullOrEmpty(portMac) || Strings.isNullOrEmpty(portName)) {
            return null;
        }

        if (isContainer(portName)) {
            return k8sNetworkService.ports().stream()
                    .filter(p -> existingContainerPortByMac(p.macAddress().toString(), portMac))
                    .findAny().orElse(null);
        } else {
            return null;
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            Port port = event.port();
            if (port == null) {
                return false;
            }

            String portName = port.annotations().value(PORT_NAME);
            DeviceId deviceId = event.subject().id();

            return !Strings.isNullOrEmpty(portName) && isContainer(portName) &&
                    allK8sDevices(k8sNodeService, k8sHostService).contains(deviceId);
        }

        private boolean isRelevantHelper(DeviceEvent event) {
            return mastershipService.isLocalMaster(event.subject().id());
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

            log.debug("K8s port {} is updated at {}",
                    event.port().annotations().value(PORT_NAME),
                    event.subject().id());

            if (!event.port().isEnabled()) {
                processPortRemoval(event);
            } else if (event.port().isEnabled()) {
                processPortAddition(event);
            }
        }

        private void processPortAddition(DeviceEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            log.debug("K8s port {} is detected from {}",
                    event.port().annotations().value(PORT_NAME),
                    event.subject().id());

            processPortAdded(event.port());
        }

        private void processPortRemoval(DeviceEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            log.debug("K8s port {} is removed from {}",
                    event.port().annotations().value(PORT_NAME),
                    event.subject().id());

            processPortRemoved(event.port());
        }
    }

    private class InternalK8sNodeListener implements K8sNodeListener {

        private boolean isRelevantHelper(K8sNodeEvent event) {
            // do not allow to proceed without mastership
            Device device = deviceService.getDevice(event.subject().intgBridge());
            if (device == null) {
                return false;
            }
            return mastershipService.isLocalMaster(device.id());
        }

        @Override
        public void event(K8sNodeEvent event) {
            K8sNode k8sNode = event.subject();

            switch (event.type()) {
                case K8S_NODE_COMPLETE:
                    executor.execute(() -> processCompleteNode(event, k8sNode));
                    break;
                case K8S_NODE_UPDATED:
                    if (k8sNode.state() == INIT) {
                        executor.execute(() -> processIncompleteNode(event, k8sNode));
                    }
                    break;
                case K8S_NODE_CREATED:
                case K8S_NODE_REMOVED:
                case K8S_NODE_INCOMPLETE:
                default:
                    break;
            }
        }

        private void processCompleteNode(K8sNodeEvent event, K8sNode k8sNode) {
            if (!isRelevantHelper(event)) {
                return;
            }

            log.info("COMPLETE node {} is detected", k8sNode.hostname());

            deviceService.getPorts(k8sNode.intgBridge()).stream()
                    .filter(port -> isContainer(port.annotations().value(PORT_NAME)))
                    .filter(Port::isEnabled)
                    .forEach(port -> {
                        log.debug("Container port {} is detected from {}",
                                port.annotations().value(PORT_NAME),
                                k8sNode.hostname());
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

        private void processIncompleteNode(K8sNodeEvent event, K8sNode k8sNode) {
            if (!isRelevantHelper(event)) {
                return;
            }

            log.info("INIT node {} is detected", k8sNode.hostname());

            deviceService.getPorts(k8sNode.intgBridge()).stream()
                    .filter(port -> isContainer(port.annotations().value(PORT_NAME)))
                    .filter(Port::isEnabled)
                    .forEach(port -> {
                        log.debug("Container port {} is detected from {}",
                                port.annotations().value(PORT_NAME),
                                k8sNode.hostname());
                        processPortInactivated(port);
                    });
        }
    }

    private class InternalK8sNetworkListener implements K8sNetworkListener {

        @Override
        public void event(K8sNetworkEvent event) {
            switch (event.type()) {
                case K8S_PORT_CREATED:
                    executor.execute(() -> processK8sPortAddition(event));
                    break;
                default:
                    break;
            }
        }

        private void processK8sPortAddition(K8sNetworkEvent event) {
            String mac = event.port().macAddress().toString();
            for (Device device : deviceService.getDevices()) {
                Port port = deviceService.getPorts(device.id()).stream()
                        .filter(Port::isEnabled)
                        .filter(p -> p.annotations().value(PORT_MAC) != null)
                        .filter(p -> p.annotations().value(PORT_NAME) != null)
                        .filter(p -> existingContainerPortByMac(mac, p.annotations().value(PORT_MAC)))
                        .findAny().orElse(null);

                if (port != null) {
                    String upperPortName = port.annotations().value(PORT_NAME).toUpperCase();
                    // we do not handle tunnel typed port
                    if (upperPortName.contains(VXLAN) || upperPortName.contains(GRE) ||
                            upperPortName.contains(GENEVE)) {
                        continue;
                    }

                    // if we have null device ID, we simply update the device ID on the k8s port
                    if (event.port().deviceId() == null) {
                        K8sPort updated = event.port().updateDeviceId(device.id());
                        k8sNetworkService.updatePort(updated);
                    }

                    processPortAdded(port);
                }
            }
        }
    }
}
