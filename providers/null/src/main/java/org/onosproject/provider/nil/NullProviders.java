/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.provider.nil;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.group.GroupProviderRegistry;
import org.onosproject.net.group.GroupProviderService;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.PacketProviderRegistry;
import org.onosproject.net.packet.PacketProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Objects;
import java.util.Properties;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.delay;
import static org.onlab.util.Tools.get;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.net.MastershipRole.NONE;
import static org.onosproject.provider.nil.OsgiPropertyConstants.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider of a fake network environment, i.e. devices, links, hosts, etc.
 * To be used for benchmarking only.
 */
@Component(immediate = true, service = NullProviders.class,
        property = {
            ENABLED + ":Boolean=" + ENABLED_DEFAULT,
            TOPO_SHAPE + "=" + TOPO_SHAPE_DEFAULT,
            DEVICE_COUNT + ":Integer=" + DEVICE_COUNT_DEFAULT,
            HOST_COUNT + ":Integer=" +  HOST_COUNT_DEFAULT,
            PACKET_RATE + ":Integer=" +  PACKET_RATE_DEFAULT,
            MUTATION_RATE + ":Double=" + MUTATION_RATE_DEFAULT,
            MASTERSHIP + "=" + MASTERSHIP_DEFAULT,
        })
public class NullProviders {

    private static final Logger log = getLogger(NullProviders.class);

    static final String SCHEME = "null";
    static final String PROVIDER_ID = "org.onosproject.provider.nil";

    private static final String FORMAT =
            "Settings: enabled={}, topoShape={}, deviceCount={}, " +
                    "hostCount={}, packetRate={}, mutationRate={}";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipAdminService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceAdminService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostProviderRegistry hostProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkProviderRegistry linkProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleProviderRegistry flowRuleProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected GroupProviderRegistry groupProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketProviderRegistry packetProviderRegistry;

    private final NullDeviceProvider deviceProvider = new NullDeviceProvider();
    private final NullLinkProvider linkProvider = new NullLinkProvider();
    private final NullHostProvider hostProvider = new NullHostProvider();
    private final NullFlowRuleProvider flowRuleProvider = new NullFlowRuleProvider();
    private final NullGroupProvider groupProvider = new NullGroupProvider();
    private final NullPacketProvider packetProvider = new NullPacketProvider();
    private final TopologyMutationDriver topologyMutationDriver = new TopologyMutationDriver();
    private final PortStatsDriver portStatsDriver = new PortStatsDriver();

    private DeviceProviderService deviceProviderService;
    private HostProviderService hostProviderService;
    private LinkProviderService linkProviderService;
    private FlowRuleProviderService flowRuleProviderService;
    private GroupProviderService groupProviderService;
    private PacketProviderService packetProviderService;

    private TopologySimulator simulator;

    /** Enables or disables the provider. */
    private boolean enabled = false;

    /** Topology shape: configured, linear, reroute, tree, spineleaf, mesh, grid. */
    private String topoShape = TOPO_SHAPE_DEFAULT;

    /** Number of devices to generate. */
    private int deviceCount = DEVICE_COUNT_DEFAULT;

    /** Number of host to generate per device. */
    private int hostCount = HOST_COUNT_DEFAULT;

    /** Packet-in/s rate; 0 for no packets. */
    private int packetRate = PACKET_RATE_DEFAULT;

    /** Link event/s topology mutation rate; 0 for no mutations. */
    private double mutationRate = MUTATION_RATE_DEFAULT;

