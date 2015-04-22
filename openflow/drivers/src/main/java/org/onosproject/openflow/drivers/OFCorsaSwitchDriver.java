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
package org.onosproject.openflow.drivers;

import com.google.common.collect.Lists;
import org.onlab.packet.Ethernet;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMatchV3;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;

//import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Corsa switch driver for BGP Router deployment.
 */
public class OFCorsaSwitchDriver extends AbstractOpenFlowSwitch {

    protected static final int FIRST_TABLE = 0;
    protected static final int VLAN_MPLS_TABLE = 1;
    protected static final int VLAN_TABLE = 2;
    protected static final int MPLS_TABLE = 3;
    protected static final int ETHER_TABLE = 4;
    protected static final int COS_MAP_TABLE = 5;
    protected static final int FIB_TABLE = 6;
    protected static final int LOCAL_TABLE = 9;


    private AtomicBoolean handShakeComplete = new AtomicBoolean(false);

    private int barrierXid;

    OFCorsaSwitchDriver(Dpid dpid, OFDescStatsReply desc) {
        super(dpid);

        setSwitchDescription(desc);
    }

    /**
     * Used by the default sendMsg to 'write' to the switch.
     * This method is indirectly used by generic onos services like proxyarp
     * to request packets from the default flow table. In a multi-table
     * pipeline, these requests are redirected to the correct table.
     *
     * For the Corsa switch, the equivalent table is the LOCAL TABLE
     *
     */
    @Override
    public void write(OFMessage msg) {
/*        if (msg.getType() == OFType.FLOW_MOD) {
            OFFlowMod flowMod = (OFFlowMod) msg;
            OFFlowMod.Builder builder = flowMod.createBuilder();
            builder.setTableId(TableId.of(LOCAL_TABLE));
            channel.write(Collections.singletonList(builder.build()));
        } else {
            channel.write(Collections.singletonList(msg));
        }
*/
        channel.write(Collections.singletonList(msg));
    }

    @Override
    public void write(List<OFMessage> msgs) {
/*        List<OFMessage> newMsgs = new ArrayList<OFMessage>();
        for (OFMessage msg : msgs) {
            if (msg.getType() == OFType.FLOW_MOD) {
                OFFlowMod flowMod = (OFFlowMod) msg;
                OFFlowMod.Builder builder = flowMod.createBuilder();
                builder.setTableId(TableId.of(LOCAL_TABLE));
                newMsgs.add(builder.build());
            } else {
                newMsgs.add(msg);
            }
        }
        channel.write(newMsgs);
*/
        channel.write(msgs);
    }

