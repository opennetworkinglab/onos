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

import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.types.TableId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Corsa switch driver for BGP Router deployment.
 */
public class OFCorsaSwitchDriver extends AbstractOpenFlowSwitch {
    private static final int FIRST_TABLE = 0;
    private static final int VLAN_MPLS_TABLE = 1;
    private static final int VLAN_TABLE = 2;
    private static final int MPLS_TABLE = 3;
    private static final int ETHER_TABLE = 4;
    private static final int COS_MAP_TABLE = 5;
    private static final int FIB_TABLE = 6;
    private static final int LOCAL_TABLE = 9;

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
        if (msg.getType() == OFType.FLOW_MOD) {
            OFFlowMod flowMod = (OFFlowMod) msg;
            OFFlowMod.Builder builder = flowMod.createBuilder();
            builder.setTableId(TableId.of(LOCAL_TABLE));
            channel.write(Collections.singletonList(builder.build()));
        } else {
            channel.write(Collections.singletonList(msg));
        }
    }

    @Override
    public void write(List<OFMessage> msgs) {
        List<OFMessage> newMsgs = new ArrayList<OFMessage>();
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
    }

    @Override
    public void transformAndSendMsg(OFMessage msg, TableType type) {
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
    public void startDriverHandshake() {}

    @Override
    public boolean isDriverHandshakeComplete() {
        return true;
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {}
}
