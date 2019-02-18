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

package org.onosproject.drivers.arista;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

final class AristaUtils {

    private static final String API_ENDPOINT = "/command-api";
    private static final String JSONRPC = "jsonrpc";
    private static final String METHOD = "method";
    private static final String RUN_CMDS = "runCmds";
    private static final String VERSION = "version";
    private static final String ID = "id";
    private static final String PARAMS = "params";
    private static final String FORMAT = "format";
    private static final String TIMESTAMPS = "timestamps";
    private static final String CMDS = "cmds";
    private static final String ENABLE = "enable";
    private static final String JSON = "json";
    private static final String TWO_POINT_ZERO = "2.0";
    private static final String ONOS_REST = "onos-rest";
    private static final Boolean FALSE = false;
    private static final int VERSION_1 = 1;
    private static final String RESULT = "result";
    private static final String ERROR = "error";

    public static final int RESULT_START_INDEX = 1;

    private static final Logger log = getLogger(AristaUtils.class);

    private AristaUtils() {

    }

    public static Optional<JsonNode> retrieveCommandResult(DriverHandler handler, String cmd) {
        List<String> cmds = Lists.newArrayList();

        cmds.add(cmd);

        return retrieveCommandResult(handler, cmds);
    }

    public static Optional<JsonNode> retrieveCommandResult(DriverHandler handler, List<String> cmds) {
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = checkNotNull(handler.data()).deviceId();
        String request = generate(cmds);

        log.debug("request :{}", request);

        String response = controller.post(deviceId, API_ENDPOINT, new ByteArrayInputStream(request.getBytes()),
                MediaType.APPLICATION_JSON_TYPE, String.class);

        log.debug("response :{}", response);

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = (ObjectNode) mapper.readTree(response);

            if (node.has(ERROR)) {
                log.error("Error {}", node.get(ERROR));
                return Optional.empty();
            } else {
                return Optional.ofNullable(node.get(RESULT));
            }
        } catch (IOException e) {
            log.warn("IO exception occurred because of ", e);
        }
        return Optional.empty();
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
        node.put(ID, ONOS_REST);

        return node.toString();
    }


    public static boolean getWithChecking(DriverHandler handler, String command) {
        List<String> cmds = new ArrayList<>();

        cmds.add(command);

        return getWithChecking(handler, cmds);
    }

    public static boolean getWithChecking(DriverHandler handler, List<String> commands) {
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = checkNotNull(handler.data()).deviceId();
        String response = generate(commands);

        log.debug("request :{}", response);

        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode json = om.readTree(response);
            JsonNode errNode = json.findPath(ERROR);

            if (errNode.isMissingNode()) {
                return true;
            }

            log.error("Error get with checking {}", errNode.asText(""));
            for (String str : commands) {
                log.error("Command Failed due to Cmd : {}", str);
            }
            return false;
        } catch (IOException e) {
            log.error("IO exception occured because of ", e);
            return false;
        }
    }
}