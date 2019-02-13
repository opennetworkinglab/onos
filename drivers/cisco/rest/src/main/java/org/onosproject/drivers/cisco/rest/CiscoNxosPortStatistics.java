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
package org.onosproject.drivers.cisco.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers port statistics from a Cisco NXOS device.
 */
public class CiscoNxosPortStatistics extends AbstractHandlerBehaviour implements PortStatisticsDiscovery {

    private static final String SHOW_INTERFACE_CMD = "show interface";
    private static final String ROW_INTERFACE = "ROW_interface";
    private static final String RESULT = "result";
    private static final String ETH_INPKTS = "eth_inpkts";
    private static final String ETH_OUTPKTS = "eth_outpkts";
    private static final String ETH_INBYTES = "eth_inbytes";
    private static final String ETH_OUTBYTES = "eth_outbytes";
    private static final String ETH_INERR = "eth_inerr";
    private static final String ETH_OUTERR = "eth_outerr";
    private static final String ETH_INERR1 = "eth_inerr";
    private static final String ETH_OUTERR1 = "eth_outerr";
    private static final String WHITE_SPACE_FORMAT = "%s %s";

    private final Logger log = getLogger(getClass());

    @Override
    public Collection<PortStatistics> discoverPortStatistics() {
        DriverHandler handler = handler();
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = handler.data().deviceId();
        DeviceService deviceService = this.handler().get(DeviceService.class);
        List<Port> ports = deviceService.getPorts(deviceId);
        Collection<PortStatistics> portStatistics = Lists.newArrayList();

        ports.stream()
                .filter(Port::isEnabled)
                .forEach(port -> portStatistics.add(discoverSpecifiedPortStatistics(port, controller, deviceId)));

        return ImmutableList.copyOf(portStatistics);
    }

    private PortStatistics discoverSpecifiedPortStatistics(Port port,
                                                           RestSBController controller,
                                                           DeviceId deviceId) {
        String portName = port.annotations().value(AnnotationKeys.PORT_NAME);
        ArrayList<String> cmd = Lists.newArrayList();
        cmd.add(String.format(WHITE_SPACE_FORMAT, SHOW_INTERFACE_CMD, portName));

        String request = NxApiRequest.generate(cmd, NxApiRequest.CommandType.CLI);
        String response = NxApiRequest.post(controller, deviceId, request);

        return buildStatisticsFromResponse(response, port.number(), deviceId);
    }

    private DefaultPortStatistics buildStatisticsFromResponse(String response,
                                                              PortNumber portNumber,
                                                              DeviceId deviceId) {
        DefaultPortStatistics defaultPortStatistics = null;

        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode json = om.readTree(response);
            JsonNode res = json.get(RESULT);
            JsonNode deviceInterface = res.findValue(ROW_INTERFACE);
            long packetsReceived = deviceInterface.get(ETH_INPKTS).asLong();
            long packetsSent = deviceInterface.get(ETH_OUTPKTS).asLong();
            long bytesReceived = deviceInterface.get(ETH_INBYTES).asLong();
            long bytesSent = deviceInterface.get(ETH_OUTBYTES).asLong();
            long packetsRxDropped = deviceInterface.get(ETH_INERR).asLong();
            long packetsTxDropped = deviceInterface.get(ETH_OUTERR).asLong();
            long packetsRxErrors = deviceInterface.get(ETH_INERR1).asLong();
            long packetsTxErrors = deviceInterface.get(ETH_OUTERR1).asLong();

            DefaultPortStatistics.Builder builder = DefaultPortStatistics.builder();
            defaultPortStatistics = builder.setPort(portNumber)
                    .setPacketsReceived(packetsReceived)
                    .setPacketsSent(packetsSent)
                    .setBytesReceived(bytesReceived)
                    .setBytesSent(bytesSent)
                    .setPacketsRxDropped(packetsRxDropped)
                    .setPacketsTxDropped(packetsTxDropped)
                    .setPacketsRxErrors(packetsRxErrors)
                    .setPacketsTxErrors(packetsTxErrors)
                    .setDeviceId(deviceId)
                    .build();

        } catch (IOException e) {
            log.error("Cannot read or process NxApi response: {}", e.toString());
        }

        log.debug("Port statistics: {}", defaultPortStatistics);
        return defaultPortStatistics;
    }
}
