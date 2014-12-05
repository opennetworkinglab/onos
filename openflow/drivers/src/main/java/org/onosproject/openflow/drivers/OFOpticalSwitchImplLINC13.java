/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.openflow.drivers;

import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFCircuitPortStatus;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsReply;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsRequest;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortOptical;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LINC-OE Optical Emulator switch class.
 */
public class OFOpticalSwitchImplLINC13 extends AbstractOpenFlowSwitch {

    private final AtomicBoolean driverHandshakeComplete;
    private long barrierXidToWaitFor = -1;

    private OFPortDescStatsReply wPorts;

    OFOpticalSwitchImplLINC13(Dpid dpid, OFDescStatsReply desc) {
        super(dpid);
        driverHandshakeComplete = new AtomicBoolean(false);
        setSwitchDescription(desc);
    }

    @Override
    public String toString() {
        return "OFOpticalSwitchImplLINC13 [" + ((channel != null)
                ? channel.getRemoteAddress() : "?")
                + " DPID[" + ((getStringId() != null) ? getStringId() : "?") + "]]";
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
            log.error("LINC-OE exception while sending experimenter port desc:",
                     e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean isDriverHandshakeComplete() {
        if (!startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeNotStarted();
        }
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
                if (m.getXid() == barrierXidToWaitFor) {
                    log.debug("LINC-OE Received barrier response");
                }
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
                log.warn("****LINC-OE Port Status {} {}", getStringId(), m);
                processOFPortStatus((OFCircuitPortStatus) m);
                break;
            case QUEUE_GET_CONFIG_REPLY:
                break;
            case ROLE_REPLY:
                break;
            case STATS_REPLY:
                OFStatsReply stats = (OFStatsReply) m;
                if (stats.getStatsType() == OFStatsType.EXPERIMENTER) {
                    log.warn("LINC-OE : Received stats reply message {}", m);
                    processHandshakeOFExperimenterPortDescRequest(
                            (OFCircuitPortsReply) m);
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

    public void processOFPortStatus(OFCircuitPortStatus ps) {
        log.debug("LINC-OE ..OF Port Status :", ps);

    }

    private void processHandshakeOFExperimenterPortDescRequest(
            OFCircuitPortsReply sr) {
        Collection<OFPortOptical> entries = sr.getEntries();
        List<OFPortDesc> ofPortDescList = new ArrayList<>(entries.size());
        for (OFPortOptical entry : entries) {
            log.warn("LINC:OE port message {}", entry.toString());
            ofPortDescList.add(factory().buildPortDesc().
                    setPortNo(entry.getPortNo())
                                           .setConfig(entry.getConfig())
                                           .setState(entry.getState())
                                           .setHwAddr(entry.getHwAddr())
                                           .setName(entry.getName())
                                           .build());

        }
        setExperimenterPortDescReply(factory().buildPortDescStatsReply().
                setEntries(ofPortDescList).build());
    }

    private void setExperimenterPortDescReply(OFPortDescStatsReply reply) {
        wPorts = reply;
    }


    private void sendHandshakeOFExperimenterPortDescRequest() throws
            IOException {
        // send multi part message for port description for optical switches
        OFCircuitPortsRequest circuitPortsRequest = factory()
                .buildCircuitPortsRequest().setXid(getNextTransactionId())
                .build();
        log.warn("LINC-OE : Sending experimented circuit port stats " +
                         "message " +
                         "{}",
                 circuitPortsRequest.toString());
        this.write(Collections.<OFMessage>singletonList(circuitPortsRequest));
    }

    @Override
    public List<OFPortDesc> getPorts() {
        List<OFPortDesc> portEntries = new ArrayList<>();
        portEntries.addAll(ports.getEntries());
        if (wPorts != null) {
            portEntries.addAll(wPorts.getEntries());
        }
        return Collections.unmodifiableList(portEntries);
    }

    @Override
    public void write(OFMessage msg) {
        this.channel.write(Collections.singletonList(msg));
    }

    @Override
    public void write(List<OFMessage> msgs) {
        this.channel.write(msgs);
    }

    @Override
    public Boolean supportNxRole() {
        return false;
    }

    @Override
    public boolean isOptical() {
        return true;
    }


}
