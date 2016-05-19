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
import org.onosproject.cordvtn.api.CordVtnService;
import org.onosproject.cordvtn.api.Instance;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcp.DhcpService;
import org.onosproject.dhcp.IpAssignment;
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
import org.onosproject.xosclient.api.VtnService;
import org.onosproject.xosclient.api.VtnServiceApi;
import org.onosproject.xosclient.api.VtnServiceId;
import org.onosproject.xosclient.api.XosAccess;
import org.onosproject.xosclient.api.XosClientService;
import org.slf4j.Logger;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.cordvtn.api.Instance.*;
import static org.onosproject.dhcp.IpAssignment.AssignmentStatus.Option_RangeNotEnforced;
import static org.onosproject.xosclient.api.VtnService.NetworkType.MANAGEMENT;
import static org.onosproject.xosclient.api.VtnService.NetworkType.PRIVATE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Adds or removes instances to network services.
 */
@Component(immediate = true)
@Service(value = CordVtnInstanceManager.class)
public class CordVtnInstanceManager extends AbstractProvider implements HostProvider {

    protected final Logger log = getLogger(getClass());

    private static final String XOS_ACCESS_ERROR = "XOS access is not configured";
    private static final String OPENSTACK_ACCESS_ERROR = "OpenStack access is not configured";
    private static final Ip4Address DEFAULT_DNS = Ip4Address.valueOf("8.8.8.8");
    private static final int DHCP_INFINITE_LEASE = -1;

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
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DhcpService dhcpService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected XosClientService xosClient;

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, CordVtnConfig.class, "cordvtn") {
                @Override
                public CordVtnConfig createConfig() {
                    return new CordVtnConfig();
                }
            };

    private final ExecutorService eventExecutor =
            newSingleThreadScheduledExecutor(groupedThreads("onos/cordvtn-instance", "event-handler"));
    private final PacketProcessor packetProcessor = new InternalPacketProcessor();
    private final HostListener hostListener = new InternalHostListener();
    private final NetworkConfigListener configListener = new InternalConfigListener();

    private ApplicationId appId;
    private HostProviderService hostProvider;
    private CordVtnArpProxy arpProxy; // TODO make it a component service
    private MacAddress privateGatewayMac = MacAddress.NONE;
    private XosAccess xosAccess = null;
    private OpenStackAccess osAccess = null;

    /**
     * Creates an cordvtn host location provider.
     */
    public CordVtnInstanceManager() {
        super(new ProviderId("host", CordVtnService.CORDVTN_APP_ID));
    }

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(CordVtnService.CORDVTN_APP_ID);

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

    /**
     * Adds a service instance at a given connect point.
     *
     * @param connectPoint connect point of the instance
     */
    public void addInstance(ConnectPoint connectPoint) {
        Port port = deviceService.getPort(connectPoint.deviceId(), connectPoint.port());
        if (port == null) {
            log.debug("No port found from {}", connectPoint);
            return;
        }

        VtnPort vtnPort = getVtnPort(port.annotations().value("portName"));
        if (vtnPort == null) {
            return;
        }

        VtnService vtnService = getVtnService(vtnPort.serviceId());
        if (vtnService == null) {
            return;
        }

        // Added CREATE_TIME intentionally to trigger HOST_UPDATED event for the
        // existing instances.
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                .set(SERVICE_TYPE, vtnService.serviceType().toString())
                .set(SERVICE_ID, vtnPort.serviceId().id())
                .set(PORT_ID, vtnPort.id().id())
                .set(CREATE_TIME, String.valueOf(System.currentTimeMillis()));

        HostDescription hostDesc = new DefaultHostDescription(
                vtnPort.mac(),
                VlanId.NONE,
                new HostLocation(connectPoint, System.currentTimeMillis()),
                Sets.newHashSet(vtnPort.ip()),
                annotations.build());

        HostId hostId = HostId.hostId(vtnPort.mac());
        hostProvider.hostDetected(hostId, hostDesc, false);
    }

    /**
     * Adds a service instance with given host ID and host description.
     *
     * @param hostId host id
     * @param description host description
     */
    public void addInstance(HostId hostId, HostDescription description) {
        hostProvider.hostDetected(hostId, description, false);
    }

    /**
     * Removes a service instance from a given connect point.
     *
     * @param connectPoint connect point
     */
    public void removeInstance(ConnectPoint connectPoint) {
        hostService.getConnectedHosts(connectPoint)
                .stream()
                .forEach(host -> hostProvider.hostVanished(host.id()));
    }

    /**
     * Removes service instance with given host ID.
     *
     * @param hostId host id
     */
    public void removeInstance(HostId hostId) {
        hostProvider.hostVanished(hostId);
    }

    private void instanceDetected(Instance instance) {
        VtnService service = getVtnService(instance.serviceId());
        if (service == null) {
            return;
        }

        if (service.networkType().equals(PRIVATE)) {
            arpProxy.addGateway(service.serviceIp(), privateGatewayMac);
            arpProxy.sendGratuitousArpForGateway(service.serviceIp(), Sets.newHashSet(instance));
        }
        if (!instance.isNestedInstance()) {
            registerDhcpLease(instance, service);
        }
    }

    private void instanceRemoved(Instance instance) {
        VtnService service = getVtnService(instance.serviceId());
        if (service == null) {
            return;
        }

        if (service.networkType().equals(PRIVATE) && getInstances(service.id()).isEmpty()) {
            arpProxy.removeGateway(service.serviceIp());
        }

        if (!instance.isNestedInstance()) {
            dhcpService.removeStaticMapping(instance.mac());
        }
    }

    private void registerDhcpLease(Instance instance, VtnService service) {
        Ip4Address broadcast = Ip4Address.makeMaskedAddress(
                instance.ipAddress(),
                service.subnet().prefixLength());

        IpAssignment.Builder ipBuilder = IpAssignment.builder()
                .ipAddress(instance.ipAddress())
                .leasePeriod(DHCP_INFINITE_LEASE)
                .timestamp(new Date())
                .subnetMask(Ip4Address.makeMaskPrefix(service.subnet().prefixLength()))
                .broadcast(broadcast)
                .domainServer(DEFAULT_DNS)
                .assignmentStatus(Option_RangeNotEnforced);

        if (service.networkType() != MANAGEMENT) {
            ipBuilder = ipBuilder.routerAddress(service.serviceIp().getIp4Address());
        }

        log.debug("Set static DHCP mapping for {} {}", instance.mac(), instance.ipAddress());
        dhcpService.setStaticMapping(instance.mac(), ipBuilder.build());
    }

    private VtnService getVtnService(VtnServiceId serviceId) {
        checkNotNull(osAccess, OPENSTACK_ACCESS_ERROR);
        checkNotNull(xosAccess, XOS_ACCESS_ERROR);

        // TODO remove openstack access when XOS provides all information
        VtnServiceApi serviceApi = xosClient.getClient(xosAccess).vtnService();
        VtnService service = serviceApi.service(serviceId, osAccess);
        if (service == null) {
            log.warn("Failed to get VtnService for {}", serviceId);
        }
        return service;
    }

    private VtnPort getVtnPort(String portName) {
        checkNotNull(osAccess, OPENSTACK_ACCESS_ERROR);
        checkNotNull(xosAccess, XOS_ACCESS_ERROR);

        // TODO remove openstack access when XOS provides all information
        VtnPortApi portApi = xosClient.getClient(xosAccess).vtnPort();
        VtnPort vtnPort = portApi.vtnPort(portName, osAccess);
        if (vtnPort == null) {
            log.warn("Failed to get port information of {}", portName);
        }
        return vtnPort;
    }

    private Set<Instance> getInstances(VtnServiceId serviceId) {
        return StreamSupport.stream(hostService.getHosts().spliterator(), false)
                .filter(host -> Objects.equals(
                        serviceId.id(),
                        host.annotations().value(Instance.SERVICE_ID)))
                .map(Instance::of)
                .collect(Collectors.toSet());
    }

    private void readConfiguration() {
        CordVtnConfig config = configRegistry.getConfig(appId, CordVtnConfig.class);
        if (config == null) {
            log.debug("No configuration found");
            return;
        }

        log.info("Load CORD-VTN configurations");

        xosAccess = config.xosAccess();
        osAccess = config.openstackAccess();
        privateGatewayMac = config.privateGatewayMac();

        Map<IpAddress, MacAddress> publicGateways = config.publicGateways();
        publicGateways.entrySet()
                .stream()
                .forEach(entry -> {
                    arpProxy.addGateway(entry.getKey(), entry.getValue());
                    log.debug("Added public gateway IP {}, MAC {}",
                              entry.getKey(), entry.getValue());
                });
        // TODO notice gateway MAC change to VMs holds this gateway IP
    }

    private class InternalHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            if (!mastershipService.isLocalMaster(host.location().deviceId())) {
                // do not allow to proceed without mastership
                return;
            }

            Instance instance = Instance.of(host);
            switch (event.type()) {
                case HOST_UPDATED:
                case HOST_ADDED:
                    eventExecutor.execute(() -> instanceDetected(instance));
                    break;
                case HOST_REMOVED:
                    eventExecutor.execute(() -> instanceRemoved(instance));
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
                    readConfiguration();
                    break;
                default:
                    break;
            }
        }
    }
}
