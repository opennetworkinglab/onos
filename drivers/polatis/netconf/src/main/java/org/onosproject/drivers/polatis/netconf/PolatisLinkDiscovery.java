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

package org.onosproject.drivers.polatis.netconf;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Port;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.behaviour.LinkDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.util.Set;
import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import static org.onosproject.drivers.polatis.netconf.PolatisUtility.*;

import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.netconfGet;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORTPEER;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORTDIR;


/**
 * Reads peer-port fields from a Polatis switch, parses them and returns a set of LinkDescriptions.
 */
public class PolatisLinkDiscovery extends AbstractHandlerBehaviour implements LinkDiscovery {

    private final Logger log = getLogger(getClass());

    private static final String JSON_PEERPORT_PEER_KEY = "peer";
    private static final String JSON_PEERPORT_PEER_DEVICEID_KEY = "id";
    private static final String JSON_PEERPORT_PEER_PORTID_KEY = "port";
    private static final String SCHEME_NAME = "linkdiscovery";

    /**
     * Constructor just used to initiate logging when LinkDiscovery behaviour is invoked.
     */
    public PolatisLinkDiscovery() {
        log.debug("Running PolatisLinkDiscovery handler");
    }

    /**
     * Returns the set of LinkDescriptions originating from a Polatis switch.
     * <p>
     * This is the callback required by the LinkDiscovery behaviour.
     * @return Set of outbound unidirectional links as LinkDescriptions
     */
    @Override
    public Set<LinkDescription> getLinks() {
        Set<LinkDescription> links = new HashSet<>();
        DeviceId deviceID = handler().data().deviceId();
        log.debug("*** Checking peer-port fields on device {}", deviceID.toString());
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        if (controller == null || controller.getDevicesMap() == null
                || controller.getDevicesMap().get(deviceID) == null) {
            log.warn("NETCONF session to device {} not yet established, will try again...", deviceID);
            return links;
        }
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        Device device = deviceService.getDevice(deviceID);
        int numInputPorts = Integer.parseInt(device.annotations().value(KEY_INPUTPORTS));
        int numOutputPorts = Integer.parseInt(device.annotations().value(KEY_OUTPUTPORTS));
        log.trace("Talking to device " + handler().data().deviceId().toString());
        String reply = netconfGet(handler(), getPortsFilter());
        // Get port details from switch as PortDescription objects
        List<PortDescription> ports = parsePorts(reply, numInputPorts, numOutputPorts);
        int numPeerPortEntries = 0;
        int numPortsScanned = 0;
        ObjectMapper mapper = new ObjectMapper();
        for (PortDescription port : ports) {
            numPortsScanned++;
            if (deviceService.getPort(new ConnectPoint(deviceID, port.portNumber())).isEnabled()) {
                String peerPortData = port.annotations().value(KEY_PORTPEER);
                if (!peerPortData.equals("")) {
                    numPeerPortEntries++;
                    if (peerPortData.charAt(0) == '{') {
                        ConnectPoint nearEndCP = new ConnectPoint(deviceID, port.portNumber());
                        ConnectPoint farEndCP = null;
                        try {
                            farEndCP = parsePeerportDataForCP(mapper.readTree(peerPortData));
                        } catch (JsonProcessingException jpe) {
                            log.debug("Error processing peer-port JSON: {}", jpe.toString());
                        }
                        if (farEndCP != null) {
                            log.trace("Found ref on port {} to peer ConnectPoint: {}", port.portNumber(),
                                      farEndCP.toString());
                            if (checkPeer(nearEndCP, farEndCP, this.handler(), true)) {
                                log.trace("Peer {} checks out", farEndCP.toString());
                                // now add link to Set<LinkDescription>
                                DefaultAnnotations annotations = DefaultAnnotations.builder()
                                                                 .set(KEY_LINKBIDIR, VALUE_FALSE)
                                                                 .set(KEY_LINKALLOWED, VALUE_TRUE)
                                                                 .build();
                                ConnectPoint aEndCP = nearEndCP;
                                ConnectPoint bEndCP = farEndCP;
                                // reverse direction of unidirectional link if near-end port is INPUT
                                if (port.annotations().value(KEY_PORTDIR).equals(VALUE_INPUT)) {
                                    aEndCP = farEndCP;
                                    bEndCP = nearEndCP;
                                }
                                LinkDescription newLinkDesc = new DefaultLinkDescription(aEndCP, bEndCP,
                                                                  Link.Type.OPTICAL, true, annotations);
                                links.add(newLinkDesc);
                                log.debug("Adding link {}", newLinkDesc);
                            }
                        }
                    }
                }
            }
        }
        log.debug("Scanned {} ports, {} had peer-port entries, {} {} valid", numPortsScanned, numPeerPortEntries,
                  links.size(), links.size() == 1 ? "is" : "are");
        log.trace("Links found on this iteration: {}", links);
        return links;
    }

