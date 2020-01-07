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

package org.onosproject.drivers.cisco.rest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.collect.Lists;
import org.onlab.packet.ChassisId;
import org.onlab.packet.MacAddress;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers the device, ports information from a Cisco Nexus Switch REST device.
 */
public class DeviceDescriptionDiscoveryCiscoImpl extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {
    private final Logger log = getLogger(getClass());

    private static final String SHOW_INTERFACES_CMD = "show interface";
    private static final String SHOW_VERSION_CMD = "show version";
    private static final String SHOW_MODULE_CMD = "show module";

    private static final String JSON_BODY = "body";
    private static final String JSON_RESULT = "result";
    private static final String JSON_INTERFACE = "interface";
    private static final String JSON_ROW_INTERFACE = "ROW_interface";
    private static final String JSON_ROW_MODULE = "ROW_modinfo";
    private static final String JSON_ROW_MODULE_MAC = "ROW_modmacinfo";

    private static final String MANUFACTURER = "manufacturer";
    private static final String CHASSIS_ID = "chassis_id";
    private static final String CISCO_SERIAL_BOARD = "proc_board_id";
    private static final String KICKSTART_VER = "kickstart_ver_str";
    private static final String INTERFACE_STATE = "state";
    private static final String INTERFACE_ADMIN_STATE = "admin_state";
    private static final String INTERFACE_ENABLED = "enabled";
    private static final String INTERFACE_DISABLED = "disabled";
    private static final String STATE_UP = "up";
    private static final String MODULE_MODEL = "model";
    private static final String MODULE_INTERFACE = "modinf";
    private static final String MODULE_MAC = "modmac";
    private static final String MODULE_SERIAL = "serialnum";

    private static final String INTERFACE_ETHERNET = "Ethernet";
    private static final String INTERFACE_PORTCHANNEL = "port-channel";
    private static final String INTERFACE_BW = "eth_bw";
    private static final String INTERFACE_MAC = "eth_bia_addr";
    private static final String BREAKOUT = "breakout";
    private static final String UNKNOWN = "unknown";

    private static final String MODULE_ANNOTATION_FORMAT = "%s:%s:%s";
    private static final String MODULE_BRACKET_FORMAT = "[%s]";

    @Override
    public DeviceDescription discoverDeviceDetails() {
        DeviceId deviceId = handler().data().deviceId();

        ArrayList<String> cmd = new ArrayList<>();
        cmd.add(SHOW_VERSION_CMD);
        cmd.add(SHOW_MODULE_CMD);

        String response = NxApiRequest.postClis(handler(), cmd);

        String mrf = UNKNOWN;
        String hwVer = UNKNOWN;
        String swVer = UNKNOWN;
        String serialNum = UNKNOWN;
        String module = UNKNOWN;

        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode json = om.readTree(response);

            JsonNode body = json.at("/0/body");
            if (!body.isMissingNode()) {
                mrf = body.at("/" + MANUFACTURER).asText();
                hwVer = body.at("/" + CHASSIS_ID).asText();
                swVer = body.at("/" + KICKSTART_VER).asText();
                serialNum = body.at("/" + CISCO_SERIAL_BOARD).asText();
            }


            JsonNode modInfo = json.at("/1/" + JSON_ROW_MODULE);
            JsonNode modMacInfo = json.at("/1/" + JSON_ROW_MODULE_MAC);

            if (!modInfo.isMissingNode()) {
                List<String> modulesAnn = prepareModuleAnnotation(modInfo, modMacInfo);
                module = String.format(MODULE_BRACKET_FORMAT, String.join(",", modulesAnn));
            }
        } catch (JsonParseException e) {
            log.error("Failed to parse Json", e);
        } catch (JsonMappingException e) {
            log.error("Failed to map Json", e);
        } catch (JsonProcessingException e) {
            log.error("Failed to processing Json", e);
        }
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder();

        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        Device device = deviceService.getDevice(deviceId);
        if (device != null) {
            annotations.putAll(device.annotations());
        }

        return new DefaultDeviceDescription(deviceId.uri(), Device.Type.SWITCH,
                                            mrf, hwVer, swVer, serialNum,
                                            new ChassisId(), annotations.build());
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        List<PortDescription> ports = Lists.newArrayList();

