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
package org.onosproject.driver.optical.handshaker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.Device;
import org.onosproject.openflow.controller.OpenFlowOpticalSwitch;
import org.onosproject.openflow.controller.PortDescPropertyType;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFCircuitPortStatus;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsReply;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsRequest;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowStatsRequest;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFObject;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescPropOpticalTransport;
import org.projectfloodlight.openflow.protocol.OFPortOptical;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmExpOchSigId;
import org.projectfloodlight.openflow.types.CircuitSignalID;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U8;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
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
 * As LINC implements custom OF optical extensions (in contrast to the final standard as specified in
 * ONF TS-022 (March 15, 2015), we need to rewrite flow stat requests and flow mods in {@link #sendMsg(OFMessage)}.
 */
public class OfOpticalSwitchImplLinc13 extends AbstractOpenFlowSwitch implements OpenFlowOpticalSwitch {

    private final AtomicBoolean driverHandshakeComplete = new AtomicBoolean(false);
    private long barrierXidToWaitFor = -1;

    private List<OFPortOptical> opticalPorts;

    @Override
    public void startDriverHandshake() {
        log.debug("Starting driver handshake for sw {}", getStringId());
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;
        try {
            sendHandshakeOFExperimenterPortDescRequest();
        } catch (IOException e) {
            log.error("LINC-OE exception while sending experimenter port desc:",
                     e);
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
                log.debug("****LINC-OE Port Status {} {}", getStringId(), m);
                processOFPortStatus((OFCircuitPortStatus) m);
                break;
            case QUEUE_GET_CONFIG_REPLY:
                break;
            case ROLE_REPLY:
                break;
            case STATS_REPLY:
                OFStatsReply stats = (OFStatsReply) m;
                if (stats.getStatsType() == OFStatsType.EXPERIMENTER) {
                    log.debug("LINC-OE : Received stats reply message {}", m);
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
        log.debug("LINC-OE : Sending experimented circuit port stats " +
                         "message " +
                         "{}",
                 circuitPortsRequest.toString());
        this.sendHandshakeMessage(circuitPortsRequest);
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

    /**
     * Rewrite match object to use LINC OF optical extensions.
     *
     * @param match original match
     * @return rewritten match
     */
    private Match rewriteMatch(Match match) {
        Match.Builder mBuilder = factory().buildMatch();
        for (MatchField mf : match.getMatchFields()) {
            if (mf == MatchField.EXP_OCH_SIG_ID) {
                mBuilder.setExact(MatchField.OCH_SIGID, (CircuitSignalID) match.get(mf));
                continue;
            }
            if (mf == MatchField.EXP_OCH_SIGTYPE) {
                mBuilder.setExact(MatchField.OCH_SIGTYPE, (U8) match.get(mf));
                continue;
            }
            mBuilder.setExact(mf, match.get(mf));
        }

        return mBuilder.build();
    }

    /**
     * Rewrite actions to use LINC OF optical extensions.
     *
     * @param actions original actions
     * @return rewritten actions
     */
    private List<OFAction> rewriteActions(List<OFAction> actions) {
        List<OFAction> newActions = new LinkedList<>();

        for (OFAction action : actions) {
            if (!(action instanceof OFActionSetField)) {
                newActions.add(action);
                continue;
            }

            OFActionSetField sf = (OFActionSetField) action;
            if (!(sf.getField() instanceof OFOxmExpOchSigId)) {
                newActions.add(action);
                continue;
            }

            OFOxmExpOchSigId oxm = (OFOxmExpOchSigId) sf.getField();
            CircuitSignalID signalId = oxm.getValue();

            newActions.add(
                    factory().actions().circuit(factory().oxms().ochSigid(signalId)));
        }

        return newActions;
    }

    @Override
    public void sendMsg(OFMessage msg) {
        // Ignore everything but flow mods and stat requests
        if (!(msg instanceof OFFlowMod || msg instanceof OFFlowStatsRequest)) {
            super.sendMsg(msg);
            return;
        }

        Match newMatch;
        OFMessage newMsg = null;

        if (msg instanceof OFFlowStatsRequest) {
            // Rewrite match only
            OFFlowStatsRequest fsr = (OFFlowStatsRequest) msg;
            newMatch = rewriteMatch(fsr.getMatch());
            newMsg = fsr.createBuilder().setMatch(newMatch).build();
        } else if (msg instanceof OFFlowMod) {
            // Rewrite match and actions
            OFFlowMod fm = (OFFlowMod) msg;
            newMatch = rewriteMatch(fm.getMatch());
            List<OFAction> actions = rewriteActions(fm.getActions());
            newMsg = fm.createBuilder().setMatch(newMatch).setActions(actions).build();
        }

        super.sendMsg(newMsg);
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
     * @param port given port number
     * @return true if the port is a tap (OCh), false otherwise (OMS port)
     */
    private boolean isOChPort(OFPort port) {

        return portDescs().containsKey(port);
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
            if (isOChPort(p.getPortNo())) {
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
            return ImmutableList.of();
        }

        return opticalPorts;
    }

    @Override
    public Set<PortDescPropertyType> getPortTypes() {
        return ImmutableSet.of(PortDescPropertyType.OPTICAL_TRANSPORT);
    }
}
