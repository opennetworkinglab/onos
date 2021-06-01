/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.drivers.gnmi;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import gnmi.Gnmi;
import gnmi.Gnmi.GetRequest;
import gnmi.Gnmi.GetResponse;
import gnmi.Gnmi.Path;
import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.grpc.utils.AbstractGrpcHandlerBehaviour;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Behaviour to get port statistics from device via gNMI.
 */
public class OpenConfigGnmiPortStatisticsDiscovery
        extends AbstractGrpcHandlerBehaviour<GnmiClient, GnmiController>
        implements PortStatisticsDiscovery {

    private static final String LAST_CHANGE = "last-change";

    public OpenConfigGnmiPortStatisticsDiscovery() {
        super(GnmiController.class);
    }

    @Override
    public Collection<PortStatistics> discoverPortStatistics() {
        if (!setupBehaviour("discoverPortStatistics()")) {
            return Collections.emptyList();
        }

        Map<String, PortNumber> ifacePortNumberMapping = Maps.newHashMap();
        List<Port> ports = deviceService.getPorts(deviceId);
        GetRequest.Builder getRequest = GetRequest.newBuilder();
        getRequest.setEncoding(Gnmi.Encoding.PROTO);

        // Use this path to get all counters from specific interface(port)
        // /interfaces/interface[port-name]/state/counters/[counter name]
        ports.forEach(port -> {
            String portName = port.number().name();
            Path path = interfaceCounterPath(portName);
            getRequest.addPath(path);
            ifacePortNumberMapping.put(portName, port.number());
        });

        GetResponse getResponse = Futures.getUnchecked(client.get(getRequest.build()));

        Map<String, Long> inPkts = Maps.newHashMap();
        Map<String, Long> outPkts = Maps.newHashMap();
        Map<String, Long> inBytes = Maps.newHashMap();
        Map<String, Long> outBytes = Maps.newHashMap();
        Map<String, Long> inDropped = Maps.newHashMap();
        Map<String, Long> outDropped = Maps.newHashMap();
        Map<String, Long> inErrors = Maps.newHashMap();
        Map<String, Long> outErrors = Maps.newHashMap();
        Map<String, Duration> timestamps = Maps.newHashMap();

        // Collect responses and sum {in,out,dropped} packets
        getResponse.getNotificationList().forEach(notification -> {
            notification.getUpdateList().forEach(update -> {
                Path path = update.getPath();
                String ifName = interfaceNameFromPath(path);
                timestamps.putIfAbsent(ifName, Duration.ofNanos(notification.getTimestamp()));

                // Last element is the counter name
                String counterName = path.getElem(path.getElemCount() - 1).getName();
                long counterValue = update.getVal().getUintVal();


                switch (counterName) {
                    case "in-octets":
                        inBytes.put(ifName, counterValue);
                        break;
                    case "out-octets":
                        outBytes.put(ifName, counterValue);
                        break;
                    case "in-discards":
                    case "in-fcs-errors":
                        inDropped.compute(ifName, (k, v) -> v == null ? counterValue : v + counterValue);
                        break;
                    case "out-discards":
                        outDropped.put(ifName, counterValue);
                        break;
                    case "in-errors":
                        inErrors.put(ifName, counterValue);
                        break;
                    case "out-errors":
                        outErrors.put(ifName, counterValue);
                        break;
                    case "in-unicast-pkts":
                    case "in-broadcast-pkts":
                    case "in-multicast-pkts":
                    case "in-unknown-protos":
                        inPkts.compute(ifName, (k, v) -> v == null ? counterValue : v + counterValue);
                        break;
                    case "out-unicast-pkts":
                    case "out-broadcast-pkts":
                    case "out-multicast-pkts":
                        outPkts.compute(ifName, (k, v) -> v == null ? counterValue : v + counterValue);
                        break;
                    default:
                        log.warn("Unsupported counter name {}, ignored", counterName);
                        break;
                }
            });
        });

        // Build ONOS port stats map
        return ifacePortNumberMapping.entrySet().stream()
            .map(e -> {
                String ifName = e.getKey();
                PortNumber portNumber = e.getValue();
                Duration portActive = getDurationActive(portNumber, timestamps.get(ifName));
                return DefaultPortStatistics.builder()
                        .setDeviceId(deviceId)
                        .setPort(portNumber)
                        .setDurationSec(portActive.getSeconds())
                        .setDurationNano(portActive.getNano())
                        .setPacketsSent(outPkts.getOrDefault(ifName, 0L))
                        .setPacketsReceived(inPkts.getOrDefault(ifName, 0L))
                        .setPacketsTxDropped(outDropped.getOrDefault(ifName, 0L))
                        .setPacketsRxDropped(inDropped.getOrDefault(ifName, 0L))
                        .setBytesSent(outBytes.getOrDefault(ifName, 0L))
                        .setBytesReceived(inBytes.getOrDefault(ifName, 0L))
                        .setPacketsTxErrors(outErrors.getOrDefault(ifName, 0L))
                        .setPacketsRxErrors(inErrors.getOrDefault(ifName, 0L))
                        .build();
            })
            .collect(Collectors.toList());

    }

    private String interfaceNameFromPath(Path path) {
        // /interfaces/interface[name=iface-name]
        return path.getElem(1).getKeyOrDefault("name", null);
    }

    private Path interfaceCounterPath(String portName) {
        // /interfaces/interface[name=port-name]/state/counters
        return Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder().setName("interfaces").build())
                .addElem(Gnmi.PathElem.newBuilder().setName("interface")
                        .putKey("name", portName).build())
                .addElem(Gnmi.PathElem.newBuilder().setName("state").build())
                .addElem(Gnmi.PathElem.newBuilder().setName("counters").build())
                .build();
    }

    private Duration getDurationActive(PortNumber portNumber, Duration timestamp) {
        Port port = deviceService.getPort(deviceId, portNumber);
        if (port == null || !port.isEnabled()) {
            //FIXME log
            return Duration.ZERO;
        }

        // Set duration 0 for devices that do not support reporting last-change
        String lastChangedStr = port.annotations().value(LAST_CHANGE);
        if (lastChangedStr == null) {
            return Duration.ZERO;
        }

        try {
            long lastChanged = Long.parseLong(lastChangedStr);
            return lastChanged == 0 ? Duration.ZERO : timestamp.minus(lastChanged, ChronoUnit.NANOS);
        } catch (NullPointerException | NumberFormatException ex) {
            //FIXME log
            return Duration.ZERO;
        }
    }
}
