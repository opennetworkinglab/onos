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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.packet.ChassisId;
import org.onlab.packet.MacAddress;
import org.onlab.util.Tools;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
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
import org.onosproject.net.provider.ProviderId;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Discovers the links from a Cisco Nexus Switch REST device.
 */
public class LinkDiscoveryCiscoImpl extends AbstractHandlerBehaviour implements LinkDiscovery {

    private static final String SHOW_LLDP_NEIGHBOR_DETAIL_CMD = "show lldp neighbor detail";
    private static final String UNKNOWN = "unknown";
    private static final String JSON_RESULT = "result";
    private static final String TABLE_NBOR_DETAIL = "TABLE_nbor_detail";
    private static final String ROW_NBOR_DETAIL = "ROW_nbor_detail";
    private static final String CHASSIS_ID = "chassis_id";
    private static final String PORT_ID = "port_id";
    private static final String PORT_DESC = "port_desc";
    private static final String SYS_NAME = "sys_name";
    private static final String LOCAL_PORT_ID = "l_port_id";
    private static final String LLDP = "lldp:";

    private final Logger log = getLogger(getClass());

    @Override
    public Set<LinkDescription> getLinks() {
        String response = retrieveResponse(SHOW_LLDP_NEIGHBOR_DETAIL_CMD);
        DeviceId localDeviceId = this.handler().data().deviceId();
        DeviceService deviceService = this.handler().get(DeviceService.class);
        Set<LinkDescription> linkDescriptions = Sets.newHashSet();
        List<Port> ports = deviceService.getPorts(localDeviceId);

        if (ports.size() == 0 || Objects.isNull(response)) {
            return linkDescriptions;
        }
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode json = om.readTree(response);
            if (json == null) {
                return linkDescriptions;
            }

            JsonNode res = json.at("/" + JSON_RESULT);
            if (res.isMissingNode()) {
                return linkDescriptions;
            }

            JsonNode lldpNeighborsRow = res.at("/" + TABLE_NBOR_DETAIL);
            if (lldpNeighborsRow.isMissingNode()) {
                return linkDescriptions;
            }

            JsonNode lldpNeighbors = lldpNeighborsRow.at("/" + ROW_NBOR_DETAIL);
            if (lldpNeighbors.isMissingNode()) {
                return linkDescriptions;
            }

            Iterator<JsonNode> iterator = lldpNeighbors.elements();

            while (iterator.hasNext()) {
                JsonNode neighbors = iterator.next();
                String remoteChassisId = neighbors.get(CHASSIS_ID).asText();
                String remotePortName = neighbors.get(PORT_ID).asText();
                String remotePortDesc = neighbors.get(PORT_DESC).asText();
                String lldpLocalPort = neighbors.get(LOCAL_PORT_ID).asText()
                        .replaceAll("(Eth.{0,5})(.\\d{0,5}/\\d{0,5})", "Ethernet$2");

                Port localPort = findLocalPortByName(ports, lldpLocalPort);
                if (localPort == null) {
                    log.warn("local port not found. LldpLocalPort value: {}", lldpLocalPort);
                    continue;
                }

                Device remoteDevice = findRemoteDeviceByChassisId(deviceService, remoteChassisId);
                Port remotePort = findDestinationPortByName(remotePortName,
                                                            remotePortDesc,
                                                            deviceService,
                                                            remoteDevice);

                if (!localPort.isEnabled() || !remotePort.isEnabled()) {
                    log.debug("Ports are disabled. Cannot create a link between {}/{} and {}/{}",
                              localDeviceId, localPort, remoteDevice.id(), remotePort);
                    continue;
                }

                linkDescriptions.addAll(buildLinkPair(localDeviceId, localPort, remoteDevice.id(), remotePort));
            }
        } catch (IOException e) {
            log.error("Failed to get links ", e);
        }

