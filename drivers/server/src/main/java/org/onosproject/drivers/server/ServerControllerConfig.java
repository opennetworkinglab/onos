/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.drivers.server;

import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.mastership.MastershipService;

import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.ws.rs.ProcessingException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sets, gets, and removes controller configuration
 * from a commodity server (i.e., a REST device).
 */
public class ServerControllerConfig extends BasicServerDriver
        implements ControllerConfig {

    private final Logger log = getLogger(getClass());

    /**
     * Resource endpoints of the server agent (REST server-side).
     */
    private static final String CONTROLLERS_CONF_URL = BASE_URL + "/controllers";

    /**
     * Parameters to be exchanged with the server's agent.
     */
    private static final String PARAM_CTRL      = "controllers";
    private static final String PARAM_CTRL_IP   = "ip";
    private static final String PARAM_CTRL_PORT = "port";
    private static final String PARAM_CTRL_TYPE = "type";

    /**
     * Constructs controller configuration for server.
     */
    public ServerControllerConfig() {
        super();
        log.debug("Started");
    }

    @Override
    public List<ControllerInfo> getControllers() {
        List<ControllerInfo> controllers = Lists.newArrayList();

        DeviceId deviceId = getHandler().data().deviceId();
        checkNotNull(deviceId, DEVICE_ID_NULL);

        MastershipService mastershipService = getHandler().get(MastershipService.class);
        checkNotNull(deviceId, MASTERSHIP_NULL);

        if (!mastershipService.isLocalMaster(deviceId)) {
            log.warn(
                "I am not master for {}. " +
                "Please use master {} to get controllers for this device",
                deviceId, mastershipService.getMasterFor(deviceId));
            return controllers;
        }

        // Hit the path that provides the server's controllers
        InputStream response = null;
        try {
            response = getController().get(deviceId, CONTROLLERS_CONF_URL, JSON);
        } catch (ProcessingException pEx) {
            log.error("Failed to get controllers of device: {}", deviceId);
            return controllers;
        }

        // Load the JSON into objects
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonMap = null;
        JsonNode jsonNode = null;
        ObjectNode objNode = null;
        try {
            jsonMap  = mapper.readValue(response, Map.class);
            jsonNode = mapper.convertValue(jsonMap, JsonNode.class);
            objNode = (ObjectNode) jsonNode;
        } catch (IOException ioEx) {
            log.error("Failed to get controllers of device: {}", deviceId);
            return controllers;
        }

        if (jsonMap == null) {
            log.error("Failed to get controllers of device: {}", deviceId);
            return controllers;
        }

        JsonNode ctrlNode = objNode.path(PARAM_CTRL);

        // Fetch controller objects
        for (JsonNode cn : ctrlNode) {
            ObjectNode ctrlObjNode = (ObjectNode) cn;

            // Get the attributes of a controller
            String ctrlIpStr = get(cn, PARAM_CTRL_IP);
            int    ctrlPort  = ctrlObjNode.path(PARAM_CTRL_PORT).asInt();
            String ctrlType  = get(cn, PARAM_CTRL_TYPE);

            // Implies no controller
            if (ctrlIpStr.isEmpty()) {
                continue;
            }

            // Check data format and range
            IpAddress ctrlIp = null;
            try {
                ctrlIp = IpAddress.valueOf(ctrlIpStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e);
            }

            if ((ctrlPort < 0) || (ctrlPort > TpPort.MAX_PORT)) {
                final String msg = "Invalid controller port: " + ctrlPort;
                throw new IllegalArgumentException(msg);
            }

            controllers.add(
                new ControllerInfo(ctrlIp, ctrlPort, ctrlType)
            );
        }

        return controllers;
    }

    @Override
    public void setControllers(List<ControllerInfo> controllers) {
        DeviceId deviceId = getHandler().data().deviceId();
        checkNotNull(deviceId, DEVICE_ID_NULL);

        MastershipService mastershipService = getHandler().get(MastershipService.class);
        checkNotNull(deviceId, MASTERSHIP_NULL);

        if (!mastershipService.isLocalMaster(deviceId)) {
            log.warn(
                "I am not master for {}. " +
                "Please use master {} to set controllers for this device",
                deviceId, mastershipService.getMasterFor(deviceId));
            return;
        }

        ObjectMapper mapper = new ObjectMapper();

        // Create the object node to host the data
        ObjectNode sendObjNode = mapper.createObjectNode();

        // Insert header
        ArrayNode ctrlsArrayNode = sendObjNode.putArray(PARAM_CTRL);

        // Add each controller's information object
        for (ControllerInfo ctrl : controllers) {
            ObjectNode ctrlObjNode = mapper.createObjectNode();
            ctrlObjNode.put(PARAM_CTRL_IP,   ctrl.ip().toString());
            ctrlObjNode.put(PARAM_CTRL_PORT, ctrl.port());
            ctrlObjNode.put(PARAM_CTRL_TYPE, ctrl.type());
            ctrlsArrayNode.add(ctrlObjNode);
        }

        // Post the controllers to the device
        int response = getController().post(
            deviceId, CONTROLLERS_CONF_URL,
            new ByteArrayInputStream(sendObjNode.toString().getBytes()), JSON);

        if (!checkStatusCode(response)) {
            log.error("Failed to set controllers on device {}", deviceId);
        }

        return;
    }

    @Override
    public void removeControllers(List<ControllerInfo> controllers) {
        DeviceId deviceId = getHandler().data().deviceId();
        checkNotNull(deviceId, DEVICE_ID_NULL);

        MastershipService mastershipService = getHandler().get(MastershipService.class);
        checkNotNull(deviceId, MASTERSHIP_NULL);

        if (!mastershipService.isLocalMaster(deviceId)) {
            log.warn(
                "I am not master for {}. " +
                "Please use master {} to remove controllers from this device",
                deviceId, mastershipService.getMasterFor(deviceId));
            return;
        }

        for (ControllerInfo ctrl : controllers) {
            log.info("Remove controller with {}:{}:{}",
                ctrl.type(), ctrl.ip().toString(), ctrl.port());

            String remCtrlUrl = CONTROLLERS_CONF_URL + "/" + ctrl.ip().toString();

            // Remove this controller
            int response = getController().delete(deviceId, remCtrlUrl, null, JSON);

            if (!checkStatusCode(response)) {
                log.error("Failed to remove controller {}:{}:{} from device {}",
                    ctrl.type(), ctrl.ip().toString(), ctrl.port(), deviceId);
            }
        }

        return;
    }

}
