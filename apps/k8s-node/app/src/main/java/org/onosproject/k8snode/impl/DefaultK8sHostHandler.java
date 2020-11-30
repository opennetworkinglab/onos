/*
 * Copyright 2020-present Open Networking Foundation
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

import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snode.api.K8sBridge;
import org.onosproject.k8snode.api.K8sHost;
import org.onosproject.k8snode.api.K8sHostAdminService;
import org.onosproject.k8snode.api.K8sHostEvent;
import org.onosproject.k8snode.api.K8sHostHandler;
import org.onosproject.k8snode.api.K8sHostListener;
import org.onosproject.k8snode.api.K8sHostState;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeAdminService;
import org.onosproject.k8snode.api.K8sRouterBridge;
import org.onosproject.k8snode.api.K8sTunnelBridge;
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
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

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
import static org.onosproject.k8snode.api.Constants.OS_INTEGRATION_BRIDGE;
import static org.onosproject.k8snode.api.Constants.VXLAN;
import static org.onosproject.k8snode.api.K8sHostState.COMPLETE;
import static org.onosproject.k8snode.api.K8sHostState.DEVICE_CREATED;
import static org.onosproject.k8snode.api.K8sHostState.INCOMPLETE;
import static org.onosproject.k8snode.api.K8sHostState.INIT;
import static org.onosproject.k8snode.api.K8sNodeService.APP_ID;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service bootstraps kubernetes host.
 */
@Component(immediate = true)
public class DefaultK8sHostHandler implements K8sHostHandler {

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
    protected K8sHostAdminService k8sHostAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeAdminService k8sNodeAdminService;


    private int ovsdbPortNum = 6640;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final DeviceListener ovsdbListener = new InternalOvsdbListener();
    private final DeviceListener bridgeListener = new InternalBridgeListener();
    private final K8sHostListener k8sHostListener = new InternalK8sHostListener();

    private ApplicationId appId;
    private NodeId localNode;