    @Override
    public void transformAndSendMsg(OFMessage msg, TableType type) {
        log.trace("Trying to send {} of TableType {}", msg, type);
        if (msg.getType() == OFType.FLOW_MOD) {
            OFFlowMod flowMod = (OFFlowMod) msg;
            OFFlowMod.Builder builder = flowMod.createBuilder();
            List<OFInstruction> instructions = flowMod.getInstructions();
            List<OFInstruction> newInstructions = Lists.newArrayList();
            for (OFInstruction i : instructions) {
                if (i instanceof OFInstructionGotoTable) {
                    OFInstructionGotoTable gotoTable = (OFInstructionGotoTable) i;
                    TableType tid = TableType.values()[gotoTable.getTableId().getValue()];
                    switch (tid) {
                        case VLAN_MPLS:
                            newInstructions.add(
                                    gotoTable.createBuilder()
                                            .setTableId(TableId.of(VLAN_MPLS_TABLE)).build());
                            break;
                        case VLAN:
                            newInstructions.add(
                                    gotoTable.createBuilder()
                                            .setTableId(TableId.of(VLAN_TABLE)).build());
                            break;
                        case ETHER:
                            newInstructions.add(
                                    gotoTable.createBuilder()
                                            .setTableId(TableId.of(ETHER_TABLE)).build());
                            break;
                        case COS:
                            newInstructions.add(
                                    gotoTable.createBuilder()
                                            .setTableId(TableId.of(COS_MAP_TABLE)).build());
                            break;
                        case IP:
                            newInstructions.add(
                                    gotoTable.createBuilder()
                                            .setTableId(TableId.of(FIB_TABLE)).build());
                            break;
                        case MPLS:
                            newInstructions.add(
                                    gotoTable.createBuilder()
                                            .setTableId(TableId.of(MPLS_TABLE)).build());
                            break;
                        case ACL:
                            newInstructions.add(
                                    gotoTable.createBuilder()
                                            .setTableId(TableId.of(LOCAL_TABLE)).build());
                            break;
                        case NONE:
                            log.error("Should never have to go to Table 0");
                            /*newInstructions.add(
                                    gotoTable.createBuilder()
                                            .setTableId(TableId.of(0)).build());
                            */
                            break;
                        default:
                            log.warn("Unknown table type: {}", tid);
                    }

                } else {
                    newInstructions.add(i);
                }
            }
            switch (type) {
                case VLAN_MPLS:
                    builder.setTableId(TableId.of(VLAN_MPLS_TABLE));
                    break;
                case VLAN:
                    builder.setTableId(TableId.of(VLAN_TABLE));
                    break;
                case ETHER:
                    builder.setTableId(TableId.of(ETHER_TABLE));
                    break;
                case COS:
                    builder.setTableId(TableId.of(COS_MAP_TABLE));
                    break;
                case IP:
                    builder.setTableId(TableId.of(FIB_TABLE));
                    break;
                case MPLS:
                    builder.setTableId(TableId.of(MPLS_TABLE));
                    break;
                case ACL:
                    builder.setTableId(TableId.of(LOCAL_TABLE));
                    break;
                case FIRST:
                    builder.setTableId(TableId.of(FIRST_TABLE));
                    break;
                case NONE:
                    builder.setTableId(TableId.of(LOCAL_TABLE));
                    break;
                default:
                    log.warn("Unknown table type: {}", type);
            }
            builder.setInstructions(newInstructions);

            OFMatchV3 match = (OFMatchV3) flowMod.getMatch();
            for (OFOxm oxm: match.getOxmList()) {
                if (oxm.getMatchField() == MatchField.VLAN_VID &&
                        oxm.getValue().equals(OFVlanVidMatch.PRESENT)) {
                        Match.Builder mBuilder = factory().buildMatchV3();
                        mBuilder.setExact(MatchField.ETH_TYPE, EthType.of(Ethernet.TYPE_VLAN));
                        builder.setMatch(mBuilder.build());
                }
            }

            OFMessage msgnew = builder.build();
            channel.write(Collections.singletonList(msgnew));
            log.debug("Installed {}", msgnew);

        } else {
            channel.write(Collections.singletonList(msg));
        }
    }

    @Override
    public TableType getTableType(TableId tid) {
        switch (tid.getValue()) {
        case VLAN_MPLS_TABLE:
            return TableType.VLAN_MPLS;
        case VLAN_TABLE:
            return TableType.VLAN;
        case ETHER_TABLE:
            return TableType.ETHER;
        case COS_MAP_TABLE:
            return TableType.COS;
        case FIB_TABLE:
            return TableType.IP;
        case MPLS_TABLE:
            return TableType.MPLS;
        case LOCAL_TABLE:
            return TableType.NONE;
        case FIRST_TABLE:
            return TableType.FIRST;
        default:
            log.warn("Unknown table type: {}", tid.getValue());
            return TableType.NONE;
        }
    }

    @Override
    public Boolean supportNxRole() {
        return false;
    }

    @Override
    public void startDriverHandshake() {
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;
        OFFlowMod fm = factory().buildFlowDelete()
                .setTableId(TableId.ALL)
                .setOutGroup(OFGroup.ANY)
                .build();

        channel.write(Collections.singletonList(fm));

        barrierXid = getNextTransactionId();
        OFBarrierRequest barrier = factory().buildBarrierRequest()
                .setXid(barrierXid).build();


        channel.write(Collections.singletonList(barrier));

    }

    @Override
    public boolean isDriverHandshakeComplete() {
        if (!startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        return handShakeComplete.get();
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {
        if (!startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeNotStarted();
        }
        if (handShakeComplete.get()) {
            throw new SwitchDriverSubHandshakeCompleted(m);
        }
        if (m.getType() == OFType.BARRIER_REPLY &&
                m.getXid() == barrierXid) {
            handShakeComplete.set(true);
        }
    }
}
