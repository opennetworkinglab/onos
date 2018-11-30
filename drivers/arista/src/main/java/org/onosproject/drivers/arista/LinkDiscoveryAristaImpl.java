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
import com.google.common.base.Supplier;
import com.google.common.collect.Sets;
import org.onlab.packet.MacAddress;
import org.onlab.util.Tools;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LinkDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

public class LinkDiscoveryAristaImpl extends AbstractHandlerBehaviour implements LinkDiscovery {

    private static final String SHOW_LLDP_NEIGHBOR_DETAIL_CMD = "show lldp neighbors detail";
    private static final String LLDP_NEIGHBORS = "lldpNeighbors";
    private static final String LLDP_NEIGHBOR_INFO = "lldpNeighborInfo";
    private static final String CHASSIS_ID = "chassisId";
    private static final String PORT_ID = "interfaceDescription";
    private static final String CHASSIS_ID_TYPE = "chassisIdType";
    private static final String CHASSIS_ID_TYPE_MAC = "macAddress";

    private final Logger log = getLogger(getClass());

    @Override
    public Set<LinkDescription> getLinks() {
        return createLinksDescs(AristaUtils.retrieveCommandResult(handler(), SHOW_LLDP_NEIGHBOR_DETAIL_CMD));
    }

    private Set<LinkDescription> createLinksDescs(Optional<JsonNode> response) {
        DriverHandler handler = checkNotNull(handler());
        DeviceId localDeviceId = checkNotNull(handler.data().deviceId());
        DeviceService deviceService = handler.get(DeviceService.class);
        Set<LinkDescription> linkDescriptions = Sets.newHashSet();
        List<Port> ports = deviceService.getPorts(localDeviceId);

        if (ports.isEmpty() || Objects.isNull(response)) {
            return linkDescriptions;
        }

        if (!response.isPresent()) {
            return linkDescriptions;
        }

        log.debug("response: {}, {}", response, localDeviceId.toString());

        JsonNode res = response.get();

        if (res == null) {
            log.warn("result is null");
            return linkDescriptions;
        }

        JsonNode lldpNeighbors = res.findValue(LLDP_NEIGHBORS);

        if (lldpNeighbors == null) {
            log.warn("{} is null", LLDP_NEIGHBORS);
            return linkDescriptions;
        }

        Iterator<Map.Entry<String, JsonNode>> lldpNeighborsIter = lldpNeighbors.fields();

        while (lldpNeighborsIter.hasNext()) {
            Map.Entry<String, JsonNode> neighbor = lldpNeighborsIter.next();
            String lldpLocalPort = neighbor.getKey();
            JsonNode neighborValue = neighbor.getValue();

            log.debug("lldpLocalPort: {}", lldpLocalPort);
            log.debug("neighborValue: {}", neighborValue.toString());

            if (lldpLocalPort.isEmpty()) {
                continue;
            }

            JsonNode neighborInfo = neighborValue.findValue(LLDP_NEIGHBOR_INFO);

            if (neighborInfo == null) {
                log.warn("{} is null", LLDP_NEIGHBOR_INFO);
                continue;
            }

            Iterator<JsonNode> neighborInfoIter = neighborInfo.elements();

            while (neighborInfoIter.hasNext()) {
                JsonNode info = neighborInfoIter.next();
                String chassisIdType = info.get(CHASSIS_ID_TYPE).asText("");

                if (chassisIdType == null) {
                    log.warn("{} is null", CHASSIS_ID_TYPE);
                    continue;
                }

                if (!chassisIdType.equals(CHASSIS_ID_TYPE_MAC)) {
                    log.warn("{} is not mac: {}", CHASSIS_ID_TYPE_MAC, chassisIdType);
                    continue;
                }

                JsonNode remotePortNameNode = info.findValue(PORT_ID);

                if (remotePortNameNode == null) {
                    continue;
                }

                String remoteChassisId = info.get(CHASSIS_ID).asText("");
                String remotePortName = remotePortNameNode.asText("");

                log.debug("{}: {}, {}: {}", CHASSIS_ID, remoteChassisId, PORT_ID, remotePortName);

                Optional<Port> localPort = findLocalPortByName(ports, lldpLocalPort);

                if (!localPort.isPresent()) {
                    log.warn("local port not found. lldpLocalPort value: {}", lldpLocalPort);
                    continue;
                }

                Optional<Device> remoteDevice = findRemoteDeviceByChassisId(deviceService, remoteChassisId);

                if (!remoteDevice.isPresent()) {
                    log.warn("remote device not found. remoteChassisId value: {}", remoteChassisId);
                    continue;
                }

                Optional<Port> remotePort = findDestinationPortByName(
                        remotePortName,
                        deviceService,
                        remoteDevice.get());

                if (!remotePort.isPresent()) {
                    log.warn("remote port not found. remotePortName value: {}", remotePortName);
                    continue;
                }

                if (!localPort.get().isEnabled() || !remotePort.get().isEnabled()) {
                    log.debug("Ports are disabled. Cannot create a link between {}/{} and {}/{}",
                            localDeviceId, localPort.get(), remoteDevice.get().id(), remotePort.get());
                    continue;
                }

                linkDescriptions
                        .addAll(buildLinkPair(localDeviceId, localPort.get(),
                                remoteDevice.get().id(), remotePort.get()));
            }
        }

        log.debug("returning linkDescriptions: {}", linkDescriptions);

        return linkDescriptions;
    }

