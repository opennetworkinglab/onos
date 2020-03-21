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
package org.onosproject.ofoverlay.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.cluster.ClusterService;
import org.onosproject.event.Event;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControlProtocolVersion;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.DefaultBridgeDescription;
import org.onosproject.net.behaviour.DefaultTunnelDescription;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelEndPoints;
import org.onosproject.net.behaviour.TunnelKeys;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.ofoverlay.impl.util.NetworkAddress;
import org.onosproject.ofoverlay.impl.util.OvsDatapathType;
import org.onosproject.ofoverlay.impl.util.OvsVersion;
import org.onosproject.ofoverlay.impl.util.SshUtil;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.workflow.api.AbstractWorklet;
import org.onosproject.workflow.api.JsonDataModel;
import org.onosproject.workflow.api.WorkflowContext;
import org.onosproject.workflow.api.WorkflowException;
import org.onosproject.workflow.api.StaticDataModel;
import org.onosproject.workflow.model.accessinfo.SshAccessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.workflow.api.CheckCondition.check;

/**
 * Class for defining OVS workflows.
 */
public final class Ovs {

    private static final Logger log = LoggerFactory.getLogger(Ovs.class);

    private static final String MODEL_MGMT_IP = "/mgmtIp";
    private static final String BRIDGE_NAME = "/bridgeName";
    private static final String MODEL_OVSDB_PORT = "/ovsdbPort";
    private static final String MODEL_OVS_VERSION = "/ovsVersion";
    private static final String MODEL_OVS_DATAPATH_TYPE = "/ovsDatapathType";
    private static final String MODEL_SSH_ACCESSINFO = "/sshAccessInfo";
    private static final String MODEL_OF_DEVID_BRIDGE = "/ofDevId";
    private static final String MODEL_OF_DEVID_FOR_OVERLAY_UNDERLAY_BRIDGE = "/ofDevIdBrIntBrPhy";
    private static final String MODEL_PHY_PORTS = "/physicalPorts";
    private static final String MODEL_VTEP_IP = "/vtepIp";

    private static final String BRIDGE_OVERLAY = "br-int";
    private static final String BRIDGE_UNDERLAY = "br-phy";

    private static final int DEVID_IDX_BRIDGE_OVERLAY = 0;
    private static final int DEVID_IDX_BRIDGE_UNDERLAY_NOVA = 1;

    private static final ControlProtocolVersion BRIDGE_DEFAULT_OF_VERSION = ControlProtocolVersion.OF_1_3;
    private static final int OPENFLOW_PORT = 6653;
    private static final String OPENFLOW_CHANNEL_PROTO = "tcp";
    private static final String OVSDB_DEVICE_PREFIX = "ovsdb:";

    private static final long TIMEOUT_DEVICE_CREATION_MS = 60000L;
    private static final long TIMEOUT_PORT_ADDITION_MS = 120000L;


    /**
     * Utility class for OVS workflow.
     */
    public static final class OvsUtil {

        private OvsUtil() {

        }

        private static final String OPENFLOW_DEVID_FORMAT = "of:%08x%08x";

        /**
         * Builds Open-flow device id with ip address, and index.
         *
         * @param addr  ip address
         * @param index index
         * @return created device id
         */
        public static DeviceId buildOfDeviceId(IpAddress addr, int index) {
            if (addr.isIp4()) {
                Ip4Address v4Addr = addr.getIp4Address();
                return DeviceId.deviceId(String.format(OPENFLOW_DEVID_FORMAT, v4Addr.toInt(), index));
            } else if (addr.isIp6()) {
                Ip6Address v6Addr = addr.getIp6Address();
                return DeviceId.deviceId(String.format(OPENFLOW_DEVID_FORMAT, v6Addr.hashCode(), index));
            } else {
                return DeviceId.deviceId(String.format(OPENFLOW_DEVID_FORMAT, addr.hashCode(), index));
            }
        }

