/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.driver.optical.handshaker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.onosproject.drivers.optical.OpticalAdjacencyLinkService;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.optical.OpticalAnnotations;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowOpticalSwitch;
import org.onosproject.openflow.controller.PortDescPropertyType;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFCircuitPortStatus;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsReply;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsRequest;
import org.projectfloodlight.openflow.protocol.OFExpExtAdId;
import org.projectfloodlight.openflow.protocol.OFExpPortAdidOtn;
import org.projectfloodlight.openflow.protocol.OFExpPortAdjacency;
import org.projectfloodlight.openflow.protocol.OFExpPortAdjacencyId;
import org.projectfloodlight.openflow.protocol.OFExpPortAdjacencyReply;
import org.projectfloodlight.openflow.protocol.OFExpPortAdjacencyRequest;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFObject;
import org.projectfloodlight.openflow.protocol.OFOplinkPortPower;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortOptical;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFOplinkPortPowerRequest;
import org.projectfloodlight.openflow.protocol.OFOplinkPortPowerReply;


/**
 * Driver for Oplink single WSS 8D ROADM.
 *
 * Driver implements custom handshaker and supports for Optical channel Port based on OpenFlow OTN extension.
 * The device consists of Och ports, and performances wavelength cross-connect among the ports.
 */
public class OplinkRoadm extends AbstractOpenFlowSwitch implements OpenFlowOpticalSwitch {

    private final AtomicBoolean driverHandshakeComplete = new AtomicBoolean(false);
    private List<OFPortOptical> opticalPorts;

    @Override
    public List<? extends OFObject> getPortsOf(PortDescPropertyType type) {
        return ImmutableList.copyOf(opticalPorts);
    }

    @Override
    /**
     * Returns a list of standard (Ethernet) ports.
     *
     * @return List of ports
     */
    public List<OFPortDesc> getPorts() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Set<PortDescPropertyType> getPortTypes() {
        return ImmutableSet.of(PortDescPropertyType.OPTICAL_TRANSPORT);
    }

    @Override
    public Boolean supportNxRole() {
        return false;
    }

    @Override
    public void startDriverHandshake() {
        log.warn("Starting driver handshake for sw {}", getStringId());
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;
        try {
            sendHandshakeOFExperimenterPortDescRequest();
        } catch (IOException e) {
            log.error("OPLK ROADM exception while sending experimenter port desc:", e);
        }
    }

