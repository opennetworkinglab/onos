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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.onlab.packet.ChassisId;
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
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers device information from a Cisco NXOS device.
 */
public class CiscoNxosDeviceDescription extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {
    private final Logger log = getLogger(getClass());

    private static final String UNKNOWN = "unknown";

    private static final String SHOW_INTERFACES_CMD = "show interface";
    private static final String SHOW_VERSION_CMD = "show version";

    private static final String MANUFACTURER = "manufacturer";
    private static final String CHASSIS_ID = "chassis_id";
    private static final String KICKSTART_VER = "kickstart_ver_str";
    private static final String ROW_INTERFACE = "ROW_interface";
    private static final String INTERFACE = "interface";
    private static final String ETH = "Eth";
    private static final String ETHERNET = "Ethernet";
    private static final String STATE = "state";
    private static final String UP = "up";
    private static final String ETH_BW = "eth_bw";
    private static final String SLASH = "/";
    private static final String ZERO = "0";
    private static final int ONE_THOUSAND = 1000;


    @Override
    public DeviceDescription discoverDeviceDetails() {
        DriverHandler handler = handler();
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = handler.data().deviceId();

        ArrayList<String> cmd = new ArrayList<>();
        cmd.add(SHOW_VERSION_CMD);

        String req = NxApiRequest.generate(cmd, NxApiRequest.CommandType.CLI);

        String response = NxApiRequest.post(controller, deviceId, req);

        String mrf = UNKNOWN;
        String hwVer = UNKNOWN;
        String swVer = UNKNOWN;
        String serialNum = UNKNOWN;

        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode json = om.readTree(response);

            JsonNode body = json.findValue("body");
            if (body != null) {
                mrf = body.get(MANUFACTURER).asText();
                hwVer = body.get(CHASSIS_ID).asText();
                swVer = body.get(KICKSTART_VER).asText();
            }
        } catch (IOException e) {
            log.error("Failed to to retrieve Device Information {}", e);
        }

        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        Device device = deviceService.getDevice(deviceId);
        return new DefaultDeviceDescription(device.id().uri(), Device.Type.SWITCH,
                mrf, hwVer, swVer, serialNum,
                new ChassisId(), (SparseAnnotations) device.annotations());
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        DriverHandler handler = handler();
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = handler.data().deviceId();

        ArrayList<String> cmd = new ArrayList<>();
        cmd.add(SHOW_INTERFACES_CMD);

        String req = NxApiRequest.generate(cmd, NxApiRequest.CommandType.CLI);

        String response = NxApiRequest.post(controller, deviceId, req);

        // parse interface information from response
        List<PortDescription> ports = Lists.newArrayList();
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode json = om.readTree(response);

            JsonNode interfaces = json.findValue(ROW_INTERFACE);
            if (interfaces != null) {
                interfaces.forEach(itf -> {
                    String ifName = itf.get(INTERFACE).asText();
                    if (ifName.startsWith(ETH)) {
                        String ifNum = ifName.substring(ETHERNET.length()).replace(SLASH, ZERO);
                        boolean state = itf.get(STATE).asText().equals(UP);
                        long portSpeed = itf.get(ETH_BW).asLong() / ONE_THOUSAND; //in Mbps
                        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                                .set(AnnotationKeys.PORT_NAME, ifName);
                        PortDescription desc = new DefaultPortDescription(PortNumber.portNumber(ifNum), state,
                                Port.Type.FIBER, portSpeed, annotations.build());
                        ports.add(desc);
                    }
                });
            }
        } catch (IOException e) {
            log.error("Failed to to retrieve Interfaces {}", e);
        }

        return ports;
    }
}
