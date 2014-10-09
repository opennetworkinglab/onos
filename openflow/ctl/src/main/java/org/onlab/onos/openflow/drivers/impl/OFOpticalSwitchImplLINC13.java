package org.onlab.onos.openflow.drivers.impl;

import org.onlab.onos.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onlab.onos.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onlab.onos.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.onlab.onos.openflow.controller.Dpid;
import org.onlab.onos.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsReply;
import org.projectfloodlight.openflow.protocol.OFCircuitPortsRequest;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFMatchV3;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortOptical;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionCircuit;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmInPort;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOchSigid;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOchSigidBasic;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOchSigtype;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOchSigtypeBasic;
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

    private final Logger log =
            LoggerFactory.getLogger(OFOpticalSwitchImplLINC13.class);

    OFOpticalSwitchImplLINC13(Dpid dpid,OFDescStatsReply desc) {
        super(dpid);
        //setAttribute("optical", "true");
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
        log.debug("Starting driver handshake for sw {}", getStringId());
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;
        try {
            sendHandshakeOFExperimenterPortDescRequest();
        } catch (IOException e) {
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
                break;
            case QUEUE_GET_CONFIG_REPLY:
                break;
            case ROLE_REPLY:
                break;
            case STATS_REPLY:
                log.debug("LINC-OE : Received stats reply message {}", m);
                processHandshakeOFExperimenterPortDescRequest(
                        (OFCircuitPortsReply) m);
                driverHandshakeComplete.set(true);
               /* try {
                    testMA();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
                break;
            default:
                log.debug("Received message {} during switch-driver " +
                                  "subhandshake " + "from switch {} ... " +
                                  "Ignoring message", m,
                          getStringId());

        }
    }


    private void processHandshakeOFExperimenterPortDescRequest(
            OFCircuitPortsReply sr) {
        Collection<OFPortOptical> entries = sr.getEntries();
        List<OFPortDesc> ofPortDescList = new ArrayList<>(entries.size());
        for (OFPortOptical entry : entries) {
            ofPortDescList.add(factory().buildPortDesc().
                    setPortNo(entry.getPortNo())
                                           .setConfig(entry.getConfig())
                                           .setState(entry.getState())
                                           .setHwAddr(entry.getHwAddr())
                                           .setName(entry.getName())
                                           .build());
        }
        setPortDescReply(factory().buildPortDescStatsReply().
                setEntries(ofPortDescList).build());
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
        channel.write(Collections.singletonList(circuitPortsRequest));
    }



    //todo for testing
    public static final U8 SIGNAL_TYPE = U8.of((short) 1);
    private void testMA() throws IOException {
        log.debug("LINC OE *** Testing MA ");
        short lambda = 100;
        if (getId() == 0x0000ffffffffff02L) {
            final int inport = 10;
            final int outport = 20;
            //Circuit signal id
            CircuitSignalID sigID = getSignalID(lambda);

            OFOxmOchSigid fieldSigIDMatch = factory().oxms().ochSigid(sigID);
            OFOxmOchSigtype fieldSigType = factory()
                    .oxms()
                    .ochSigtype(SIGNAL_TYPE);

            OFOxmOchSigidBasic ofOxmOchSigidBasic =
                    factory().oxms().ochSigidBasic(sigID);

            OFOxmOchSigtypeBasic ofOxmOchSigtypeBasic =
                    factory().oxms().ochSigtypeBasic(SIGNAL_TYPE);

            //Match Port
            OFOxmInPort fieldPort = factory().oxms()
                                                .inPort(OFPort.of(inport));
            OFMatchV3 matchPort =
                    factory()
                            .buildMatchV3().
                            setOxmList(OFOxmList.of(fieldPort,
                                                    fieldSigType,
                                                    fieldSigIDMatch)).build();


            // Set Action outport ,sigType and sigID
            List<OFAction> actionList = new ArrayList<>();
            OFAction actionOutPort =
                    factory().actions().output(OFPort.of(outport),
                                                  Short.MAX_VALUE);

            OFActionCircuit actionCircuit = factory()
                    .actions()
                    .circuit(ofOxmOchSigidBasic);
            OFActionCircuit setActionSigType = factory()
                    .actions()
                    .circuit(ofOxmOchSigtypeBasic);

            actionList.add(actionOutPort);
            actionList.add(setActionSigType);
            actionList.add(actionCircuit);

            OFInstruction instructionAction =
                    factory().instructions().buildApplyActions()
                                .setActions(actionList)
                                .build();
            List<OFInstruction> instructions =
                    Collections.singletonList(instructionAction);

            OFMessage opticalFlowEntry =
                    factory().buildFlowAdd()
                                .setMatch(matchPort)
                                .setInstructions(instructions)
                                .setXid(getNextTransactionId())
                                .build();
            log.debug("Adding optical flow in sw {}", getStringId());
            List<OFMessage> msglist = new ArrayList<>(1);
            msglist.add(opticalFlowEntry);
            write(msglist);
        } else if (getId() == 0x0000ffffffffff03L) {
            final int inport = 21;
            final int outport = 22;
            //Circuit signal id
            CircuitSignalID sigID = getSignalID(lambda);

            OFOxmOchSigid fieldSigIDMatch = factory().oxms().ochSigid(sigID);
            OFOxmOchSigtype fieldSigType = factory()
                    .oxms()
                    .ochSigtype(SIGNAL_TYPE);

            OFOxmOchSigidBasic ofOxmOchSigidBasic =
                    factory().oxms().ochSigidBasic(sigID);

            OFOxmOchSigtypeBasic ofOxmOchSigtypeBasic =
                    factory().oxms().ochSigtypeBasic(SIGNAL_TYPE);

            //Match Port,SigType,SigID
            OFOxmInPort fieldPort = factory()
                    .oxms()
                    .inPort(OFPort.of(inport));
            OFMatchV3 matchPort = factory()
                    .buildMatchV3()
                    .setOxmList(OFOxmList.of(fieldPort,
                                             fieldSigType,
                                             fieldSigIDMatch))
                    .build();

            // Set Action outport ,SigType, sigID
            List<OFAction> actionList = new ArrayList<>();
            OFAction actionOutPort =
                    factory().actions().output(OFPort.of(outport),
                                                  Short.MAX_VALUE);

            OFActionCircuit setActionSigType = factory()
                    .actions()
                    .circuit(ofOxmOchSigtypeBasic);
            OFActionCircuit actionCircuit = factory()
                    .actions()
                    .circuit(ofOxmOchSigidBasic);


            actionList.add(actionOutPort);
            actionList.add(setActionSigType);
            actionList.add(actionCircuit);

            OFInstruction instructionAction =
                    factory().instructions().buildApplyActions()
                                .setActions(actionList)
                                .build();
            List<OFInstruction> instructions =
                    Collections.singletonList(instructionAction);

            OFMessage opticalFlowEntry =
                    factory().buildFlowAdd()
                                 .setMatch(matchPort)
                                 .setInstructions(instructions)
                                 .setXid(getNextTransactionId())
                                 .build();
            log.debug("Adding optical flow in sw {}", getStringId());
            List<OFMessage> msglist = new ArrayList<>(1);
            msglist.add(opticalFlowEntry);
            write(msglist);

        } else if (getId() == 0x0000ffffffffff04L) {
            final int inport = 23;
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
                                                         fieldSigType,
                                                         fieldSigIDMatch))
                                .build();

            // Set Action outport
            List<OFAction> actionList = new ArrayList<>();
            OFAction actionOutPort =
                    factory().actions().output(OFPort.of(outport),
                                                  Short.MAX_VALUE);

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
                                 .setInstructions(instructions)
                                 .setXid(getNextTransactionId())
                                 .build();
            log.debug("Adding optical flow in sw {}", getStringId());
            List<OFMessage> msglist = new ArrayList<>(1);
            msglist.add(opticalFlowEntry);
            write(msglist);
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

    @Override
    public void write(OFMessage msg) {
        this.channel.write(msg);
    }

    @Override
    public void write(List<OFMessage> msgs) {
        this.channel.write(msgs);
    }

    @Override
    public Boolean supportNxRole() {
        return false;
    }

}
