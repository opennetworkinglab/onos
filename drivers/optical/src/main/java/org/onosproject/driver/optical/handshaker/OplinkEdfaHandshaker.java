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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.onosproject.net.Device;
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
import org.projectfloodlight.openflow.protocol.OFExpPortAdjacencyReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFObject;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortOptical;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFOplinkPortPowerReply;

import static org.onosproject.net.Device.Type;

/**
 * Oplink open flow EDFA handshaker - for Open Flow 1.3.
 * Driver for Oplink EDFA openflow device.
 * Driver implements custom handshaker and supports Optical Port based on OpenFlow OTN extension.
 */
public class OplinkEdfaHandshaker extends AbstractOpenFlowSwitch implements OpenFlowOpticalSwitch {

    private final AtomicBoolean driverHandshakeComplete = new AtomicBoolean(false);
    private List<OFPortOptical> opticalPorts = new ArrayList<>();
    private OplinkHandshakerUtil oplinkUtil = new OplinkHandshakerUtil(this);

    @Override
    public List<? extends OFObject> getPortsOf(PortDescPropertyType type) {
        // Expected type is OPTICAL_TRANSPORT
        if (type == PortDescPropertyType.OPTICAL_TRANSPORT) {
            return ImmutableList.copyOf(opticalPorts);
        }
        // Any other type, return empty
        log.warn("Unexpected port description property type: {}", type);
        return ImmutableList.of();
    }

    /**
     * Returns a list of standard (Ethernet) ports.
     *
     * @return List of ports
     */
    @Override
    public List<OFPortDesc> getPorts() {
        return ImmutableList.of();
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
        log.info("Starting driver handshake for sw {}", getStringId());
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;
        try {
            sendHandshakeOFExperimenterPortDescRequest();
        } catch (IOException e) {
            log.error("OPLK EDFA exception while sending experimenter port desc:", e);
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
                log.debug("OPLK EDFA Received barrier response");
                break;
            case ERROR:
                log.error("Switch {} Error {}", getStringId(), m);
                break;
            case PORT_STATUS:
                processOFPortStatus((OFCircuitPortStatus) m);
                break;
            case STATS_REPLY:
                OFStatsReply stats = (OFStatsReply) m;
                if (stats.getStatsType() == OFStatsType.EXPERIMENTER) {
                    log.debug("OPLK EDFA : Received multipart (port desc) reply message {}", m);
                    //OTN Optical extension 1.0 port-desc
                    createOpticalPortList((OFCircuitPortsReply) m);
                    driverHandshakeComplete.set(true);
                }
                break;
            default:
                log.warn("Received message {} during switch-driver " +
                                 "subhandshake from switch {} ... " +
                                 "Ignoring message", m, getStringId());
        }
    }

    @Override
    public Device.Type deviceType() {
        return Type.OPTICAL_AMPLIFIER;
    }

    @Override
    public final void sendMsg(OFMessage m) {
        List<OFMessage> messages = new ArrayList<>();
        messages.add(m);

        if (m.getType() == OFType.STATS_REQUEST) {
            OFStatsRequest sr = (OFStatsRequest) m;
            log.debug("OPLK EDFA rebuilding stats request type {}", sr.getStatsType());
            switch (sr.getStatsType()) {
                case PORT:
                    // add Oplink experiment message to get the port's current power
                    messages.add(oplinkUtil.buildPortPowerRequest());
                    // add experiment message to get adjacent ports
                    messages.add(oplinkUtil.buildPortAdjacencyRequest());
                    break;
                default:
                    break;
            }
        } else {
            log.debug("OPLK EDFA sends msg:{}, as is", m.getType());
        }

        super.sendMsg(messages);
    }

    @Override
    public List<PortDescription> processExpPortStats(OFMessage msg) {
        if (msg instanceof OFOplinkPortPowerReply) {
            return oplinkUtil.buildPortPowerDescriptions(((OFOplinkPortPowerReply) msg).getEntries());
        } else if (msg instanceof OFExpPortAdjacencyReply) {
            return oplinkUtil.buildPortAdjacencyDescriptions(((OFExpPortAdjacencyReply) msg).getEntries());
        }
        return Collections.emptyList();
    }

    private void processOFPortStatus(OFCircuitPortStatus ps) {
        log.debug("OPLK EDFA ..OF Port Status :", ps);
    }

    private void sendHandshakeOFExperimenterPortDescRequest() throws IOException {
        // Send multipart message for port description for optical switches
        OFCircuitPortsRequest circuitPortsRequest = oplinkUtil.buildCircuitPortsRequest();
        log.debug("OPLK EDFA : Sending experimented port description message {}", circuitPortsRequest);
        sendHandshakeMessage(circuitPortsRequest);
    }

    /**
     * Builds list of OFPortOptical ports based on the multi-part circuit ports reply.
     * Ensure the optical transport port's signal type is configured correctly.
     *
     * @param wPorts OF reply with circuit ports
     */
    private void createOpticalPortList(OFCircuitPortsReply wPorts) {
        opticalPorts.addAll(wPorts.getEntries());
    }
}