        /**
         * Builds OVS data path type.
         *
         * @param strOvsDatapathType string ovs data path type
         * @return ovs data path type
         * @throws WorkflowException workflow exception
         */
        public static final OvsDatapathType buildOvsDatapathType(String strOvsDatapathType) throws WorkflowException {
            try {
                return OvsDatapathType.valueOf(strOvsDatapathType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new WorkflowException(e);
            }
        }

        /**
         * Gets OVSDB behavior.
         *
         * @param context        workflow context
         * @param mgmtIp         management ip
         * @param behaviourClass behavior class
         * @param <T>            behavior class
         * @return OVSDB behavior
         * @throws WorkflowException workflow exception
         */
        public static final <T extends Behaviour> T getOvsdbBehaviour(WorkflowContext context, String mgmtIp,
                                                             Class<T> behaviourClass) throws WorkflowException {

            DriverService driverService = context.getService(DriverService.class);

            DeviceId devId = ovsdbDeviceId(mgmtIp);
            DriverHandler handler = driverService.createHandler(devId);
            if (Objects.isNull(handler)) {
                throw new WorkflowException("Failed to get DriverHandler for " + devId);
            }
            T behaviour;
            try {
                behaviour = handler.behaviour(behaviourClass);
                if (Objects.isNull(behaviour)) {
                    throw new WorkflowException("Failed to get " + behaviourClass + " for " + devId + "-" + handler);
                }
            } catch (IllegalArgumentException e) {
                throw new WorkflowException("Failed to get " + behaviourClass + " for " + devId + "-" + handler);
            }
            return behaviour;
        }

        /**
         * Gets bridge description.
         *
         * @param bridgeConfig bridge config
         * @param bridgeName   bridge name
         * @return bridge description optional
         */
        public static final Optional<BridgeDescription> getBridgeDescription(BridgeConfig bridgeConfig,
                                                                             String bridgeName) {
            try {
                Collection<BridgeDescription> bridges = bridgeConfig.getBridges();
                for (BridgeDescription br : bridges) {
                    if (Objects.equals(bridgeName, br.name())) {
                        return Optional.of(br);
                    }
                }
            } catch (Exception e) {
                log.error("Exception : ", e);
            }
            return Optional.empty();
        }

        public static BridgeDescription getBridgeNames(BridgeConfig bridgeConfig) {
            Collection<BridgeDescription> bridges = bridgeConfig.getBridges();
            if (bridges.size() > 0) {
                for (BridgeDescription description : bridges) {
                    return description;
                }
            }
            return null;
        }


        /**
         * Builds OVSDB device id.
         *
         * @param mgmtIp management ip address string
         * @return OVSDB device id
         */
        public static final DeviceId ovsdbDeviceId(String mgmtIp) {
            return DeviceId.deviceId(OVSDB_DEVICE_PREFIX.concat(mgmtIp));
        }

        /**
         * Returns {@code true} if this bridge is available;
         * returns {@code false} otherwise.
         *
         * @param context workflow context
         * @param devId   device id
         * @return {@code true} if this bridge is available; {@code false} otherwise.
         * @throws WorkflowException workflow exception
         */
        public static final boolean isAvailableBridge(WorkflowContext context, DeviceId devId)
                throws WorkflowException {

            if (Objects.isNull(devId)) {
                throw new WorkflowException("Invalid device id in data model");
            }

            DeviceService deviceService = context.getService(DeviceService.class);
            Device dev = deviceService.getDevice(devId);
            if (Objects.isNull(dev)) {
                return false;
            }

            return deviceService.isAvailable(devId);
        }

        /**
         * Gets openflow controller information list.
         *
         * @param context workflow context
         * @return openflow controller information list
         * @throws WorkflowException workflow exception
         */
        public static final List<ControllerInfo> getOpenflowControllerInfoList(WorkflowContext context)
                throws WorkflowException {
            ClusterService clusterService = context.getService(ClusterService.class);
            java.util.List<org.onosproject.net.behaviour.ControllerInfo> controllers = new ArrayList<>();
            Sets.newHashSet(clusterService.getNodes()).forEach(
                    controller -> {
                        org.onosproject.net.behaviour.ControllerInfo ctrlInfo =
                                new org.onosproject.net.behaviour.ControllerInfo(controller.ip(),
                                        OPENFLOW_PORT,
                                        OPENFLOW_CHANNEL_PROTO);
                        controllers.add(ctrlInfo);
                    }
            );
            return controllers;
        }

        /**
         * Creates bridge.
         *
         * @param bridgeConfig  bridge config
         * @param name          bridge name to create
         * @param dpid          openflow data path id of bridge to create
         * @param ofControllers openflow controller information list
         * @param datapathType  OVS data path type
         */
        public static final void createBridge(BridgeConfig bridgeConfig, String name, String dpid,
                                              List<ControllerInfo> ofControllers, OvsDatapathType datapathType) {
            BridgeDescription.Builder bridgeDescBuilder = DefaultBridgeDescription.builder()
                    .name(name)
                    .failMode(BridgeDescription.FailMode.SECURE)
                    .datapathId(dpid)
                    .disableInBand()
                    .controlProtocols(Collections.singletonList(BRIDGE_DEFAULT_OF_VERSION))
                    .controllers(ofControllers);

            if (datapathType != null && !(datapathType.equals(OvsDatapathType.EMPTY))) {
                bridgeDescBuilder.datapathType(datapathType.toString());
                log.info("create {} with dataPathType {}", name, datapathType);
            }

            BridgeDescription bridgeDesc = bridgeDescBuilder.build();
            bridgeConfig.addBridge(bridgeDesc);
        }

        /**
         * Index of data path id in openflow device id.
         */
        private static final int DPID_BEGIN_INDEX = 3;

        /**
         * Gets bridge data path id.
         *
         * @param devId device id
         * @return bridge data path id
         */
        public static final String bridgeDatapathId(DeviceId devId) {
            return devId.toString().substring(DPID_BEGIN_INDEX);
        }

        /**
         * Gets OVSDB client.
         *
         * @param context      workflow context
         * @param strMgmtIp    management ip address
         * @param intOvsdbPort OVSDB port
         * @return ovsdb client
         * @throws WorkflowException workflow exception
         */
        public static final OvsdbClientService getOvsdbClient(
                WorkflowContext context, String strMgmtIp, int intOvsdbPort) throws WorkflowException {
            IpAddress mgmtIp = IpAddress.valueOf(strMgmtIp);
            TpPort ovsdbPort = TpPort.tpPort(intOvsdbPort);
            OvsdbController ovsdbController = context.getService(OvsdbController.class);
            return ovsdbController.getOvsdbClient(new OvsdbNodeId(mgmtIp, ovsdbPort.toInt()));
        }

        /**
         * Checks whether 2 controller informations include same controller information.
         *
         * @param a controller information list
         * @param b controller information list
         * @return {@code true} if 2 controller informations include same controller information
         */
        public static boolean isEqual(List<ControllerInfo> a, List<ControllerInfo> b) {
            if (a == b) {
                return true;
            } else if (a == null) {
                // equivalent to (a == null && b != null)
                return false;
            } else if (b == null) {
                // equivalent to (a != null && b == null)
                return false;
            } else if (a.size() != b.size()) {
                return false;
            }

            return a.containsAll(b);
        }

        /**
         * Gets the name of the port.
         *
         * @param port port
         * @return the name of the port
         */
        public static final String portName(Port port) {
            return port.annotations().value(PORT_NAME);
        }
    }

