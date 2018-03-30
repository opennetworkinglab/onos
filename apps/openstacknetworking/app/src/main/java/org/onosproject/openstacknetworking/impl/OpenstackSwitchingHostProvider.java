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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
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
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.impl.HostBasedInstancePort.ANNOTATION_CREATE_TIME;
import static org.onosproject.openstacknetworking.impl.HostBasedInstancePort.ANNOTATION_NETWORK_ID;
import static org.onosproject.openstacknetworking.impl.HostBasedInstancePort.ANNOTATION_PORT_ID;

@Service
@Component(immediate = true)
public final class OpenstackSwitchingHostProvider extends AbstractProvider implements HostProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String PORT_NAME_PREFIX_VM = "tap";
    private static final String ERR_ADD_HOST = "Failed to add host: ";
    private static final String ANNOTATION_SEGMENT_ID = "segId";
    private static final String SONA_HOST_SCHEME = "sona";

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
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    private final ExecutorService deviceEventExecutor =
            Executors.newSingleThreadExecutor(groupedThreads("openstacknetworking", "device-event"));
    private final InternalDeviceListener internalDeviceListener = new InternalDeviceListener();
    private final InternalOpenstackNodeListener internalNodeListener = new InternalOpenstackNodeListener();

    private HostProviderService hostProvider;

    /**
     * Creates OpenStack switching host provider.
     */
    public OpenstackSwitchingHostProvider() {
        super(new ProviderId(SONA_HOST_SCHEME, OPENSTACK_NETWORKING_APP_ID));
    }

    @Activate
    void activate() {
        coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        deviceService.addListener(internalDeviceListener);
        osNodeService.addListener(internalNodeListener);
        hostProvider = hostProviderRegistry.register(this);

        log.info("Started");
    }

    @Deactivate
    void deactivate() {
        hostProviderRegistry.unregister(this);
        osNodeService.removeListener(internalNodeListener);
        deviceService.removeListener(internalDeviceListener);

        deviceEventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void triggerProbe(Host host) {
        // no probe is required
    }

    /**
     * Processes port addition event.
     * Once a port addition event is detected, it tries to create a host instance
     * with openstack augmented host information such as networkId, portId,
     * createTime, segmentId and notifies to host provider.
     *
     * @param port port object used in ONOS
     */
    private void processPortAdded(Port port) {
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

        MacAddress macAddr = MacAddress.valueOf(osPort.getMacAddress());
        Set<IpAddress> fixedIps = osPort.getFixedIps().stream()
                .map(ip -> IpAddress.valueOf(ip.getIpAddress()))
                .collect(Collectors.toSet());
        ConnectPoint connectPoint = new ConnectPoint(port.element().id(), port.number());

        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                .set(ANNOTATION_NETWORK_ID, osPort.getNetworkId())
                .set(ANNOTATION_PORT_ID, osPort.getId())
                .set(ANNOTATION_CREATE_TIME, String.valueOf(System.currentTimeMillis()));

        if (osNet.getNetworkType() != NetworkType.FLAT) {
            annotations.set(ANNOTATION_SEGMENT_ID, osNet.getProviderSegID());

        }

        HostDescription hostDesc = new DefaultHostDescription(
                macAddr,
                VlanId.NONE,
                new HostLocation(connectPoint, System.currentTimeMillis()),
                fixedIps,
                annotations.build());

        HostId hostId = HostId.hostId(macAddr);
        hostProvider.hostDetected(hostId, hostDesc, false);
    }

    /**
     * Processes port removal event.
     * Once a port removal event is detected, it tries to look for a host
     * instance through host provider by giving connect point information,
     * and vanishes it.
     *
     * @param port port object used in ONOS
     */
    private void processPortRemoved(Port port) {
        ConnectPoint connectPoint = new ConnectPoint(port.element().id(), port.number());
        hostService.getConnectedHosts(connectPoint)
                    .forEach(host -> hostProvider.hostVanished(host.id()));
    }

    /**
     * An internal device listener which listens the port events generated from
     * OVS integration bridge.
     */
    private class InternalDeviceListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            Device device = event.subject();
            if (!mastershipService.isLocalMaster(device.id())) {
                // do not allow to proceed without mastership
                return false;
            }
            Port port = event.port();
            if (port == null) {
                return false;
            }
            String portName = port.annotations().value(PORT_NAME);

            return !Strings.isNullOrEmpty(portName) &&
                    portName.startsWith(PORT_NAME_PREFIX_VM);
        }

        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case PORT_UPDATED:
                    if (!event.port().isEnabled()) {
                        portRemovedHelper(deviceEventExecutor, event);
                    } else if (event.port().isEnabled()) {
                        portAddedHelper(deviceEventExecutor, event);
                    }
                    break;
                case PORT_ADDED:
                    portAddedHelper(deviceEventExecutor, event);
                    break;
                case PORT_REMOVED:
                    portRemovedHelper(deviceEventExecutor, event);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * A helper method which logs the port addition event and performs port
     * addition action.
     *
     * @param executor device executor service
     * @param event device event
     */
    private void portAddedHelper(ExecutorService executor, DeviceEvent event) {
        executor.execute(() -> {
            log.debug("Instance port {} is detected from {}",
                    event.port().annotations().value(PORT_NAME),
                    event.subject().id());
            processPortAdded(event.port());
        });
    }

    /**
     * A helper method which logs the port removal event and performs port
     * removal action.
     *
     * @param executor device executor service
     * @param event device event
     */
    private void portRemovedHelper(ExecutorService executor, DeviceEvent event) {
        executor.execute(() -> {
            log.debug("Instance port {} is removed from {}",
                    event.port().annotations().value(PORT_NAME),
                    event.subject().id());
            processPortRemoved(event.port());
        });
    }

    private class InternalOpenstackNodeListener implements OpenstackNodeListener {

        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {
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
                    deviceEventExecutor.execute(() -> {
                        log.info("COMPLETE node {} is detected", osNode.hostname());
                        processCompleteNode(event.subject());
                    });
                    break;
                case OPENSTACK_NODE_INCOMPLETE:
                    log.warn("{} is changed to INCOMPLETE state", osNode);
                    break;
                case OPENSTACK_NODE_CREATED:
                case OPENSTACK_NODE_UPDATED:
                case OPENSTACK_NODE_REMOVED:
                    // not reacts to the events other than complete and incomplete states
                    break;
                default:
                    log.warn("Unsupported openstack node event type");
                    break;
            }
        }

        private void processCompleteNode(OpenstackNode osNode) {
            deviceService.getPorts(osNode.intgBridge()).stream()
                    .filter(port -> port.annotations().value(PORT_NAME)
                            .startsWith(PORT_NAME_PREFIX_VM) &&
                            port.isEnabled())
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
                        hostProvider.hostVanished(host.id());
                });
        }
    }
}
