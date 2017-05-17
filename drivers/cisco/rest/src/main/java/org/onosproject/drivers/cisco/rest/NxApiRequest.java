/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.drivers.cisco.rest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import org.onosproject.net.DeviceId;
import org.onosproject.protocol.rest.RestSBController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.ws.rs.core.MediaType;

/**
 * Generate json-rpc request for Cisco NX-API.
 */
public final class NxApiRequest {
    public enum CommandType {
        /**
         * Command outputs are in JSON format.
         */
        CLI,
        /**
         * Command outputs are in raw ASCII text.
         */
        CLI_ASCII,
    }

    private static final String CMD = "cmd";
    private static final String VERSION = "version";
    private static final String JSONRPC = "jsonrpc";
    private static final String TWO_POINT_ZERO = "2.0";
    private static final String METHOD = "method";
    private static final String CLI = "cli";
    private static final String CLI_ASCII = "cli_ascii";
    private static final String PARAMS = "params";
    private static final String ID = "id";
    private static final int ONE = 1;

    private static final String API_URI = "/ins";
    private static final String APP_JSON_RPC = "application/json-rpc";

    private NxApiRequest() {
    }

    /**
     * Generates a NX-API request message to execute on the Cisco NXOS device.
     * @param commands list of commands to execute
     * @param type response message format
     * @return the NX-API request string
     */
    public static String generate(List<String> commands, CommandType type) {
        ObjectMapper om = new ObjectMapper();
        ArrayNode aryNode = om.createArrayNode();

        if (commands == null) {
            return aryNode.toString();
        }

        IntStream.range(0, commands.size()).forEach(idx -> {
            ObjectNode parm = om.createObjectNode();
            parm.put(CMD, commands.get(idx));
            parm.put(VERSION, ONE);

            ObjectNode node = om.createObjectNode();
            node.put(JSONRPC, TWO_POINT_ZERO);
            switch (type) {
                case CLI_ASCII:
                    node.put(METHOD, CLI_ASCII);
                    break;
                case CLI:
                default:
                    node.put(METHOD, CLI);
                    break;
            }

            node.set(PARAMS, parm);
            node.put(ID, idx + 1);

            aryNode.add(node);
        });

        return aryNode.toString();
    }

    /**
     * Sends NX-API request message to the device.
     * @param controller RestSBController for Cisco REST device
     * @param deviceId DeviceId for Cisco REST device
     * @param request NX-API request string
     * @return the response string
     */
    public static String post(RestSBController controller, DeviceId deviceId, String request) {
        InputStream stream = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
        String response = controller.post(deviceId, API_URI, stream,
                MediaType.valueOf(APP_JSON_RPC), String.class);

        return response;
    }
}