    /** Mastership given as 'random' or 'node1=dpid,dpid/node2=dpid,...'. */
    private String mastership = MASTERSHIP_DEFAULT;


    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());

        deviceProviderService = deviceProviderRegistry.register(deviceProvider);
        hostProviderService = hostProviderRegistry.register(hostProvider);
        linkProviderService = linkProviderRegistry.register(linkProvider);
        flowRuleProviderService = flowRuleProviderRegistry.register(flowRuleProvider);
        groupProviderService = groupProviderRegistry.register(groupProvider);
        packetProviderService = packetProviderRegistry.register(packetProvider);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        tearDown();

        deviceProviderRegistry.unregister(deviceProvider);
        hostProviderRegistry.unregister(hostProvider);
        linkProviderRegistry.unregister(linkProvider);
        flowRuleProviderRegistry.unregister(flowRuleProvider);
        groupProviderRegistry.unregister(groupProvider);
        packetProviderRegistry.unregister(packetProvider);

        deviceProviderService = null;
        hostProviderService = null;
        linkProviderService = null;
        flowRuleProviderService = null;
        groupProviderService = null;
        packetProviderService = null;

        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();

        boolean newEnabled;
        int newDeviceCount, newHostCount, newPacketRate;
        double newMutationRate;
        String newTopoShape, newMastership;
        try {
            String s = get(properties, ENABLED);
            newEnabled = isNullOrEmpty(s) ? enabled : Boolean.parseBoolean(s.trim());

            newTopoShape = get(properties, TOPO_SHAPE);
            newMastership = get(properties, MASTERSHIP);

            s = get(properties, DEVICE_COUNT);
            newDeviceCount = isNullOrEmpty(s) ? deviceCount : Integer.parseInt(s.trim());

            s = get(properties, HOST_COUNT);
            newHostCount = isNullOrEmpty(s) ? hostCount : Integer.parseInt(s.trim());

            s = get(properties, PACKET_RATE);
            newPacketRate = isNullOrEmpty(s) ? packetRate : Integer.parseInt(s.trim());

            s = get(properties, MUTATION_RATE);
            newMutationRate = isNullOrEmpty(s) ? mutationRate : Double.parseDouble(s.trim());

        } catch (NumberFormatException e) {
            log.warn(e.getMessage());
            newEnabled = enabled;
            newTopoShape = topoShape;
            newDeviceCount = deviceCount;
            newHostCount = hostCount;
            newPacketRate = packetRate;
            newMutationRate = mutationRate;
            newMastership = mastership;
        }

        // Any change in the following parameters implies hard restart
        if (newEnabled != enabled || !Objects.equals(newTopoShape, topoShape) ||
                newDeviceCount != deviceCount || newHostCount != hostCount) {
            enabled = newEnabled;
            topoShape = newTopoShape;
            deviceCount = newDeviceCount;
            hostCount = newHostCount;
            packetRate = newPacketRate;
            mutationRate = newMutationRate;
            restartSimulation();
        }

        // Any change in the following parameters implies just a rate change
        if (newPacketRate != packetRate || newMutationRate != mutationRate) {
            packetRate = newPacketRate;
            mutationRate = newMutationRate;
            adjustRates();
        }

        // Any change in mastership implies just reassignments.
        if (!Objects.equals(newMastership, mastership)) {
            mastership = newMastership;
            reassignMastership();
        }

        log.info(FORMAT, enabled, topoShape, deviceCount, hostCount,
                 packetRate, mutationRate);
    }

    /**
     * Returns the currently active topology simulator.
     *
     * @return current simulator; null if none is active
     */
    public TopologySimulator currentSimulator() {
        return simulator;
    }

    /**
     * Severs the link between the specified end-points in both directions.
     *
     * @param one link endpoint
     * @param two link endpoint
     */
    public void severLink(ConnectPoint one, ConnectPoint two) {
        if (enabled) {
            topologyMutationDriver.severLink(one, two);
        }
    }

    /**
     * Severs the link between the specified end-points in both directions.
     *
     * @param one link endpoint
     * @param two link endpoint
     */
    public void repairLink(ConnectPoint one, ConnectPoint two) {
        if (enabled) {
            topologyMutationDriver.repairLink(one, two);
        }
    }

    /**
     * Fails the specified device.
     *
     * @param deviceId device identifier
     */
    public void failDevice(DeviceId deviceId) {
        if (enabled) {
            topologyMutationDriver.failDevice(deviceId);
        }
    }

    /**
     * Repairs the specified device.
     *
     * @param deviceId device identifier
     */
    public void repairDevice(DeviceId deviceId) {
        if (enabled) {
            topologyMutationDriver.repairDevice(deviceId);
        }
    }


    // Resets simulation based on the current configuration parameters.
    private void restartSimulation() {
        tearDown();
        if (enabled) {
            setUp();
        }
    }

    // Sets up the topology simulation and all providers.
    private void setUp() {
        simulator = selectSimulator(topoShape);
        simulator.init(topoShape, deviceCount, hostCount,
                       new DefaultServiceDirectory(),
                       deviceProviderService, hostProviderService,
                       linkProviderService);
        flowRuleProvider.start(flowRuleProviderService);
        groupProvider.start(groupProviderService);
        packetProvider.start(packetRate, hostService, deviceService,
                             packetProviderService);
        simulator.setUpTopology();
        groupProvider.initDevicesGroupTable(simulator.deviceIds);
        topologyMutationDriver.start(mutationRate, linkService, deviceService,
                                     linkProviderService, deviceProviderService,
                                     simulator);
    }

    // Selects the simulator based on the specified name.
    private TopologySimulator selectSimulator(String topoShape) {
        if (topoShape.matches("linear([,].*|$)")) {
            return new LinearTopologySimulator();
        } else if (topoShape.matches("centipede([,].*|$)")) {
            return new CentipedeTopologySimulator();
        } else if (topoShape.matches("reroute([,].*|$)")) {
            return new RerouteTopologySimulator();
        } else if (topoShape.matches("tree([,].*|$)")) {
            return new TreeTopologySimulator();
        } else if (topoShape.matches("agglink([,].*|$)")) {
            return new AggLinkTopologySimulator();
        } else if (topoShape.matches("spineleaf([,].*|$)")) {
            return new SpineLeafTopologySimulator();
        } else if (topoShape.matches("mesh([,].*|$)")) {
            return new MeshTopologySimulator();
        } else if (topoShape.matches("grid([,].*|$)")) {
            return new GridTopologySimulator();
        } else if (topoShape.matches("fattree([,].*|$)")) {
            return new FatTreeTopologySimulator();
        } else if (topoShape.matches("chordal([,].*|$)")) {
            return new ChordalTopologySimulator();
        } else if (topoShape.matches("custom([,].*|$)")) {
            return new CustomTopologySimulator();
        } else {
            return new ConfiguredTopologySimulator();
        }
    }

    // Shuts down the topology simulator and all providers.
    private void tearDown() {
        if (simulator != null) {
            topologyMutationDriver.stop();
            packetProvider.stop();
            flowRuleProvider.stop();
            groupProvider.stop();
            delay(500);
            simulator.tearDownTopology();
            simulator = null;

        }
    }

    // Changes packet and mutation rates.
    private void adjustRates() {
        packetProvider.adjustRate(packetRate);
        topologyMutationDriver.adjustRate(mutationRate);
    }

    // Re-assigns mastership roles.
    private void reassignMastership() {
        if (mastership.equals(MASTERSHIP_DEFAULT)) {
            mastershipService.balanceRoles();
        } else {
            NodeId localNode = clusterService.getLocalNode().id();
            rejectMastership();
            String[] nodeSpecs = mastership.split("/");
            for (int i = 0; i < nodeSpecs.length; i++) {
                String[] specs = nodeSpecs[i].split("=");
                if (specs[0].equals(localNode.toString())) {
                    String[] ids = specs[1].split(",");
                    for (String id : ids) {
                        mastershipService.setRole(localNode, deviceId(id), MASTER);
                    }
                    break;
                }
            }
        }
    }

    // Rejects mastership of all devices.
    private void rejectMastership() {
        NodeId localNode = clusterService.getLocalNode().id();
        deviceService.getDevices()
                .forEach(device -> mastershipService.setRole(localNode, device.id(),
                                                             NONE));
    }

    /**
     * Enables or disables simulated port statistics.
     *
     * @param on true to enable
     */
    public void enablePortStats(boolean on) {
        if (on) {
            portStatsDriver.start(deviceService, deviceProviderService);
        } else {
            portStatsDriver.stop();
        }
    }

    // Null provider base class.
    abstract static class AbstractNullProvider extends AbstractProvider {
        protected AbstractNullProvider() {
            super(new ProviderId(SCHEME, PROVIDER_ID));
        }
    }

    // Device provider facade.
    private class NullDeviceProvider extends AbstractNullProvider implements DeviceProvider {
        @Override
        public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
            deviceProviderService.receivedRoleReply(deviceId, newRole, newRole);
        }

        @Override
        public boolean isReachable(DeviceId deviceId) {
            return simulator != null &&
                    (simulator.contains(deviceId) || !deviceService.getPorts(deviceId).isEmpty()) &&
                    (simulator instanceof CustomTopologySimulator ||
                            topologyMutationDriver.isReachable(deviceId));
        }

        @Override
        public void triggerProbe(DeviceId deviceId) {
        }

        @Override
        public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                    boolean enable) {
            // TODO maybe required
        }
    }

    // Host provider facade.
    private class NullHostProvider extends AbstractNullProvider implements HostProvider {
        @Override
        public void triggerProbe(Host host) {
        }
    }

    // Host provider facade.
    private class NullLinkProvider extends AbstractNullProvider implements LinkProvider {
    }

}
