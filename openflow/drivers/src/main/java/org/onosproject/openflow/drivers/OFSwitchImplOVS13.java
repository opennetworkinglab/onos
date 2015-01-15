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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMatchV3;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Nicira, Inc. Make
 * (Hardware Desc.) : Open vSwitch Model (Datapath Desc.) : None Software :
 * 2.1.0 (or whatever version + build) Serial : None
 */
public class OFSwitchImplOVS13 extends AbstractOpenFlowSwitch {

    private final AtomicBoolean driverHandshakeComplete;
    private OFFactory factory;
    private long barrierXidToWaitFor = -1;

    private static final short MIN_PRIORITY = 0x0;
    private static final int OFPCML_NO_BUFFER = 0xffff;

    public OFSwitchImplOVS13(Dpid dpid, OFDescStatsReply desc) {
        super(dpid);
        driverHandshakeComplete = new AtomicBoolean(false);
        setSwitchDescription(desc);
    }

    @Override
    public String toString() {
        return "OFSwitchImplOVS13 [" + ((channel != null)
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
        factory = factory();
        configureSwitch();
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
                driverHandshakeComplete.set(true);
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
            // OFAsyncGetReply asrep = (OFAsyncGetReply)m;
            // decodeAsyncGetReply(asrep);
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
            // processStatsReply((OFStatsReply) m);
            break;

        default:
            log.debug("Received message {} during switch-driver subhandshake "
                    + "from switch {} ... Ignoring message", m, getStringId());

        }
    }

    private void configureSwitch() {
        sendBarrier(true);
    }

    private void sendBarrier(boolean finalBarrier) {
        int xid = getNextTransactionId();
        if (finalBarrier) {
            barrierXidToWaitFor = xid;
        }
        OFBarrierRequest br = factory
                .buildBarrierRequest()
                .setXid(xid)
                .build();
        write(br);
    }

    @Override
    public Boolean supportNxRole() {
        return false;
    }

    @Override
    public void write(OFMessage msg) {
        channel.write(Collections.singletonList(msg));

    }

    @Override
    public void write(List<OFMessage> msgs) {
        channel.write(msgs);
    }

    /**
     * By default if none of the booleans in the call are set, then the
     * table-miss entry is added with no instructions, which means that pipeline
     * execution will stop, and the action set associated with the packet will
     * be executed.
     *
     * @param tableToAdd
     * @param toControllerNow as an APPLY_ACTION instruction
     * @param toControllerWrite as a WRITE_ACITION instruction
     * @param toTable as a GOTO_TABLE instruction
     * @param tableToSend
     */
    @SuppressWarnings("unchecked")
    private void populateTableMissEntry(int tableToAdd, boolean toControllerNow,
            boolean toControllerWrite,
            boolean toTable, int tableToSend) {
        OFOxmList oxmList = OFOxmList.EMPTY;
        OFMatchV3 match = factory.buildMatchV3()
                .setOxmList(oxmList)
                .build();
        OFAction outc = factory.actions()
                .buildOutput()
                .setPort(OFPort.CONTROLLER)
                .setMaxLen(OFPCML_NO_BUFFER)
                .build();
        List<OFInstruction> instructions = new ArrayList<OFInstruction>();
        if (toControllerNow) {
            // table-miss instruction to send to controller immediately
            OFInstruction instr = factory.instructions()
                    .buildApplyActions()
                    .setActions(Collections.singletonList(outc))
                    .build();
            instructions.add(instr);
        }

        if (toControllerWrite) {
            // table-miss instruction to write-action to send to controller
            // this will be executed whenever the action-set gets executed
            OFInstruction instr = factory.instructions()
                    .buildWriteActions()
                    .setActions(Collections.singletonList(outc))
                    .build();
            instructions.add(instr);
        }

        if (toTable) {
            // table-miss instruction to goto-table x
            OFInstruction instr = factory.instructions()
                    .gotoTable(TableId.of(tableToSend));
            instructions.add(instr);
        }

        if (!toControllerNow && !toControllerWrite && !toTable) {
            // table-miss has no instruction - at which point action-set will be
            // executed - if there is an action to output/group in the action
            // set
            // the packet will be sent there, otherwise it will be dropped.
            instructions = Collections.EMPTY_LIST;
        }

        OFMessage tableMissEntry = factory.buildFlowAdd()
                .setTableId(TableId.of(tableToAdd))
                .setMatch(match) // match everything
                .setInstructions(instructions)
                .setPriority(MIN_PRIORITY)
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                .setXid(getNextTransactionId())
                .build();
        write(tableMissEntry);
    }

}