    /**
     * Work-let class for creating OVSDB device.
     */
    public static class CreateOvsdbDevice extends AbstractWorklet {

        @JsonDataModel(path = MODEL_MGMT_IP)
        String strMgmtIp;

        @JsonDataModel(path = MODEL_OVSDB_PORT)
        Integer intOvsdbPort;

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            OvsdbClientService ovsdbClient = OvsUtil.getOvsdbClient(context, strMgmtIp, intOvsdbPort);
            return ovsdbClient == null || !ovsdbClient.isConnected();
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {
            IpAddress mgmtIp = IpAddress.valueOf(strMgmtIp);
            TpPort ovsdbPort = TpPort.tpPort(intOvsdbPort);
            OvsdbController ovsdbController = context.getService(OvsdbController.class);
            context.waitCompletion(DeviceEvent.class, OVSDB_DEVICE_PREFIX.concat(strMgmtIp),
                    () -> ovsdbController.connect(mgmtIp, ovsdbPort),
                    TIMEOUT_DEVICE_CREATION_MS
            );
        }

        @Override
        public boolean isCompleted(WorkflowContext context, Event event) throws WorkflowException {
            if (!(event instanceof DeviceEvent)) {
                return false;
            }
            DeviceEvent deviceEvent = (DeviceEvent) event;
            Device device = deviceEvent.subject();
            switch (deviceEvent.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_UPDATED:
                    return context.getService(DeviceService.class).isAvailable(device.id());
                default:
                    return false;
            }
        }

        @Override
        public void timeout(WorkflowContext context) throws WorkflowException {
            if (!isNext(context)) {
                context.completed(); //Complete the job of worklet by timeout
            } else {
                super.timeout(context);
            }
        }
    }


    /**
     * Work-let class for removing OVSDB device.
     */
    public static class RemoveOvsdbDevice extends AbstractWorklet {

        @JsonDataModel(path = MODEL_MGMT_IP)
        String strMgmtIp;

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            DeviceId devId = DeviceId.deviceId(OVSDB_DEVICE_PREFIX.concat(strMgmtIp));

            Device dev = context.getService(DeviceService.class).getDevice(devId);
            return dev != null;
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {
            IpAddress mgmtIp = IpAddress.valueOf(strMgmtIp);
            check(mgmtIp != null, "mgmt ip is invalid");
            DeviceId devId = DeviceId.deviceId(OVSDB_DEVICE_PREFIX.concat(strMgmtIp));
            DeviceAdminService adminService = context.getService(DeviceAdminService.class);

            context.waitCompletion(DeviceEvent.class, devId.toString(),
                    () -> adminService.removeDevice(devId),
                    TIMEOUT_DEVICE_CREATION_MS
            );
        }

        @Override
        public boolean isCompleted(WorkflowContext context, Event event) throws WorkflowException {
            if (!(event instanceof DeviceEvent)) {
                return false;
            }
            DeviceEvent deviceEvent = (DeviceEvent) event;
            switch (deviceEvent.type()) {
                case DEVICE_REMOVED:
                    return !isNext(context);
                default:
                    return false;
            }
        }

        @Override
        public void timeout(WorkflowContext context) throws WorkflowException {
            if (!isNext(context)) {
                context.completed(); //Complete worklet by timeout
            } else {
                super.timeout(context);
            }
        }
    }

    /**
     * Work-let class for updating OVS version.
     */
    public static class UpdateOvsVersion extends AbstractWorklet {

        @JsonDataModel(path = MODEL_OVS_VERSION, optional = true)
        String strOvsVersion;

        @JsonDataModel(path = MODEL_SSH_ACCESSINFO)
        JsonNode strSshAccessInfo;

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            return strOvsVersion == null;
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            SshAccessInfo sshAccessInfo = SshAccessInfo.valueOf(strSshAccessInfo);
            check(Objects.nonNull(sshAccessInfo), "Invalid ssh access info " + context.data());

            OvsVersion ovsVersion = SshUtil.exec(sshAccessInfo,
                    session -> SshUtil.fetchOvsVersion(session));

            check(Objects.nonNull(ovsVersion), "Failed to fetch ovs version " + context.data());
            strOvsVersion = ovsVersion.toString();

            context.completed();
        }
    }

    /**
     * Work-let class for updating overlay bridge device id.
     */
    public static class UpdateBridgeId extends AbstractWorklet {

        @JsonDataModel(path = MODEL_MGMT_IP)
        String strMgmtIp;

