/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.driver.handshaker;

import org.onosproject.net.Device;
import com.google.common.collect.ImmutableSet;
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
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescPropOpticalTransport;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortOptical;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.types.OFPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LINC-OE Optical Emulator switch class.
 *
 * The LINC ROADM emulator exposes two types of ports: OCh ports connect to ports in the packet layer,
 * while OMS ports connect to an OMS port on a neighbouring ROADM.
 *
 * LINC sends the tap ports (OCh for our purposes) in the regular port desc stats reply,
 * while it sends *all* ports (both tap and WDM ports, i.e., OCh and OMS) in the experimenter port desc stats reply.
 *
 */
public class OFOpticalSwitchImplLINC13
 extends AbstractOpenFlowSwitch implements OpenFlowOpticalSwitch {

    private final AtomicBoolean driverHandshakeComplete = new AtomicBoolean(false);
    private long barrierXidToWaitFor = -1;

    private List<OFPortOptical> opticalPorts;

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

    public void processOFPortStatus(OFCircuitPortStatus ps) {
        log.debug("LINC-OE ..OF Port Status :", ps);
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
        this.sendHandshakeMessage(circuitPortsRequest);
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
    public Boolean supportNxRole() {
        return false;
    }

    @Override
    public Device.Type deviceType() {
        return Device.Type.ROADM;
    }

    /**
     * Checks if given port is also part of the regular port desc stats, i.e., is the port a tap port.
     *
     * @param port given OF port
     * @return true if the port is a tap (OCh), false otherwise (OMS port)
     */
    private boolean hasPort(OFPort port) {
        for (OFPortDescStatsReply reply : this.ports) {
            for (OFPortDesc p : reply.getEntries()) {
                if (p.getPortNo().equals(port)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Creates an OpenFlow optical port based on the given port and transport type.
     *
     * @param port OpenFlow optical port
     * @param type transport type
     * @return OpenFlow optical port
     */
    private OFPortOptical createOpticalPort(OFPortOptical port, short type) {
        List<OFPortDescPropOpticalTransport> descList = new ArrayList<>(port.getDesc().size());

        for (OFPortDescPropOpticalTransport desc : port.getDesc()) {
            OFPortDescPropOpticalTransport newDesc = desc.createBuilder()
                    .setType(desc.getType())
                    .setPortSignalType(type)
                    .setPortType(desc.getPortType())
                    .setReserved(desc.getReserved())
                    .build();
            descList.add(newDesc);
        }

        OFPortOptical newPort = port.createBuilder()
                .setConfig(port.getConfig())
                .setDesc(descList)
                .setHwAddr(port.getHwAddr())
                .setName(port.getName())
                .setPortNo(port.getPortNo())
                .setState(port.getState())
                .build();

        return newPort;
    }

    /**
     * Builds list of OFPortOptical ports based on the multi-part circuit ports reply.
     *
     * Ensure the optical transport port's signal type is configured correctly.
     *
     * @param wPorts OF reply with circuit ports
     */
    private void createOpticalPortList(OFCircuitPortsReply wPorts) {
        opticalPorts = new ArrayList<>(wPorts.getEntries().size());

        for (OFPortOptical p : wPorts.getEntries()) {
            short signalType;

            // FIXME: use constants once loxi has full optical extensions
            if (hasPort(p.getPortNo())) {
                signalType = 5;      // OCH port
            } else {
                signalType = 2;      // OMS port
            }

            opticalPorts.add(createOpticalPort(p, signalType));
        }
    }

    @Override
    public List<? extends OFObject> getPortsOf(PortDescPropertyType type) {
        if (!type.equals(PortDescPropertyType.OPTICAL_TRANSPORT)) {
            return Collections.EMPTY_LIST;
        }

        return opticalPorts;
    }

    @Override
    public Set<PortDescPropertyType> getPortTypes() {
        return ImmutableSet.of(PortDescPropertyType.OPTICAL_TRANSPORT);
    }
}
