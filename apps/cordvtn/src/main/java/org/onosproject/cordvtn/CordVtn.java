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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcp.DhcpService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Port;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
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
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackNetwork;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstackinterface.OpenstackSubnet;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
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
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

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
    protected OpenstackInterfaceService openstackService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DhcpService dhcpService;

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, CordVtnConfig.class, "cordvtn") {
                @Override
                public CordVtnConfig createConfig() {
                    return new CordVtnConfig();
                }
            };

    private static final String DEFAULT_TUNNEL = "vxlan";
    private static final String SERVICE_ID = "serviceId";
    private static final String OPENSTACK_PORT_ID = "openstackPortId";
    private static final String DATA_PLANE_IP = "dataPlaneIp";
    private static final String DATA_PLANE_INTF = "dataPlaneIntf";
    private static final String S_TAG = "stag";
    private static final String VSG_HOST_ID = "vsgHostId";
    private static final String CREATED_TIME = "createdTime";

    private static final Ip4Address DEFAULT_DNS = Ip4Address.valueOf("8.8.8.8");

    private final ExecutorService eventExecutor =
            newSingleThreadScheduledExecutor(groupedThreads("onos/cordvtn", "event-handler"));

    private final PacketProcessor packetProcessor = new InternalPacketProcessor();
    private final HostListener hostListener = new InternalHostListener();
    private final NetworkConfigListener configListener = new InternalConfigListener();

    private ApplicationId appId;
    private HostProviderService hostProvider;
    private CordVtnRuleInstaller ruleInstaller;
    private CordVtnArpProxy arpProxy;
    private volatile MacAddress privateGatewayMac = MacAddress.NONE;

    /**
     * Creates an cordvtn host location provider.
     */
    public CordVtn() {
        super(new ProviderId("host", CORDVTN_APP_ID));
    }

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.cordvtn");
        ruleInstaller = new CordVtnRuleInstaller(appId, flowRuleService,
                                                 deviceService,
                                                 driverService,
                                                 groupService,
                                                 configRegistry,
                                                 DEFAULT_TUNNEL);

        arpProxy = new CordVtnArpProxy(appId, packetService, hostService);
        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));
        arpProxy.requestPacket();

        hostService.addListener(hostListener);
        hostProvider = hostProviderRegistry.register(this);

        configRegistry.registerConfigFactory(configFactory);
        configService.addListener(configListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostProviderRegistry.unregister(this);
        hostService.removeListener(hostListener);

        packetService.removeProcessor(packetProcessor);

        configRegistry.unregisterConfigFactory(configFactory);
        configService.removeListener(configListener);

        eventExecutor.shutdown();
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
    public void createServiceDependency(CordServiceId tServiceId, CordServiceId pServiceId,
                                        boolean isBidirectional) {
        CordService tService = getCordService(tServiceId);
        CordService pService = getCordService(pServiceId);

        if (tService == null || pService == null) {
            log.error("Failed to create CordService for {}", tServiceId.id());
            return;
        }

        log.info("Service dependency from {} to {} created.", tService.id().id(), pService.id().id());
        ruleInstaller.populateServiceDependencyRules(tService, pService, isBidirectional);
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

        Host existingHost = hostService.getHost(hostId);
        if (existingHost != null) {
            String serviceId = existingHost.annotations().value(SERVICE_ID);
            if (serviceId == null || !serviceId.equals(vPort.networkId())) {
                // this host is not injected by cordvtn or a stale host, remove it
                hostProvider.hostVanished(existingHost.id());
            }
        }

        // Included CREATED_TIME to annotation intentionally to trigger HOST_UPDATED
        // event so that the flow rule population for this host can happen.
        // This ensures refreshing data plane by pushing network config always make
        // the data plane synced.
        Set<IpAddress> fixedIp = Sets.newHashSet(vPort.fixedIps().values());
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                .set(SERVICE_ID, vPort.networkId())
                .set(OPENSTACK_PORT_ID, vPort.id())
                .set(DATA_PLANE_IP, node.dpIp().ip().toString())
                .set(DATA_PLANE_INTF, node.dpIntf())
                .set(CREATED_TIME, String.valueOf(System.currentTimeMillis()));

        String serviceVlan = getServiceVlan(vPort);
        if (serviceVlan != null) {
            annotations.set(S_TAG, serviceVlan);
        }

        HostDescription hostDesc = new DefaultHostDescription(
                mac,
                VlanId.NONE,
                new HostLocation(connectPoint, System.currentTimeMillis()),
                fixedIp,
                annotations.build());

        hostProvider.hostDetected(hostId, hostDesc, false);
    }

    @Override
    public void removeServiceVm(ConnectPoint connectPoint) {
        hostService.getConnectedHosts(connectPoint)
                .stream()
                .forEach(host -> hostProvider.hostVanished(host.id()));
    }

    @Override
    public void updateVirtualSubscriberGateways(HostId vSgHostId, String serviceVlan,
                                                Map<IpAddress, MacAddress> vSgs) {
        Host vSgHost = hostService.getHost(vSgHostId);
        if (vSgHost == null || !vSgHost.annotations().value(S_TAG).equals(serviceVlan)) {
            log.debug("Invalid vSG updates for {}", serviceVlan);
            return;
        }

        log.info("Updates vSGs in {} with {}", vSgHost.id(), vSgs.toString());
        vSgs.entrySet().stream()
                .filter(entry -> hostService.getHostsByMac(entry.getValue()).isEmpty())
                .forEach(entry -> addVirtualSubscriberGateway(
                        vSgHost,
                        entry.getKey(),
                        entry.getValue(),
                        serviceVlan));

        hostService.getConnectedHosts(vSgHost.location()).stream()
                .filter(host -> !host.mac().equals(vSgHost.mac()))
                .filter(host -> !vSgs.values().contains(host.mac()))
                .forEach(host -> {
                    log.info("Removed vSG {}", host.toString());
                    hostProvider.hostVanished(host.id());
                });
    }

    /**
     * Adds virtual subscriber gateway to the system.
     *
     * @param vSgHost host virtual machine of this vSG
     * @param vSgIp vSG ip address
     * @param vSgMac vSG mac address
     * @param serviceVlan service vlan
     */
    private void addVirtualSubscriberGateway(Host vSgHost, IpAddress vSgIp, MacAddress vSgMac,
                                             String serviceVlan) {
        log.info("vSG with IP({}) MAC({}) added", vSgIp.toString(), vSgMac.toString());

        HostId hostId = HostId.hostId(vSgMac);
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                .set(S_TAG, serviceVlan)
                .set(VSG_HOST_ID, vSgHost.id().toString())
                .set(CREATED_TIME, String.valueOf(System.currentTimeMillis()));

        HostDescription hostDesc = new DefaultHostDescription(
                vSgMac,
                VlanId.NONE,
                vSgHost.location(),
                Sets.newHashSet(vSgIp),
                annotations.build());

        hostProvider.hostDetected(hostId, hostDesc, false);
    }

    /**
     * Returns public ip addresses of vSGs running inside a give vSG host.
     *
     * @param vSgHost vSG host
     * @return map of ip and mac address, or empty map
     */
    private Map<IpAddress, MacAddress> getSubscriberGateways(Host vSgHost) {
        String vPortId = vSgHost.annotations().value(OPENSTACK_PORT_ID);
        String serviceVlan = vSgHost.annotations().value(S_TAG);

        OpenstackPort vPort = openstackService.port(vPortId);
        if (vPort == null) {
            log.warn("Failed to get OpenStack port {} for VM {}", vPortId, vSgHost.id());
            return Maps.newHashMap();
        }

        if (!serviceVlan.equals(getServiceVlan(vPort))) {
            log.error("Host({}) s-tag does not match with vPort s-tag", vSgHost.id());
            return Maps.newHashMap();
        }

        return vPort.allowedAddressPairs();
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
     * @return ip address, or null
     */
    private IpAddress getTunnelIp(Host host) {
        String ip = host.annotations().value(DATA_PLANE_IP);
        return ip == null ? null : IpAddress.valueOf(ip);
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
     * Returns s-tag from a given OpenStack port.
     *
     * @param vPort openstack port
     * @return s-tag string
     */
    private String getServiceVlan(OpenstackPort vPort) {
        checkNotNull(vPort);

        if (vPort.name() != null && vPort.name().startsWith(S_TAG)) {
            return vPort.name().split("-")[1];
        } else {
            return null;
        }
    }

    /**
     * Returns service ID of this host.
     *
     * @param host host
     * @return service id, or null if not found
     */
    private String getServiceId(Host host) {
        return host.annotations().value(SERVICE_ID);
    }

    /**
     * Returns hosts associated with a given OpenStack network.
     *
     * @param vNet openstack network
     * @return set of hosts
     */
    private Set<Host> getHostsWithOpenstackNetwork(OpenstackNetwork vNet) {
        checkNotNull(vNet);

        String vNetId = vNet.id();
        return StreamSupport.stream(hostService.getHosts().spliterator(), false)
                .filter(host -> Objects.equals(vNetId, getServiceId(host)))
                .collect(Collectors.toSet());
    }

    /**
     * Registers static DHCP lease for a given host.
     *
     * @param host host
     * @param service cord service
     */
    private void registerDhcpLease(Host host, CordService service) {
        List<Ip4Address> options = Lists.newArrayList();
        options.add(Ip4Address.makeMaskPrefix(service.serviceIpRange().prefixLength()));
        options.add(service.serviceIp().getIp4Address());
        options.add(service.serviceIp().getIp4Address());
        options.add(DEFAULT_DNS);

        log.debug("Set static DHCP mapping for {}", host.mac());
        dhcpService.setStaticMapping(host.mac(),
                                     host.ipAddresses().stream().findFirst().get().getIp4Address(),
                                     true,
                                     options);
    }

    /**
     * Handles VM detected situation.
     *
     * @param host host
     */
    private void serviceVmAdded(Host host) {
        String serviceVlan = host.annotations().value(S_TAG);
        if (serviceVlan != null) {
            virtualSubscriberGatewayAdded(host, serviceVlan);
        }

        String vNetId = host.annotations().value(SERVICE_ID);
        if (vNetId == null) {
            // ignore this host, it is not the service VM, or it's a vSG
            return;
        }

        OpenstackNetwork vNet = openstackService.network(vNetId);
        if (vNet == null) {
            log.warn("Failed to get OpenStack network {} for VM {}.",
                     vNetId, host.id());
            return;
        }

        log.info("VM is detected, MAC: {} IP: {}",
                 host.mac(),
                 host.ipAddresses().stream().findFirst().get());

        CordService service = getCordService(vNet);
        if (service == null) {
            return;
        }

        switch (service.serviceType()) {
            case MANAGEMENT:
                ruleInstaller.populateManagementNetworkRules(host, service);
                break;
            case PRIVATE:
                arpProxy.addGateway(service.serviceIp(), privateGatewayMac);
            case PUBLIC:
            default:
                // TODO check if the service needs an update on its group buckets after done CORD-433
                ruleInstaller.updateServiceGroup(service);
                // sends gratuitous ARP here for the case of adding existing VMs
                // when ONOS or cordvtn app is restarted
                arpProxy.sendGratuitousArpForGateway(service.serviceIp(), Sets.newHashSet(host));
                break;
        }

        registerDhcpLease(host, service);
        ruleInstaller.populateBasicConnectionRules(host, getTunnelIp(host), vNet);
    }

    /**
     * Handles VM removed situation.
     *
     * @param host host
     */
    private void serviceVmRemoved(Host host) {
        String serviceVlan = host.annotations().value(S_TAG);
        if (serviceVlan != null) {
            virtualSubscriberGatewayRemoved(host);
        }

        String vNetId = host.annotations().value(SERVICE_ID);
        if (vNetId == null) {
            // ignore it, it's not the service VM or it's a vSG
            return;
        }

        OpenstackNetwork vNet = openstackService.network(vNetId);
        if (vNet == null) {
            log.warn("Failed to get OpenStack network {} for VM {}",
                     vNetId, host.id());
            return;
        }

        log.info("VM is vanished, MAC: {} IP: {}",
                 host.mac(),
                 host.ipAddresses().stream().findFirst().get());

        ruleInstaller.removeBasicConnectionRules(host);
        dhcpService.removeStaticMapping(host.mac());

        CordService service = getCordService(vNet);
        if (service == null) {
            return;
        }

        switch (service.serviceType()) {
            case MANAGEMENT:
                ruleInstaller.removeManagementNetworkRules(host, service);
                break;
            case PRIVATE:
                if (getHostsWithOpenstackNetwork(vNet).isEmpty()) {
                    arpProxy.removeGateway(service.serviceIp());
                }
            case PUBLIC:
            default:
                // TODO check if the service needs an update on its group buckets after done CORD-433
                ruleInstaller.updateServiceGroup(service);
                break;
        }
    }


    /**
     * Handles virtual subscriber gateway VM or container.
     *
     * @param host new host with stag, it can be vsg VM or vsg
     * @param serviceVlan service vlan
     */
    private void virtualSubscriberGatewayAdded(Host host, String serviceVlan) {
        Map<IpAddress, MacAddress> vSgs;
        Host vSgHost;

        String vSgHostId = host.annotations().value(VSG_HOST_ID);
        if (vSgHostId == null) {
            log.debug("vSG VM detected {}", host.id());

            vSgHost = host;
            vSgs = getSubscriberGateways(vSgHost);
            vSgs.entrySet().stream().forEach(entry -> addVirtualSubscriberGateway(
                    vSgHost,
                    entry.getKey(),
                    entry.getValue(),
                    serviceVlan));
        } else {
            vSgHost = hostService.getHost(HostId.hostId(vSgHostId));
            if (vSgHost == null) {
                return;
            }

            log.debug("vSG detected {}", host.id());
            vSgs = getSubscriberGateways(vSgHost);
        }

        ruleInstaller.populateSubscriberGatewayRules(vSgHost, vSgs.keySet());
    }

    /**
     * Handles virtual subscriber gateway removed.
     *
     * @param vSg vsg host to remove
     */
    private void virtualSubscriberGatewayRemoved(Host vSg) {
        String vSgHostId = vSg.annotations().value(VSG_HOST_ID);
        if (vSgHostId == null) {
            return;
        }

        Host vSgHost = hostService.getHost(HostId.hostId(vSgHostId));
        if (vSgHost == null) {
            return;
        }

        log.info("vSG removed {}", vSg.id());
        Map<IpAddress, MacAddress> vSgs = getSubscriberGateways(vSgHost);
        ruleInstaller.populateSubscriberGatewayRules(vSgHost, vSgs.keySet());
    }

    /**
     * Sets service network gateway MAC address and sends out gratuitous ARP to all
     * VMs to update the gateway MAC address.
     *
     * @param newMac mac address to update
     */
    private void setPrivateGatewayMac(MacAddress newMac) {
        if (newMac == null || newMac.equals(privateGatewayMac)) {
            // no updates, do nothing
            return;
        }

        privateGatewayMac = newMac;
        log.debug("Set service gateway MAC address to {}", privateGatewayMac.toString());

        // TODO get existing service list from XOS and replace the loop below
        Set<String> vNets = Sets.newHashSet();
        hostService.getHosts().forEach(host -> vNets.add(host.annotations().value(SERVICE_ID)));
        vNets.remove(null);

        vNets.stream().forEach(vNet -> {
            CordService service = getCordService(CordServiceId.of(vNet));
            if (service != null) {
                arpProxy.addGateway(service.serviceIp(), privateGatewayMac);
                arpProxy.sendGratuitousArpForGateway(service.serviceIp(), service.hosts().keySet());
            }
        });
    }

    /**
     * Sets public gateway MAC address.
     *
     * @param publicGateways gateway ip and mac address pairs
     */
    private void setPublicGatewayMac(Map<IpAddress, MacAddress> publicGateways) {
        publicGateways.entrySet()
                .stream()
                .forEach(entry -> {
                    arpProxy.addGateway(entry.getKey(), entry.getValue());
                    log.debug("Added public gateway IP {}, MAC {}",
                              entry.getKey().toString(), entry.getValue().toString());
                });
        // TODO notice gateway MAC change to VMs holds this gateway IP
    }

    /**
     * Updates configurations.
     */
    private void readConfiguration() {
        CordVtnConfig config = configRegistry.getConfig(appId, CordVtnConfig.class);
        if (config == null) {
            log.debug("No configuration found");
            return;
        }

        setPrivateGatewayMac(config.privateGatewayMac());
        setPublicGatewayMac(config.publicGateways());
   }

    private class InternalHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            if (!mastershipService.isLocalMaster(host.location().deviceId())) {
                // do not allow to proceed without mastership
                return;
            }

            switch (event.type()) {
                case HOST_UPDATED:
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
            if (ethPacket == null || ethPacket.getEtherType() != Ethernet.TYPE_ARP) {
                return;
            }

            arpProxy.processArpPacket(context, ethPacket);
        }
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(CordVtnConfig.class)) {
                return;
            }

            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    log.info("Network configuration changed");
                    eventExecutor.execute(CordVtn.this::readConfiguration);
                    break;
                default:
                    break;
            }
        }
    }
}