    private Optional<Port> findLocalPortByName(List<Port> ports, String lldpLocalPort) {
        Optional<Port> localPort = ports.stream()
                .filter(port -> lldpLocalPort.equalsIgnoreCase(port.annotations().value(AnnotationKeys.PORT_NAME)))
                .findAny();

        if (!localPort.isPresent()) {
            localPort = ports.stream()
                    .filter(port -> lldpLocalPort.equalsIgnoreCase(port.annotations().value(AnnotationKeys.NAME)))
                    .findAny();

            if (!localPort.isPresent()) {
                return Optional.empty();
            }
        }

        return localPort;
    }

    private Optional<Device> findRemoteDeviceByChassisId(DeviceService deviceService, String remoteChassisIdString) {
        String forMacTmp = remoteChassisIdString
                .replace(".", "")
                .replaceAll("(.{2})", "$1:")
                .trim()
                .substring(0, 17);
        MacAddress mac = MacAddress.valueOf(forMacTmp);
        Supplier<Stream<Device>> deviceStream = () ->
                StreamSupport.stream(deviceService.getAvailableDevices().spliterator(), false);
        Optional<Device> remoteDeviceOptional = deviceStream.get()
                .filter(device -> device.chassisId() != null
                        && MacAddress.valueOf(device.chassisId().value()).equals(mac))
                .findAny();

        if (remoteDeviceOptional.isPresent()) {
            log.debug("remoteDevice found by chassis id: {}", forMacTmp);
            return remoteDeviceOptional;
        } else {
            remoteDeviceOptional = deviceStream.get().filter(device ->
                Tools.stream(deviceService.getPorts(device.id()))
                        .anyMatch(port -> port.annotations().keys().contains(AnnotationKeys.PORT_MAC)
                                && MacAddress.valueOf(port.annotations().value(AnnotationKeys.PORT_MAC))
                        .equals(mac)))
                    .findAny();
            if (remoteDeviceOptional.isPresent()) {
                log.debug("remoteDevice found by port mac: {}", forMacTmp);
                return remoteDeviceOptional;
            } else {
                return Optional.empty();
            }
        }
    }

    private Optional<Port> findDestinationPortByName(String remotePortName,
                                           DeviceService deviceService,
                                           Device remoteDevice) {
        Optional<Port> remotePort = deviceService.getPorts(remoteDevice.id())
                .stream().filter(port -> remotePortName.equals(port.annotations().value(AnnotationKeys.PORT_NAME)))
                .findAny();

        if (remotePort.isPresent()) {
            return remotePort;
        } else {
            int portNumber = Integer.valueOf(remotePortName.replaceAll("\\D+", ""));
            DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                    .set(AnnotationKeys.PORT_NAME, remotePortName);

            return Optional.of(new DefaultPort(remoteDevice, PortNumber.portNumber(portNumber, remotePortName),
                    true,
                    annotations.build()));
        }
    }

    private static Set<LinkDescription> buildLinkPair(DeviceId localDevId,
                                                      Port localPort,
                                                      DeviceId remoteDevId,
                                                      Port remotePort) {

        Set<LinkDescription> linkDescriptions = Sets.newHashSet();
        ConnectPoint local = new ConnectPoint(localDevId, localPort.number());
        ConnectPoint remote = new ConnectPoint(remoteDevId, remotePort.number());
        DefaultAnnotations annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.LAYER, "ETHERNET")
                .build();

        linkDescriptions.add(new DefaultLinkDescription(
                remote, local, Link.Type.DIRECT, true, annotations));

        return linkDescriptions;
    }
}
