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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeEvent;
import org.onosproject.k8snode.api.K8sNodeListener;
import org.onosproject.k8snode.api.K8sNodeService;
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
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.api.Constants.PORT_NAME_PREFIX_CONTAINER;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.isContainer;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;

/**
 * A provider used to feed host information for kubernetes.
 */
@Service
@Component(immediate = true)
public class K8sSwitchingHostProvider extends AbstractProvider implements HostProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ERR_ADD_HOST = "Failed to add host: ";
    private static final String SONA_HOST_SCHEME = "sona";
    private static final int PORT_PREFIX_LENGTH = 3;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry hostProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected K8sNetworkService k8sNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected K8sNodeService k8sNodeService;

    private HostProviderService hostProviderService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "device-event"));
    private final InternalDeviceListener internalDeviceListener =
            new InternalDeviceListener();
    private final InternalK8sNodeListener internalK8sNodeListener =
            new InternalK8sNodeListener();

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
        hostProviderService = hostProviderRegistry.register(this);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostProviderRegistry.unregister(this);
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
        K8sPort k8sPort = portToK8sPort(port);
        if (k8sPort == null) {
            log.warn(ERR_ADD_HOST + "Kubernetes port for {} not found", port);
            return;
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
    }

    /**
     * Converts ONOS port to kubernetes port.
     *
     * @param port ONOS port
     * @return mapped kubernetes port
     */
    private K8sPort portToK8sPort(Port port) {
        String portName = port.annotations().value(PORT_NAME);
        if (Strings.isNullOrEmpty(portName)) {
            return null;
        }

        if (isContainer(portName)) {
            return k8sNetworkService.ports().stream()
                    .filter(p -> p.portId().contains(portName.substring(PORT_PREFIX_LENGTH)))
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

            return !Strings.isNullOrEmpty(portName) &&
                    portName.startsWith(PORT_NAME_PREFIX_CONTAINER);
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

            processPortAddition(event);
        }

        private void processPortRemoval(DeviceEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            processPortRemoval(event);
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
                    executor.execute(() -> processCompleteNode(event, event.subject()));
                    break;
                case K8S_NODE_INCOMPLETE:
                    log.warn("{} is changed to INCOMPLETE state", k8sNode);
                    break;
                case K8S_NODE_CREATED:
                case K8S_NODE_UPDATED:
                case K8S_NODE_REMOVED:
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
    }
}
