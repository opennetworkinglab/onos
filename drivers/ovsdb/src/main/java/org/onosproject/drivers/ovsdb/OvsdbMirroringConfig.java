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

package org.onosproject.drivers.ovsdb;

import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.MirroringConfig;
import org.onosproject.net.behaviour.MirroringDescription;
import org.onosproject.net.behaviour.MirroringStatistics;
import org.onosproject.net.behaviour.MirroringName;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.ovsdb.controller.OvsdbBridge;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbConstant;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbMirror;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.onlab.util.Tools.delay;

/**
 * Implementation of mirror config which allows to add, delete and get mirrorings statistics.
 */
public class OvsdbMirroringConfig extends AbstractHandlerBehaviour implements MirroringConfig {

    private static Logger log = LoggerFactory.getLogger(OvsdbMirroringConfig.class);

    /**
     * Adds a mirroring with a given description.
     *
     * @param bridge the bridge name
     * @param mirroringDescription mirroring description
     * @return true if succeeds, or false
     */
    @Override
    public boolean addMirroring(BridgeName bridge, MirroringDescription mirroringDescription) {
        DriverHandler handler = handler();
        OvsdbClientService ovsdbClient = getOvsdbClientService(handler);
        OvsdbMirror mirror = OvsdbMirror.builder(mirroringDescription).build();
        return ovsdbClient.createMirror(bridge.name(), mirror);
    }

    /**
     * Removes a mirroring.
     *
     * @param mirroringName mirroring name
     */
    @Override
    public void deleteMirroring(MirroringName mirroringName) {
        DriverHandler handler = handler();
        OvsdbClientService ovsdbClient = getOvsdbClientService(handler);
        ovsdbClient.dropMirror(mirroringName);
    }

    /**
     * Returns a collection of MirroringStatistics.
     *
     * @return statistics collection
     */
    @Override
    public Collection<MirroringStatistics> getMirroringStatistics() {
        DriverHandler handler = handler();
        OvsdbClientService ovsdbClient = getOvsdbClientService(handler);
        return ovsdbClient.getMirroringStatistics(handler.data().deviceId());
    }

    /**
     * Helper method which is used for getting OvsdbClientService.
     */
    private OvsdbClientService getOvsdbClientService(DriverHandler handler) {

        OvsdbController ovsController = handler.get(OvsdbController.class);
        DeviceService deviceService = handler.get(DeviceService.class);
        DeviceId deviceId = handler.data().deviceId();

        String[] splits = deviceId.toString().split(":");
        if (splits == null || splits.length < 1) {
            log.warn("Wrong deviceId format");
            return null;
        }

        /**
         * Each type of device has to be managed in a different way.
         */
        switch (splits[0]) {
            case "ovsdb":
                OvsdbNodeId nodeId = changeDeviceIdToNodeId(deviceId);
                return ovsController.getOvsdbClient(nodeId);
            case "of":
                String[] mgmtAddress = deviceService.getDevice(deviceId)
                        .annotations().value(AnnotationKeys.MANAGEMENT_ADDRESS).split(":");
                String targetIp = mgmtAddress[0];
                TpPort targetPort = null;
                if (mgmtAddress.length > 1) {
                    targetPort = TpPort.tpPort(Integer.parseInt(mgmtAddress[1]));
                }
                List<OvsdbNodeId> nodeIds = ovsController.getNodeIds().stream()
                        .filter(nodeID -> nodeID.getIpAddress().equals(targetIp))
                        .collect(Collectors.toList());
                if (nodeIds.size() == 0) {
                    //TODO decide what port?
                    ovsController.connect(IpAddress.valueOf(targetIp),
                                          targetPort == null ? TpPort.tpPort(OvsdbConstant.OVSDBPORT) : targetPort);
                    delay(1000); //FIXME... connect is async
                }
                List<OvsdbClientService> clientServices = ovsController.getNodeIds().stream()
                        .filter(nodeID -> nodeID.getIpAddress().equals(targetIp))
                        .map(ovsController::getOvsdbClient)
                        .filter(cs -> cs.getBridges().stream().anyMatch(b -> dpidMatches(b, deviceId)))
                        .collect(Collectors.toList());
                checkState(clientServices.size() > 0, "No clientServices found");
                //FIXME add connection to management address if null --> done ?
                return clientServices.size() > 0 ? clientServices.get(0) : null;
            default:
                log.warn("Unmanaged device type");
        }
        return null;

    }

    private static boolean dpidMatches(OvsdbBridge bridge, DeviceId deviceId) {
        checkArgument(bridge.datapathId().isPresent());

        String bridgeDpid = "of:" + bridge.datapathId().get();
        String ofDpid = deviceId.toString();
        return bridgeDpid.equals(ofDpid);
    }

    /**
     * OvsdbNodeId(IP) is used in the adaptor while DeviceId(ovsdb:IP)
     * is used in the core. So DeviceId need be changed to OvsdbNodeId.
     *
     * @param deviceId the device id in ovsdb:ip format
     * @return the ovsdb node id
     */
    private OvsdbNodeId changeDeviceIdToNodeId(DeviceId deviceId) {
        String[] splits = deviceId.toString().split(":");
        if (splits == null || splits.length < 1) {
            return null;
        }
        IpAddress ipAddress = IpAddress.valueOf(splits[1]);
        return new OvsdbNodeId(ipAddress, 0);
    }

}