    @Activate
    protected void activate() {
        appId = coreService.getAppId(APP_ID);
        localNode = clusterService.getLocalNode().id();

        leadershipService.runForLeadership(appId.name());
        deviceService.addListener(ovsdbListener);
        deviceService.addListener(bridgeListener);
        k8sHostAdminService.addListener(k8sHostListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sHostAdminService.removeListener(k8sHostListener);
        deviceService.removeListener(bridgeListener);
        deviceService.removeListener(ovsdbListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void processInitState(K8sHost k8sHost) {
        if (!isOvsdbConnected(k8sHost, ovsdbPortNum, ovsdbController, deviceService)) {
            ovsdbController.connect(k8sHost.hostIp(), tpPort(ovsdbPortNum));
            return;
        }

        for (K8sTunnelBridge tunBridge : k8sHost.tunBridges()) {
            if (!deviceService.isAvailable(tunBridge.deviceId())) {
                createBridge(k8sHost.ovsdb(), tunBridge);
            }
        }

        for (K8sRouterBridge routerBridge : k8sHost.routerBridges()) {
            if (!deviceService.isAvailable(routerBridge.deviceId())) {
                createBridge(k8sHost.ovsdb(), routerBridge);
            }
        }
    }

    @Override
    public void processDeviceCreatedState(K8sHost k8sHost) {
        try {
            if (!isOvsdbConnected(k8sHost, ovsdbPortNum, ovsdbController, deviceService)) {
                ovsdbController.connect(k8sHost.hostIp(), tpPort(ovsdbPortNum));
                return;
            }

            // create patch ports into tunnel bridge face to integration bridge
            for (K8sTunnelBridge bridge : k8sHost.tunBridges()) {
                for (String node : k8sHost.nodeNames()) {
                    K8sNode k8sNode = k8sNodeAdminService.node(node);
                    if (k8sNode.segmentId() == bridge.tunnelId()) {
                        createTunnelPatchInterfaces(k8sHost.ovsdb(), bridge, k8sNode);
                        createInterPatchInterfaces(k8sHost.ovsdb(), k8sNode);
                    }
                }
            }

            // create tunnel ports
            for (K8sTunnelBridge bridge : k8sHost.tunBridges()) {
                if (!isTunPortEnabled(bridge, bridge.vxlanPortName())) {
                    createVxlanTunnelInterface(k8sHost.ovsdb(), bridge);
                }

                if (!isTunPortEnabled(bridge, bridge.grePortName())) {
                    createGreTunnelInterface(k8sHost.ovsdb(), bridge);
                }

                if (!isTunPortEnabled(bridge, bridge.genevePortName())) {
                    createGeneveTunnelInterface(k8sHost.ovsdb(), bridge);
                }
            }

            // create patch ports into router bridge face to external bridge
            for (K8sRouterBridge bridge : k8sHost.routerBridges()) {
                for (String node : k8sHost.nodeNames()) {
                    K8sNode k8sNode = k8sNodeAdminService.node(node);
                    if (k8sNode.segmentId() == bridge.segmentId()) {
                        createRouterPatchInterfaces(k8sHost.ovsdb(), bridge, k8sNode);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception occurred because of {}", e);
        }
    }

    @Override
    public void processCompleteState(K8sHost k8sHost) {
        // do something if needed
    }

    @Override
    public void processIncompleteState(K8sHost k8sHost) {
        // do something if needed
    }

    private void createBridge(DeviceId ovsdb, K8sBridge bridge) {
        Device device = deviceService.getDevice(ovsdb);

        List<ControllerInfo> controllers = clusterService.getNodes().stream()
                .map(n -> new ControllerInfo(n.ip(), DEFAULT_OFPORT, DEFAULT_OF_PROTO))
                .collect(Collectors.toList());

        String dpid = bridge.dpid().substring(DPID_BEGIN);

        BridgeDescription.Builder builder = DefaultBridgeDescription.builder()
                .name(bridge.name())
                .failMode(BridgeDescription.FailMode.SECURE)
                .datapathId(dpid)
                .disableInBand()
                .controllers(controllers);

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        bridgeConfig.addBridge(builder.build());
    }

    private void createTunnelPatchInterfaces(DeviceId ovsdb, K8sBridge bridge, K8sNode k8sNode) {
        Device device = deviceService.getDevice(ovsdb);
        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create patch interface on {}", ovsdb);
            return;
        }

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);

        // tunnel bridge -> k8s integration bridge
        PatchDescription brTunIntPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(bridge.name())
                        .ifaceName(k8sNode.tunToIntgPatchPortName())
                        .peer(k8sNode.intgToTunPatchPortName())
                        .build();

        ifaceConfig.addPatchMode(k8sNode.tunToIntgPatchPortName(), brTunIntPatchDesc);
    }

    private void createInterPatchInterfaces(DeviceId ovsdb, K8sNode k8sNode) {
        Device device = deviceService.getDevice(ovsdb);
        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create patch interface on {}", ovsdb);
            return;
        }

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);

        // openstack integration bridge -> k8s integration bridge
        PatchDescription osIntK8sIntPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(OS_INTEGRATION_BRIDGE)
                        .ifaceName(k8sNode.osToK8sIntgPatchPortName())
                        .peer(k8sNode.k8sIntgToOsPatchPortName())
                        .build();
        ifaceConfig.addPatchMode(k8sNode.osToK8sIntgPatchPortName(), osIntK8sIntPatchDesc);

        // openstack integration bridge -> k8s external bridge
        PatchDescription osIntK8sExPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(OS_INTEGRATION_BRIDGE)
                        .ifaceName(k8sNode.osToK8sExtPatchPortName())
                        .peer(k8sNode.k8sExtToOsPatchPortName())
                        .build();
        ifaceConfig.addPatchMode(k8sNode.osToK8sExtPatchPortName(), osIntK8sExPatchDesc);
    }

    private void createRouterPatchInterfaces(DeviceId ovsdb, K8sBridge bridge, K8sNode k8sNode) {
        Device device = deviceService.getDevice(ovsdb);
        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create patch interface on {}", ovsdb);
            return;
        }

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);

        // router bridge -> external bridge
        PatchDescription brRouterExtPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(bridge.name())
                        .ifaceName(k8sNode.routerToExtPatchPortName())
                        .peer(k8sNode.extToRouterPatchPortName())
                        .build();

        ifaceConfig.addPatchMode(k8sNode.tunToIntgPatchPortName(), brRouterExtPatchDesc);
    }

    private void createVxlanTunnelInterface(DeviceId ovsdb, K8sTunnelBridge bridge) {
        createTunnelInterface(ovsdb, bridge, VXLAN, bridge.vxlanPortName());
    }

    private void createGreTunnelInterface(DeviceId ovsdb, K8sTunnelBridge bridge) {
        createTunnelInterface(ovsdb, bridge, GRE, bridge.grePortName());
    }

    private void createGeneveTunnelInterface(DeviceId ovsdb, K8sTunnelBridge bridge) {
        createTunnelInterface(ovsdb, bridge, GENEVE, bridge.genevePortName());
    }

    private void createTunnelInterface(DeviceId ovsdb, K8sTunnelBridge bridge,
                                       String type, String intfName) {
        if (isTunPortEnabled(bridge, intfName)) {
            return;
        }

        Device device = deviceService.getDevice(ovsdb);
        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create tunnel interface on {}", ovsdb);
            return;
        }

