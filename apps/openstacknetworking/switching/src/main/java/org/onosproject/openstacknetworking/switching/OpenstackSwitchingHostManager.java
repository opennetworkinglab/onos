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
package org.onosproject.openstacknetworking.switching;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcp.DhcpService;
import org.onosproject.dhcp.IpAssignment;
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
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackNetwork;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstackinterface.OpenstackSubnet;
import org.onosproject.openstacknode.OpenstackNode;
import org.onosproject.openstacknode.OpenstackNodeEvent;
import org.onosproject.openstacknode.OpenstackNodeListener;
import org.onosproject.openstacknode.OpenstackNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.dhcp.IpAssignment.AssignmentStatus.Option_RangeNotEnforced;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknetworking.Constants.*;

@Service
@Component(immediate = true)
public final class OpenstackSwitchingHostManager extends AbstractProvider
        implements HostProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry hostProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DhcpService dhcpService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackInterfaceService openstackService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService openstackNodeService;

    private final ExecutorService deviceEventExecutor =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackswitching", "device-event"));
    private final ExecutorService configEventExecutor =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackswitching", "config-event"));
    private final InternalDeviceListener internalDeviceListener = new InternalDeviceListener();
    private final InternalOpenstackNodeListener internalNodeListener = new InternalOpenstackNodeListener();

    private HostProviderService hostProvider;

    /**
     * Creates OpenStack switching host provider.
     */
    public OpenstackSwitchingHostManager() {
        super(new ProviderId("host", SWITCHING_APP_ID));
    }

    @Activate
    protected void activate() {
        coreService.registerApplication(SWITCHING_APP_ID);
        deviceService.addListener(internalDeviceListener);
        openstackNodeService.addListener(internalNodeListener);
        hostProvider = hostProviderRegistry.register(this);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostProviderRegistry.unregister(this);
        deviceService.removeListener(internalDeviceListener);
        openstackNodeService.removeListener(internalNodeListener);

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
        OpenstackPort osPort = openstackService.port(port);
        if (osPort == null) {
            log.warn("Failed to get OpenStack port for {}", port);
            return;
        }

        OpenstackNetwork osNet = openstackService.network(osPort.networkId());
        if (osNet == null) {
            log.warn("Failed to get OpenStack network {}",
                    osPort.networkId());
            return;
        }

        OpenstackSubnet openstackSubnet = openstackService.subnets().stream()
                .filter(n -> n.networkId().equals(osPort.networkId()))
                .findFirst().orElse(null);
        if (openstackSubnet == null) {
            log.warn("Failed to find subnet for {}", osPort);
            return;
        }

        registerDhcpInfo(osPort, openstackSubnet);
        ConnectPoint connectPoint = new ConnectPoint(port.element().id(), port.number());
        // TODO remove gateway IP from host annotation
        Map.Entry<String, Ip4Address> fixedIp = osPort.fixedIps().entrySet().stream().findFirst().get();

        // Added CREATE_TIME intentionally to trigger HOST_UPDATED event for the
        // existing instances.
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                .set(NETWORK_ID, osPort.networkId())
                .set(SUBNET_ID, fixedIp.getKey())
                .set(PORT_ID, osPort.id())
                .set(VXLAN_ID, osNet.segmentId())
                .set(TENANT_ID, osNet.tenantId())
                // TODO remove gateway IP from host annotation
                .set(GATEWAY_IP, openstackSubnet.gatewayIp())
                .set(CREATE_TIME, String.valueOf(System.currentTimeMillis()));

        HostDescription hostDesc = new DefaultHostDescription(
                osPort.macAddress(),
                VlanId.NONE,
                new HostLocation(connectPoint, System.currentTimeMillis()),
                Sets.newHashSet(osPort.fixedIps().values()),
                annotations.build());

        HostId hostId = HostId.hostId(osPort.macAddress());
        hostProvider.hostDetected(hostId, hostDesc, false);
    }

    private void processPortRemoved(Port port) {
        ConnectPoint connectPoint = new ConnectPoint(port.element().id(), port.number());
        removeHosts(connectPoint);
    }

    private void removeHosts(ConnectPoint connectPoint) {
        hostService.getConnectedHosts(connectPoint).stream()
                .forEach(host -> {
                    dhcpService.removeStaticMapping(host.mac());
                    hostProvider.hostVanished(host.id());
                });
    }

    private void registerDhcpInfo(OpenstackPort openstackPort, OpenstackSubnet openstackSubnet) {
        checkNotNull(openstackPort);
        checkNotNull(openstackSubnet);
        checkArgument(!openstackPort.fixedIps().isEmpty());

        Ip4Address ipAddress = openstackPort.fixedIps().values().stream().findFirst().get();
        IpPrefix subnetPrefix = IpPrefix.valueOf(openstackSubnet.cidr());
        Ip4Address broadcast = Ip4Address.makeMaskedAddress(
                ipAddress,
                subnetPrefix.prefixLength());

        // TODO: supports multiple DNS servers
        Ip4Address domainServer = openstackSubnet.dnsNameservers().isEmpty() ?
                DNS_SERVER_IP : openstackSubnet.dnsNameservers().get(0);

        IpAssignment ipAssignment = IpAssignment.builder()
                .ipAddress(ipAddress)
                .leasePeriod(DHCP_INFINITE_LEASE)
                .timestamp(new Date())
                .subnetMask(Ip4Address.makeMaskPrefix(subnetPrefix.prefixLength()))
                .broadcast(broadcast)
                .domainServer(domainServer)
                .assignmentStatus(Option_RangeNotEnforced)
                .routerAddress(Ip4Address.valueOf(openstackSubnet.gatewayIp()))
                .build();

        dhcpService.setStaticMapping(openstackPort.macAddress(), ipAssignment);
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            if (!mastershipService.isLocalMaster(device.id())) {
                // do not allow to proceed without mastership
                return;
            }

            Port port = event.port();
            if (port == null) {
                return;
            }

            String portName = port.annotations().value(PORT_NAME);
            if (Strings.isNullOrEmpty(portName) ||
                    !portName.startsWith(PORT_NAME_PREFIX_VM)) {
                // handles VM connected port event only
                return;
            }

            switch (event.type()) {
                case PORT_UPDATED:
                    if (!event.port().isEnabled()) {
                        deviceEventExecutor.execute(() -> processPortRemoved(event.port()));
                    }
                    break;
                case PORT_ADDED:
                    deviceEventExecutor.execute(() -> processPortAdded(event.port()));
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalOpenstackNodeListener implements OpenstackNodeListener {

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode node = event.node();
            // TODO check leadership of the node and make only the leader process

            switch (event.type()) {
                case COMPLETE:
                    log.info("COMPLETE node {} detected", node.hostname());

                    // adds existing VMs running on the complete state node
                    deviceService.getPorts(node.intBridge()).stream()
                            .filter(port -> port.annotations().value(PORT_NAME)
                                    .startsWith(PORT_NAME_PREFIX_VM) &&
                                    port.isEnabled())
                            .forEach(port -> {
                                deviceEventExecutor.execute(() -> processPortAdded(port));
                                log.info("VM is detected on {}", port);
                            });

                    // removes stale VMs
                    hostService.getHosts().forEach(host -> {
                        if (deviceService.getPort(host.location().deviceId(),
                                host.location().port()) == null) {
                            deviceEventExecutor.execute(() -> removeHosts(host.location()));
                            log.info("Removed stale VM {}", host.location());
                        }
                    });
                    break;
                case INCOMPLETE:
                    log.warn("{} is changed to INCOMPLETE state", node);
                    break;
                case INIT:
                case DEVICE_CREATED:
                default:
                    break;
            }
        }
    }
}
