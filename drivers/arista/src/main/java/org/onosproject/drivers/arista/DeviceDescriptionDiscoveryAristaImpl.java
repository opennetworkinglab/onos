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

package org.onosproject.drivers.arista;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.protocol.rest.RestSBController;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers the ports from Arista EOS device.
 */
public class DeviceDescriptionDiscoveryAristaImpl extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private static final String JSON = "json";
    private static final String RESULT = "result";
    private static final String INTERFACE_STATUSES = "interfaceStatuses";
    private static final String LINK_STATUS = "linkStatus";
    private static final String LINE_PROTOCOL_STATUS = "lineProtocolStatus";
    private static final String BANDWIDTH = "bandwidth";
    private static final String ETHERNET = "Ethernet";
    private static final String MANAGEMENT = "Management";
    private static final String INTERFACE_TYPE = "interfaceType";
    private static final int WEIGHTING_FACTOR_MANAGEMENT_INTERFACE = 100;
    private static final String JSONRPC = "jsonrpc";
    private static final String METHOD = "method";
    private static final String RUN_CMDS = "runCmds";
    private static final String VERSION = "version";
    private static final String ID = "id";
    private static final String GET_PORT = "GetPort";
    private static final String PARAMS = "params";
    private static final String FORMAT = "format";
    private static final String TIMESTAMPS = "timestamps";
    private static final String CMDS = "cmds";
    private static final String SHOW_INTERFACES_STATUS = "show interfaces status";
    private static final String TWO_POINT_ZERO = "2.0";
    private static final long MBPS = 1000000;

    private final Logger log = getLogger(getClass());

    private static final String API_ENDPOINT = "/command-api/";

    @Override
    public DeviceDescription discoverDeviceDetails() {
        log.info("No description to be added for device");
        //TODO to be implemented if needed.
        return null;
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        List<PortDescription> ports = Lists.newArrayList();
        DriverHandler handler = handler();
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = handler.data().deviceId();

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode sendObjNode = mapper.createObjectNode();

        sendObjNode.put(JSONRPC, TWO_POINT_ZERO)
                .put(METHOD, RUN_CMDS)
                .put(ID, GET_PORT)
                .putObject(PARAMS)
                .put(FORMAT, JSON)
                .put(TIMESTAMPS, false)
                .put(VERSION, 1)
                .putArray(CMDS).add(SHOW_INTERFACES_STATUS);

        String response = controller.post(deviceId, API_ENDPOINT,
                new ByteArrayInputStream(sendObjNode.toString().getBytes()),
                MediaType.APPLICATION_JSON, String.class);

        try {
            ObjectNode node = (ObjectNode) mapper.readTree(response);
            ArrayNode arrayNode = (ArrayNode) node.get(RESULT);

            JsonNode jsonNode = arrayNode.iterator().next().get(INTERFACE_STATUSES);

            jsonNode.fieldNames().forEachRemaining(name -> {
                JsonNode interfaceNode = jsonNode.get(name);

                Long bandwidth = interfaceNode.path(BANDWIDTH).asLong() / MBPS;

                SparseAnnotations annotations = DefaultAnnotations.builder()
                        .set(AnnotationKeys.BANDWIDTH, bandwidth.toString())
                        .set(AnnotationKeys.NAME, name)
                        .set(LINK_STATUS, interfaceNode.path(LINK_STATUS).asText())
                        .set(LINE_PROTOCOL_STATUS, interfaceNode.path(LINE_PROTOCOL_STATUS).asText())
                        .set(INTERFACE_TYPE, interfaceNode.path(INTERFACE_TYPE).asText())
                        .build();

                PortDescription portDescription = new DefaultPortDescription(PortNumber
                        .portNumber(getPortNumber(name)),
                        true, Port.Type.FIBER, bandwidth, annotations);
                ports.add(portDescription);

            });

        } catch (IOException e) {
            log.warn("IO exception occured because of ", e);
        }
        return ports;
    }

    private int getPortNumber(String interfaceName) {
        if (interfaceName.startsWith(ETHERNET)) {
            return Integer.valueOf(interfaceName.substring(ETHERNET.length()));
        } else {
            return Integer.valueOf(interfaceName.substring(MANAGEMENT.length())).intValue()
                    + WEIGHTING_FACTOR_MANAGEMENT_INTERFACE;
        }
    }
}

