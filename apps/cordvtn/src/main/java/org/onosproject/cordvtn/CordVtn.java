/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.cordvtn;

import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Port;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openstackswitching.OpenstackNetwork;
import org.onosproject.openstackswitching.OpenstackPort;
import org.onosproject.openstackswitching.OpenstackSubnet;
import org.onosproject.openstackswitching.OpenstackSwitchingService;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provisions virtual tenant networks with service chaining capability
 * in OpenStack environment.
 */
@Component(immediate = true)
@Service
public class CordVtn extends AbstractProvider implements CordVtnService, HostProvider {

    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry hostProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackSwitchingService openstackService;

    private static final int NUM_THREADS = 1;
    private static final String DEFAULT_TUNNEL = "vxlan";
    private static final String SERVICE_ID = "serviceId";
    private static final String LOCATION_IP = "locationIp";
    private static final String OPENSTACK_VM_ID = "openstackVmId";

    private final ExecutorService eventExecutor = Executors
            .newFixedThreadPool(NUM_THREADS, groupedThreads("onos/cordvtn", "event-handler"));

    private final PacketProcessor packetProcessor = new InternalPacketProcessor();
    private final HostListener hostListener = new InternalHostListener();

    private HostProviderService hostProvider;
    private CordVtnRuleInstaller ruleInstaller;
    private CordVtnArpProxy arpProxy;

    /**
     * Creates an cordvtn host location provider.
     */
    public CordVtn() {
        super(new ProviderId("host", CORDVTN_APP_ID));
    }

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication("org.onosproject.cordvtn");

        ruleInstaller = new CordVtnRuleInstaller(appId, flowRuleService,
                                                 deviceService,
                                                 driverService,
                                                 groupService,
                                                 mastershipService,
                                                 DEFAULT_TUNNEL);

