/*
 * Copyright 2016 Open Networking Laboratory
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.openflow.controller.OpenFlowOpticalSwitch;
import org.onosproject.openflow.controller.PortDescPropertyType;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFCircuitPortStatus;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsReply;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsRequest;
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
public class OplinkRoadmHandshaker extends AbstractOpenFlowSwitch implements OpenFlowOpticalSwitch {

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
        OFMessage newMsg = m;

        if (m.getType() == OFType.STATS_REQUEST) {
            OFStatsRequest sr = (OFStatsRequest) m;
            log.debug("OPLK ROADM rebuilding stats request type {}", sr.getStatsType());
            switch (sr.getStatsType()) {
                case PORT:
                    //replace with Oplink experiment stats message to get the port current power
                    OFOplinkPortPowerRequest pRequest = this.factory().buildOplinkPortPowerRequest()
                            .setXid(sr.getXid())
                            .setFlags(sr.getFlags())
                            .build();
                    newMsg = pRequest;
                    break;
                default:
                    break;
            }
        } else {
            log.debug("OPLK ROADM sends msg:{}, as is", m.getType());
        }

        super.sendMsg(newMsg);
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
        }
        return Collections.emptyList();
    }

    private OFOplinkPortPower getPortPower(List<OFOplinkPortPower> portPowers, PortNumber portNum) {
        for (OFOplinkPortPower power : portPowers) {
            if (power.getPort() == portNum.toLong()) {
                return power;
            }
        }
        return null;
    }

    private List<PortDescription> buildPortPowerDescriptions(List<OFOplinkPortPower> portPowers) {
        DeviceService deviceService = this.handler().get(DeviceService.class);
        List<Port> ports = deviceService.getPorts(this.data().deviceId());
        final List<PortDescription> portDescs = new ArrayList<>();
        for (Port port : ports) {
            DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
            builder.putAll(port.annotations());
            OFOplinkPortPower power = getPortPower(portPowers, port.number());
            if (power != null) {
                builder.set(AnnotationKeys.CURRENT_POWER, Long.toString(power.getPowerValue()));
            }
            portDescs.add(new DefaultPortDescription(port.number(), port.isEnabled(),
                    port.type(), port.portSpeed(), builder.build()));
        }
        return portDescs;
    }
}