    @Override
    public boolean isDriverHandshakeComplete() {
        return driverHandshakeComplete.get();
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {

        if (!startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeNotStarted();
        }

        if (driverHandshakeComplete.get()) {
            throw new SwitchDriverSubHandshakeCompleted(m);
        }

        switch (m.getType()) {
            case BARRIER_REPLY:
                log.debug("OPLK ROADM Received barrier response");
                break;
            case ERROR:
                log.error("Switch {} Error {}", getStringId(), m);
                break;
            case FEATURES_REPLY:
                break;
            case FLOW_REMOVED:
                break;
            case GET_ASYNC_REPLY:
                break;
            case PACKET_IN:
                break;
            case PORT_STATUS:
                processOFPortStatus((OFCircuitPortStatus) m);
                break;
            case QUEUE_GET_CONFIG_REPLY:
                break;
            case ROLE_REPLY:
                break;
            case STATS_REPLY:
                OFStatsReply stats = (OFStatsReply) m;
                if (stats.getStatsType() == OFStatsType.EXPERIMENTER) {
                    log.warn("OPLK ROADM : Received multipart (port desc) reply message {}", m);
                    //OTN Optical extension 1.0 port-desc
                    createOpticalPortList((OFCircuitPortsReply) m);
                    driverHandshakeComplete.set(true);
                }
                break;
            default:
                log.warn("Received message {} during switch-driver " +
                                 "subhandshake " + "from switch {} ... " +
                                 "Ignoring message", m,
                         getStringId());

        }
    }

    private void processOFPortStatus(OFCircuitPortStatus ps) {
        log.debug("OPLK ROADM ..OF Port Status :", ps);
    }

    @Override
    public Device.Type deviceType() {
        return Device.Type.ROADM;
    }

    @Override
    public final void sendMsg(OFMessage m) {
        List<OFMessage> messages = new ArrayList<>();
        messages.add(m);

        if (m.getType() == OFType.STATS_REQUEST) {
            OFStatsRequest sr = (OFStatsRequest) m;
            log.debug("OPLK ROADM rebuilding stats request type {}", sr.getStatsType());
            switch (sr.getStatsType()) {
                case PORT:
                    //add Oplink experiment stats message to get the port's current power
                    OFOplinkPortPowerRequest powerRequest = this.factory().buildOplinkPortPowerRequest()
                            .setXid(sr.getXid())
                            .setFlags(sr.getFlags())
                            .build();
                    messages.add(powerRequest);
                    // add experiment message to get adjacent ports
                    OFExpPortAdjacencyRequest adjacencyRequest = this.factory().buildExpPortAdjacencyRequest()
                            .setXid(sr.getXid())
                            .setFlags(sr.getFlags())
                            .build();
                    messages.add(adjacencyRequest);
                    break;
                default:
                    break;
            }
        } else {
            log.debug("OPLK ROADM sends msg:{}, as is", m.getType());
        }

        for (OFMessage message : messages) {
            super.sendMsg(message);
        }
    }

    private void sendHandshakeOFExperimenterPortDescRequest() throws IOException {
        // send multi part message for port description for optical switches
        OFCircuitPortsRequest circuitPortsRequest = factory()
                .buildCircuitPortsRequest().setXid(getNextTransactionId())
                .build();
        log.info("OPLK ROADM : Sending experimented circuit port stats " +
                 "message " +
                 "{}",
                 circuitPortsRequest);
        this.sendHandshakeMessage(circuitPortsRequest);
    }

    /**
     * Builds list of OFPortOptical ports based on the multi-part circuit ports reply.
     * Ensure the optical transport port's signal type is configured correctly.
     *
     * @param wPorts OF reply with circuit ports
     */
    private void createOpticalPortList(OFCircuitPortsReply wPorts) {
        opticalPorts = new ArrayList<>();
        opticalPorts.addAll(wPorts.getEntries());
    }

    @Override
    public List<PortDescription> processExpPortStats(OFMessage msg) {
        if (msg instanceof OFOplinkPortPowerReply) {
            return buildPortPowerDescriptions(((OFOplinkPortPowerReply) msg).getEntries());
        } else if (msg instanceof OFExpPortAdjacencyReply) {
            return buildPortAdjacencyDescriptions(((OFExpPortAdjacencyReply) msg).getEntries());
        }
        return Collections.emptyList();
    }

    private List<PortDescription> buildPortPowerDescriptions(List<OFOplinkPortPower> portPowers) {
        DeviceService deviceService = this.handler().get(DeviceService.class);
        List<Port> ports = deviceService.getPorts(this.data().deviceId());
        HashMap<Long, OFOplinkPortPower> powerMap = new HashMap<>(portPowers.size());
        portPowers.forEach(power -> powerMap.put((long) power.getPort(), power));
        final List<PortDescription> portDescs = new ArrayList<>();
        for (Port port : ports) {
            DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
            builder.putAll(port.annotations());
            OFOplinkPortPower power = powerMap.get(port.number().toLong());
            if (power != null) {
                builder.set(OpticalAnnotations.CURRENT_POWER, Long.toString(power.getPowerValue()));
            }
            portDescs.add(new DefaultPortDescription(port.number(), port.isEnabled(),
                    port.type(), port.portSpeed(), builder.build()));
        }
        return portDescs;
    }

    private OplinkPortAdjacency getNeighbor(OFExpPortAdjacency ad) {
        for (OFExpPortAdjacencyId adid : ad.getProperties()) {
            List<OFExpExtAdId> otns = adid.getAdId();
            if (otns != null && otns.size() > 0) {
                OFExpPortAdidOtn otn = (OFExpPortAdidOtn) otns.get(0);
                // ITU-T G.7714 ETH MAC Format (in second 16 bytes of the following)
                // |---------------------------------------------------------------------------|
                // | Other format (16 bytes)                                                   |
                // |---------------------------------------------------------------------------|
                // | Header (2 bytes) | ID (4 bits) | MAC (6 bytes) | Port (4 bytes) | Unused  |
                // |---------------------------------------------------------------------------|
                ChannelBuffer buffer = ChannelBuffers.buffer(32);
                otn.getOpspec().write32Bytes(buffer);
                long mac = buffer.getLong(18) << 4 >>> 16;
                int port = (int) (buffer.getLong(24) << 4 >>> 32);
                // Oplink does not use the 4 most significant bytes of Dpid so Dpid can be
                // constructed from MAC address
                return new OplinkPortAdjacency(DeviceId.deviceId(Dpid.uri(new Dpid(mac))),
                        PortNumber.portNumber(port));
            }
        }
        return null;
    }

    private List<PortDescription> buildPortAdjacencyDescriptions(List<OFExpPortAdjacency> portAds) {
        DeviceService deviceService = this.handler().get(DeviceService.class);
        List<Port> ports = deviceService.getPorts(this.data().deviceId());

        // Map port's number with port's adjacency
        HashMap<Long, OFExpPortAdjacency> adMap = new HashMap<>(portAds.size());
        portAds.forEach(ad -> adMap.put((long) ad.getPortNo().getPortNumber(), ad));

        List<PortDescription> portDescs = new ArrayList<>();
        for (Port port : ports) {
            DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
            Annotations oldAnnotations = port.annotations();
            builder.putAll(oldAnnotations);
            OFExpPortAdjacency ad = adMap.get(port.number().toLong());
            if (ad != null) {
                // neighbor discovered, add to port descriptions
                OplinkPortAdjacency neighbor = getNeighbor(ad);
                String newId = neighbor.getDeviceId().toString();
                String newPort = neighbor.getPort().toString();
                // Check if annotation already exists
                if (!newId.equals(oldAnnotations.value(OpticalAnnotations.NEIGHBOR_ID)) ||
                    !newPort.equals(oldAnnotations.value(OpticalAnnotations.NEIGHBOR_PORT))) {
                    builder.set(OpticalAnnotations.NEIGHBOR_ID, newId);
                    builder.set(OpticalAnnotations.NEIGHBOR_PORT, newPort);
                }
                addLink(port.number(), neighbor);
            } else {
                // no neighbors found
                builder.remove(OpticalAnnotations.NEIGHBOR_ID);
                builder.remove(OpticalAnnotations.NEIGHBOR_PORT);
                removeLink(port.number());
            }
            portDescs.add(new DefaultPortDescription(port.number(), port.isEnabled(),
                    port.type(), port.portSpeed(), builder.build()));
        }
        return portDescs;
    }

    private void addLink(PortNumber portNumber, OplinkPortAdjacency neighbor) {
        ConnectPoint dst = new ConnectPoint(handler().data().deviceId(), portNumber);
        ConnectPoint src = new ConnectPoint(neighbor.getDeviceId(), neighbor.getPort());
        OpticalAdjacencyLinkService adService =
                this.handler().get(OpticalAdjacencyLinkService.class);
        adService.linkDetected(new DefaultLinkDescription(src, dst, Link.Type.OPTICAL));
    }

    // Remove incoming link with port if there are any.
    private void removeLink(PortNumber portNumber) {
        ConnectPoint dst = new ConnectPoint(handler().data().deviceId(), portNumber);
        // Check so only incoming links are removed
        Set<Link> links = this.handler().get(LinkService.class).getIngressLinks(dst);
        if (!links.isEmpty()) {
            OpticalAdjacencyLinkService adService =
                    this.handler().get(OpticalAdjacencyLinkService.class);
            adService.linksVanished(dst);
        }
    }

    private class OplinkPortAdjacency {
        private DeviceId deviceId;
        private PortNumber portNumber;

        public OplinkPortAdjacency(DeviceId deviceId, PortNumber portNumber) {
            this.deviceId = deviceId;
            this.portNumber = portNumber;
        }

        public DeviceId getDeviceId() {
            return deviceId;
        }

        public PortNumber getPort() {
            return portNumber;
        }
    }
}