        @JsonDataModel(path = MODEL_OF_DEVID_BRIDGE, optional = true)
        ObjectNode strOfDevId;

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            return strOfDevId == null;
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            BridgeConfig bridgeConfig = OvsUtil.getOvsdbBehaviour(context, strMgmtIp, BridgeConfig.class);
            BridgeDescription bridgeName = OvsUtil.getBridgeNames(bridgeConfig);
            strOfDevId = JsonNodeFactory.instance.objectNode();
            if (Objects.nonNull(bridgeName)) {
                Optional<BridgeDescription> optBd = OvsUtil.getBridgeDescription(bridgeConfig, bridgeName.name());
                if (optBd.isPresent()) {
                    String ofDevIdOverlay = strOfDevId.get(BRIDGE_OVERLAY).asText();
                    String ofDevIdUnderlay = strOfDevId.get(BRIDGE_UNDERLAY).asText();
                    if (Objects.nonNull(ofDevIdOverlay) || Objects.nonNull(ofDevIdUnderlay)) {
                        strOfDevId.put(BRIDGE_OVERLAY, OvsUtil.buildOfDeviceId(
                                IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_OVERLAY).toString());
                        strOfDevId.put(BRIDGE_UNDERLAY, OvsUtil.buildOfDeviceId(
                                IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_UNDERLAY_NOVA).toString());
                    } else {
                        strOfDevId.put(BRIDGE_OVERLAY, OvsUtil.buildOfDeviceId(
                                IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_OVERLAY).toString());
                        strOfDevId.put(BRIDGE_UNDERLAY, OvsUtil.buildOfDeviceId(
                                IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_UNDERLAY_NOVA).toString());
                        log.info("Failed to find devId. Updates of device id with new device id {}", strOfDevId);
                    }
                }
            } else {
                strOfDevId.put(BRIDGE_OVERLAY, OvsUtil.buildOfDeviceId(
                        IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_OVERLAY).toString());
                strOfDevId.put(BRIDGE_UNDERLAY, OvsUtil.buildOfDeviceId(
                        IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_UNDERLAY_NOVA).toString());
                log.info("Failed to find description. Updates of device id with new device id {}",
                        strOfDevId);
            }
            context.completed();
        }
    }

    public static class CreateBridge extends AbstractWorklet {

        @StaticDataModel(path = BRIDGE_NAME)
        String bridgeName;

        @JsonDataModel(path = MODEL_MGMT_IP)
        String strMgmtIp;

        @JsonDataModel(path = MODEL_OVSDB_PORT)
        Integer intOvsdbPort;

        @JsonDataModel(path = MODEL_OVS_DATAPATH_TYPE)
        String strOvsDatapath;

        @JsonDataModel(path = MODEL_OF_DEVID_BRIDGE, optional = true)
        ObjectNode strOfDevId;

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            check(strOfDevId != null, "invalid strOfDevIdUnderlay");
            String bridgeId = strOfDevId.get(bridgeName).asText();
            return !OvsUtil.isAvailableBridge(context, DeviceId.deviceId(bridgeId));
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            check(strOfDevId != null, "invalid strOfDevIdOverlay");
            BridgeConfig bridgeConfig = OvsUtil.getOvsdbBehaviour(context, strMgmtIp, BridgeConfig.class);
            List<ControllerInfo> ofControllers = OvsUtil.getOpenflowControllerInfoList(context);
            String bridge = strOfDevId.get(bridgeName).asText();
            DeviceId ofDeviceId = DeviceId.deviceId(bridge);

            if (ofControllers == null || ofControllers.size() == 0) {
                throw new WorkflowException("Invalid of controllers");
            }

            Optional<BridgeDescription> optBd = OvsUtil.getBridgeDescription(bridgeConfig, bridgeName);
            if (!optBd.isPresent()) {

                // If bridge does not exist, just creates a new bridge.
                context.waitCompletion(DeviceEvent.class, ofDeviceId.toString(),
                        () -> OvsUtil.createBridge(bridgeConfig,
                                bridgeName,
                                OvsUtil.bridgeDatapathId(ofDeviceId),
                                ofControllers,
                                OvsUtil.buildOvsDatapathType(strOvsDatapath)),
                        TIMEOUT_DEVICE_CREATION_MS
                );
                return;

            } else {
                BridgeDescription bd = optBd.get();
                if (OvsUtil.isEqual(ofControllers, bd.controllers())) {
                    log.error("{} has valid controller setting({})", bridgeName, bd.controllers());
                    context.completed();
                    return;
                }

                OvsdbClientService ovsdbClient = OvsUtil.getOvsdbClient(context, strMgmtIp, intOvsdbPort);
                if (ovsdbClient == null || !ovsdbClient.isConnected()) {
                    throw new WorkflowException("Invalid ovsdb client for " + strMgmtIp);
                }

                // If controller settings are not matched, set controller with valid controller information.
                context.waitCompletion(DeviceEvent.class, ofDeviceId.toString(),
                        () -> ovsdbClient.setControllersWithDeviceId(bd.deviceId().get(), ofControllers),
                        TIMEOUT_DEVICE_CREATION_MS
                );
                return;
            }
        }

        @Override
        public boolean isCompleted(WorkflowContext context, Event event) throws WorkflowException {
            if (!(event instanceof DeviceEvent)) {
                return false;
            }
            DeviceEvent deviceEvent = (DeviceEvent) event;
            Device device = deviceEvent.subject();
            switch (deviceEvent.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_UPDATED:
                    return context.getService(DeviceService.class).isAvailable(device.id());
                default:
                    return false;
            }
        }

        @Override
        public void timeout(WorkflowContext context) throws WorkflowException {
            if (!isNext(context)) {
                context.completed(); //Complete the job of worklet by timeout
            } else {
                super.timeout(context);
            }
        }

    }

    /**
     * Work-let class for creating overlay openflow bridge.
     */
    public static class CreateOverlayBridgeMultiEvent extends AbstractWorklet {

        @JsonDataModel(path = MODEL_MGMT_IP)
        String strMgmtIp;

        @JsonDataModel(path = MODEL_OVSDB_PORT)
        Integer intOvsdbPort;

        @JsonDataModel(path = MODEL_OVS_DATAPATH_TYPE)
        String strOvsDatapath;

        @JsonDataModel(path = MODEL_OF_DEVID_BRIDGE, optional = true)
        ObjectNode strOfDevIdOverlay;

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            check(strOfDevIdOverlay != null, "invalid strOfDevIdOverlay");
            String bridge = strOfDevIdOverlay.get(BRIDGE_OVERLAY).asText();
            return !OvsUtil.isAvailableBridge(context, DeviceId.deviceId(bridge));
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            check(strOfDevIdOverlay != null, "invalid strOfDevIdOverlay");
            BridgeConfig bridgeConfig = OvsUtil.getOvsdbBehaviour(context, strMgmtIp, BridgeConfig.class);
            List<ControllerInfo> ofControllers = OvsUtil.getOpenflowControllerInfoList(context);
            String bridge = strOfDevIdOverlay.get(BRIDGE_OVERLAY).asText();
            DeviceId ofDeviceId = DeviceId.deviceId(bridge);

            if (ofControllers == null || ofControllers.size() == 0) {
                throw new WorkflowException("Invalid of controllers");
            }

            Optional<BridgeDescription> optBd = OvsUtil.getBridgeDescription(bridgeConfig, BRIDGE_OVERLAY);
            if (!optBd.isPresent()) {

                Set<String> eventHints = Sets.newHashSet(ofDeviceId.toString());

                context.waitAnyCompletion(DeviceEvent.class, eventHints,
                        () -> OvsUtil.createBridge(bridgeConfig,
                                BRIDGE_OVERLAY,
                                OvsUtil.bridgeDatapathId(ofDeviceId),
                                ofControllers,
                                OvsUtil.buildOvsDatapathType(strOvsDatapath)),
                        TIMEOUT_DEVICE_CREATION_MS
                );
                return;

            } else {
                BridgeDescription bd = optBd.get();
                if (OvsUtil.isEqual(ofControllers, bd.controllers())) {
                    log.error("{} has valid controller setting({})", BRIDGE_OVERLAY, bd.controllers());
                    context.completed();
                    return;
                }

                OvsdbClientService ovsdbClient = OvsUtil.getOvsdbClient(context, strMgmtIp, intOvsdbPort);
                if (ovsdbClient == null || !ovsdbClient.isConnected()) {
                    throw new WorkflowException("Invalid ovsdb client for " + strMgmtIp);
                }

                // If controller settings are not matched, set controller with valid controller information.
                Set<String> eventHints = Sets.newHashSet(ofDeviceId.toString());

                context.waitAnyCompletion(DeviceEvent.class, eventHints,
                        () -> ovsdbClient.setControllersWithDeviceId(bd.deviceId().get(), ofControllers),
                        TIMEOUT_DEVICE_CREATION_MS
                );
                return;
            }
        }

        @Override
        public boolean isCompleted(WorkflowContext context, Event event) throws WorkflowException {
            if (!(event instanceof DeviceEvent)) {
                return false;
            }
            DeviceEvent deviceEvent = (DeviceEvent) event;
            Device device = deviceEvent.subject();
            switch (deviceEvent.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_UPDATED:
                    return context.getService(DeviceService.class).isAvailable(device.id());
                default:
                    return false;
            }
        }

        @Override
        public void timeout(WorkflowContext context) throws WorkflowException {
            if (!isNext(context)) {
                context.completed(); //Complete the job of worklet by timeout
            } else {
                super.timeout(context);
            }
        }
    }

    /**
     * Work-let class for creating vxlan port on the overlay bridge.
     */
    public static class CreateOverlayBridgeVxlanPort extends AbstractWorklet {

        @JsonDataModel(path = MODEL_MGMT_IP)
        String strMgmtIp;

        @JsonDataModel(path = MODEL_OF_DEVID_BRIDGE, optional = true)
        ObjectNode strOfDevIdOverlay;

        private static final String OVS_VXLAN_PORTNAME = "vxlan";

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            check(strOfDevIdOverlay != null, "invalid strOfDevIdOverlay");
            String bridge = strOfDevIdOverlay.get(BRIDGE_OVERLAY).asText();
            DeviceId deviceId = DeviceId.deviceId(bridge);
            if (Objects.isNull(deviceId)) {
                throw new WorkflowException("Invalid br-int bridge, before creating VXLAN port");
            }

            DeviceService deviceService = context.getService(DeviceService.class);
            return !deviceService.getPorts(deviceId)
                    .stream()
                    .filter(port -> OvsUtil.portName(port).contains(OVS_VXLAN_PORTNAME) && port.isEnabled())
                    .findAny().isPresent();
        }


        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            check(strOfDevIdOverlay != null, "invalid strOfDevIdOverlay");
            TunnelDescription description = DefaultTunnelDescription.builder()
                    .deviceId(BRIDGE_OVERLAY)
                    .ifaceName(OVS_VXLAN_PORTNAME)
                    .type(TunnelDescription.Type.VXLAN)
                    .remote(TunnelEndPoints.flowTunnelEndpoint())
                    .key(TunnelKeys.flowTunnelKey())
                    .build();

            String bridge = strOfDevIdOverlay.get(BRIDGE_OVERLAY).asText();
            DeviceId ofDeviceId = DeviceId.deviceId(bridge);
            InterfaceConfig interfaceConfig = OvsUtil.getOvsdbBehaviour(context, strMgmtIp, InterfaceConfig.class);

            context.waitCompletion(DeviceEvent.class, ofDeviceId.toString(),
                    () -> interfaceConfig.addTunnelMode(BRIDGE_OVERLAY, description),
                    TIMEOUT_DEVICE_CREATION_MS
            );
        }

        @Override
        public boolean isCompleted(WorkflowContext context, Event event) throws WorkflowException {
            if (!(event instanceof DeviceEvent)) {
                return false;
            }
            DeviceEvent deviceEvent = (DeviceEvent) event;
            switch (deviceEvent.type()) {
                case PORT_ADDED:
                    return !isNext(context);
                default:
                    return false;
            }
        }

        @Override
        public void timeout(WorkflowContext context) throws WorkflowException {
            if (!isNext(context)) {
                context.completed(); //Complete the job of worklet by timeout
            } else {
                super.timeout(context);
            }
        }
    }

    /**
     * Work-let class for adding physical ports on the underlay openflow bridge.
     */
    public static class AddPhysicalPortsOnUnderlayBridge extends AbstractWorklet {

        @JsonDataModel(path = MODEL_MGMT_IP)
        String strMgmtIp;

        @JsonDataModel(path = MODEL_OVSDB_PORT)
        Integer intOvsdbPort;

        @JsonDataModel(path = MODEL_OF_DEVID_BRIDGE, optional = true)
        ObjectNode strOfDevIdUnderlay;

        @JsonDataModel(path = MODEL_OVS_DATAPATH_TYPE)
        String strOvsDatapath;

        @JsonDataModel(path = MODEL_PHY_PORTS)
        ArrayNode arrNodePhysicalPorts;

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {
            check(strOfDevIdUnderlay != null, "invalid strOfDevIdUnderlay");
            String bridge = strOfDevIdUnderlay.get(BRIDGE_UNDERLAY).asText();
            DeviceId brphyDevId = DeviceId.deviceId(bridge);
            return !hasAllPhysicalPorts(context, brphyDevId);
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {
            check(strOfDevIdUnderlay != null, "invalid strOfDevIdUnderlay");
            String bridge = strOfDevIdUnderlay.get(BRIDGE_UNDERLAY).asText();
            DeviceId brphyDevId = DeviceId.deviceId(bridge);

            context.waitCompletion(DeviceEvent.class, brphyDevId.toString(),
                    () -> addPhysicalPorts(context, brphyDevId, BRIDGE_UNDERLAY, strOvsDatapath),
                    TIMEOUT_PORT_ADDITION_MS
            );
        }

        @Override
        public boolean isCompleted(WorkflowContext context, Event event) throws WorkflowException {
            if (!(event instanceof DeviceEvent)) {
                return false;
            }
            DeviceEvent deviceEvent = (DeviceEvent) event;
            switch (deviceEvent.type()) {
                case PORT_ADDED:
                    return !isNext(context);
                default:
                    return false;
            }
        }

        @Override
        public void timeout(WorkflowContext context) throws WorkflowException {
            if (!isNext(context)) {
                context.completed(); //Complete the job of worklet by timeout
            } else {
                super.timeout(context);
            }
        }

        private final List<String> getPhysicalPorts(WorkflowContext context) throws WorkflowException {
            List<String> ports = Lists.newArrayList();
            for (JsonNode jsonNode : arrNodePhysicalPorts) {
                check(jsonNode instanceof TextNode, "Invalid physical ports " + arrNodePhysicalPorts);
                ports.add(jsonNode.asText());
            }
            return ports;
        }

        private final boolean hasAllPhysicalPorts(WorkflowContext context, DeviceId devId) throws WorkflowException {

            List<Port> devPorts = context.getService(DeviceService.class).getPorts(devId);
            check(devPorts != null, "Invalid device ports for " + devId);
            List<String> physicalPorts = getPhysicalPorts(context);
            check(physicalPorts != null, "Invalid physical ports" + context);

            log.info("physicalPorts: {} for {}", physicalPorts, devId);
            for (String port : physicalPorts) {
                if (devPorts.stream().noneMatch(p -> OvsUtil.portName(p).contains(port))) {
                    return false;
                }
            }
            return true;
        }

        private final boolean hasPort(WorkflowContext context, DeviceId devId, String portName)
                throws WorkflowException {

            List<Port> devPorts = context.getService(DeviceService.class).getPorts(devId);
            check(devPorts != null, "Invalid device ports for " + devId);
            return devPorts.stream().anyMatch(p -> OvsUtil.portName(p).contains(portName));
        }

        private final void addPhysicalPorts(WorkflowContext context, DeviceId devId, String bridgeName,
                                            String strOvsDatapathType)
                throws WorkflowException {
            OvsdbClientService ovsdbClient = OvsUtil.getOvsdbClient(context, strMgmtIp, intOvsdbPort);
            check(ovsdbClient != null, "Invalid ovsdb client");

            List<String> physicalPorts = getPhysicalPorts(context);
            check(physicalPorts != null, "Invalid physical ports");

            OvsDatapathType datapathType = OvsUtil.buildOvsDatapathType(strOvsDatapathType);
            check(datapathType != null, "Invalid data path type");

            List<String> sortedPhyPorts = physicalPorts.stream().sorted().collect(Collectors.toList());

            for (String port : sortedPhyPorts) {
                if (hasPort(context, devId, port)) {
                    continue;
                }
                log.info("adding port {} on {}", port, devId);
                switch (datapathType) {
                    case NETDEV:
                        throw new WorkflowException("NETDEV datapathType are not supported");
                        //break;
                    case SYSTEM:
                    default:
                        ovsdbClient.createPort(BridgeName.bridgeName(bridgeName).name(), port);
                }
            }
        }
    }

    /**
     * Work-let class for configure local ip of underlay openflow bridge.
     */
    public static class ConfigureUnderlayBridgeLocalIp extends AbstractWorklet {

        @JsonDataModel(path = MODEL_SSH_ACCESSINFO)
        JsonNode strSshAccessInfo;

        @JsonDataModel(path = MODEL_VTEP_IP)
        String strVtepIp;

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            SshAccessInfo sshAccessInfo = SshAccessInfo.valueOf(strSshAccessInfo);
            check(Objects.nonNull(sshAccessInfo), "Invalid ssh access info " + context.data());

            NetworkAddress vtepIp = NetworkAddress.valueOf(strVtepIp);
            check(Objects.nonNull(vtepIp), "Invalid vtep ip " + context.data());

            return !SshUtil.exec(sshAccessInfo,
                    session ->
                            SshUtil.hasIpAddrOnInterface(session, BRIDGE_UNDERLAY, vtepIp)
                                    && SshUtil.isIpLinkUpOnInterface(session, BRIDGE_UNDERLAY)
            );
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            SshAccessInfo sshAccessInfo = SshAccessInfo.valueOf(strSshAccessInfo);
            check(Objects.nonNull(sshAccessInfo), "Invalid ssh access info " + context.data());

            NetworkAddress vtepIp = NetworkAddress.valueOf(strVtepIp);
            check(Objects.nonNull(vtepIp), "Invalid vtep ip " + context.data());

            SshUtil.exec(sshAccessInfo,
                    session -> {
                        SshUtil.addIpAddrOnInterface(session, BRIDGE_UNDERLAY, vtepIp);
                        SshUtil.setIpLinkUpOnInterface(session, BRIDGE_UNDERLAY);
                        return "";
                    });

            context.completed();
        }
    }

    /**
     * Work-let class for deleting overlay bridge config.
     */
    public static class DeleteOverlayBridgeConfig extends AbstractWorklet {

        @JsonDataModel(path = MODEL_MGMT_IP)
        String strMgmtIp;

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            BridgeConfig bridgeConfig = OvsUtil.getOvsdbBehaviour(context, strMgmtIp, BridgeConfig.class);

            Collection<BridgeDescription> bridges = bridgeConfig.getBridges();
            return bridges.stream()
                    .anyMatch(bd -> Objects.equals(bd.name(), BRIDGE_OVERLAY));
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            BridgeConfig bridgeConfig = OvsUtil.getOvsdbBehaviour(context, strMgmtIp, BridgeConfig.class);

            bridgeConfig.deleteBridge(BridgeName.bridgeName(BRIDGE_OVERLAY));

            for (int i = 0; i < 10; i++) {
                if (!isNext(context)) {
                    context.completed();
                    return;
                }
                sleep(50);
            }
            throw new WorkflowException("Timeout happened for removing config");
        }

        protected void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Work-let class for removing overlay bridge openflow device.
     */
    public static class RemoveOverlayBridgeOfDevice extends AbstractWorklet {

        @JsonDataModel(path = MODEL_MGMT_IP)
        String strMgmtIp;

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            DeviceId devId = OvsUtil.buildOfDeviceId(IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_OVERLAY);

            Device dev = context.getService(DeviceService.class).getDevice(devId);
            return dev != null;
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            DeviceId devId = OvsUtil.buildOfDeviceId(IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_OVERLAY);

            DeviceAdminService adminService = context.getService(DeviceAdminService.class);

            context.waitCompletion(DeviceEvent.class, devId.toString(),
                    () -> adminService.removeDevice(devId),
                    TIMEOUT_DEVICE_CREATION_MS
            );
        }

        @Override
        public boolean isCompleted(WorkflowContext context, Event event) throws WorkflowException {
            if (!(event instanceof DeviceEvent)) {
                return false;
            }
            DeviceEvent deviceEvent = (DeviceEvent) event;
            switch (deviceEvent.type()) {
                case DEVICE_REMOVED:
                    return !isNext(context);
                default:
                    return false;
            }
        }

        @Override
        public void timeout(WorkflowContext context) throws WorkflowException {
            if (!isNext(context)) {
                context.completed(); //Complete the job of worklet by timeout
            } else {
                super.timeout(context);
            }
        }
    }

    /**
     * Work-let class for deleting underlay bridge config.
     */
    public static class DeleteUnderlayBridgeConfig extends AbstractWorklet {

        @JsonDataModel(path = MODEL_MGMT_IP)
        String strMgmtIp;

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            BridgeConfig bridgeConfig = OvsUtil.getOvsdbBehaviour(context, strMgmtIp, BridgeConfig.class);

            Collection<BridgeDescription> bridges = bridgeConfig.getBridges();
            return bridges.stream()
                    .anyMatch(bd -> Objects.equals(bd.name(), BRIDGE_UNDERLAY));
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            BridgeConfig bridgeConfig = OvsUtil.getOvsdbBehaviour(context, strMgmtIp, BridgeConfig.class);

            bridgeConfig.deleteBridge(BridgeName.bridgeName(BRIDGE_UNDERLAY));

            for (int i = 0; i < 10; i++) {
                if (!isNext(context)) {
                    context.completed();
                    return;
                }
                sleep(50);
            }
            throw new WorkflowException("Timeout happened for removing config");
        }

        protected void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Work-let class for removing underlay bridge openflow device.
     */
    public static class RemoveUnderlayBridgeOfDevice extends AbstractWorklet {

        @JsonDataModel(path = MODEL_MGMT_IP)
        String strMgmtIp;

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            DeviceId devId = OvsUtil.buildOfDeviceId(IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_UNDERLAY_NOVA);

            Device dev = context.getService(DeviceService.class).getDevice(devId);
            return dev != null;
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            DeviceId devId = OvsUtil.buildOfDeviceId(IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_UNDERLAY_NOVA);

            DeviceAdminService adminService = context.getService(DeviceAdminService.class);

            context.waitCompletion(DeviceEvent.class, devId.toString(),
                    () -> adminService.removeDevice(devId),
                    TIMEOUT_DEVICE_CREATION_MS
            );
        }

        @Override
        public boolean isCompleted(WorkflowContext context, Event event) throws WorkflowException {
            if (!(event instanceof DeviceEvent)) {
                return false;
            }
            DeviceEvent deviceEvent = (DeviceEvent) event;
            switch (deviceEvent.type()) {
                case DEVICE_REMOVED:
                    return !isNext(context);
                default:
                    return false;
            }
        }

        @Override
        public void timeout(WorkflowContext context) throws WorkflowException {
            if (!isNext(context)) {
                context.completed(); //Complete the job of worklet by timeout
            } else {
                super.timeout(context);
            }
        }
    }

    /**
     * Work-let class for removing underlay bridge and overlay openflow device.
     */
    public static class RemoveBridgeOfDevice extends AbstractWorklet {

        @JsonDataModel(path = MODEL_MGMT_IP)
        String strMgmtIp;

        @JsonDataModel(path = MODEL_OF_DEVID_FOR_OVERLAY_UNDERLAY_BRIDGE, optional = true)
        ObjectNode ofDevId;

        @Override
        public boolean isNext(WorkflowContext context) throws WorkflowException {

            boolean isOfDevicePresent = true;

            if (ofDevId == null) {
                ofDevId = JsonNodeFactory.instance.objectNode();
                ofDevId.put(String.valueOf(DEVID_IDX_BRIDGE_OVERLAY), OvsUtil.buildOfDeviceId(
                        IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_OVERLAY).toString());
                ofDevId.put(String.valueOf(DEVID_IDX_BRIDGE_UNDERLAY_NOVA), OvsUtil.buildOfDeviceId(
                        IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_UNDERLAY_NOVA).toString());
            }

            if (context.getService(DeviceService.class).
                    getDevice(DeviceId.deviceId(
                            ofDevId.get(String.valueOf(DEVID_IDX_BRIDGE_OVERLAY)).asText())) == null) {
                isOfDevicePresent = false;
            }
            if (context.getService(DeviceService.class).
                    getDevice(DeviceId.deviceId(
                            ofDevId.get(String.valueOf(DEVID_IDX_BRIDGE_UNDERLAY_NOVA)).asText())) == null) {
                isOfDevicePresent = false;
            }

            return isOfDevicePresent;
        }

        @Override
        public void process(WorkflowContext context) throws WorkflowException {

            DeviceAdminService adminService = context.getService(DeviceAdminService.class);
            String ofDevIdOverlay;
            String ofDevIdUnderlay;

            if (ofDevId == null) {
                ofDevId = JsonNodeFactory.instance.objectNode();
                ofDevId.put(String.valueOf(DEVID_IDX_BRIDGE_OVERLAY), OvsUtil.buildOfDeviceId(
                        IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_OVERLAY).toString());
                ofDevId.put(String.valueOf(DEVID_IDX_BRIDGE_UNDERLAY_NOVA), OvsUtil.buildOfDeviceId(
                        IpAddress.valueOf(strMgmtIp), DEVID_IDX_BRIDGE_UNDERLAY_NOVA).toString());
            }

            ofDevIdOverlay = ofDevId.get(String.valueOf(DEVID_IDX_BRIDGE_OVERLAY)).asText();
            ofDevIdUnderlay = ofDevId.get(String.valueOf(DEVID_IDX_BRIDGE_UNDERLAY_NOVA)).asText();

            Set<String> eventHints = Sets.newHashSet(ofDevIdOverlay, ofDevIdUnderlay);

            context.waitAnyCompletion(DeviceEvent.class, eventHints,
                    () -> {
                        adminService.removeDevice(DeviceId.deviceId(ofDevIdOverlay));
                        adminService.removeDevice(DeviceId.deviceId(ofDevIdUnderlay));
                    },
                    TIMEOUT_DEVICE_CREATION_MS
            );
        }

        @Override
        public boolean isCompleted(WorkflowContext context, Event event) throws WorkflowException {
            if (!(event instanceof DeviceEvent)) {
                return false;
            }
            DeviceEvent deviceEvent = (DeviceEvent) event;
            switch (deviceEvent.type()) {
                case DEVICE_REMOVED:
                    log.trace("GOT DEVICE REMOVED EVENT FOR DEVICE {}", event.subject());
                    return !isNext(context);
                default:
                    return false;
            }
        }

        @Override
        public void timeout(WorkflowContext context) throws WorkflowException {
            if (!isNext(context)) {
                context.completed(); //Complete the job of worklet by timeout
            } else {
                super.timeout(context);
            }
        }
    }
}

