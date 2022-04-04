/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.impl;

import com.google.common.collect.Lists;
import org.onlab.packet.IpAddress;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigService;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeAdminService;
import org.onosproject.kubevirtnode.api.KubevirtNodeEvent;
import org.onosproject.kubevirtnode.api.KubevirtNodeHandler;
import org.onosproject.kubevirtnode.api.KubevirtNodeListener;
import org.onosproject.kubevirtnode.api.KubevirtNodeState;
import org.onosproject.kubevirtnode.api.KubevirtPhyInterface;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.DefaultBridgeDescription;
import org.onosproject.net.behaviour.DefaultPatchDescription;
import org.onosproject.net.behaviour.DefaultTunnelDescription;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.behaviour.PatchDescription;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelEndPoints;
import org.onosproject.net.behaviour.TunnelKey;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.TpPort.tpPort;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnode.api.Constants.BRIDGE_PREFIX;
import static org.onosproject.kubevirtnode.api.Constants.FLOW_KEY;
import static org.onosproject.kubevirtnode.api.Constants.GENEVE;
import static org.onosproject.kubevirtnode.api.Constants.GRE;
import static org.onosproject.kubevirtnode.api.Constants.INTEGRATION_BRIDGE;
import static org.onosproject.kubevirtnode.api.Constants.INTEGRATION_TO_PHYSICAL_PREFIX;
import static org.onosproject.kubevirtnode.api.Constants.INTEGRATION_TO_TUNNEL;
import static org.onosproject.kubevirtnode.api.Constants.PHYSICAL_TO_INTEGRATION_SUFFIX;
import static org.onosproject.kubevirtnode.api.Constants.STT;
import static org.onosproject.kubevirtnode.api.Constants.TENANT_BRIDGE_PREFIX;
import static org.onosproject.kubevirtnode.api.Constants.TUNNEL_BRIDGE;
import static org.onosproject.kubevirtnode.api.Constants.TUNNEL_TO_INTEGRATION;
import static org.onosproject.kubevirtnode.api.Constants.VXLAN;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.GATEWAY;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.WORKER;
import static org.onosproject.kubevirtnode.api.KubevirtNodeService.APP_ID;
import static org.onosproject.kubevirtnode.api.KubevirtNodeState.COMPLETE;
import static org.onosproject.kubevirtnode.api.KubevirtNodeState.DEVICE_CREATED;
import static org.onosproject.kubevirtnode.api.KubevirtNodeState.INCOMPLETE;
import static org.onosproject.kubevirtnode.api.KubevirtNodeState.INIT;
import static org.onosproject.kubevirtnode.impl.OsgiPropertyConstants.AUTO_RECOVERY;
import static org.onosproject.kubevirtnode.impl.OsgiPropertyConstants.AUTO_RECOVERY_DEFAULT;
import static org.onosproject.kubevirtnode.impl.OsgiPropertyConstants.OVSDB_PORT;
import static org.onosproject.kubevirtnode.impl.OsgiPropertyConstants.OVSDB_PORT_NUM_DEFAULT;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.addOrRemoveSystemInterface;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.getBooleanProperty;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.getOvsdbClient;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.isOvsdbConnected;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.resolveHostname;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.structurePortName;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service bootstraps kubernetes node based on its type.
 */
@Component(immediate = true,
        property = {
                OVSDB_PORT + ":Integer=" + OVSDB_PORT_NUM_DEFAULT,
                AUTO_RECOVERY + ":Boolean=" + AUTO_RECOVERY_DEFAULT
        }
)
public class DefaultKubevirtNodeHandler implements KubevirtNodeHandler {

    private final Logger log = getLogger(getClass());

    private static final String DEFAULT_OF_PROTO = "tcp";
    private static final int DEFAULT_OFPORT = 6653;
    private static final int DPID_BEGIN = 3;
    private static final int NETWORK_BEGIN = 3;
    private static final long SLEEP_SHORT_MS = 1000; // we wait 1s
    private static final long SLEEP_MID_MS = 2000; // we wait 2s
    private static final long SLEEP_LONG_MS = 5000; // we wait 5s

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceAdminService deviceAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OvsdbController ovsdbController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeAdminService nodeAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtApiConfigService apiConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    /** OVSDB server listen port. */
    private int ovsdbPortNum = OVSDB_PORT_NUM_DEFAULT;