    private ConnectPoint parsePeerportDataForCP(JsonNode peerData) {
    DeviceId idTo = DeviceId.NONE;
    PortNumber portNumTo = PortNumber.portNumber(0L);
        JsonNode peer = peerData.get(JSON_PEERPORT_PEER_KEY);
        if (peer != null) {
            log.trace("Found peer element: {}", peer.toString());
            JsonNode devIDNode = peer.get(JSON_PEERPORT_PEER_DEVICEID_KEY);
            if (devIDNode != null) {
                String devID = peer.get(JSON_PEERPORT_PEER_DEVICEID_KEY).asText();
                log.trace("Found devID: {}", devID);
                idTo = DeviceId.deviceId(devID);
                JsonNode portNode = peer.get(JSON_PEERPORT_PEER_PORTID_KEY);
                if (portNode != null) {
                    portNumTo = PortNumber.portNumber(portNode.asInt());
                        if (portNumTo.toLong() != 0) {
                            log.trace("Found legal peer JSON element: {}={}, {}={}", JSON_PEERPORT_PEER_DEVICEID_KEY,
                                      idTo.toString(), JSON_PEERPORT_PEER_PORTID_KEY, portNumTo.toString());
                        } else {
                            log.trace("Malformed peer-port JSON: non-numerical or zero port value");
                        }
                    } else {
                        log.trace("Malformed peer-port JSON: unable to find \"{}\" key in {}",
                                  JSON_PEERPORT_PEER_PORTID_KEY, peer.toString());
                    }
                } else {
                    log.trace("Malformed peer-port JSON: unable to find \"{}\" key in {}",
                              JSON_PEERPORT_PEER_DEVICEID_KEY, peer.toString());
                }
            } else {
                log.trace("Malformed peer-port JSON: unable to find \"{}\" key in {}", JSON_PEERPORT_PEER_KEY,
                          peerData.toString());
            }
        return (idTo.equals(DeviceId.NONE) || portNumTo.toLong() == 0) ? null : new ConnectPoint(idTo, portNumTo);
    }

    private boolean checkPeer(ConnectPoint nearEndCP, ConnectPoint peerCP, DriverHandler handler, boolean direct) {
        // check peerCP exists and is available (either via device service or direct from device)
        DeviceId peerDeviceID = peerCP.deviceId();
        boolean result = false;
        DeviceService deviceService = checkNotNull(handler.get(DeviceService.class));
        if (deviceService.isAvailable(peerDeviceID)) {
            log.trace("Peer device {} exists", peerDeviceID.toString());
            Device device = deviceService.getDevice(peerDeviceID);
            int numInputPorts = Integer.parseInt(device.annotations().value(KEY_INPUTPORTS));
            int numOutputPorts = Integer.parseInt(device.annotations().value(KEY_OUTPUTPORTS));
            List<Port> ports = deviceService.getPorts(peerDeviceID);
            PortNumber farEndPortNum = peerCP.port();
            Port port = deviceService.getPort(peerCP);
            if (port != null) {
                if (port.isEnabled()) {
                    log.trace("Peer port {} exists", port.number().toLong());
                    // check far end peer-port entry (use device service or retrieve direct from switch)
                    Port peerPort = deviceService.getPort(peerDeviceID, farEndPortNum);
                    String farEndPortPeerportData = peerPort.annotations().value(KEY_PORTPEER);
                    if (direct) {
                        log.trace("Checking device {} DIRECT", handler.data().deviceId());
                        //A bit of a cludge it seems but temporarily open a new NETCONF session to far-end device
                        NetconfController controller = checkNotNull(handler.get(NetconfController.class));
                        NetconfSession farEndDeviceSession = controller.getDevicesMap().get(peerDeviceID).getSession();
                        String reply = netconfGet(farEndDeviceSession, getPortFilter(farEndPortNum));
                        PortDescription peerPortDescDirect = parsePorts(reply, numInputPorts, numOutputPorts).get(0);
                        log.trace("peerPortDesc from device: " + peerPortDescDirect.toString());
                        String farEndPortPeerportDataDirect = peerPortDescDirect.annotations().value(KEY_PORTPEER);
                        farEndPortPeerportData = farEndPortPeerportDataDirect;
                    }
                    if (!farEndPortPeerportData.equals("")) {
                        if (farEndPortPeerportData.charAt(0) == '{') {
                            log.trace("Far-end peer-port value:" + farEndPortPeerportData);
                            ObjectMapper mapper = new ObjectMapper();
                            ConnectPoint checkNearEndCP = null;
                            try {
                                checkNearEndCP = parsePeerportDataForCP(mapper.readTree(farEndPortPeerportData));
                            } catch (JsonProcessingException jpe) {
                                log.trace("Error processing peer-port JSON: {}", jpe.toString());
                            }
                            if (nearEndCP.equals(checkNearEndCP)) {
                                log.trace("Reciprocal peer port entries match: nearEnd={}, farEnd={}", nearEndCP,
                                          checkNearEndCP);
                                result = true;
                            } else {
                                log.trace("Peer-port entry for far-end port ({}) does not match near-end " +
                                          "port number ({})", checkNearEndCP, nearEndCP);
                            }
                        }
                    } else {
                        log.trace("Null peer-port entry for far-end port ({})", peerCP);
                    }
                } else {
                    log.trace("Peer port {} is DISABLED", port);
                }
            } else {
                log.trace("Peer port {} does not exist", port);
            }
        } else {
            log.trace("Far end device does not exist or is not available");
        }
        return result;
    }
}