        TunnelDescription tunnelDesc = buildTunnelDesc(bridge, type, intfName);

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);
        ifaceConfig.addTunnelMode(intfName, tunnelDesc);
    }

    private TunnelDescription buildTunnelDesc(K8sTunnelBridge bridge, String type, String intfName) {
        TunnelKey<String> key = new TunnelKey<>(String.valueOf(bridge.tunnelId()));

        if (VXLAN.equals(type) || GRE.equals(type) || GENEVE.equals(type)) {
            TunnelDescription.Builder tdBuilder =
                    DefaultTunnelDescription.builder()
                            .deviceId(bridge.name())
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

    private boolean isOvsdbConnected(K8sHost host, int ovsdbPort,
                                     OvsdbController ovsdbController,
                                     DeviceService deviceService) {
        OvsdbClientService client = getOvsdbClient(host, ovsdbPort, ovsdbController);
        return deviceService.isAvailable(host.ovsdb()) &&
                client != null &&
                client.isConnected();
    }

    private OvsdbClientService getOvsdbClient(K8sHost host, int ovsdbPort,
                                              OvsdbController ovsdbController) {
        OvsdbNodeId ovsdb = new OvsdbNodeId(host.hostIp(), ovsdbPort);
        return ovsdbController.getOvsdbClient(ovsdb);
    }

    private boolean isCurrentStateDone(K8sHost k8sHost) {
        switch (k8sHost.state()) {
            case INIT:
                return isInitStateDone(k8sHost);
            case DEVICE_CREATED:
                return isDeviceCreatedStateDone(k8sHost);
            case COMPLETE:
            case INCOMPLETE:
                return false;
            default:
                return true;
        }
    }

    private boolean isInitStateDone(K8sHost k8sHost) {
        if (!isOvsdbConnected(k8sHost, ovsdbPortNum,
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

        for (K8sBridge tunBridge : k8sHost.tunBridges()) {
            if (!deviceService.isAvailable(tunBridge.deviceId())) {
                return false;
            }
        }

        for (K8sBridge routerBridge: k8sHost.routerBridges()) {
            if (!deviceService.isAvailable(routerBridge.deviceId())) {
                return false;
            }
        }

        return true;
    }

    private boolean isDeviceCreatedStateDone(K8sHost k8sHost) {
        try {
            // we need to wait a while, in case interface and bridge
            // creation requires some time
            sleep(SLEEP_MS);
        } catch (InterruptedException e) {
            log.error("Exception caused during init state checking...");
        }

        // checks whether all tunneling ports exist
        for (K8sTunnelBridge bridge: k8sHost.tunBridges()) {
            if (!isTunPortEnabled(bridge, bridge.vxlanPortName())) {
                return false;
            }
            if (!isTunPortEnabled(bridge, bridge.grePortName())) {
                return false;
            }
            if (!isTunPortEnabled(bridge, bridge.genevePortName())) {
                return false;
            }
        }

        // checks whether all patch ports attached to tunnel bridge exist
        for (K8sTunnelBridge bridge : k8sHost.tunBridges()) {
            for (String node : k8sHost.nodeNames()) {
                K8sNode k8sNode = k8sNodeAdminService.node(node);
                if (!isTunPortEnabled(bridge, k8sNode.tunToIntgPatchPortName())) {
                    return false;
                }
            }
        }

        // checks whether all patch ports attached to router bridge exist
        for (K8sRouterBridge bridge : k8sHost.routerBridges()) {
            for (String node : k8sHost.nodeNames()) {
                K8sNode k8sNode = k8sNodeAdminService.node(node);
                if (!isRouterPortEnabled(bridge, k8sNode.routerToExtPatchPortName())) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isTunPortEnabled(K8sTunnelBridge tunBridge, String intf) {
        return deviceService.isAvailable(tunBridge.deviceId()) &&
                deviceService.getPorts(tunBridge.deviceId()).stream()
                        .anyMatch(port -> Objects.equals(
                                port.annotations().value(PORT_NAME), intf) &&
                                port.isEnabled());
    }

    private boolean isRouterPortEnabled(K8sRouterBridge routerBridge, String intf) {
        return deviceService.isAvailable(routerBridge.deviceId()) &&
                deviceService.getPorts(routerBridge.deviceId()).stream()
                        .anyMatch(port -> Objects.equals(
                                port.annotations().value(PORT_NAME), intf) &&
                                port.isEnabled());
    }

    /**
     * Configures the kubernetes host with new state.
     *
     * @param k8sHost       kubernetes host
     * @param newState      a new state
     */
    private void setState(K8sHost k8sHost, K8sHostState newState) {
        if (k8sHost.state() == newState) {
            return;
        }
        K8sHost updated = k8sHost.updateState(newState);
        k8sHostAdminService.updateHost(updated);
        log.info("Changed {} state: {}", k8sHost.hostIp(), newState);
    }

    /**
     * Bootstraps a new kubernetes host.
     *
     * @param k8sHost kubernetes host
     */
    private void bootstrapHost(K8sHost k8sHost) {
        if (isCurrentStateDone(k8sHost)) {
            setState(k8sHost, k8sHost.state().nextState());
        } else {
            log.trace("Processing {} state for {}", k8sHost.state(),
                    k8sHost.hostIp());
            k8sHost.state().process(this, k8sHost);
        }
    }

    private void processHostRemoval(K8sHost k8sHost) {
        OvsdbClientService client = getOvsdbClient(k8sHost, ovsdbPortNum, ovsdbController);
        if (client == null) {
            log.info("Failed to get ovsdb client");
            return;
        }

        // delete tunnel bridge from the host
        k8sHost.tunBridges().forEach(br -> client.dropBridge(br.name()));

        // delete router bridge from the host
        k8sHost.routerBridges().forEach(br -> client.dropBridge(br.name()));
    }

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

                        K8sHost k8sHost = k8sHostAdminService.host(device.id());

                        if (k8sHost == null) {
                            return;
                        }

                        if (deviceService.isAvailable(device.id())) {
                            log.debug("OVSDB {} detected", device.id());
                            bootstrapHost(k8sHost);
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

                        K8sHost k8sHost = k8sHostAdminService.hostByTunBridge(device.id());

                        if (k8sHost == null) {
                            return;
                        }

                        if (deviceService.isAvailable(device.id())) {
                            log.debug("Tunnel bridge created on {}",
                                    k8sHost.hostIp());
                            log.debug("OVSDB {} detected", device.id());
                            bootstrapHost(k8sHost);
                        } else if (k8sHost.state() == COMPLETE) {
                            log.info("Device {} disconnected", device.id());
                            setState(k8sHost, INCOMPLETE);
                        }

                        if (k8sHost.state() == INCOMPLETE ||
                                k8sHost.state() == DEVICE_CREATED) {
                            log.info("Device {} is reconnected", device.id());
                            k8sHostAdminService.updateHost(
                                    k8sHost.updateState(INIT));
                        }
                    });
                    break;
                case PORT_UPDATED:
                case PORT_ADDED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }

                        K8sHost tunnelHost = k8sHostAdminService.hostByTunBridge(device.id());

                        if (tunnelHost == null) {
                            return;
                        }

                        if (tunnelHost.state() == DEVICE_CREATED) {
                            // we bootstrap the host whenever any ports added to the tunnel bridge
                            tunnelHost.tunBridges().stream().filter(
                                    br -> br.deviceId().equals(device.id())
                            ).findAny().ifPresent(tunBridge -> bootstrapHost(tunnelHost));
                        }

                        K8sHost routerHost = k8sHostAdminService.hostByRouterBridge(device.id());

                        if (routerHost == null) {
                            return;
                        }

                        if (routerHost.state() == DEVICE_CREATED) {
                            // we bootstrap the host whenever any ports added to the router bridge
                            routerHost.routerBridges().stream().filter(
                                    br -> br.deviceId().equals(device.id())
                            ).findAny().ifPresent(routerBridge -> bootstrapHost(routerHost));
                        }
                    });
                    break;
                case PORT_REMOVED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }

                        K8sHost k8sHost = k8sHostAdminService.hostByTunBridge(device.id());

                        if (k8sHost == null) {
                            return;
                        }

                        Port port = event.port();
                        String portName = port.annotations().value(PORT_NAME);
                        if (k8sHost.state() == COMPLETE) {
                            K8sTunnelBridge tunBridge = k8sHost.tunBridges().stream().filter(
                                    br -> br.deviceId().equals(device.id())
                            ).findAny().orElse(null);

                            if (tunBridge != null) {
                                if (Objects.equals(portName, tunBridge.vxlanPortName()) ||
                                        Objects.equals(portName, tunBridge.grePortName()) ||
                                        Objects.equals(portName, tunBridge.genevePortName())) {
                                    log.warn("Interface {} removed from {}",
                                            portName, event.subject().id());
                                    setState(k8sHost, INCOMPLETE);
                                }
                            }
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

    private class InternalK8sHostListener implements K8sHostListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNode, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sHostEvent event) {
            switch (event.type()) {
                case K8S_HOST_CREATED:
                case K8S_HOST_UPDATED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }

                        bootstrapHost(event.subject());
                    });
                    break;
                case K8S_HOST_REMOVED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }

                        processHostRemoval(event.subject());
                    });
                    break;
                case K8S_HOST_INCOMPLETE:
                default:
                    break;
            }
        }
    }
}