    /** Indicates whether auto-recover kubernetes node status on switch re-conn event. */
    private boolean autoRecovery = AUTO_RECOVERY_DEFAULT;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final DeviceListener ovsdbListener = new InternalOvsdbListener();
    private final DeviceListener bridgeListener = new InternalBridgeListener();
    private final KubevirtNodeListener kubevirtNodeListener = new InternalKubevirtNodeListener();

    private ApplicationId appId;
    private NodeId localNode;

    @Activate
    protected void activate() {
        appId = coreService.getAppId(APP_ID);
        localNode = clusterService.getLocalNode().id();

        componentConfigService.registerProperties(getClass());
        leadershipService.runForLeadership(appId.name());
        deviceService.addListener(ovsdbListener);
        deviceService.addListener(bridgeListener);
        nodeAdminService.addListener(kubevirtNodeListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        nodeAdminService.removeListener(kubevirtNodeListener);
        deviceService.removeListener(bridgeListener);
        deviceService.removeListener(ovsdbListener);
        componentConfigService.unregisterProperties(getClass(), false);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        readComponentConfiguration(context);

        log.info("Modified");
    }

    @Override
    public void processInitState(KubevirtNode node) {
        if (!isOvsdbConnected(node, ovsdbPortNum, ovsdbController, deviceService)) {
            ovsdbController.connect(node.managementIp(), tpPort(ovsdbPortNum));
            return;
        }
        if (!deviceService.isAvailable(node.intgBridge())) {
            createBridge(node, INTEGRATION_BRIDGE, node.intgBridge());
        }

        if (!deviceService.isAvailable(node.tunBridge())) {
            createBridge(node, TUNNEL_BRIDGE, node.tunBridge());
        }
    }

    @Override
    public void processDeviceCreatedState(KubevirtNode node) {
        try {
            if (!isOvsdbConnected(node, ovsdbPortNum, ovsdbController, deviceService)) {
                ovsdbController.connect(node.managementIp(), tpPort(ovsdbPortNum));
                return;
            }

            // create patch ports between integration to other bridges
            // for now, we do not directly connect br-int with br-tun,
            // as br-int only deals with FLAT and VLAN network
            // createPatchInterfaces(node);

            if (node.dataIp() != null && !isIntfEnabled(node, VXLAN)) {
                createVxlanTunnelInterface(node);
            }

            if (node.dataIp() != null && !isIntfEnabled(node, GRE)) {
                createGreTunnelInterface(node);
            }

            if (node.dataIp() != null && !isIntfEnabled(node, GENEVE)) {
                createGeneveTunnelInterface(node);
            }

            if (node.dataIp() != null && !isIntfEnabled(node, STT)) {
                createSttTunnelInterface(node);
            }
        } catch (Exception e) {
            log.error("Exception occurred because of {}", e);
        }
    }

    @Override
    public void processCompleteState(KubevirtNode node) {
        // do something if needed
    }

    @Override
    public void processIncompleteState(KubevirtNode node) {
        // do something if needed
    }

    @Override
    public void processOnBoardedState(KubevirtNode node) {
        // do something if needed
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        Integer ovsdbPortConfigured = Tools.getIntegerProperty(properties, OVSDB_PORT);
        if (ovsdbPortConfigured == null) {
            ovsdbPortNum = OVSDB_PORT_NUM_DEFAULT;
            log.info("OVSDB port is NOT configured, default value is {}", ovsdbPortNum);
        } else {
            ovsdbPortNum = ovsdbPortConfigured;
            log.info("Configured. OVSDB port is {}", ovsdbPortNum);
        }

        Boolean autoRecoveryConfigured =
                getBooleanProperty(properties, AUTO_RECOVERY);
        if (autoRecoveryConfigured == null) {
            autoRecovery = AUTO_RECOVERY_DEFAULT;
            log.info("Auto recovery flag is NOT " +
                    "configured, default value is {}", autoRecovery);
        } else {
            autoRecovery = autoRecoveryConfigured;
            log.info("Configured. Auto recovery flag is {}", autoRecovery);
        }
    }

    /**
     * Creates a bridge with a given name on a given kubernetes node.
     *
     * @param node          kubevirt node
     * @param bridgeName    bridge name
     * @param devId         device identifier
     */
    private void createBridge(KubevirtNode node, String bridgeName, DeviceId devId) {
        Device device = deviceService.getDevice(node.ovsdb());

        IpAddress controllerIp = apiConfigService.apiConfig().controllerIp();
        String serviceFqdn = apiConfigService.apiConfig().serviceFqdn();
        IpAddress serviceIp = null;

        if (controllerIp == null) {
            if (serviceFqdn != null) {
                serviceIp = resolveHostname(serviceFqdn);
            }

            if (serviceIp != null) {
                controllerIp = serviceIp;
            } else {
                controllerIp = apiConfigService.apiConfig().ipAddress();
            }
        }

        ControllerInfo controlInfo = new ControllerInfo(controllerIp, DEFAULT_OFPORT, DEFAULT_OF_PROTO);
        List<ControllerInfo> controllers = Lists.newArrayList(controlInfo);

        String dpid = devId.toString().substring(DPID_BEGIN);

        BridgeDescription.Builder builder = DefaultBridgeDescription.builder()
                .name(bridgeName)
                .failMode(BridgeDescription.FailMode.SECURE)
                .datapathId(dpid)
                .disableInBand()
                .controllers(controllers);

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        bridgeConfig.addBridge(builder.build());
    }

    /**
     * Creates a VXLAN tunnel interface in a given kubevirt node.
     *
     * @param node       kubevirt node
     */
    private void createVxlanTunnelInterface(KubevirtNode node) {
        createTunnelInterface(node, VXLAN, VXLAN);
    }

    /**
     * Creates a GRE tunnel interface in a given kubevirt node.
     *
     * @param node       kubevirt node
     */
    private void createGreTunnelInterface(KubevirtNode node) {
        createTunnelInterface(node, GRE, GRE);
    }

    /**
     * Creates a GENEVE tunnel interface in a given kubevirt node.
     *
     * @param node       kubevirt node
     */
    private void createGeneveTunnelInterface(KubevirtNode node) {
        createTunnelInterface(node, GENEVE, GENEVE);
    }

    private void createSttTunnelInterface(KubevirtNode node) {
        createTunnelInterface(node, STT, STT);
    }

    /**
     * Creates a tunnel interface in a given kubernetes node.
     *
     * @param node       kubevirt node
     * @param type       kubevirt type
     * @param intfName   tunnel interface name
     */
    private void createTunnelInterface(KubevirtNode node,
                                       String type, String intfName) {
        if (isIntfEnabled(node, intfName)) {
            return;
        }

        Device device = deviceService.getDevice(node.ovsdb());
        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create tunnel interface on {}", node.ovsdb());
            return;
        }

        TunnelDescription tunnelDesc = buildTunnelDesc(type, intfName);

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);
        ifaceConfig.addTunnelMode(intfName, tunnelDesc);
    }

    /**
     * Builds tunnel description according to the network type.
     *
     * @param type      network type
     * @param intfName  tunnel interface
     * @return tunnel description
     */
    private TunnelDescription buildTunnelDesc(String type, String intfName) {
        TunnelKey<String> key = new TunnelKey<>(FLOW_KEY);
        if (VXLAN.equals(type) || GRE.equals(type) || GENEVE.equals(type) || STT.equals(type)) {
            TunnelDescription.Builder tdBuilder =
                    DefaultTunnelDescription.builder()
                            .deviceId(TUNNEL_BRIDGE)
                            .ifaceName(intfName)
                            .remote(TunnelEndPoints.flowTunnelEndpoint())
                            .key(key);

            switch (type) {
                case VXLAN:
                    tdBuilder.type(TunnelDescription.Type.VXLAN);
                    break;
                case GRE:
                    tdBuilder.type(TunnelDescription.Type.GRE);
                    break;
                case GENEVE:
                    tdBuilder.type(TunnelDescription.Type.GENEVE);
                    break;
                case STT:
                    tdBuilder.type(TunnelDescription.Type.STT);
                    break;
                default:
                    return null;
            }

            return tdBuilder.build();
        }
        return null;
    }

    /**
     * Checks whether a given network interface in a given kubernetes node
     * is enabled or not.
     *
     * @param node          kubevirt node
     * @param intf          network interface name
     * @return true if the given interface is enabled, false otherwise
     */
    private boolean isIntfEnabled(KubevirtNode node, String intf) {
        return deviceService.isAvailable(node.tunBridge()) &&
                deviceService.getPorts(node.tunBridge()).stream()
                        .anyMatch(port -> Objects.equals(
                                port.annotations().value(PORT_NAME), intf) &&
                                port.isEnabled());
    }

    /**
     * Bootstraps a new kubevirt node.
     *
     * @param node kubevirt node
     */
    private void bootstrapNode(KubevirtNode node) {
        if (isCurrentStateDone(node)) {
            setState(node, node.state().nextState());
        } else {
            log.trace("Processing {} state for {}", node.state(), node.hostname());
            node.state().process(this, node);
        }
    }

    /**
     * Removes the existing kubevirt node.
     *
     * @param node kubevirt node
     */
    private void removeNode(KubevirtNode node) {
        OvsdbClientService client = getOvsdbClient(node, ovsdbPortNum, ovsdbController);
        if (client == null) {
            log.info("Failed to get ovsdb client");
            return;
        }

        // purges all the flow rules installed on the node
        flowRuleService.purgeFlowRules(node.intgBridge());
        flowRuleService.purgeFlowRules(node.tunBridge());

        // unprovision physical interfaces from the node
        // this procedure includes detaching physical port from physical bridge,
        // remove patch ports from br-int, removing physical bridge
        unprovisionPhysicalInterfaces(node);

        // delete tunnel bridge from the node
        client.dropBridge(TUNNEL_BRIDGE);

        // delete integration bridge from the node
        client.dropBridge(INTEGRATION_BRIDGE);
    }

    /**
     * Checks whether all requirements for this state are fulfilled or not.
     *
     * @param node       kubevirt node
     * @return true if all requirements are fulfilled, false otherwise
     */
    private boolean isCurrentStateDone(KubevirtNode node) {
        switch (node.state()) {
            case INIT:
                return isInitStateDone(node);
            case DEVICE_CREATED:
                return isDeviceCreatedStateDone(node);
            case COMPLETE:
            case INCOMPLETE:
            case ON_BOARDED:
                // always return false
                // run init CLI to re-trigger node bootstrap
                return false;
            default:
                return true;
        }
    }

    private boolean isInitStateDone(KubevirtNode node) {
        if (!isOvsdbConnected(node, ovsdbPortNum,
                ovsdbController, deviceService)) {
            return false;
        }

        try {
            // we need to wait a while, in case interfaces and bridges
            // creation requires some time
            sleep(SLEEP_SHORT_MS);
        } catch (InterruptedException e) {
            log.error("Exception caused during init state checking...");
        }

        cleanPhysicalInterfaces(node);

        // provision new physical interfaces on the given node
        // this includes creating physical bridge, attaching physical port
        // to physical bridge, adding patch ports to both physical bridge and br-int
        provisionPhysicalInterfaces(node);

        if (node.type() == GATEWAY) {
            createPatchInterfaceBetweenBrIntBrTun(node);
        }

        return node.intgBridge() != null && node.tunBridge() != null &&
                deviceService.isAvailable(node.intgBridge()) &&
                deviceService.isAvailable(node.tunBridge());
    }

    private boolean isDeviceCreatedStateDone(KubevirtNode node) {

        try {
            // we need to wait a while, in case tunneling ports
            // creation requires some time
            sleep(SLEEP_MID_MS);
        } catch (InterruptedException e) {
            log.error("Exception caused during init state checking...");
        }

        if (node.dataIp() != null && !isIntfEnabled(node, VXLAN)) {
            log.warn("VXLAN interface is not enabled!");
            return false;
        }
        if (node.dataIp() != null && !isIntfEnabled(node, GRE)) {
            log.warn("GRE interface is not enabled!");
            return false;
        }
        if (node.dataIp() != null && !isIntfEnabled(node, GENEVE)) {
            log.warn("GENEVE interface is not enabled!");
            return false;
        }

        // we make the STT tunnel port check optional

        for (KubevirtPhyInterface phyIntf : node.phyIntfs()) {
            if (phyIntf == null) {
                log.warn("Physnet interface is invalid");
                return false;
            }

            try {
                // we need to wait a while, in case tunneling ports
                // creation requires some time
                sleep(SLEEP_LONG_MS);
            } catch (InterruptedException e) {
                log.error("Exception caused during init state checking...");
            }

            String patchPortName = structurePortName(
                    INTEGRATION_TO_PHYSICAL_PREFIX + phyIntf.network());

            if (node.type() == WORKER) {
                String bridgeName = BRIDGE_PREFIX + phyIntf.network();
                if (!(hasPhyBridge(node, bridgeName) &&
                        hasPhyPatchPort(node, patchPortName) &&
                        hasPhyIntf(node, phyIntf.intf()))) {
                    log.warn("PhyBridge {}", hasPhyBridge(node, bridgeName));
                    log.warn("hasPhyPatchPort {}", hasPhyPatchPort(node, patchPortName));
                    log.warn("hasPhyIntf {}", hasPhyIntf(node, phyIntf.intf()));
                    return false;
                }
            } else {
                //In case node type is GATEWAY, we create physical bridges connected to the contoller.
                //By doing so, ONOS immediately recognizes the status of physical interface and performs RM procedures.
                Port phyIntfPort = deviceService.getPorts(phyIntf.physBridge()).stream()
                        .filter(port -> port.annotations().value(PORT_NAME).equals(phyIntf.intf()))
                        .findAny().orElse(null);
                if (phyIntfPort == null) {
                    log.warn("There's no connected physical port {} on physical device {}",
                            phyIntf.intf(), phyIntf.physBridge());
                }

                if (!(deviceService.isAvailable(phyIntf.physBridge()) &&
                        hasPhyPatchPort(node, patchPortName) &&
                        hasPhyIntf(node, phyIntf.intf()) &&
                        phyIntfPort.isEnabled())) {
                    log.warn("PhysBridge {}", deviceService.isAvailable(phyIntf.physBridge()));
                    log.warn("hasPhyPatchPort {}", hasPhyPatchPort(node, patchPortName));
                    log.warn("hasPhyIntf {}", hasPhyIntf(node, phyIntf.intf()));
                    log.warn("physical interface port {}", phyIntfPort.isEnabled());
                    return false;
                }
            }

        }

        if (node.type() == GATEWAY) {
            if (!(hasPhyIntf(node, INTEGRATION_TO_TUNNEL) &&
                    hasPhyIntf(node, TUNNEL_TO_INTEGRATION))) {
                log.warn("IntToTunPort {}", hasPhyIntf(node, INTEGRATION_TO_TUNNEL));
                log.warn("TunToIntPort {}", hasPhyIntf(node, TUNNEL_TO_INTEGRATION));
                return false;
            }
        }
        return true;
    }

    private boolean hasPhyBridge(KubevirtNode node, String bridgeName) {
        BridgeConfig bridgeConfig =
                deviceService.getDevice(node.ovsdb()).as(BridgeConfig.class);
        return bridgeConfig.getBridges().stream()
                .anyMatch(br -> br.name().equals(bridgeName));
    }
    /**
     * Configures the kubernetes node with new state.
     *
     * @param node          kubevirt node
     * @param newState      a new state
     */
    private void setState(KubevirtNode node, KubevirtNodeState newState) {
        if (node.state() == newState) {
            return;
        }
        KubevirtNode updated = node.updateState(newState);
        nodeAdminService.updateNode(updated);
        log.info("Changed {} state: {}", node.hostname(), newState);
    }

    private void provisionPhysicalInterfaces(KubevirtNode node) {
        node.phyIntfs().forEach(pi -> {
            String bridgeName = BRIDGE_PREFIX + pi.network();
            String patchPortName =
                    structurePortName(INTEGRATION_TO_PHYSICAL_PREFIX + pi.network());
            if (node.type() == WORKER && !hasPhyBridge(node, bridgeName)) {
                createPhysicalBridge(node, pi);
                createPhysicalPatchPorts(node, pi);
                attachPhysicalPort(node, pi);

                log.info("Creating physnet bridge {} for worker node {}", bridgeName, node.hostname());
                log.info("Creating patch ports for physnet {} for worker node {}", bridgeName, node.hostname());

            } else if (node.type() == GATEWAY && (!deviceService.isAvailable(pi.physBridge()))) {
                createPhysicalBridgeWithConnectedMode(node, pi);
                createPhysicalPatchPorts(node, pi);
                attachPhysicalPort(node, pi);

                log.info("Creating physnet bridge {} for gateway node {}", bridgeName, node.hostname());
                log.info("Creating patch ports for physnet {} for gateway node {}", bridgeName, node.hostname());
            } else {
                // in case physical bridge exists, but patch port is missing,
                // we will add patch port to connect br-physnet with physical bridge
                if (!hasPhyPatchPort(node, patchPortName)) {
                    createPhysicalPatchPorts(node, pi);

                    log.info("Creating patch ports for physnet {}", bridgeName);
                }

                // in case physical bridge exists, but physnet interface is missing,
                // we will add the physnet interface to connect br-physnet to the external
                if (!hasPhyIntf(node, pi.intf())) {
                    attachPhysicalPort(node, pi);

                    log.info("Attaching external ports for physnet {}", bridgeName);
                }
            }
        });
    }

    private void cleanPhysicalInterfaces(KubevirtNode node) {
        Device device = deviceService.getDevice(node.ovsdb());

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);

        Set<String> bridgeNames = bridgeConfig.getBridges().stream()
                .map(BridgeDescription::name).collect(Collectors.toSet());

        Set<String> phyNetworkNames = node.phyIntfs().stream()
                .map(pi -> BRIDGE_PREFIX + pi.network()).collect(Collectors.toSet());

        // we remove existing physical bridges and patch ports, if the physical
        // bridges are not defined in kubevirt node
        for (String brName : bridgeNames) {
            // integration bridge and tunnel bridge should NOT be treated as
            // physical bridges
            if (brName.equals(INTEGRATION_BRIDGE) ||
                    brName.equals(TUNNEL_BRIDGE) ||
                    brName.startsWith(TENANT_BRIDGE_PREFIX)) {
                continue;
            }

            if (!phyNetworkNames.contains(brName)) {
                removePhysicalPatchPorts(node, brName.substring(NETWORK_BEGIN));
                removePhysicalBridge(node, brName.substring(NETWORK_BEGIN));
                log.info("Removing physical bridge {}...", brName);
            }
        }
    }


    private void createPatchInterfaceBetweenBrIntBrTun(KubevirtNode node) {
        Device device = deviceService.getDevice(node.ovsdb());

        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create patch interface on {}", node.ovsdb());
            return;
        }

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);

        // int bridge -> tunnel bridge
        PatchDescription brIntTunPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(INTEGRATION_BRIDGE)
                        .ifaceName(INTEGRATION_TO_TUNNEL)
                        .peer(TUNNEL_TO_INTEGRATION)
                        .build();

        ifaceConfig.addPatchMode(INTEGRATION_TO_TUNNEL, brIntTunPatchDesc);

        // tunnel bridge -> int bridge
        PatchDescription brTunIntPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(TUNNEL_BRIDGE)
                        .ifaceName(TUNNEL_TO_INTEGRATION)
                        .peer(INTEGRATION_TO_TUNNEL)
                        .build();

        ifaceConfig.addPatchMode(TUNNEL_TO_INTEGRATION, brTunIntPatchDesc);
    }

    private void unprovisionPhysicalInterfaces(KubevirtNode node) {
        node.phyIntfs().forEach(pi -> {
            detachPhysicalPort(node, pi.network(), pi.intf());
            removePhysicalPatchPorts(node, pi.network());
            removePhysicalBridge(node, pi.network());
        });
    }

    private boolean hasPhyPatchPort(KubevirtNode node, String patchPortName) {
        List<Port> ports = deviceService.getPorts(node.intgBridge());
        return ports.stream().anyMatch(p ->
                p.annotations().value(PORT_NAME).equals(patchPortName));
    }

    private boolean hasPhyIntf(KubevirtNode node, String intfName) {
        BridgeConfig bridgeConfig =
                deviceService.getDevice(node.ovsdb()).as(BridgeConfig.class);
        return bridgeConfig.getPorts().stream()
                .anyMatch(p -> p.annotations().value(PORT_NAME).equals(intfName));
    }

    private void createPhysicalBridge(KubevirtNode osNode,
                                      KubevirtPhyInterface phyInterface) {
        Device device = deviceService.getDevice(osNode.ovsdb());

        String bridgeName = BRIDGE_PREFIX + phyInterface.network();

        BridgeDescription.Builder builder = DefaultBridgeDescription.builder()
                .name(bridgeName)
                .mcastSnoopingEnable();

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        bridgeConfig.addBridge(builder.build());
    }

    private void createPhysicalBridgeWithConnectedMode(KubevirtNode osNode,
                                                       KubevirtPhyInterface phyInterface) {
        Device device = deviceService.getDevice(osNode.ovsdb());
        IpAddress controllerIp = apiConfigService.apiConfig().controllerIp();
        String serviceFqdn = apiConfigService.apiConfig().serviceFqdn();
        IpAddress serviceIp = null;

        if (controllerIp == null) {
            if (serviceFqdn != null) {
                serviceIp = resolveHostname(serviceFqdn);
            }

            if (serviceIp != null) {
                controllerIp = serviceIp;
            } else {
                controllerIp = apiConfigService.apiConfig().ipAddress();
            }
        }

        ControllerInfo controlInfo = new ControllerInfo(controllerIp, DEFAULT_OFPORT, DEFAULT_OF_PROTO);
        List<ControllerInfo> controllers = Lists.newArrayList(controlInfo);

        String dpid = phyInterface.physBridge().toString().substring(DPID_BEGIN);

        String bridgeName = BRIDGE_PREFIX + phyInterface.network();

        BridgeDescription.Builder builder = DefaultBridgeDescription.builder()
                .name(bridgeName)
                .failMode(BridgeDescription.FailMode.SECURE)
                .datapathId(dpid)
                .mcastSnoopingEnable()
                .disableInBand()
                .controllers(controllers);

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        bridgeConfig.addBridge(builder.build());
    }

    private void removePhysicalBridge(KubevirtNode node, String network) {
        Device device = deviceService.getDevice(node.ovsdb());

        BridgeName bridgeName = BridgeName.bridgeName(BRIDGE_PREFIX + network);

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        bridgeConfig.deleteBridge(bridgeName);
    }

    private void createPhysicalPatchPorts(KubevirtNode node,
                                          KubevirtPhyInterface phyInterface) {
        Device device = deviceService.getDevice(node.ovsdb());

        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create patch interface on {}", node.ovsdb());
            return;
        }

        String physicalDeviceId = BRIDGE_PREFIX + phyInterface.network();

        String intToPhyPatchPort = structurePortName(
                INTEGRATION_TO_PHYSICAL_PREFIX + phyInterface.network());
        String phyToIntPatchPort = structurePortName(
                phyInterface.network() + PHYSICAL_TO_INTEGRATION_SUFFIX);

        // integration bridge -> physical bridge
        PatchDescription intToPhyPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(INTEGRATION_BRIDGE)
                        .ifaceName(intToPhyPatchPort)
                        .peer(phyToIntPatchPort)
                        .build();

        // physical bridge -> integration bridge
        PatchDescription phyToIntPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(physicalDeviceId)
                        .ifaceName(phyToIntPatchPort)
                        .peer(intToPhyPatchPort)
                        .build();

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);
        ifaceConfig.addPatchMode(INTEGRATION_TO_PHYSICAL_PREFIX +
                phyInterface.network(), intToPhyPatchDesc);
        ifaceConfig.addPatchMode(phyInterface.network() +
                PHYSICAL_TO_INTEGRATION_SUFFIX, phyToIntPatchDesc);

        addOrRemoveSystemInterface(node, physicalDeviceId,
                phyInterface.intf(), deviceService, true);
    }

    private void removePhysicalPatchPorts(KubevirtNode node, String network) {
        Device device = deviceService.getDevice(node.ovsdb());

        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to remove patch interface on {}", node.ovsdb());
            return;
        }

        String intToPhyPatchPort = structurePortName(
                INTEGRATION_TO_PHYSICAL_PREFIX + network);

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);
        ifaceConfig.removePatchMode(intToPhyPatchPort);
    }

    private void attachPhysicalPort(KubevirtNode node,
                                    KubevirtPhyInterface phyInterface) {

        String physicalDeviceId = BRIDGE_PREFIX + phyInterface.network();

        addOrRemoveSystemInterface(node, physicalDeviceId,
                phyInterface.intf(), deviceService, true);
    }

    private void detachPhysicalPort(KubevirtNode node, String network, String portName) {
        String physicalDeviceId = BRIDGE_PREFIX + network;

        addOrRemoveSystemInterface(node, physicalDeviceId, portName, deviceService, false);
    }

    private KubevirtNode nodeByTunOrPhyBridge(DeviceId deviceId) {
        KubevirtNode node = nodeAdminService.nodeByTunBridge(deviceId);
        if (node == null) {
            node = nodeAdminService.nodeByPhyBridge(deviceId);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    /**
     * An internal OVSDB listener. This listener is used for listening the
     * network facing events from OVSDB device. If a new OVSDB device is detected,
     * ONOS tries to bootstrap the kubernetes node.
     */
    private class InternalOvsdbListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.subject().type() == Device.Type.CONTROLLER;
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNode, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();

            switch (event.type()) {
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_ADDED:
                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper()) {
                            return;
                        }

                        KubevirtNode node = nodeAdminService.node(device.id());

                        if (node == null) {
                            return;
                        }

                        if (deviceService.isAvailable(device.id())) {
                            log.debug("OVSDB {} detected", device.id());
                            bootstrapNode(node);
                        }
                    });
                    break;
                case PORT_ADDED:
                case PORT_REMOVED:
                case DEVICE_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }
    }

    /**
     * An internal integration bridge listener. This listener is used for
     * listening the events from integration bridge. To listen the events from
     * other types of bridge such as provider bridge or tunnel bridge, we need
     * to augment KubevirtNodeService.node() method.
     */
    private class InternalBridgeListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.subject().type() == Device.Type.SWITCH;
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNode, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            Port port = event.port();

            switch (event.type()) {
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_ADDED:
                    eventExecutor.execute(() -> processDeviceAddition(device));
                    break;
                case PORT_UPDATED:
                case PORT_ADDED:
                    eventExecutor.execute(() -> processPortAdditionOrUpdate(device, port));
                    break;
                case PORT_REMOVED:
                    eventExecutor.execute(() -> processPortRemoval(device, port));
                    break;
                case DEVICE_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }

        void processDeviceAddition(Device device) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtNode node = nodeAdminService.node(device.id());

            if (node == null) {
                return;
            }

            if (deviceService.isAvailable(device.id())) {
                log.debug("Bridge created on {}", node.hostname());
                bootstrapNode(node);
            } else if (node.state() == COMPLETE) {
                log.info("Device {} disconnected", device.id());
                setState(node, INCOMPLETE);
            }

            if (autoRecovery) {
                if (node.state() == INCOMPLETE || node.state() == DEVICE_CREATED) {
                    log.info("Device {} is reconnected", device.id());
                    nodeAdminService.updateNode(node.updateState(INIT));
                }
            }
        }

        void processPortAdditionOrUpdate(Device device, Port port) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtNode node = nodeByTunOrPhyBridge(device.id());

            if (node == null) {
                return;
            }

            String portName = port.annotations().value(PORT_NAME);
            if (node.state() == DEVICE_CREATED && (
                    Objects.equals(portName, VXLAN) ||
                            Objects.equals(portName, GRE) ||
                            Objects.equals(portName, GENEVE) ||
                            Objects.equals(portName, STT))) {
                log.info("Interface {} added or updated to {}",
                        portName, device.id());
                bootstrapNode(node);
            }

            //When the physical port is down, in the middle of normal operation, we set the node state to INCOMPLTE
            //so that respective handlers do their related jobs.
            if (node.state() == COMPLETE && node.type().equals(GATEWAY) && !port.isEnabled()) {
                node.phyIntfs().stream()
                        .filter(pi -> pi.intf().equals(portName))
                        .findAny()
                        .ifPresent(pi -> setState(node, INCOMPLETE));
            }

            //When the physical port up again, we set the node state to INIT
            //so that respective handlers do their related jobs.
            if (node.state() == INCOMPLETE && node.type().equals(GATEWAY) && port.isEnabled()) {
                node.phyIntfs().stream()
                        .filter(pi -> pi.intf().equals(portName))
                        .findAny()
                        .ifPresent(pi -> setState(node, INIT));
            }
        }

        void processPortRemoval(Device device, Port port) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtNode node = nodeAdminService.node(device.id());

            if (node == null) {
                return;
            }

            String portName = port.annotations().value(PORT_NAME);
            if (node.state() == COMPLETE && (
                    Objects.equals(portName, VXLAN) ||
                            Objects.equals(portName, GRE) ||
                            Objects.equals(portName, GENEVE) ||
                            Objects.equals(portName, STT))) {
                log.warn("Interface {} removed from {}", portName, device.id());
                setState(node, INCOMPLETE);
            }
        }
    }

    /**
     * An internal kubevirt node listener.
     * The notification is triggered by KubevirtNodeStore.
     */
    private class InternalKubevirtNodeListener implements KubevirtNodeListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNode, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtNodeEvent event) {
            switch (event.type()) {
                case KUBEVIRT_NODE_CREATED:
                case KUBEVIRT_NODE_UPDATED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }
                        if (event.subject() == null) {
                            return;
                        }
                        bootstrapNode(event.subject());
                    });
                    break;
                case KUBEVIRT_NODE_REMOVED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }
                        removeNode(event.subject());
                    });
                    break;
                case KUBEVIRT_NODE_INCOMPLETE:
                default:
                    break;
            }
        }
    }
}
