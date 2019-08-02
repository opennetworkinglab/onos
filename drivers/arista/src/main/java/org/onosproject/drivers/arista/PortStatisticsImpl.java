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
package org.onosproject.drivers.arista;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers port statistics from a Arista Rest devices.
 */
public class PortStatisticsImpl extends AbstractHandlerBehaviour implements PortStatisticsDiscovery {

    private static final String IN_UCASTPKTS = "inUcastPkts";
    private static final String IN_BRODCASTPKTS = "inBroadcastPkts";
    private static final String IN_MULICASTPKTS = "inMulticastPkts";
    private static final String IN_OCTETS = "inOctets";
    private static final String IN_TOTALERRORS = "totalInErrors";
    private static final String IN_DISCARDS = "inDiscards";
    private static final String OUT_UCASTPKTS = "outUcastPkts";
    private static final String OUT_BRODCASTPKTS = "outBroadcastPkts";
    private static final String OUT_MULICASTPKTS = "outMulticastPkts";
    private static final String OUT_OCTETS = "outOctets";
    private static final String OUT_TOTALERRORS = "totalOutErrors";
    private static final String OUT_DISCARDS = "outDiscards";
    private static final String COUNTER = "counterRefreshTime";
    private static final String INTERFACES = "interfaces";
    private static final String INTERFACE_COUNTERS = "interfaceCounters";

    private static final String SHOW_INTERFACES = "show interfaces";
    private static final int SEC_TO_NSEC = 1000000000;

    private final Logger log = getLogger(getClass());

    @Override
    public Collection<PortStatistics> discoverPortStatistics() {
        Collection<PortStatistics> portStatistics = Lists.newArrayList();
        try {
            DeviceId deviceId = handler().data().deviceId();
            DeviceService deviceService = this.handler().get(DeviceService.class);
            List<Port> ports = deviceService.getPorts(deviceId);
            Optional<JsonNode> result = AristaUtils.retrieveCommandResult(handler(), SHOW_INTERFACES);

            if (!result.isPresent()) {
                return portStatistics;
            }

            JsonNode interfaces = result.get().findValue(INTERFACES);

            if (interfaces == null) {
                return portStatistics;
            }

            Iterator<Map.Entry<String, JsonNode>> ifIterator = interfaces.fields();
            while (ifIterator.hasNext()) {
                Map.Entry<String, JsonNode> intf = ifIterator.next();
                String ifName = intf.getKey();
                JsonNode interfaceNode = intf.getValue();
                JsonNode interfaceCounters = interfaceNode.get(INTERFACE_COUNTERS);
                if (interfaceCounters == null) {
                    continue;
                }
                ports.stream().filter(Port::isEnabled)
                        .filter(port -> {
                            String portName = port.annotations().value(AnnotationKeys.PORT_NAME);
                            return portName != null && portName.equals(ifName);
                        })
                        .findAny()
                        .ifPresent(port -> portStatistics.add(
                                buildStatisticsForPort(interfaceCounters, port.number(), deviceId)));
            }
        } catch (Exception e) {
            log.error("Exception occurred because of", e);
        }
        return portStatistics;
    }

    private DefaultPortStatistics buildStatisticsForPort(JsonNode portResponse,
                                                         PortNumber portNumber,
                                                         DeviceId deviceId) {
        DefaultPortStatistics defaultPortStatistics = null;

        try {
            long packetsReceivedUcast = portResponse.get(IN_UCASTPKTS).asLong();
            long packetsReceivedMcast = portResponse.get(IN_MULICASTPKTS).asLong();
            long packetsReceivedBcast = portResponse.get(IN_BRODCASTPKTS).asLong();
            long packetsSentUcast = portResponse.get(OUT_UCASTPKTS).asLong();
            long packetsSentMcast = portResponse.get(OUT_MULICASTPKTS).asLong();
            long packetsSentBcast = portResponse.get(OUT_BRODCASTPKTS).asLong();
            long bytesReceived = portResponse.get(IN_OCTETS).asLong();
            long bytesSent = portResponse.get(OUT_OCTETS).asLong();
            long packetsRxDropped = portResponse.get(IN_DISCARDS).asLong();
            long packetsTxDropped = portResponse.get(OUT_DISCARDS).asLong();
            long packetsRxErrors = portResponse.get(IN_TOTALERRORS).asLong();
            long packetsTxErrors = portResponse.get(OUT_TOTALERRORS).asLong();
            double counter = portResponse.get(COUNTER).asDouble();

            long packetsSent = packetsSentUcast + packetsSentMcast + packetsSentBcast;
            long packetsReceived = packetsReceivedUcast + packetsReceivedMcast + packetsReceivedBcast;
            long counterSec = (long) counter;
            long counterNano = (long) (counter * SEC_TO_NSEC);

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
                    .setDurationSec(counterSec)
                    .setDurationNano(counterNano)
                    .setAnnotations(DefaultAnnotations.builder().build())
                    .build();

        } catch (Exception e) {
            log.error("Cannot process port statistics calculation: {}", e.toString());
        }

        log.debug("Port statistics: {}", defaultPortStatistics);
        return defaultPortStatistics;
    }
}