        log.debug("Returning linkDescriptions: {}", linkDescriptions);
        return linkDescriptions;

    }

    private Port findLocalPortByName(List<Port> ports, String lldpLocalPort) {
        Optional<Port> localPort = ports.stream()
                .filter(port -> lldpLocalPort.equalsIgnoreCase(port.annotations().value(PORT_NAME))).findAny();
        if (!localPort.isPresent()) {
            return null;
        }
        return localPort.get();
    }

    private Device findRemoteDeviceByChassisId(DeviceService deviceService, String remoteChassisIdString) {
        String forMacTmp = remoteChassisIdString.replace(".", "").replaceAll("(.{2})", "$1:").trim().substring(0, 17);
        MacAddress mac = MacAddress.valueOf(forMacTmp);
        ChassisId remoteChassisId = new ChassisId(mac.toLong());
        Optional<Device> remoteDeviceOptional;
        Supplier<Stream<Device>> deviceStream = () ->
                StreamSupport.stream(deviceService.getAvailableDevices().spliterator(), false);
        remoteDeviceOptional = deviceStream.get()
                .filter(device -> device.chassisId() != null
                        && MacAddress.valueOf(device.chassisId().value()).equals(mac))
                .findAny();

        if (remoteDeviceOptional.isPresent()) {
            return remoteDeviceOptional.get();
        } else {
            remoteDeviceOptional = deviceStream.get().filter(device ->
                Tools.stream(deviceService.getPorts(device.id())).anyMatch(port ->
                    port.annotations().keys().contains(AnnotationKeys.PORT_MAC)
                            && MacAddress.valueOf(port.annotations().value(AnnotationKeys.PORT_MAC))
                            .equals(mac))).findAny();
            if (remoteDeviceOptional.isPresent()) {
                return remoteDeviceOptional.get();
            } else {
                log.debug("remote device not found. remoteChassisId value: {}", remoteChassisId);
                return new DefaultDevice(ProviderId.NONE,
                                         DeviceId.deviceId(LLDP + mac.toString()),
                                         Device.Type.SWITCH,
                                         UNKNOWN,
                                         UNKNOWN,
                                         UNKNOWN,
                                         UNKNOWN,
                                         remoteChassisId,
                                         DefaultAnnotations.EMPTY);
            }
        }
    }

    private Port findDestinationPortByName(String remotePortName,
                                           String remotePortDesc,
                                           DeviceService deviceService,
                                           Device remoteDevice) {
        Optional<Port> remotePort = deviceService.getPorts(remoteDevice.id())
                .stream().filter(port -> remotePortName.equals(port.annotations().value(PORT_NAME))).findAny();
        if (remotePort.isPresent()) {
            return remotePort.get();
        } else {
            Optional<Port> remotePortByDesc = deviceService.getPorts(remoteDevice.id())
                    .stream().filter(port -> remotePortDesc.equals(port.annotations().value(PORT_NAME))).findAny();
            if (remotePortByDesc.isPresent()) {
                return remotePortByDesc.get();
            } else {
                int portNumber = Integer.valueOf(remotePortName.replaceAll("\\D+", ""));
                DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                        .set(AnnotationKeys.PORT_NAME, remotePortName);
                return new DefaultPort(remoteDevice, PortNumber.portNumber(portNumber),
                                       true,
                                       annotations.build());
            }
        }
    }

    private String retrieveResponse(String command) {
        DriverHandler handler = handler();
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = handler.data().deviceId();

        String req = NxApiRequest.generate(Lists.newArrayList(command), NxApiRequest.CommandType.CLI);
        log.debug("request :" + req);

        InputStream stream = new ByteArrayInputStream(req.getBytes(StandardCharsets.UTF_8));
        return controller.post(deviceId, "/ins", stream, MediaType.valueOf("application/json-rpc"), String.class);
    }

    private static Set<LinkDescription> buildLinkPair(DeviceId localDevId,
                                                      Port localPort,
                                                      DeviceId remoteDevId,
                                                      Port remotePort) {

        Set<LinkDescription> linkDescriptions = Sets.newHashSet();
        ConnectPoint local = new ConnectPoint(localDevId, localPort.number());
        ConnectPoint remote = new ConnectPoint(remoteDevId, remotePort.number());
        DefaultAnnotations annotations = DefaultAnnotations.builder()
                .set("layer", "ETHERNET")
                .build();
        linkDescriptions.add(new DefaultLinkDescription(
                remote, local, Link.Type.DIRECT, true, annotations));

        return linkDescriptions;
    }
}
