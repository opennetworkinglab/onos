/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.onosproject.openstacknode.OpenstackNode;
import org.onosproject.openstacknode.OpenstackNodeEvent;
import org.onosproject.openstacknode.OpenstackNodeListener;
import org.onosproject.openstacknode.OpenstackNodeService;
import org.openstack4j.model.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknetworking.api.Constants.*;
import static org.onosproject.openstacknetworking.impl.HostBasedInstancePort.ANNOTATION_CREATE_TIME;
import static org.onosproject.openstacknetworking.impl.HostBasedInstancePort.ANNOTATION_NETWORK_ID;
import static org.onosproject.openstacknetworking.impl.HostBasedInstancePort.ANNOTATION_PORT_ID;

@Service
@Component(immediate = true)
public final class OpenstackSwitchingHostProvider extends AbstractProvider implements HostProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String PORT_NAME_PREFIX_VM = "tap";
    private static final String ERR_ADD_HOST = "Failed to add host: ";

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
    private final ExecutorService configEventExecutor =
            Executors.newSingleThreadExecutor(groupedThreads("openstacknetworking", "config-event"));
    private final InternalDeviceListener internalDeviceListener = new InternalDeviceListener();
    private final InternalOpenstackNodeListener internalNodeListener = new InternalOpenstackNodeListener();

    private HostProviderService hostProvider;

    /**
     * Creates OpenStack switching host provider.
     */
    public OpenstackSwitchingHostProvider() {
        super(new ProviderId("sona", OPENSTACK_NETWORKING_APP_ID));
    }

    @Activate
    protected void activate() {
        coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        deviceService.addListener(internalDeviceListener);
        osNodeService.addListener(internalNodeListener);
        hostProvider = hostProviderRegistry.register(this);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostProviderRegistry.unregister(this);
        osNodeService.removeListener(internalNodeListener);
        deviceService.removeListener(internalDeviceListener);

        deviceEventExecutor.shutdown();
        configEventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void triggerProbe(Host host) {
        // no probe is required
    }

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

        HostDescription hostDesc = new DefaultHostDescription(
                macAddr,
                VlanId.NONE,
                new HostLocation(connectPoint, System.currentTimeMillis()),
                fixedIps,
                annotations.build());

        HostId hostId = HostId.hostId(macAddr);
        hostProvider.hostDetected(hostId, hostDesc, false);
    }

    private void processPortRemoved(Port port) {
        ConnectPoint connectPoint = new ConnectPoint(port.element().id(), port.number());
        hostService.getConnectedHosts(connectPoint).forEach(host -> {
                    hostProvider.hostVanished(host.id());
                });
    }

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
            if (Strings.isNullOrEmpty(portName) ||
                    !portName.startsWith(PORT_NAME_PREFIX_VM)) {
                // handles Nova created port event only
                return false;
            }
            return true;
        }

        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case PORT_UPDATED:
                    if (!event.port().isEnabled()) {
                        deviceEventExecutor.execute(() -> {
                            log.debug("Instance port {} is removed from {}",
                                     event.port().annotations().value(PORT_NAME),
                                     event.subject().id());
                            processPortRemoved(event.port());
                        });
                    } else if (event.port().isEnabled()) {
                        deviceEventExecutor.execute(() -> {
                            log.debug("Instance Port {} is detected from {}",
                                     event.port().annotations().value(PORT_NAME),
                                     event.subject().id());
                            processPortAdded(event.port());
                        });
                    }
                    break;
                case PORT_ADDED:
                    deviceEventExecutor.execute(() -> {
                        log.debug("Instance port {} is detected from {}",
                                 event.port().annotations().value(PORT_NAME),
                                 event.subject().id());
                        processPortAdded(event.port());
                    });
                    break;
                case PORT_REMOVED:
                    deviceEventExecutor.execute(() -> {
                        log.debug("Instance port {} is removed from {}",
                                event.port().annotations().value(PORT_NAME),
                                event.subject().id());
                        processPortRemoved(event.port());
                    });
                default:
                    break;
            }
        }
    }

    private class InternalOpenstackNodeListener implements OpenstackNodeListener {

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode osNode = event.subject();
            // TODO check leadership of the node and make only the leader process

            switch (event.type()) {
                case COMPLETE:
                    deviceEventExecutor.execute(() -> {
                        log.info("COMPLETE node {} is detected", osNode.hostname());
                        processCompleteNode(event.subject());
                    });
                    break;
                case INCOMPLETE:
                    log.warn("{} is changed to INCOMPLETE state", osNode);
                    break;
                case INIT:
                case DEVICE_CREATED:
                default:
                    break;
            }
        }

        private void processCompleteNode(OpenstackNode osNode) {
            deviceService.getPorts(osNode.intBridge()).stream()
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
