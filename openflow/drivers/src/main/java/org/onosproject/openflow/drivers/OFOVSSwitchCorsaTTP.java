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
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.types.TableId;

import java.util.Collections;
import java.util.List;

public class OFOVSSwitchCorsaTTP extends OFCorsaSwitchDriver {

    OFOVSSwitchCorsaTTP(Dpid dpid, OFDescStatsReply desc) {
        super(dpid, desc);
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
            OFMessage msgnew = builder.build();
            channel.write(Collections.singletonList(msgnew));
            log.debug("Installed {}", msgnew);

        } else {
            channel.write(Collections.singletonList(msg));
        }
    }
}
