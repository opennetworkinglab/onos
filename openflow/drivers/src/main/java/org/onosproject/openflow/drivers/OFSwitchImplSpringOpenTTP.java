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
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.types.TableId;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//TODO: Knock-off this class as we don't need any switch/app specific
//drivers in the south bound layers.
public class OFSwitchImplSpringOpenTTP extends AbstractOpenFlowSwitch {

    private OFFactory factory;

    private final AtomicBoolean driverHandshakeComplete;
    private AtomicBoolean haltStateMachine;

    /* Default table ID - compatible with CpqD switch */
    private static final int TABLE_VLAN = 0;
    private static final int TABLE_TMAC = 1;
    private static final int TABLE_IPV4_UNICAST = 2;
    private static final int TABLE_MPLS = 3;
    private static final int TABLE_ACL = 5;

    /*
     * Set the default values. These variables will get overwritten based on the
     * switch vendor type
     */
    protected int vlanTableId = TABLE_VLAN;
    protected int tmacTableId = TABLE_TMAC;
    protected int ipv4UnicastTableId = TABLE_IPV4_UNICAST;
    protected int mplsTableId = TABLE_MPLS;
    protected int aclTableId = TABLE_ACL;

    protected OFSwitchImplSpringOpenTTP(Dpid dpid, OFDescStatsReply desc) {
        super(dpid);
        driverHandshakeComplete = new AtomicBoolean(false);
        haltStateMachine = new AtomicBoolean(false);
        setSwitchDescription(desc);
    }

    @Override
    public String toString() {
        return "OFSwitchImplSpringOpenTTP ["
                + ((channel != null) ? channel.getRemoteAddress() : "?")
                + " DPID["
                + ((this.getStringId() != null) ? this.getStringId() : "?")
                + "]]";
    }

    @Override
    public Boolean supportNxRole() {
        return null;
    }

    @Override
    public void startDriverHandshake() {
        log.debug("Starting driver handshake for sw {}", getStringId());
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;
        factory = this.factory();

        driverHandshakeComplete.set(true);
        log.debug("Driver handshake is complete");

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
    }

    @Override
    public void write(OFMessage msg) {
        channel.write(Collections.singletonList(msg));
    }

    @Override
    public void write(List<OFMessage> msgs) {
        channel.write(msgs);
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
                    TableType tid = TableType.values()[gotoTable.getTableId()
                            .getValue()];
                    newInstructions.add(gotoTable.createBuilder()
                            .setTableId(getTableId(tid)).build());
                } else {
                    newInstructions.add(i);
                }
            }
            builder.setTableId(getTableId(type));
            builder.setInstructions(newInstructions);
            OFMessage msgnew = builder.build();
            channel.write(Collections.singletonList(msgnew));
            log.trace("Installed {}", msgnew);

        } else {
            channel.write(Collections.singletonList(msg));
        }
    }

    @Override
    public TableType getTableType(TableId tid) {
        switch (tid.getValue()) {
        case TABLE_IPV4_UNICAST:
            return TableType.IP;
        case TABLE_MPLS:
            return TableType.MPLS;
        case TABLE_ACL:
            return TableType.ACL;
        case TABLE_VLAN:
            return TableType.VLAN;
        case TABLE_TMAC:
            return TableType.ETHER;
        default:
            log.error("Table type for Table id {} is not supported in the driver",
                      tid);
            return TableType.NONE;
        }
    }

    private TableId getTableId(TableType tableType) {
        switch (tableType) {
        case IP:
            return TableId.of(ipv4UnicastTableId);
        case MPLS:
            return TableId.of(mplsTableId);
        case ACL:
            return TableId.of(aclTableId);
        case VLAN:
            return TableId.of(vlanTableId);
        case ETHER:
            return TableId.of(tmacTableId);
        default: {
            log.error("Table type {} is not supported in the driver", tableType);
            return TableId.NONE;
        }
        }
    }

}
