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

package org.onosproject.drivers.arista;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.onlab.packet.ChassisId;
import org.onlab.packet.MacAddress;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers the ports from Arista EOS device.
 */
public class DeviceDescriptionDiscoveryAristaImpl extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private static final String UNKNOWN = "unknown";
    private static final String INTERFACE_STATUSES = "interfaceStatuses";
    private static final String LINK_STATUS = "linkStatus";
    private static final String LINE_PROTOCOL_STATUS = "lineProtocolStatus";
    private static final String BANDWIDTH = "bandwidth";
    private static final String ETHERNET = "Ethernet";
    private static final String MANAGEMENT = "Management";
    private static final String INTERFACE_TYPE = "interfaceType";
    private static final String INTERFACES = "interfaces";
    private static final String BURNED_IN_ADDRESS = "burnedInAddress";
    private static final String PHYSICAL_ADDRESS = "physicalAddress";
    private static final String MODEL_NAME = "modelName";
    private static final String SW_VERSION = "version";
    private static final String SERIAL_NUMBER = "serialNumber";
    private static final String SYSTEM_MAC_ADDRESS = "systemMacAddress";
    private static final int WEIGHTING_FACTOR_MANAGEMENT_INTERFACE = 10000;
    private static final String MANUFACTURER = "Arista Networks";
    private static final String SHOW_INTERFACES_STATUS = "show interfaces status";
    private static final String SHOW_INTERFACES = "show interfaces";
    private static final String SHOW_VERSION = "show version";
    private static final long MBPS = 1000000;

    private final Logger log = getLogger(getClass());

    @Override
    public DeviceDescription discoverDeviceDetails() {
        try {
            Optional<JsonNode> result = AristaUtils.retrieveCommandResult(handler(), SHOW_VERSION);

            if (!result.isPresent()) {
                return null;
            }

            JsonNode jsonNode = result.get().get(AristaUtils.RESULT_START_INDEX);
            String hwVer = jsonNode.get(MODEL_NAME).asText(UNKNOWN);
            String swVer = jsonNode.get(SW_VERSION).asText(UNKNOWN);
            String serialNum = jsonNode.get(SERIAL_NUMBER).asText(UNKNOWN);
            String systemMacAddress = jsonNode.get(SYSTEM_MAC_ADDRESS).asText("").replace(":", "");
            DeviceId deviceId = checkNotNull(handler().data().deviceId());
            DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
            Device device = deviceService.getDevice(deviceId);
            ChassisId chassisId = systemMacAddress.isEmpty() ? new ChassisId() : new ChassisId(systemMacAddress);

            log.debug("systemMacAddress: {}", systemMacAddress);

            SparseAnnotations annotations = device == null ?
                    DefaultAnnotations.builder().build() : (SparseAnnotations) device.annotations();

            return new DefaultDeviceDescription(deviceId.uri(), Device.Type.SWITCH,
                    MANUFACTURER, hwVer, swVer, serialNum, chassisId, annotations);
        } catch (Exception e) {
            log.error("Exception occurred because of {}, trace: {}", e, e.getStackTrace());
            return null;
        }
    }

    @Override
    public List<PortDescription> discoverPortDetails() {

        Map<String, MacAddress> macAddressMap = getMacAddressesByInterface();
        List<PortDescription> ports = Lists.newArrayList();
        DeviceId deviceId = checkNotNull(handler().data().deviceId());

        try {
            Optional<JsonNode> result = AristaUtils.retrieveCommandResult(handler(), SHOW_INTERFACES_STATUS);

            if (!result.isPresent()) {
                log.warn("{} Device unable to get interfaces status information.", deviceId);
                return ports;
            }

            JsonNode jsonNode = result.get().findValue(INTERFACE_STATUSES);
            jsonNode.fieldNames().forEachRemaining(name -> {
                JsonNode interfaceNode = jsonNode.get(name);

                Long bandwidth = interfaceNode.path(BANDWIDTH).asLong() / MBPS;

                String macAddress = macAddressMap.containsKey(name) ? macAddressMap.get(name).toString() : "";

                SparseAnnotations annotations = DefaultAnnotations.builder()
                        .set(AnnotationKeys.BANDWIDTH, bandwidth.toString())
                        .set(AnnotationKeys.NAME, name)
                        .set(AnnotationKeys.PORT_NAME, name)
                        .set(AnnotationKeys.PORT_MAC, macAddress)
                        .set(LINK_STATUS, interfaceNode.path(LINK_STATUS).asText())
                        .set(LINE_PROTOCOL_STATUS, interfaceNode.path(LINE_PROTOCOL_STATUS).asText())
                        .set(INTERFACE_TYPE, interfaceNode.path(INTERFACE_TYPE).asText())
                        .build();

                int portNumber;

                try {
                    portNumber = getPortNumber(name);
                } catch (Exception e) {
                    log.debug("Interface does not have port number: {}", name);
                    return;
                }

                PortDescription portDescription = DefaultPortDescription.builder()
                        .withPortNumber(PortNumber.portNumber(portNumber))
                        .isEnabled(true)
                        .type(Port.Type.FIBER)
                        .portSpeed(bandwidth)
                        .annotations(annotations)
                        .build();
                ports.add(portDescription);

            });

        } catch (Exception e) {
            log.error("Exception occurred because of {}, trace: {}", e, e.getStackTrace());
        }
        return ports;
    }

    private int getPortNumber(String interfaceName) {
        if (interfaceName.startsWith(ETHERNET)) {
            return Integer.valueOf(interfaceName.substring(ETHERNET.length()).replace('/', '0'));
        } else {
            return Integer.valueOf(interfaceName.substring(MANAGEMENT.length())).intValue()
                    + WEIGHTING_FACTOR_MANAGEMENT_INTERFACE;
        }
    }

    private Map<String, MacAddress> getMacAddressesByInterface() {

        DeviceId deviceId = checkNotNull(handler().data().deviceId());
        Map<String, MacAddress> macAddressMap = new HashMap();

        try {
            Optional<JsonNode> result = AristaUtils.retrieveCommandResult(handler(), SHOW_INTERFACES);

            if (!result.isPresent()) {
                log.warn("{} Device unable to get interface information.", deviceId);
                return macAddressMap;
            }

            JsonNode jsonNode = result.get().findValue(INTERFACES);

            jsonNode.fieldNames().forEachRemaining(name -> {
                JsonNode interfaceNode = jsonNode.get(name);
                JsonNode macAddressNode = interfaceNode.get(BURNED_IN_ADDRESS);

                if (macAddressNode == null) {
                    log.debug("Interface does not have {}: {}", BURNED_IN_ADDRESS, name);
                    return;
                }

                String macAddress = macAddressNode.asText("");

                if (macAddress.isEmpty()) {
                    macAddressNode = interfaceNode.get(PHYSICAL_ADDRESS);

                    if (macAddressNode == null) {
                        log.debug("Interface does not have {}: {}", PHYSICAL_ADDRESS, name);
                        return;
                    }

                    macAddress = macAddressNode.asText("");

                    if (macAddress.isEmpty()) {
                        log.debug("Interface does not have any mac address: {}", name);
                        return;
                    }
                }

                try {
                    macAddressMap.put(name, MacAddress.valueOf(macAddress));
                } catch (IllegalArgumentException e) {
                    log.error("Cannot parse macAddress: {}", macAddress);
                }
            });
        } catch (Exception e) {
            log.error("Exception occurred because of {}, trace: {}", e, e.getStackTrace());
        }

        return macAddressMap;
    }
}

