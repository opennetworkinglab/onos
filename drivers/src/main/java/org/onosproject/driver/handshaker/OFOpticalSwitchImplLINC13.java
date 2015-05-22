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

import org.onosproject.openflow.controller.OpenFlowOpticalSwitch;
import org.onosproject.openflow.controller.PortDescPropertyType;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFCircuitPortStatus;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsReply;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsRequest;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFInstructionType;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFObject;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionCircuit;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.types.CircuitSignalID;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.projectfloodlight.openflow.protocol.OFFlowMod.Builder;

/**
 * LINC-OE Optical Emulator switch class.
 */
public class OFOpticalSwitchImplLINC13
 extends AbstractOpenFlowSwitch implements OpenFlowOpticalSwitch {
    // default number of lambdas, assuming 50GHz channels.
    private static final int NUM_CHLS = 80;
    private final OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);

    private final AtomicBoolean driverHandshakeComplete = new AtomicBoolean(false);
    private long barrierXidToWaitFor = -1;

    private OFCircuitPortsReply wPorts;
    // book-keeping maps for allocated Linc-OE lambdas
    protected final ConcurrentMap<OFPort, BitSet> portChannelMap = new ConcurrentHashMap<>();
    protected final ConcurrentMap<Match, Integer> matchMap = new ConcurrentHashMap<>();

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
                    wPorts = (OFCircuitPortsReply) m;
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
        return ImmutableList.copyOf(super.getPorts());
    }


    @Override
    public Boolean supportNxRole() {
        return false;
    }

    @Override
    public boolean isOptical() {
        return true;
    }

    @Override
    public List<? extends OFObject> getPortsOf(PortDescPropertyType type) {
        return ImmutableList.copyOf(wPorts.getEntries());
    }

    @Override
    public Set<PortDescPropertyType> getPortTypes() {
        return ImmutableSet.of(PortDescPropertyType.OPTICAL_TRANSPORT);
    }

    @Override
    public OFMessage prepareMessage(OFMessage msg) {
        if (OFVersion.OF_13 != msg.getVersion() || msg.getType() != OFType.FLOW_MOD) {
            return msg;
        }
        OFFlowMod fm = (OFFlowMod) msg;
        Match match = fm.getMatch();
        // Don't touch FlowMods that aren't Optical-related.
        if (match.get(MatchField.OCH_SIGTYPE) == null) {
            return msg;
        }

        OFMessage newFM;
        Builder builder = null;
        List<OFAction> actions = new ArrayList<>();
        if (fm.getCommand() == OFFlowModCommand.ADD) {
            builder = factory.buildFlowAdd();
            int lambda = allocateLambda(match.get(MatchField.IN_PORT), match);
            CircuitSignalID sigid = new CircuitSignalID((byte) 1, (byte) 2, (short) lambda, (short) 1);
            List<OFInstruction> instructions = fm.getInstructions();

            newFM = buildFlowMod(builder, fm, buildMatch(match, sigid), buildActions(instructions, sigid));
        } else if (fm.getCommand() == OFFlowModCommand.DELETE) {
            builder = factory.buildFlowDelete();
            int lambda = freeLambda(match.get(MatchField.IN_PORT), match);
            CircuitSignalID sigid = new CircuitSignalID((byte) 1, (byte) 2, (short) lambda, (short) 1);

            newFM = buildFlowMod(builder, fm, buildMatch(match, sigid), actions);
        } else {
            newFM = msg;
        }
        log.debug("new FM = {}", newFM);
        return newFM;
    }

    // fetch the next available channel as the flat lambda value, or the lambda
    // associated with a port/match combination
    private int allocateLambda(OFPort port, Match match) {
        Integer lambda = null;
        synchronized (this) {
            BitSet channels = portChannelMap.getOrDefault(port, new BitSet(NUM_CHLS + 1));
            lambda = matchMap.get(match);
            if (lambda == null) {
                // TODO : double check behavior when bitset is full
                // Linc lambdas start at 1.
                lambda = channels.nextClearBit(1);
                channels.set(lambda);
                portChannelMap.put(port, channels);
                matchMap.put(match, lambda);
            }
        }
        return lambda;
    }

    // free lambda that was mapped to Port/Match combination and return its
    // value to caller.
    private int freeLambda(OFPort port, Match match) {
        synchronized (this) {
            Integer lambda = matchMap.get(match);
            if (lambda != null) {
                portChannelMap.get(port).clear(lambda);
                return lambda;
            }
            // 1 is a sane-ish default for Linc.
            return 1;
        }
    }

    // build matches - *tons of assumptions are made here based on Linc-OE's behavior.*
    // gridType = 1 (DWDM)
    // channelSpacing = 2 (50GHz)
    // spectralWidth = 1 (fixed grid default value)
    private Match buildMatch(Match original, CircuitSignalID sigid) {
        Match.Builder mBuilder = factory.buildMatch();

        original.getMatchFields().forEach(mf -> {
            String name = mf.getName();
            if (MatchField.OCH_SIGID.getName().equals(name)) {
                mBuilder.setExact(MatchField.OCH_SIGID, sigid);
            } else if (MatchField.OCH_SIGTYPE.getName().equals(name)) {
                mBuilder.setExact(MatchField.OCH_SIGTYPE, U8.of((short) 1));
            } else if (MatchField.IN_PORT.getName().equals(name)) {
                mBuilder.setExact(MatchField.IN_PORT, original.get(MatchField.IN_PORT));
            }
        });

        return mBuilder.build();
    }

    private List<OFAction> buildActions(List<OFInstruction> iList, CircuitSignalID sigid) {
        Map<OFInstructionType, OFInstruction> instructions = iList.stream()
                .collect(Collectors.toMap(OFInstruction::getType, inst -> inst));

        OFInstruction inst = instructions.get(OFInstructionType.APPLY_ACTIONS);
        if (inst == null) {
            return Collections.emptyList();
        }

        List<OFAction> actions = new ArrayList<>();
        OFInstructionApplyActions iaa = (OFInstructionApplyActions) inst;
        if (iaa.getActions() == null) {
            return actions;
        }
        iaa.getActions().forEach(action -> {
            if (OFActionType.EXPERIMENTER == action.getType()) {
                OFActionCircuit.Builder cBuilder = factory.actions().buildCircuit()
                        .setField(factory.oxms()
                                .buildOchSigid()
                                .setValue(sigid)
                                .build());
                actions.add(cBuilder.build());
            } else {
                actions.add(action);
            }
        });
        return actions;
    }

    private OFMessage buildFlowMod(Builder builder, OFFlowMod fm, Match m, List<OFAction> act) {
        return builder
                .setXid(fm.getXid())
                .setCookie(fm.getCookie())
                .setCookieMask(fm.getCookieMask())
                .setTableId(fm.getTableId())
                .setIdleTimeout(fm.getIdleTimeout())
                .setHardTimeout(fm.getHardTimeout())
                .setBufferId(fm.getBufferId())
                .setOutPort(fm.getOutPort())
                .setOutGroup(fm.getOutGroup())
                .setFlags(fm.getFlags())
                .setMatch(m)
                .setActions(act)
                .build();
    }
}
