/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.arista;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sets, gets and removes the openflow controller configuration from a Arista Rest device.
 */
public class ControllerConfigAristaImpl extends AbstractHandlerBehaviour implements ControllerConfig {

    private static final String CONFIGURE_TERMINAL = "configure";
    private static final String OPENFLOW_CMD = "openflow";
    private static final String REMOVE_CONTROLLER_CMD = "no controller tcp:%s:%d";
    private static final String API_ENDPOINT = "/command-api/";
    private static final String JSON = "json";
    private static final String JSONRPC = "jsonrpc";
    private static final String METHOD = "method";
    private static final String RUN_CMDS = "runCmds";
    private static final String VERSION = "version";
    private static final String ID = "id";
    private static final String PARAMS = "params";
    private static final String FORMAT = "format";
    private static final String TIMESTAMPS = "timestamps";
    private static final String CMDS = "cmds";
    private static final String TWO_POINT_ZERO = "2.0";
    private static final String REMOVE_CONTROLLERS = "removeControllers";
    private static final String ENABLE = "enable";
    private static final int MAX_CONTROLLERS = 8;
    private static final Boolean FALSE = false;
    private static final int VERSION_1 = 1;

    private final Logger log = getLogger(getClass());

    @Override
    public List<ControllerInfo> getControllers() {
        throw new UnsupportedOperationException("get controllers configuration is not supported");
    }

    @Override
    public void setControllers(List<ControllerInfo> controllers) {
        throw new UnsupportedOperationException("set controllers configuration is not supported");
    }

    @Override
    public void removeControllers(List<ControllerInfo> controllers) {
        DriverHandler handler = handler();
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = handler.data().deviceId();

        List<String> cmds = new ArrayList<>();
        cmds.add(CONFIGURE_TERMINAL);
        cmds.add(OPENFLOW_CMD);
        controllers.stream().limit(MAX_CONTROLLERS).forEach(c -> cmds
                .add(String.format(REMOVE_CONTROLLER_CMD, c.ip().toString(), c.port())));

        String request = generate(cmds);
        log.info("request :{}", request);

        InputStream stream = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
        String response = controller.post(deviceId, API_ENDPOINT, stream,
                MediaType.APPLICATION_JSON_TYPE, String.class);
        log.info("response :{}", response);

        try {
            JsonNode json = new ObjectMapper().readTree(response);
        } catch (IOException e) {
            log.error("Cannot communicate with device {} , exception {}", deviceId, e);
        }
    }

    /**
     * Generates a ObjectNode from a list of commands in String format.
     *
     * @param commands a list of commands
     * @return an ObjectNode generated from a list of commands in String format
     */
    private static String generate(List<String> commands) {
        ObjectMapper om = new ObjectMapper();

        ArrayNode cmds = om.createArrayNode();
        cmds.add(ENABLE);
        commands.stream().forEach(cmds::add); //commands here

        ObjectNode parm = om.createObjectNode();
        parm.put(FORMAT, JSON);
        parm.put(TIMESTAMPS, FALSE);
        parm.put(CMDS, cmds);
        parm.put(VERSION, VERSION_1);

        ObjectNode node = om.createObjectNode();
        node.put(JSONRPC, TWO_POINT_ZERO);
        node.put(METHOD, RUN_CMDS);

        node.put(PARAMS, parm);
        node.put(ID, REMOVE_CONTROLLERS);

        return node.toString();
    }
}
