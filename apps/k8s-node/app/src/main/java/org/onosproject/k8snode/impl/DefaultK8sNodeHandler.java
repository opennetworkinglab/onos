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
package org.onosproject.k8snode.impl;

import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snode.api.K8sApiConfigService;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeAdminService;
import org.onosproject.k8snode.api.K8sNodeEvent;
import org.onosproject.k8snode.api.K8sNodeHandler;
import org.onosproject.k8snode.api.K8sNodeListener;
import org.onosproject.k8snode.api.K8sNodeService;
import org.onosproject.k8snode.api.K8sNodeState;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
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
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.TpPort.tpPort;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snode.api.Constants.GENEVE;
import static org.onosproject.k8snode.api.Constants.GRE;
import static org.onosproject.k8snode.api.Constants.VXLAN;
import static org.onosproject.k8snode.api.K8sApiConfig.Mode.NORMAL;
import static org.onosproject.k8snode.api.K8sNodeService.APP_ID;
import static org.onosproject.k8snode.api.K8sNodeState.COMPLETE;
import static org.onosproject.k8snode.api.K8sNodeState.DEVICE_CREATED;
import static org.onosproject.k8snode.api.K8sNodeState.INCOMPLETE;
import static org.onosproject.k8snode.impl.OsgiPropertyConstants.AUTO_RECOVERY;
import static org.onosproject.k8snode.impl.OsgiPropertyConstants.AUTO_RECOVERY_DEFAULT;
import static org.onosproject.k8snode.impl.OsgiPropertyConstants.OVSDB_PORT;
import static org.onosproject.k8snode.impl.OsgiPropertyConstants.OVSDB_PORT_NUM_DEFAULT;
import static org.onosproject.k8snode.util.K8sNodeUtil.getBooleanProperty;
import static org.onosproject.k8snode.util.K8sNodeUtil.getOvsdbClient;
import static org.onosproject.k8snode.util.K8sNodeUtil.isOvsdbConnected;
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
public class DefaultK8sNodeHandler implements K8sNodeHandler {

    private final Logger log = getLogger(getClass());

    private static final String DEFAULT_OF_PROTO = "tcp";
    private static final int DEFAULT_OFPORT = 6653;
    private static final int DPID_BEGIN = 3;
    private static final long SLEEP_MS = 3000; // we wait 3s

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
    protected K8sApiConfigService k8sApiConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeService k8sNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeAdminService k8sNodeAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    /** OVSDB server listen port. */
    private int ovsdbPortNum = OVSDB_PORT_NUM_DEFAULT;

