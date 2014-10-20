package org.onlab.onos.openflow.drivers.impl;

import org.onlab.onos.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onlab.onos.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onlab.onos.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.onlab.onos.openflow.controller.Dpid;
import org.onlab.onos.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFCircuitPortStatus;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsReply;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsRequest;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFMatchV3;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortOptical;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionCircuit;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmInPort;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOchSigid;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOchSigidBasic;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOchSigtype;
import org.projectfloodlight.openflow.types.CircuitSignalID;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Logger log =
            LoggerFactory.getLogger(OFOpticalSwitchImplLINC13.class);

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
                log.error("Switch {} Error {}", getStringId(), (OFErrorMsg) m);
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
                /*try {
                    testMA();
                    testReverseMA();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
                break;
            default:
                log.warn("Received message {} during switch-driver " +
                                 "subhandshake " + "from switch {} ... " +
                                 "Ignoring message", m,
                         getStringId());

        }
    }

    //Todo
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


    public static final U8 SIGNAL_TYPE = U8.of((short) 10);
    private void testMA() throws IOException {
        log.debug("LINC OE *** Testing MA ");
        short lambda = 1;
        if (getId() == 0x0000ffffffffff01L) {
            final int inport = 10;
            final int outport = 20;
            //Circuit signal id
            CircuitSignalID sigID = getSignalID(lambda);

            OFOxmOchSigidBasic ofOxmOchSigidBasic =
                    factory().oxms().ochSigidBasic(sigID);


            //Match Port
            OFOxmInPort fieldPort = factory().oxms()
                                                .inPort(OFPort.of(inport));
            OFMatchV3 matchPort =
                    factory()
                            .buildMatchV3().
                            setOxmList(OFOxmList.of(fieldPort)).build();


            // Set Action outport ,sigType and sigID
            List<OFAction> actionList = new ArrayList<>();
            OFAction actionOutPort =
                    factory().actions().output(OFPort.of(outport),
                                                  0xffff);

            OFActionCircuit actionCircuit = factory()
                    .actions()
                    .circuit(ofOxmOchSigidBasic);

            actionList.add(actionCircuit);
            actionList.add(actionOutPort);

            OFInstruction instructionAction =
                    factory().instructions().buildApplyActions()
                                .setActions(actionList)
                                .build();
            List<OFInstruction> instructions =
                    Collections.singletonList(instructionAction);

            OFMessage opticalFlowEntry =
                    factory().buildFlowAdd()
                                .setMatch(matchPort)
                                .setPriority(100)
                                .setInstructions(instructions)
                                .setXid(getNextTransactionId())
                                .build();
            log.debug("Adding optical flow in sw {}", getStringId());
            List<OFMessage> msglist = new ArrayList<>(1);
            msglist.add(opticalFlowEntry);
            write(msglist);
            sendBarrier(true);
        } else if (getId() == 0x0000ffffffffff03L) {
            final int inport = 30;
            final int outport = 31;
            //Circuit signal id
            CircuitSignalID sigID = getSignalID(lambda);

            OFOxmOchSigid fieldSigIDMatch = factory().oxms().ochSigid(sigID);
            OFOxmOchSigtype fieldSigType = factory()
                    .oxms()
                    .ochSigtype(SIGNAL_TYPE);

            OFOxmOchSigidBasic ofOxmOchSigidBasic =
                    factory().oxms().ochSigidBasic(sigID);

            //Match Port,SigType,SigID
            OFOxmInPort fieldPort = factory()
                    .oxms()
                    .inPort(OFPort.of(inport));
            OFMatchV3 matchPort = factory()
                    .buildMatchV3()
                    .setOxmList(OFOxmList.of(fieldPort,
                                             fieldSigIDMatch,
                                             fieldSigType
                    ))
                    .build();

            // Set Action outport ,SigType, sigID
            List<OFAction> actionList = new ArrayList<>();
            OFAction actionOutPort =
                    factory().actions().output(OFPort.of(outport),
                                                  0xffff);

            OFActionCircuit actionCircuit = factory()
                    .actions()
                    .circuit(ofOxmOchSigidBasic);



            //actionList.add(setActionSigType);
            actionList.add(actionCircuit);
            actionList.add(actionOutPort);

            OFInstruction instructionAction =
                    factory().instructions().buildApplyActions()
                                .setActions(actionList)
                                .build();
            List<OFInstruction> instructions =
                    Collections.singletonList(instructionAction);

            OFMessage opticalFlowEntry =
                    factory().buildFlowAdd()
                                .setMatch(matchPort)
                                .setPriority(100)
                                .setInstructions(instructions)
                                .setXid(getNextTransactionId())
                                .build();
            log.debug("Adding optical flow in sw {}", getStringId());
            List<OFMessage> msglist = new ArrayList<>(1);
            msglist.add(opticalFlowEntry);
            write(msglist);
            sendBarrier(true);

        } else if (getId() == 0x0000ffffffffff02L) {
            final int inport = 21;
            final int outport = 11;
            //Circuit signal id
            CircuitSignalID sigID = getSignalID(lambda);

            OFOxmOchSigid fieldSigIDMatch = factory().oxms().ochSigid(sigID);
            OFOxmOchSigtype fieldSigType = factory()
                    .oxms()
                    .ochSigtype(SIGNAL_TYPE);


            //Match Port, sig type and sig id
            OFOxmInPort fieldPort = factory()
                    .oxms()
                    .inPort(OFPort.of(inport));
            OFMatchV3 matchPort =
                    factory().buildMatchV3()
                                .setOxmList(OFOxmList.of(fieldPort,
                                                         fieldSigIDMatch,
                                                         fieldSigType
                                ))
                                .build();

            // Set Action outport
            List<OFAction> actionList = new ArrayList<>();
            OFAction actionOutPort =
                    factory().actions().output(OFPort.of(outport),
                                                  0xffff);

            actionList.add(actionOutPort);

            OFInstruction instructionAction =
                    factory().instructions().buildApplyActions()
                                .setActions(actionList)
                                .build();
            List<OFInstruction> instructions =
                    Collections.singletonList(instructionAction);

            OFMessage opticalFlowEntry =
                    factory().buildFlowAdd()
                                .setMatch(matchPort)
                                .setPriority(100)
                                .setInstructions(instructions)
                                .setXid(getNextTransactionId())
                                .build();
            log.debug("Adding optical flow in sw {}", getStringId());
            List<OFMessage> msglist = new ArrayList<>(1);
            msglist.add(opticalFlowEntry);
            write(msglist);
            sendBarrier(true);
        }

    }
    private void testReverseMA() throws IOException {
        log.debug("LINC OE *** Testing MA ");
        short lambda = 1;
        if (getId() == 0x0000ffffffffff02L) {
            final int inport = 11;
            final int outport = 21;
            //Circuit signal id
            CircuitSignalID sigID = getSignalID(lambda);

            OFOxmOchSigidBasic ofOxmOchSigidBasic =
                    factory().oxms().ochSigidBasic(sigID);

            //Match Port
            OFOxmInPort fieldPort = factory().oxms()
                                                .inPort(OFPort.of(inport));
            OFMatchV3 matchPort =
                    factory()
                            .buildMatchV3().
                            setOxmList(OFOxmList.of(fieldPort)).build();


            // Set Action outport ,sigType and sigID
            List<OFAction> actionList = new ArrayList<>();
            OFAction actionOutPort =
                    factory().actions().output(OFPort.of(outport),
                                                  0xffff);

            OFActionCircuit actionCircuit = factory()
                    .actions()
                    .circuit(ofOxmOchSigidBasic);
            actionList.add(actionCircuit);
            actionList.add(actionOutPort);

            OFInstruction instructionAction =
                    factory().instructions().buildApplyActions()
                                .setActions(actionList)
                                .build();
            List<OFInstruction> instructions =
                    Collections.singletonList(instructionAction);

            OFMessage opticalFlowEntry =
                    factory().buildFlowAdd()
                                .setMatch(matchPort)
                                .setPriority(100)
                                .setInstructions(instructions)
                                .setXid(getNextTransactionId())
                                .build();
            log.debug("Adding optical flow in sw {}", getStringId());
            List<OFMessage> msglist = new ArrayList<>(1);
            msglist.add(opticalFlowEntry);
            write(msglist);
            sendBarrier(true);
        } else if (getId() == 0x0000ffffffffff03L) {
            final int inport = 31;
            final int outport = 30;
            //Circuit signal id
            CircuitSignalID sigID = getSignalID(lambda);

            OFOxmOchSigid fieldSigIDMatch = factory().oxms().ochSigid(sigID);
            OFOxmOchSigtype fieldSigType = factory()
                    .oxms()
                    .ochSigtype(SIGNAL_TYPE);

            OFOxmOchSigidBasic ofOxmOchSigidBasic =
                    factory().oxms().ochSigidBasic(sigID);

            //Match Port,SigType,SigID
            OFOxmInPort fieldPort = factory()
                    .oxms()
                    .inPort(OFPort.of(inport));
            OFMatchV3 matchPort = factory()
                    .buildMatchV3()
                    .setOxmList(OFOxmList.of(fieldPort,
                                             fieldSigIDMatch,
                                             fieldSigType
                    ))
                    .build();

            // Set Action outport ,SigType, sigID
            List<OFAction> actionList = new ArrayList<>();
            OFAction actionOutPort =
                    factory().actions().output(OFPort.of(outport),
                                                  0xffff);
            OFActionCircuit actionCircuit = factory()
                    .actions()
                    .circuit(ofOxmOchSigidBasic);

            actionList.add(actionCircuit);
            actionList.add(actionOutPort);

            OFInstruction instructionAction =
                    factory().instructions().buildApplyActions()
                                .setActions(actionList)
                                .build();
            List<OFInstruction> instructions =
                    Collections.singletonList(instructionAction);

            OFMessage opticalFlowEntry =
                    factory().buildFlowAdd()
                                .setMatch(matchPort)
                                .setPriority(100)
                                .setInstructions(instructions)
                                .setXid(getNextTransactionId())
                                .build();
            log.debug("Adding optical flow in sw {}", getStringId());
            List<OFMessage> msglist = new ArrayList<>(1);
            msglist.add(opticalFlowEntry);
            write(msglist);
            sendBarrier(true);

        } else if (getId() == 0x0000ffffffffff01L) {
            final int inport = 20;
            final int outport = 10;
            //Circuit signal id
            CircuitSignalID sigID = getSignalID(lambda);

            OFOxmOchSigid fieldSigIDMatch = factory().oxms().ochSigid(sigID);
            OFOxmOchSigtype fieldSigType = factory()
                    .oxms()
                    .ochSigtype(SIGNAL_TYPE);


            //Match Port, sig type and sig id
            OFOxmInPort fieldPort = factory()
                    .oxms()
                    .inPort(OFPort.of(inport));
            OFMatchV3 matchPort =
                    factory().buildMatchV3()
                                .setOxmList(OFOxmList.of(fieldPort,
                                                         fieldSigIDMatch,
                                                         fieldSigType
                                ))
                                .build();

            // Set Action outport
            List<OFAction> actionList = new ArrayList<>();
            OFAction actionOutPort =
                    factory().actions().output(OFPort.of(outport),
                                                  0xffff);

            actionList.add(actionOutPort);

            OFInstruction instructionAction =
                    factory().instructions().buildApplyActions()
                                .setActions(actionList)
                                .build();
            List<OFInstruction> instructions =
                    Collections.singletonList(instructionAction);

            OFMessage opticalFlowEntry =
                    factory().buildFlowAdd()
                                .setMatch(matchPort)
                                .setPriority(100)
                                .setInstructions(instructions)
                                .setXid(getNextTransactionId())
                                .build();
            log.debug("Adding optical flow in sw {}", getStringId());
            List<OFMessage> msglist = new ArrayList<>(1);
            msglist.add(opticalFlowEntry);
            write(msglist);
            sendBarrier(true);
        }

    }


    // Todo remove - for testing purpose only
    private static CircuitSignalID getSignalID(short lambda) {
        byte myGrid = 1;
        byte myCs = 2;
        short myCn = lambda;
        short mySw = 1;

        CircuitSignalID signalID = new CircuitSignalID(myGrid,
                                                       myCs,
                                                       myCn,
                                                       mySw);
        return signalID;
    }

    private void sendBarrier(boolean finalBarrier) throws IOException {
        int xid = getNextTransactionId();
        if (finalBarrier) {
            barrierXidToWaitFor = xid;
        }
        OFBarrierRequest br = factory()
                .buildBarrierRequest()
                .setXid(xid)
                .build();
        sendMsg(br);
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
