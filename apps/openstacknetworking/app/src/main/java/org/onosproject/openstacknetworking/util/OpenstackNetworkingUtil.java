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
package org.onosproject.openstacknetworking.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.network.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static org.onosproject.openstacknetworking.api.Constants.PCISLOT;
import static org.onosproject.openstacknetworking.api.Constants.PCI_VENDOR_INFO;
import static org.onosproject.openstacknetworking.api.Constants.PORT_NAME_PREFIX_MAP;

/**
 * An utility that used in openstack networking app.
 */
public final class OpenstackNetworkingUtil {

    protected static final Logger log = LoggerFactory.getLogger(OpenstackNetworkingUtil.class);

    private static final int HEX_RADIX = 16;
    private static final int ZERO_FUNCTION_NUMBER = 0;
    private static final String PREFIX_DEVICE_NUMBER = "s";
    private static final String PREFIX_FUNCTION_NUMBER = "f";

    /**
     * Prevents object instantiation from external.
     */
    private OpenstackNetworkingUtil() {
    }

    /**
     * Interprets JSON string to corresponding openstack model entity object.
     *
     * @param input JSON string
     * @param entityClazz openstack model entity class
     * @return openstack model entity object
     */
    public static ModelEntity jsonToModelEntity(InputStream input, Class entityClazz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonTree = mapper.enable(INDENT_OUTPUT).readTree(input);
            log.trace(new ObjectMapper().writeValueAsString(jsonTree));
            return ObjectMapperSingleton.getContext(entityClazz)
                    .readerFor(entityClazz)
                    .readValue(jsonTree);
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Converts openstack model entity object into JSON object.
     *
     * @param entity openstack model entity object
     * @param entityClazz openstack model entity class
     * @return JSON object
     */
    public static ObjectNode modelEntityToJson(ModelEntity entity, Class entityClazz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String strModelEntity = ObjectMapperSingleton.getContext(entityClazz)
                    .writerFor(entityClazz)
                    .writeValueAsString(entity);
            log.trace(strModelEntity);
            return (ObjectNode) mapper.readTree(strModelEntity.getBytes());
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }

    /**
     * Obtains the gateway node by device in compute node. Note that the gateway
     * node is determined by device's device identifier.
     *
     * @param gws           a collection of gateway nodes
     * @param deviceId      device identifier
     * @return a gateway node
     */
    public static OpenstackNode getGwByComputeDevId(Set<OpenstackNode> gws, DeviceId deviceId) {
        int numOfGw = gws.size();

        if (numOfGw == 0) {
            return null;
        }

        int gwIndex = Math.abs(deviceId.hashCode()) % numOfGw;

        return getGwByIndex(gws, gwIndex);
    }

    private static OpenstackNode getGwByIndex(Set<OpenstackNode> gws, int index) {
        Map<String, OpenstackNode> hashMap = new HashMap<>();
        gws.forEach(gw -> hashMap.put(gw.hostname(), gw));
        TreeMap<String, OpenstackNode> treeMap = new TreeMap<>(hashMap);
        Iterator<String> iteratorKey = treeMap.keySet().iterator();

        int intIndex = 0;
        OpenstackNode gw = null;
        while (iteratorKey.hasNext()) {
            String key = iteratorKey.next();

            if (intIndex == index) {
                gw = treeMap.get(key);
            }
            intIndex++;
        }
        return gw;
    }

    /**
     * Extract the interface name with the supplied port.
     *
     * @param port port
     * @return interface name
     */
    public static String getIntfNameFromPciAddress(Port port) {
        if (port.getProfile() != null && port.getProfile().get(PCISLOT) == null) {
            log.error("Failed to retrieve the interface name because of no pci_slot information from the port");
            return null;
        }
        String busNumHex = port.getProfile().get(PCISLOT).toString().split(":")[1];
        String busNumDecimal = String.valueOf(Integer.parseInt(busNumHex, HEX_RADIX));

        String deviceNumHex = port.getProfile().get(PCISLOT).toString()
                .split(":")[2]
                .split("\\.")[0];
        String deviceNumDecimal = String.valueOf(Integer.parseInt(deviceNumHex, HEX_RADIX));

        String functionNumHex = port.getProfile().get(PCISLOT).toString()
                .split(":")[2]
                .split("\\.")[1];
        String functionNumDecimal = String.valueOf(Integer.parseInt(functionNumHex, HEX_RADIX));

        String intfName;

        String vendorInfoForPort = String.valueOf(port.getProfile().get(PCI_VENDOR_INFO));

        if (vendorInfoForPort == null) {
            log.error("Failed to retrieve the interface name because of no pci vendor information from the port");
            return null;
        }
        String portNamePrefix = PORT_NAME_PREFIX_MAP.get(vendorInfoForPort);
        if (vendorInfoForPort == null) {
            log.error("Failed to retrieve the interface name because of no prefix information from the port");
            return null;
        }
        if (functionNumDecimal.equals(ZERO_FUNCTION_NUMBER)) {
            intfName = portNamePrefix + busNumDecimal + PREFIX_DEVICE_NUMBER + deviceNumDecimal;
        } else {
            intfName = portNamePrefix + busNumDecimal + PREFIX_DEVICE_NUMBER + deviceNumDecimal
                    + PREFIX_FUNCTION_NUMBER + functionNumDecimal;
        }

        return intfName;
    }
}
