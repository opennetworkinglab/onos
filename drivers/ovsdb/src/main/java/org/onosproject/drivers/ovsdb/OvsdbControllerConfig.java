/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.ovsdb.controller.OvsdbBridge;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbConstant;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.onlab.util.Tools.delay;

/**
 * Implementation of controller config which allows to get and set controllers.
 */
public class OvsdbControllerConfig extends AbstractHandlerBehaviour implements ControllerConfig {
    @Override
    public List<ControllerInfo> getControllers() {
        DriverHandler handler = handler();
        OvsdbClientService clientService = getOvsdbClientService(handler);
        Set<ControllerInfo> controllers = clientService.getControllers(
                handler().data().deviceId());
        return new ArrayList<>(controllers);
    }

    @Override
    public void setControllers(List<ControllerInfo> controllers) {
        DriverHandler handler = handler();
        OvsdbClientService clientService = getOvsdbClientService(handler);
        if (!clientService.getControllers(handler().data().deviceId())
                .equals(ImmutableSet.copyOf(controllers))) {
            clientService.setControllersWithDeviceId(handler().
                    data().deviceId(), controllers);
        }
    }

    // Used for getting OvsdbClientService.
    private OvsdbClientService getOvsdbClientService(DriverHandler handler) {
        OvsdbController ovsController = handler.get(OvsdbController.class);
        DeviceService deviceService = handler.get(DeviceService.class);
        DeviceId ofDeviceId = handler.data().deviceId();
        String[] mgmtAddress = deviceService.getDevice(ofDeviceId)
                .annotations().value(AnnotationKeys.MANAGEMENT_ADDRESS).split(":");
        String targetIp = mgmtAddress[0];
        TpPort targetPort = null;
        if (mgmtAddress.length > 1) {
            targetPort = TpPort.tpPort(Integer.parseInt(mgmtAddress[1]));
        }

        List<OvsdbNodeId> nodeIds = ovsController.getNodeIds().stream()
                .filter(nodeId -> nodeId.getIpAddress().equals(targetIp))
                .collect(Collectors.toList());
        if (nodeIds.isEmpty()) {
            //TODO decide what port?
            ovsController.connect(IpAddress.valueOf(targetIp),
                                  targetPort == null ? TpPort.tpPort(OvsdbConstant.OVSDBPORT) : targetPort);
            delay(1000); //FIXME... connect is async
        }
        List<OvsdbClientService> clientServices = ovsController.getNodeIds().stream()
                .filter(nodeId -> nodeId.getIpAddress().equals(targetIp))
                .map(ovsController::getOvsdbClient)
                .filter(cs -> cs.getBridges().stream().anyMatch(b -> dpidMatches(b, ofDeviceId)))
                .collect(Collectors.toList());
        checkState(!clientServices.isEmpty(), "No clientServices found");
        //FIXME add connection to management address if null --> done ?
        return !clientServices.isEmpty() ? clientServices.get(0) : null;
    }

    private static boolean dpidMatches(OvsdbBridge bridge, DeviceId deviceId) {
        checkArgument(bridge.datapathId().isPresent());

        String bridgeDpid = "of:" + bridge.datapathId().get();
        String ofDpid = deviceId.toString();
        return bridgeDpid.equals(ofDpid);
    }
}