        try {
            String response;
            try {
                response = NxApiRequest.postCli(handler(), SHOW_INTERFACES_CMD);
            } catch (NullPointerException e) {
                log.error("Failed to perform {} command on the device {}",
                          SHOW_INTERFACES_CMD, handler().data().deviceId());
                return ports;
            }

            ObjectMapper om = new ObjectMapper();
            JsonNode json = om.readTree(response);
            JsonNode res = json.get(JSON_RESULT);
            JsonNode interfaces = res.findValue(JSON_ROW_INTERFACE);
            Iterator<JsonNode> iter = interfaces.elements();
            Integer ifCount = 1;
            while (iter.hasNext()) {
                JsonNode ifs = iter.next();
                String ifName = ifs.get(JSON_INTERFACE).asText();

                if (isPortValid(ifName)) {
                    Port.Type portType = Port.Type.VIRTUAL;
                    long portSpeed = ifs.get(INTERFACE_BW).asLong() / 1000; //Mbps
                    String portMac = ifs.get(INTERFACE_MAC).asText();
                    MacAddress mac = MacAddress.valueOf(
                            portMac.replace(".", "").replaceAll("(.{2})", "$1:").trim().substring(0, 17));
                    boolean state = STATE_UP.equals(ifs.get(INTERFACE_STATE).asText());
                    String adminState = STATE_UP.equals(ifs.get(INTERFACE_ADMIN_STATE).asText())
                            ? INTERFACE_ENABLED : INTERFACE_DISABLED;

                    DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                            .set(AnnotationKeys.PORT_NAME, ifName)
                            .set(AnnotationKeys.PORT_MAC, mac.toString())
                            .set(AnnotationKeys.ADMIN_STATE, adminState);

                    if (isValidPhysicalPort(ifName)) {
                        String interfaceNumber = ifName.replace(INTERFACE_ETHERNET, "");
                        String[] interfaceLocation = interfaceNumber.split("/");
                        portType = Port.Type.FIBER;

                        if (interfaceLocation.length == 3) {
                            String breakout = ifName.substring(0, ifName.lastIndexOf("/"));
                            annotations.set(BREAKOUT, breakout);
                        }
                    }
                    PortDescription desc = DefaultPortDescription.builder()
                            .withPortNumber(PortNumber.portNumber(ifCount))
                            .isEnabled(state)
                            .type(portType).portSpeed(portSpeed).annotations(annotations.build())
                            .build();
                    ports.add(desc);
                    ifCount++;
                }
            }
        } catch (Exception e) {
            log.error("Exception occurred because of ", e);
        }

        return ports;
    }

    private boolean isValidPhysicalPort(String portName) {
        return portName.startsWith(INTERFACE_ETHERNET);
    }

    private boolean isValidVirtualPort(String portName) {
        return portName.startsWith(INTERFACE_PORTCHANNEL);
    }

    private boolean isPortValid(String portName) {
        return isValidPhysicalPort(portName) || isValidVirtualPort(portName);
    }

    private List<String> prepareModuleAnnotation(JsonNode modules, JsonNode macs) {
        List<String> modulesInfo = new ArrayList<>();
        if (modules.getNodeType() == JsonNodeType.ARRAY) {
            modules.forEach(module -> modulesInfo.add(getModuleInfo(module, macs)));
        } else if (modules.getNodeType() == JsonNodeType.OBJECT) {
            modulesInfo.add(getModuleInfo(modules, macs));
        }
        return modulesInfo;
    }

    private String getModuleInfo(JsonNode module, JsonNode moduleMac) {
        int moduleId = module.get(MODULE_INTERFACE).asInt();
        String moduleModel = module.get(MODULE_MODEL).asText();
        String moduleSerial = getModuleSerial(moduleId, moduleMac);
        return String.format(MODULE_ANNOTATION_FORMAT, moduleId, moduleModel, moduleSerial);
    }

    private String getModuleSerial(int moduleId, JsonNode macs) {
        if (macs.getNodeType() == JsonNodeType.ARRAY) {
            Optional<JsonNode> serial = StreamSupport.stream(macs.spliterator(), false)
                    .filter(mac -> mac.get(MODULE_MAC).asInt() == moduleId)
                    .findAny();
            if (serial.isPresent()) {
                return serial.get().get(MODULE_SERIAL).asText();
            }
        } else if (macs.getNodeType() == JsonNodeType.OBJECT) {
            if (macs.get(MODULE_MAC).asInt() == moduleId) {
                return macs.get(MODULE_SERIAL).asText();
            }
        }
        return UNKNOWN;
    }

}