    /** Indicates whether auto-recover kubernetes node status on switch re-conn event. */
    private boolean autoRecovery = AUTO_RECOVERY_DEFAULT;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final DeviceListener ovsdbListener = new InternalOvsdbListener();
    private final DeviceListener bridgeListener = new InternalBridgeListener();
    private final K8sNodeListener k8sNodeListener = new InternalK8sNodeListener();

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
        k8sNodeService.addListener(k8sNodeListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sNodeService.removeListener(k8sNodeListener);
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
    public void processInitState(K8sNode k8sNode) {
        if (!isOvsdbConnected(k8sNode, ovsdbPortNum, ovsdbController, deviceService)) {
            ovsdbController.connect(k8sNode.managementIp(), tpPort(ovsdbPortNum));
            return;
        }
        if (!deviceService.isAvailable(k8sNode.intgBridge())) {
            createBridge(k8sNode, k8sNode.intgBridgeName(), k8sNode.intgBridge());
        }
        if (!deviceService.isAvailable(k8sNode.extBridge())) {
            createBridge(k8sNode, k8sNode.extBridgeName(), k8sNode.extBridge());
        }
        if (!deviceService.isAvailable(k8sNode.localBridge())) {
            createBridge(k8sNode, k8sNode.localBridgeName(), k8sNode.localBridge());
        }

        if (k8sNode.mode() == NORMAL) {
            if (!deviceService.isAvailable(k8sNode.tunBridge())) {
                createBridge(k8sNode, k8sNode.tunBridgeName(), k8sNode.tunBridge());
            }
        }
    }

    @Override
    public void processDeviceCreatedState(K8sNode k8sNode) {
        try {
            if (!isOvsdbConnected(k8sNode, ovsdbPortNum, ovsdbController, deviceService)) {
                ovsdbController.connect(k8sNode.managementIp(), tpPort(ovsdbPortNum));
                return;
            }

            // create patch ports between integration and external bridges
            createPatchInterfaces(k8sNode);

            if (k8sNode.mode() == NORMAL) {
                if (k8sNode.dataIp() != null &&
                        !isIntfEnabled(k8sNode, k8sNode.vxlanPortName())) {
                    createVxlanTunnelInterface(k8sNode);
                }

                if (k8sNode.dataIp() != null &&
                        !isIntfEnabled(k8sNode, k8sNode.grePortName())) {
                    createGreTunnelInterface(k8sNode);
                }

                if (k8sNode.dataIp() != null &&
                        !isIntfEnabled(k8sNode, k8sNode.genevePortName())) {
                    createGeneveTunnelInterface(k8sNode);
                }
            }
        } catch (Exception e) {
            log.error("Exception occurred because of {}", e);
        }
    }

    @Override
    public void processCompleteState(K8sNode k8sNode) {
        // do something if needed
    }

    @Override
    public void processIncompleteState(K8sNode k8sNode) {
        // do something if needed
    }

    @Override
    public void processPreOnBoardState(K8sNode k8sNode) {
        processInitState(k8sNode);
        processDeviceCreatedState(k8sNode);
    }

    @Override
    public void processOnBoardedState(K8sNode k8sNode) {
        // do something if needed
    }

    @Override
    public void processPostOnBoardState(K8sNode k8sNode) {
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
     * @param k8sNode       kubernetes node
     * @param bridgeName    bridge name
     * @param devId         device identifier
     */
    private void createBridge(K8sNode k8sNode, String bridgeName, DeviceId devId) {
        Device device = deviceService.getDevice(k8sNode.ovsdb());

        List<ControllerInfo> controllers = k8sApiConfigService.apiConfigs().stream()
                .map(c -> new ControllerInfo(c.ipAddress(), DEFAULT_OFPORT, DEFAULT_OF_PROTO))
                .collect(Collectors.toList());

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
     * Creates a VXLAN tunnel interface in a given kubernetes node.
     *
     * @param k8sNode       kubernetes node
     */
    private void createVxlanTunnelInterface(K8sNode k8sNode) {
        createTunnelInterface(k8sNode, VXLAN, k8sNode.vxlanPortName());
    }

    /**
     * Creates a GRE tunnel interface in a given kubernetes node.
     *
     * @param k8sNode       kubernetes node
     */
    private void createGreTunnelInterface(K8sNode k8sNode) {
        createTunnelInterface(k8sNode, GRE, k8sNode.grePortName());
    }

    /**
     * Creates a GENEVE tunnel interface in a given kubernetes node.
     *
     * @param k8sNode       kubernetes node
     */
    private void createGeneveTunnelInterface(K8sNode k8sNode) {
        createTunnelInterface(k8sNode, GENEVE, k8sNode.genevePortName());
    }

    private void createPatchInterfaces(K8sNode k8sNode) {
        Device device = deviceService.getDevice(k8sNode.ovsdb());
        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create patch interface on {}", k8sNode.ovsdb());
            return;
        }

        // integration bridge -> external bridge
        PatchDescription brIntExtPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(k8sNode.intgBridgeName())
                        .ifaceName(k8sNode.intgToExtPatchPortName())
                        .peer(k8sNode.extToIntgPatchPortName())
                        .build();

        // integration bridge -> tunnel bridge
        PatchDescription brIntTunPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(k8sNode.intgBridgeName())
                        .ifaceName(k8sNode.intgToTunPatchPortName())
                        .peer(k8sNode.tunToIntgPatchPortName())
                        .build();

        // external bridge -> integration bridge
        PatchDescription brExtIntPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(k8sNode.extBridgeName())
                        .ifaceName(k8sNode.extToIntgPatchPortName())
                        .peer(k8sNode.intgToExtPatchPortName())
                        .build();

        // integration bridge -> local bridge
        PatchDescription brIntLocalPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(k8sNode.intgBridgeName())
                        .ifaceName(k8sNode.intgToLocalPatchPortName())
                        .peer(k8sNode.localToIntgPatchPortName())
                        .build();

        // local bridge -> integration bridge
        PatchDescription brLocalIntPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(k8sNode.localBridgeName())
                        .ifaceName(k8sNode.localToIntgPatchPortName())
                        .peer(k8sNode.intgToLocalPatchPortName())
                        .build();

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);
        ifaceConfig.addPatchMode(k8sNode.intgToExtPatchPortName(), brIntExtPatchDesc);
        ifaceConfig.addPatchMode(k8sNode.extToIntgPatchPortName(), brExtIntPatchDesc);
        ifaceConfig.addPatchMode(k8sNode.intgToLocalPatchPortName(), brIntLocalPatchDesc);
        ifaceConfig.addPatchMode(k8sNode.localToIntgPatchPortName(), brLocalIntPatchDesc);
        ifaceConfig.addPatchMode(k8sNode.intgToTunPatchPortName(), brIntTunPatchDesc);

        if (k8sNode.mode() == NORMAL) {
            // tunnel bridge -> integration bridge
            PatchDescription brTunIntPatchDesc =
                    DefaultPatchDescription.builder()
                            .deviceId(k8sNode.tunBridgeName())
                            .ifaceName(k8sNode.tunToIntgPatchPortName())
                            .peer(k8sNode.intgToTunPatchPortName())
                            .build();

            ifaceConfig.addPatchMode(k8sNode.tunToIntgPatchPortName(), brTunIntPatchDesc);
        } else {
            // k8s integration bridge -> openstack integration bridge
            PatchDescription k8sIntOsIntPatchDesc =
                    DefaultPatchDescription.builder()
                            .deviceId(k8sNode.intgBridgeName())
                            .ifaceName(k8sNode.k8sIntgToOsPatchPortName())
                            .peer(k8sNode.osToK8sIntgPatchPortName())
                            .build();
            ifaceConfig.addPatchMode(k8sNode.k8sIntgToOsPatchPortName(), k8sIntOsIntPatchDesc);

            // k8s external bridge -> openstack integration bridge
            PatchDescription k8sExtOsIntPatchDesc =
                    DefaultPatchDescription.builder()
                            .deviceId(k8sNode.extBridgeName())
                            .ifaceName(k8sNode.k8sExtToOsPatchPortName())
                            .peer(k8sNode.osToK8sExtPatchPortName())
                            .build();
            ifaceConfig.addPatchMode(k8sNode.k8sExtToOsPatchPortName(), k8sExtOsIntPatchDesc);

            // external bridge -> router bridge
            PatchDescription extRouterPatchDesc =
                    DefaultPatchDescription.builder()
                            .deviceId(k8sNode.extBridgeName())
                            .ifaceName(k8sNode.extToRouterPatchPortName())
                            .peer(k8sNode.routerToExtPatchPortName())
                            .build();
            ifaceConfig.addPatchMode(k8sNode.extToRouterPatchPortName(), extRouterPatchDesc);
        }
    }

    /**
     * Creates a tunnel interface in a given kubernetes node.
     *
     * @param k8sNode       kubernetes node
     */
    private void createTunnelInterface(K8sNode k8sNode,
                                       String type, String intfName) {
        if (isIntfEnabled(k8sNode, intfName)) {
            return;
        }

        Device device = deviceService.getDevice(k8sNode.ovsdb());
        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create tunnel interface on {}", k8sNode.ovsdb());
            return;
        }

        TunnelDescription tunnelDesc = buildTunnelDesc(k8sNode, type, intfName);

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);
        ifaceConfig.addTunnelMode(intfName, tunnelDesc);
    }

    /**
     * Builds tunnel description according to the network type.
     *
     * @param type      network type
     * @return tunnel description
     */
    private TunnelDescription buildTunnelDesc(K8sNode k8sNode,
                                              String type, String intfName) {
        TunnelKey<String> key = new TunnelKey<>(k8sNode.tunnelKey());
        if (VXLAN.equals(type) || GRE.equals(type) || GENEVE.equals(type)) {
            TunnelDescription.Builder tdBuilder =
                    DefaultTunnelDescription.builder()
                            .deviceId(k8sNode.tunBridgeName())
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
     * @param k8sNode       kubernetes node
     * @param intf          network interface name
     * @return true if the given interface is enabled, false otherwise
     */
    private boolean isIntfEnabled(K8sNode k8sNode, String intf) {
        return deviceService.isAvailable(k8sNode.tunBridge()) &&
                deviceService.getPorts(k8sNode.tunBridge()).stream()
                        .anyMatch(port -> Objects.equals(
                                port.annotations().value(PORT_NAME), intf) &&
                                port.isEnabled());
    }

    /**
     * Checks whether all requirements for this state are fulfilled or not.
     *
     * @param k8sNode       kubernetes node
     * @return true if all requirements are fulfilled, false otherwise
     */
    private boolean isCurrentStateDone(K8sNode k8sNode) {
        switch (k8sNode.state()) {
            case INIT:
                return isInitStateDone(k8sNode);
            case DEVICE_CREATED:
                return isDeviceCreatedStateDone(k8sNode);
            case PRE_ON_BOARD:
                return isInitStateDone(k8sNode) && isDeviceCreatedStateDone(k8sNode);
            case COMPLETE:
            case INCOMPLETE:
            case ON_BOARDED:
            case POST_ON_BOARD:
                // always return false
                // run init CLI to re-trigger node bootstrap
                return false;
            default:
                return true;
        }
    }

    private boolean isInitStateDone(K8sNode k8sNode) {
        if (!isOvsdbConnected(k8sNode, ovsdbPortNum,
                ovsdbController, deviceService)) {
            return false;
        }

        try {
            // we need to wait a while, in case interface and bridge
            // creation requires some time
            sleep(SLEEP_MS);
        } catch (InterruptedException e) {
            log.error("Exception caused during init state checking...");
        }

        boolean result = k8sNode.intgBridge() != null && k8sNode.extBridge() != null &&
                deviceService.isAvailable(k8sNode.intgBridge()) &&
                deviceService.isAvailable(k8sNode.extBridge()) &&
                deviceService.isAvailable(k8sNode.localBridge());

        if (k8sNode.mode() == NORMAL) {
            return result && deviceService.isAvailable(k8sNode.tunBridge());
        } else {
            return result;
        }
    }

    private boolean isDeviceCreatedStateDone(K8sNode k8sNode) {

        try {
            // we need to wait a while, in case interface and bridge
            // creation requires some time
            sleep(SLEEP_MS);
        } catch (InterruptedException e) {
            log.error("Exception caused during init state checking...");
        }

        if (k8sNode.mode() == NORMAL) {
            if (k8sNode.dataIp() != null &&
                    !isIntfEnabled(k8sNode, k8sNode.vxlanPortName())) {
                return false;
            }
            if (k8sNode.dataIp() != null &&
                    !isIntfEnabled(k8sNode, k8sNode.grePortName())) {
                return false;
            }
            if (k8sNode.dataIp() != null &&
                    !isIntfEnabled(k8sNode, k8sNode.genevePortName())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Configures the kubernetes node with new state.
     *
     * @param k8sNode       kubernetes node
     * @param newState      a new state
     */
    private void setState(K8sNode k8sNode, K8sNodeState newState) {
        if (k8sNode.state() == newState) {
            return;
        }
        K8sNode updated = k8sNode.updateState(newState);
        k8sNodeAdminService.updateNode(updated);
        log.info("Changed {} state: {}", k8sNode.hostname(), newState);
    }

    /**
     * Bootstraps a new kubernetes node.
     *
     * @param k8sNode kubernetes node
     */
    private void bootstrapNode(K8sNode k8sNode) {
        if (isCurrentStateDone(k8sNode)) {
            setState(k8sNode, k8sNode.state().nextState());
        } else {
            log.trace("Processing {} state for {}", k8sNode.state(),
                    k8sNode.hostname());
            k8sNode.state().process(this, k8sNode);
        }
    }

    private void processK8sNodeRemoved(K8sNode k8sNode) {
        OvsdbClientService client = getOvsdbClient(k8sNode, ovsdbPortNum, ovsdbController);
        if (client == null) {
            log.info("Failed to get ovsdb client");
            return;
        }

        if (k8sNode.mode() == NORMAL) {
            // delete tunnel bridge from the node
            client.dropBridge(k8sNode.tunBridgeName());
        } else {
            // remove the patch ports direct to the integration bridge from tunnel bridge
            removeTunnelPatchPort(k8sNode);
            // remove the patch ports direct to the external bridge from the router bridge
            removeRouterPatchPort(k8sNode);
            // remove the patch ports directs to the openstack's br-int bridge from the int and ext bridges
            removeOpenstackPatchPorts(k8sNode);
        }

        // delete integration bridge from the node
        client.dropBridge(k8sNode.intgBridgeName());

        // delete external bridge from the node
        client.dropBridge(k8sNode.extBridgeName());

        // delete local bridge from the node
        client.dropBridge(k8sNode.localBridgeName());

        // disconnect ovsdb
        // client.disconnect();
    }

    private void removeTunnelPatchPort(K8sNode k8sNode) {
        OvsdbClientService client = getOvsdbClient(k8sNode, ovsdbPortNum, ovsdbController);
        if (client == null) {
            log.info("Failed to get ovsdb client");
            return;
        }

        client.dropInterface(k8sNode.tunToIntgPatchPortName());
    }

    private void removeRouterPatchPort(K8sNode k8sNode) {
        OvsdbClientService client = getOvsdbClient(k8sNode, ovsdbPortNum, ovsdbController);
        if (client == null) {
            log.info("Failed to get ovsdb client");
            return;
        }

        client.dropInterface(k8sNode.routerToExtPatchPortName());
    }

    private void removeOpenstackPatchPorts(K8sNode k8sNode) {
        OvsdbClientService client = getOvsdbClient(k8sNode, ovsdbPortNum, ovsdbController);
        if (client == null) {
            log.info("Failed to get ovsdb client");
            return;
        }

        // remove patch port attached at br-int peers with the k8s integration bridge
        client.dropInterface(k8sNode.osToK8sIntgPatchPortName());

        // remove patch port attached at br-int peers with the k8s external bridge
        client.dropInterface(k8sNode.osToK8sExtPatchPortName());
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

                        K8sNode k8sNode = k8sNodeService.node(device.id());

                        if (k8sNode == null) {
                            return;
                        }

                        if (deviceService.isAvailable(device.id())) {
                            log.debug("OVSDB {} detected", device.id());
                            bootstrapNode(k8sNode);
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
     * to augment K8sNodeService.node() method.
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

            switch (event.type()) {
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_ADDED:
                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper()) {
                            return;
                        }

                        K8sNode k8sNode = k8sNodeService.node(device.id());

                        if (k8sNode == null) {
                            return;
                        }

                        // TODO: also need to check the external bridge's availability
                        // TODO: also need to check the local bridge's availability
                        if (deviceService.isAvailable(device.id())) {
                            log.debug("Integration bridge created on {}",
                                    k8sNode.hostname());
                            bootstrapNode(k8sNode);
                        } else if (k8sNode.state() == COMPLETE) {
                            log.info("Device {} disconnected", device.id());
                            setState(k8sNode, INCOMPLETE);
                        }

                        if (autoRecovery) {
                            if (k8sNode.state() == INCOMPLETE ||
                                    k8sNode.state() == DEVICE_CREATED) {
                                log.info("Device {} is reconnected", device.id());
                                k8sNodeAdminService.updateNode(
                                        k8sNode.updateState(K8sNodeState.INIT));
                            }
                        }
                    });
                    break;
                case PORT_UPDATED:
                case PORT_ADDED:
                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper()) {
                            return;
                        }

                        K8sNode k8sNode = k8sNodeService.node(device.id());

                        if (k8sNode == null) {
                            return;
                        }

                        Port port = event.port();
                        String portName = port.annotations().value(PORT_NAME);
                        if (k8sNode.state() == DEVICE_CREATED && (
                                Objects.equals(portName, k8sNode.vxlanPortName()) ||
                                        Objects.equals(portName, k8sNode.grePortName()) ||
                                        Objects.equals(portName, k8sNode.genevePortName()))) {
                            log.info("Interface {} added or updated to {}",
                                    portName, device.id());
                            bootstrapNode(k8sNode);
                        }
                    });
                    break;
                case PORT_REMOVED:
                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper()) {
                            return;
                        }

                        K8sNode k8sNode = k8sNodeService.node(device.id());

                        if (k8sNode == null) {
                            return;
                        }

                        Port port = event.port();
                        String portName = port.annotations().value(PORT_NAME);
                        if (k8sNode.state() == COMPLETE && (
                                Objects.equals(portName, k8sNode.vxlanPortName()) ||
                                        Objects.equals(portName, k8sNode.grePortName()) ||
                                        Objects.equals(portName, k8sNode.genevePortName()))) {
                            log.warn("Interface {} removed from {}",
                                    portName, event.subject().id());
                            setState(k8sNode, INCOMPLETE);
                        }
                    });
                    break;
                case DEVICE_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }
    }

    /**
     * An internal kubernetes node listener.
     * The notification is triggered by KubernetesNodeStore.
     */
    private class InternalK8sNodeListener implements K8sNodeListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNode, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNodeEvent event) {
            switch (event.type()) {
                case K8S_NODE_CREATED:
                case K8S_NODE_UPDATED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }
                        bootstrapNode(event.subject());
                    });
                    break;
                case K8S_NODE_REMOVED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }
                        processK8sNodeRemoved(event.subject());
                    });
                    break;
                case K8S_NODE_INCOMPLETE:
                default:
                    break;
            }
        }
    }
}
