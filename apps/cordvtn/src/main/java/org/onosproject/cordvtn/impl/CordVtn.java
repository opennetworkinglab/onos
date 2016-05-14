/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.cordvtn.impl;

import com.google.common.base.Strings;
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
import org.onosproject.cordvtn.api.CordVtnConfig;
import org.onosproject.cordvtn.api.CordVtnNode;
import org.onosproject.cordvtn.api.CordVtnService;
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
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceService;
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
import org.onosproject.xosclient.api.OpenStackAccess;
import org.onosproject.xosclient.api.VtnPort;
import org.onosproject.xosclient.api.VtnPortApi;
import org.onosproject.xosclient.api.VtnPortId;
import org.onosproject.xosclient.api.VtnService;
import org.onosproject.xosclient.api.VtnServiceApi;
import org.onosproject.xosclient.api.VtnServiceId;
import org.onosproject.xosclient.api.XosAccess;
import org.onosproject.xosclient.api.XosClientService;
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
    protected HostProviderRegistry hostProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DhcpService dhcpService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected XosClientService xosClient;

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, CordVtnConfig.class, "cordvtn") {
                @Override
                public CordVtnConfig createConfig() {
                    return new CordVtnConfig();
                }
            };

    private static final String XOS_ACCESS_ERROR = "XOS access is not configured";
    private static final String OPENSTACK_ACCESS_ERROR = "OpenStack access is not configured";

    private static final String DEFAULT_TUNNEL = "vxlan";
    private static final String SERVICE_ID = "serviceId";
    private static final String PORT_ID = "vtnPortId";
    private static final String DATA_PLANE_IP = "dataPlaneIp";
    private static final String DATA_PLANE_INTF = "dataPlaneIntf";
    private static final String S_TAG = "stag";
    private static final String VSG_HOST_ID = "vsgHostId";
    private static final String CREATE_TIME = "createTime";

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

    private volatile XosAccess xosAccess = null;
    private volatile OpenStackAccess osAccess = null;
    private volatile MacAddress privateGatewayMac = MacAddress.NONE;

    /**
     * Creates an cordvtn host location provider.
     */
    public CordVtn() {
        super(new ProviderId("host", CORDVTN_APP_ID));
    }

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(CordVtnService.CORDVTN_APP_ID);
        ruleInstaller = new CordVtnRuleInstaller(appId, flowRuleService,
                                                 deviceService,
                                                 groupService,
                                                 hostService,
                                                 configRegistry,
                                                 DEFAULT_TUNNEL);

        arpProxy = new CordVtnArpProxy(appId, packetService, hostService);
        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));
        arpProxy.requestPacket();

        hostService.addListener(hostListener);
        hostProvider = hostProviderRegistry.register(this);

        configRegistry.registerConfigFactory(configFactory);
        configRegistry.addListener(configListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostProviderRegistry.unregister(this);
        hostService.removeListener(hostListener);

        packetService.removeProcessor(packetProcessor);

        configRegistry.unregisterConfigFactory(configFactory);
        configRegistry.removeListener(configListener);

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
    public void createServiceDependency(VtnServiceId tServiceId, VtnServiceId pServiceId,
                                        boolean isBidirectional) {
        checkNotNull(osAccess, OPENSTACK_ACCESS_ERROR);
        checkNotNull(xosAccess, XOS_ACCESS_ERROR);

        // TODO remove openstack access when XOS provides all information
        VtnServiceApi serviceApi = xosClient.getClient(xosAccess).vtnService();
        VtnService tService = serviceApi.service(tServiceId, osAccess);
        VtnService pService = serviceApi.service(pServiceId, osAccess);

        if (tService == null || pService == null) {
            log.error("Failed to create dependency between {} and {}",
                      tServiceId, pServiceId);
            return;
        }

        log.info("Created dependency between {} and {}", tService.name(), pService.name());
        ruleInstaller.populateServiceDependencyRules(tService, pService, isBidirectional, true);
    }

    @Override
    public void removeServiceDependency(VtnServiceId tServiceId, VtnServiceId pServiceId) {
        checkNotNull(osAccess, OPENSTACK_ACCESS_ERROR);
        checkNotNull(xosAccess, XOS_ACCESS_ERROR);

        // TODO remove openstack access when XOS provides all information
        VtnServiceApi serviceApi = xosClient.getClient(xosAccess).vtnService();
        VtnService tService = serviceApi.service(tServiceId, osAccess);
        VtnService pService = serviceApi.service(pServiceId, osAccess);

        if (tService == null || pService == null) {
            log.error("Failed to remove dependency between {} and {}",
                      tServiceId, pServiceId);
            return;
        }

        log.info("Removed dependency between {} and {}", tService.name(), pService.name());
        ruleInstaller.populateServiceDependencyRules(tService, pService, true, false);
    }

    @Override
    public void addServiceVm(CordVtnNode node, ConnectPoint connectPoint) {
        checkNotNull(osAccess, OPENSTACK_ACCESS_ERROR);
        checkNotNull(xosAccess, XOS_ACCESS_ERROR);

        Port port = deviceService.getPort(connectPoint.deviceId(), connectPoint.port());
        String portName = port.annotations().value("portName");

        // TODO remove openstack access when XOS provides all information
        VtnPortApi portApi = xosClient.getClient(xosAccess).vtnPort();
        VtnPort vtnPort = portApi.vtnPort(portName, osAccess);
        if (vtnPort == null) {
            log.warn("Failed to get port information of {}", portName);
            return;
        }

        // Added CREATE_TIME intentionally to trigger HOST_UPDATED event for the
        // existing instances.
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                .set(SERVICE_ID, vtnPort.serviceId().id())
                .set(PORT_ID, vtnPort.id().id())
                .set(DATA_PLANE_IP, node.dpIp().ip().toString())
                .set(DATA_PLANE_INTF, node.dpIntf())
                .set(CREATE_TIME, String.valueOf(System.currentTimeMillis()));

        // TODO address service specific task in a separate package
        String serviceVlan = getServiceVlan(vtnPort);
        if (!Strings.isNullOrEmpty(serviceVlan)) {
            annotations.set(S_TAG, serviceVlan);
        }

        HostDescription hostDesc = new DefaultHostDescription(
                vtnPort.mac(),
                VlanId.NONE,
                new HostLocation(connectPoint, System.currentTimeMillis()),
                Sets.newHashSet(vtnPort.ip()),
                annotations.build());

        HostId hostId = HostId.hostId(vtnPort.mac());
        hostProvider.hostDetected(hostId, hostDesc, false);
    }

    @Override
    public void removeServiceVm(ConnectPoint connectPoint) {
        hostService.getConnectedHosts(connectPoint)
                .stream()
                .forEach(host -> hostProvider.hostVanished(host.id()));
    }

    @Override
    // TODO address service specific task in a separate package
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
    // TODO address service specific task in a separate package
    private void addVirtualSubscriberGateway(Host vSgHost, IpAddress vSgIp, MacAddress vSgMac,
                                             String serviceVlan) {
        log.info("vSG with IP({}) MAC({}) added", vSgIp.toString(), vSgMac.toString());

        HostId hostId = HostId.hostId(vSgMac);
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                .set(S_TAG, serviceVlan)
                .set(VSG_HOST_ID, vSgHost.id().toString())
                .set(CREATE_TIME, String.valueOf(System.currentTimeMillis()));

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
    // TODO address service specific task in a separate package
    private Map<IpAddress, MacAddress> getSubscriberGateways(Host vSgHost) {
        checkNotNull(osAccess, OPENSTACK_ACCESS_ERROR);
        checkNotNull(xosAccess, XOS_ACCESS_ERROR);

        String vtnPortId = vSgHost.annotations().value(PORT_ID);
        String sTag = vSgHost.annotations().value(S_TAG);

        if (Strings.isNullOrEmpty(vtnPortId) || Strings.isNullOrEmpty(sTag)) {
            log.warn("PORT_ID and S_TAG is not set, ignore {}", vSgHost);
            return Maps.newHashMap();
        }

        // TODO remove openstack access when XOS provides all information
        VtnPortApi portApi = xosClient.getClient(xosAccess).vtnPort();
        VtnPort vtnPort = portApi.vtnPort(VtnPortId.of(vtnPortId), osAccess);
        if (vtnPort == null) {
            log.warn("Failed to get port information of {}", vSgHost);
            return Maps.newHashMap();
        }

        if (!sTag.equals(getServiceVlan(vtnPort))) {
            log.error("Host({}) s-tag does not match with VTN port s-tag", vSgHost);
            return Maps.newHashMap();
        }
        return vtnPort.addressPairs();
    }

    /**
     * Returns s-tag from a given VTN port.
     *
     * @param vtnPort vtn port
     * @return s-tag string
     */
    // TODO address service specific task in a separate package
    private String getServiceVlan(VtnPort vtnPort) {
        checkNotNull(vtnPort);

        String portName = vtnPort.name();
        if (portName != null && portName.startsWith(S_TAG)) {
            return portName.split("-")[1];
        } else {
            return null;
        }
    }

    /**
     * Returns instances with a given network service.
     *
     * @param serviceId service id
     * @return set of hosts
     */
    private Set<Host> getInstances(VtnServiceId serviceId) {
        return StreamSupport.stream(hostService.getHosts().spliterator(), false)
                .filter(host -> Objects.equals(
                        serviceId.id(),
                        host.annotations().value(SERVICE_ID)))
                .collect(Collectors.toSet());
    }

    /**
     * Registers static DHCP lease for a given host.
     *
     * @param host host
     * @param service cord service
     */
    private void registerDhcpLease(Host host, VtnService service) {
        List<Ip4Address> options = Lists.newArrayList();
        options.add(Ip4Address.makeMaskPrefix(service.subnet().prefixLength()));
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
        checkNotNull(osAccess, OPENSTACK_ACCESS_ERROR);
        checkNotNull(xosAccess, XOS_ACCESS_ERROR);

        // TODO address service specific task in a separate package
        String serviceVlan = host.annotations().value(S_TAG);
        if (serviceVlan != null) {
            virtualSubscriberGatewayAdded(host, serviceVlan);
        }

        String serviceId = host.annotations().value(SERVICE_ID);
        if (Strings.isNullOrEmpty(serviceId)) {
            // ignore this host, it is not a service instance
            return;
        }

        log.info("Instance is detected {}", host);

        // TODO remove openstack access when XOS provides all information
        VtnServiceApi serviceApi = xosClient.getClient(xosAccess).vtnService();
        VtnService service = serviceApi.service(VtnServiceId.of(serviceId), osAccess);
        if (service == null) {
            log.warn("Failed to get VtnService for {}", serviceId);
            return;
        }

        switch (service.networkType()) {
            case MANAGEMENT:
                ruleInstaller.populateManagementNetworkRules(host, service);
                break;
            case PRIVATE:
                arpProxy.addGateway(service.serviceIp(), privateGatewayMac);
            case PUBLIC:
            default:
                // TODO get bidirectional information from XOS once XOS supports
                service.tenantServices().stream().forEach(
                        tServiceId -> createServiceDependency(tServiceId, service.id(), true));
                service.providerServices().stream().forEach(
                        pServiceId -> createServiceDependency(service.id(), pServiceId, true));

                ruleInstaller.updateProviderServiceGroup(service);
                // sends gratuitous ARP here for the case of adding existing VMs
                // when ONOS or cordvtn app is restarted
                arpProxy.sendGratuitousArpForGateway(service.serviceIp(), Sets.newHashSet(host));
                break;
        }

        registerDhcpLease(host, service);
        ruleInstaller.populateBasicConnectionRules(host, service, true);
    }

    /**
     * Handles VM removed situation.
     *
     * @param host host
     */
    private void serviceVmRemoved(Host host) {
        checkNotNull(osAccess, OPENSTACK_ACCESS_ERROR);
        checkNotNull(xosAccess, XOS_ACCESS_ERROR);

        // TODO address service specific task in a separate package
        if (host.annotations().value(S_TAG) != null) {
            virtualSubscriberGatewayRemoved(host);
        }

        String serviceId = host.annotations().value(SERVICE_ID);
        if (Strings.isNullOrEmpty(serviceId)) {
            // ignore this host, it is not a service instance
            return;
        }

        log.info("Instance is vanished {}", host);

        // TODO remove openstack access when XOS provides all information
        VtnServiceApi vtnServiceApi = xosClient.getClient(xosAccess).vtnService();
        VtnService service = vtnServiceApi.service(VtnServiceId.of(serviceId), osAccess);
        if (service == null) {
            log.warn("Failed to get VtnService for {}", serviceId);
            return;
        }

        // TODO need to consider the case that the service is removed also
        switch (service.networkType()) {
            case MANAGEMENT:
                break;
            case PRIVATE:
                if (getInstances(VtnServiceId.of(serviceId)).isEmpty()) {
                    arpProxy.removeGateway(service.serviceIp());
                }
            case PUBLIC:
            default:
                if (!service.tenantServices().isEmpty()) {
                    ruleInstaller.updateProviderServiceGroup(service);
                }
                if (!service.providerServices().isEmpty()) {
                    ruleInstaller.updateTenantServiceVm(host, service);
                }
                break;
        }

        dhcpService.removeStaticMapping(host.mac());
        ruleInstaller.populateBasicConnectionRules(host, service, false);
    }


    /**
     * Handles virtual subscriber gateway VM or container.
     *
     * @param host new host with stag, it can be vsg VM or vsg
     * @param serviceVlan service vlan
     */
    // TODO address service specific task in a separate package
    private void virtualSubscriberGatewayAdded(Host host, String serviceVlan) {
        Map<IpAddress, MacAddress> vSgs;
        Host vSgHost;

        String vSgHostId = host.annotations().value(VSG_HOST_ID);
        if (vSgHostId == null) {
            log.info("vSG VM detected {}", host.id());

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

            log.info("vSG detected {}", host.id());
            vSgs = getSubscriberGateways(vSgHost);
        }

        ruleInstaller.populateSubscriberGatewayRules(vSgHost, vSgs.keySet());
    }

    /**
     * Handles virtual subscriber gateway removed.
     *
     * @param vSg vsg host to remove
     */
    // TODO address service specific task in a separate package
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
        checkNotNull(osAccess, OPENSTACK_ACCESS_ERROR);
        checkNotNull(xosAccess, XOS_ACCESS_ERROR);

        if (newMac == null || newMac.equals(privateGatewayMac)) {
            // no updates, do nothing
            return;
        }

        privateGatewayMac = newMac;
        log.debug("Set service gateway MAC address to {}", privateGatewayMac.toString());

        VtnServiceApi vtnServiceApi = xosClient.getClient(xosAccess).vtnService();
        vtnServiceApi.services().stream().forEach(serviceId -> {
            VtnService service = vtnServiceApi.service(serviceId, osAccess);
            if (service != null) {
                arpProxy.addGateway(service.serviceIp(), privateGatewayMac);
                arpProxy.sendGratuitousArpForGateway(service.serviceIp(), getInstances(serviceId));
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

        xosAccess = config.xosAccess();
        osAccess = config.openstackAccess();

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
                    eventExecutor.execute(() -> serviceVmAdded(host));
                    break;
                case HOST_REMOVED:
                    eventExecutor.execute(() -> serviceVmRemoved(host));
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