        arpProxy = new CordVtnArpProxy(appId, packetService);
        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));
        arpProxy.requestPacket();

        hostService.addListener(hostListener);
        hostProvider = hostProviderRegistry.register(this);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostService.removeListener(hostListener);
        packetService.removeProcessor(packetProcessor);

        eventExecutor.shutdown();
        hostProviderRegistry.unregister(this);

        log.info("Stopped");
    }

    @Override
    public void triggerProbe(Host host) {
        /*
         * Note: In CORD deployment, we assume that all hosts are configured.
         * Therefore no probe is required.
         */
    }

    @Override
    public void createServiceDependency(CordServiceId tServiceId, CordServiceId pServiceId) {
        CordService tService = getCordService(tServiceId);
        CordService pService = getCordService(pServiceId);

        if (tService == null || pService == null) {
            log.error("Failed to create CordService for {}", tServiceId.id());
            return;
        }

        log.info("Service dependency from {} to {} created.", tService.id().id(), pService.id().id());
        ruleInstaller.populateServiceDependencyRules(tService, pService);
    }

    @Override
    public void removeServiceDependency(CordServiceId tServiceId, CordServiceId pServiceId) {
        CordService tService = getCordService(tServiceId);
        CordService pService = getCordService(pServiceId);

        if (tService == null || pService == null) {
            log.error("Failed to create CordService for {}", tServiceId.id());
            return;
        }

        log.info("Service dependency from {} to {} removed.", tService.id().id(), pService.id().id());
        ruleInstaller.removeServiceDependencyRules(tService, pService);
    }

    @Override
    public void addServiceVm(CordVtnNode node, ConnectPoint connectPoint) {
        Port port = deviceService.getPort(connectPoint.deviceId(), connectPoint.port());
        OpenstackPort vPort = openstackService.port(port);
        if (vPort == null) {
            log.warn("Failed to get OpenstackPort for {}", getPortName(port));
            return;
        }

        MacAddress mac = vPort.macAddress();
        HostId hostId = HostId.hostId(mac);

        Host host = hostService.getHost(hostId);
        if (host != null) {
            // Host is already known to the system, no HOST_ADDED event is triggered in this case.
            // It happens when the application is restarted.
            // TODO check host description if it has all the information
            serviceVmAdded(host);
            return;
        }

        Set<IpAddress> ip = Sets.newHashSet(vPort.fixedIps().values());
        SparseAnnotations annotations = DefaultAnnotations.builder()
                .set(OPENSTACK_VM_ID, vPort.deviceId())
                .set(SERVICE_ID, vPort.networkId())
                .set(LOCATION_IP, node.localIp().toString())
                .build();

        HostDescription hostDesc = new DefaultHostDescription(
                mac,
                VlanId.NONE,
                new HostLocation(connectPoint, System.currentTimeMillis()),
                ip,
                annotations);

        hostProvider.hostDetected(hostId, hostDesc, false);
    }

    @Override
    public void removeServiceVm(ConnectPoint connectPoint) {
        Host host = hostService.getConnectedHosts(connectPoint)
                .stream()
                .findFirst()
                .orElse(null);

        if (host == null) {
            log.debug("No host is connected on {}", connectPoint.toString());
            return;
        }

        hostProvider.hostVanished(host.id());
    }

    /**
     * Returns CordService by service ID.
     *
     * @param serviceId service id
     * @return cord service, or null if it fails to get network from OpenStack
     */
    private CordService getCordService(CordServiceId serviceId) {
        OpenstackNetwork vNet = openstackService.network(serviceId.id());
        if (vNet == null) {
            log.warn("Couldn't find OpenStack network for service {}", serviceId.id());
            return null;
        }

        OpenstackSubnet subnet = vNet.subnets().stream()
                .findFirst()
                .orElse(null);
        if (subnet == null) {
            log.warn("Couldn't find OpenStack subnet for service {}", serviceId.id());
            return null;
        }

        Set<CordServiceId> tServices = Sets.newHashSet();
        // TODO get tenant services from XOS

        Map<Host, IpAddress> hosts = getHostsWithOpenstackNetwork(vNet)
                .stream()
                .collect(Collectors.toMap(host -> host, this::getTunnelIp));

        return new CordService(vNet, subnet, hosts, tServices);
    }

    /**
     * Returns CordService by OpenStack network.
     *
     * @param vNet OpenStack network
     * @return cord service
     */
    private CordService getCordService(OpenstackNetwork vNet) {
        checkNotNull(vNet);

        CordServiceId serviceId = CordServiceId.of(vNet.id());
        OpenstackSubnet subnet = vNet.subnets().stream()
                .findFirst()
                .orElse(null);
        if (subnet == null) {
            log.warn("Couldn't find OpenStack subnet for service {}", serviceId);
            return null;
        }

        Set<CordServiceId> tServices = Sets.newHashSet();
        // TODO get tenant services from XOS

        Map<Host, IpAddress> hosts = getHostsWithOpenstackNetwork(vNet)
                .stream()
                .collect(Collectors.toMap(host -> host, this::getTunnelIp));

        return new CordService(vNet, subnet, hosts, tServices);
    }

    /**
     * Returns IP address for tunneling for a given host.
     *
     * @param host host
     * @return ip address
     */
    private IpAddress getTunnelIp(Host host) {
        return IpAddress.valueOf(host.annotations().value(LOCATION_IP));
    }

    /**
     * Returns port name.
     *
     * @param port port
     * @return port name
     */
    private String getPortName(Port port) {
        return port.annotations().value("portName");
    }

    /**
     * Returns hosts associated with a given OpenStack network.
     *
     * @param vNet openstack network
     * @return set of hosts
     */
    private Set<Host> getHostsWithOpenstackNetwork(OpenstackNetwork vNet) {
        checkNotNull(vNet);

        Set<Host> hosts = openstackService.ports(vNet.id()).stream()
                .filter(port -> port.deviceOwner().contains("compute"))
                .map(port -> hostService.getHostsByMac(port.macAddress())
                        .stream()
                        .findFirst()
                        .orElse(null))
                .collect(Collectors.toSet());

        hosts.remove(null);
        return hosts;
    }

    /**
     * Handles VM detected situation.
     *
     * @param host host
     */
    private void serviceVmAdded(Host host) {
        String vNetId = host.annotations().value(SERVICE_ID);
        OpenstackNetwork vNet = openstackService.network(vNetId);
        if (vNet == null) {
            log.warn("Failed to get OpenStack network {} for VM {}({}).",
                     vNetId,
                     host.id(),
                     host.annotations().value(OPENSTACK_VM_ID));
            return;
        }

        log.info("VM {} is detected, MAC: {} IP: {}",
                 host.annotations().value(OPENSTACK_VM_ID),
                 host.mac(),
                 host.ipAddresses().stream().findFirst().get());

        CordService service = getCordService(vNet);
        if (service != null) {
            // TODO check if the service needs an update on its group buckets after done CORD-433
            ruleInstaller.updateServiceGroup(service);
            arpProxy.addServiceIp(service.serviceIp());
        }

        ruleInstaller.populateBasicConnectionRules(host, getTunnelIp(host), vNet);
    }

    /**
     * Handles VM removed situation.
     *
     * @param host host
     */
    private void serviceVmRemoved(Host host) {
        String vNetId = host.annotations().value(SERVICE_ID);
        OpenstackNetwork vNet = openstackService.network(host.annotations().value(SERVICE_ID));
        if (vNet == null) {
            log.warn("Failed to get OpenStack network {} for VM {}({}).",
                     vNetId,
                     host.id(),
                     host.annotations().value(OPENSTACK_VM_ID));
            return;
        }

        log.info("VM {} is vanished, MAC: {} IP: {}",
                 host.annotations().value(OPENSTACK_VM_ID),
                 host.mac(),
                 host.ipAddresses().stream().findFirst().get());

        ruleInstaller.removeBasicConnectionRules(host);

        CordService service = getCordService(vNet);
        if (service != null) {
            // TODO check if the service needs an update on its group buckets after done CORD-433
            ruleInstaller.updateServiceGroup(service);

            if (getHostsWithOpenstackNetwork(vNet).isEmpty()) {
                arpProxy.removeServiceIp(service.serviceIp());
            }
        }
    }

    private class InternalHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            Host host = event.subject();

            switch (event.type()) {
                case HOST_ADDED:
                    eventExecutor.submit(() -> serviceVmAdded(host));
                    break;
                case HOST_REMOVED:
                    eventExecutor.submit(() -> serviceVmRemoved(host));
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            Ethernet ethPacket = context.inPacket().parsed();
            if (ethPacket == null) {
                return;
            }

            if (ethPacket.getEtherType() != Ethernet.TYPE_ARP) {
                return;
            }

            arpProxy.processArpPacket(context, ethPacket);
        }
    }
}
