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

import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.RoleState;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFAsyncGetReply;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFGroupDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFGroupFeaturesStatsReply;
import org.projectfloodlight.openflow.protocol.OFMatchV3;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthType;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmInPort;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanVid;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.util.HexString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class OFSwitchImplSpringOpenTTP extends AbstractOpenFlowSwitch {

    private OFFactory factory;

    private final AtomicBoolean driverHandshakeComplete;
    private AtomicBoolean haltStateMachine;

    private DriverState driverState;

    /* Default table ID - compatible with CpqD switch */
    private static final int TABLE_VLAN = 0;
    private static final int TABLE_TMAC = 1;
    private static final int TABLE_IPV4_UNICAST = 2;
    private static final int TABLE_MPLS = 3;
    private static final int TABLE_ACL = 5;

    private static final long TEST_FLOW_REMOVED_MASK = 0xf;
    private static final long TEST_PACKET_IN_MASK = 0x7;
    private static final long TEST_PORT_STATUS_MASK = 0x7;

    private static final int OFPCML_NO_BUFFER = 0xffff;

    private long barrierXidToWaitFor = -1;

    /* Set the default values. These variables will get
     * overwritten based on the switch vendor type
     */
    protected int vlanTableId = TABLE_VLAN;
    protected int tmacTableId = TABLE_TMAC;
    protected int ipv4UnicastTableId = TABLE_IPV4_UNICAST;
    protected int mplsTableId = TABLE_MPLS;
    protected int aclTableId = TABLE_ACL;

    /* priority values for OF message */
    private static final short MAX_PRIORITY = (short) 0xffff;
    private static final short PRIORITY_MULTIPLIER = (short) 2046;
    private static final short MIN_PRIORITY = 0x0;


    protected OFSwitchImplSpringOpenTTP(Dpid dpid, OFDescStatsReply desc) {
        super(dpid);
        driverHandshakeComplete = new AtomicBoolean(false);
        haltStateMachine = new AtomicBoolean(false);
        driverState = DriverState.INIT;
        setSwitchDescription(desc);
    }


    @Override
    public String toString() {
        return "OFSwitchImplSpringOpenTTP [" + ((channel != null)
                ? channel.getRemoteAddress() : "?")
                + " DPID[" + ((this.getStringId() != null) ?
                this.getStringId() : "?") + "]]";
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

        try {
            nextDriverState();
        } catch (IOException e) {
            log.error("Error {} during driver handshake for sw {}", e.getCause(),
                    getStringId());
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
        try {
            processOFMessage(m);
        } catch (IOException e) {
            log.error("Error generated when processing OFMessage {}", e.getCause());
        }
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
    public void sendMsg(OFMessage m, TableType tableType) {

        if (m.getType() == OFType.FLOW_MOD) {
            OFFlowMod flowMod = (OFFlowMod) m;
            OFFlowMod.Builder builder = flowMod.createBuilder();
            builder.setTableId(getTableId(tableType));
            OFFlowMod newFlowMod = builder.build();
            if (role == RoleState.MASTER) {
                this.write(newFlowMod);
            }
        } else {
            if (role == RoleState.MASTER) {
                this.write(m);
            }
        }
    }

    /*
     * Driver handshake state machine
     */

    enum DriverState {
        INIT,
        SET_TABLE_MISS_ENTRIES,
        SET_TABLE_VLAN_TMAC,
        AUDIT_GROUPS,
        SET_GROUPS,
        VERIFY_GROUPS,
        SET_ADJACENCY_LABELS,
        EXIT
    }

    protected void nextDriverState() throws IOException {
        DriverState currentState = driverState;
        if (haltStateMachine.get()) {
            return;
        }
        switch (currentState) {
            case INIT:
                driverState = DriverState.SET_TABLE_MISS_ENTRIES;
                setTableMissEntries();
                sendHandshakeBarrier();
                break;
            case SET_TABLE_MISS_ENTRIES:
                driverState = DriverState.SET_TABLE_VLAN_TMAC;
                /* TODO: read network configuration
                boolean isConfigured = getNetworkConfig();
                if (!isConfigured) {
                    return; // this will result in a handshake timeout
                }
                */
                populateTableVlan();
                populateTableTMac();
                sendHandshakeBarrier();
                break;
            case SET_TABLE_VLAN_TMAC:
                driverState = DriverState.EXIT;
                driverHandshakeComplete.set(true);
                log.debug("Driver handshake is complete");
                break;
            case EXIT:
            default:
                driverState = DriverState.EXIT;
                log.error("Driver handshake has exited for sw: {}", getStringId());
        }
    }

    private void processStatsReply(OFStatsReply sr) {
        switch (sr.getStatsType()) {
            case AGGREGATE:
                break;
            case DESC:
                break;
            case EXPERIMENTER:
                break;
            case FLOW:
                break;
            case GROUP_DESC:
                processGroupDesc((OFGroupDescStatsReply) sr);
                break;
            case GROUP_FEATURES:
                processGroupFeatures((OFGroupFeaturesStatsReply) sr);
                break;
            case METER_CONFIG:
                break;
            case METER_FEATURES:
                break;
            case PORT_DESC:
                break;
            case TABLE_FEATURES:
                break;
            default:
                break;

        }
    }

    private void processOFMessage(OFMessage m) throws IOException {
        switch (m.getType()) {
            case BARRIER_REPLY:
                processBarrierReply(m);
                break;

            case ERROR:
                processErrorMessage(m);
                break;

            case GET_ASYNC_REPLY:
                OFAsyncGetReply asrep = (OFAsyncGetReply) m;
                decodeAsyncGetReply(asrep);
                break;

            case PACKET_IN:
                // not ready to handle packet-ins
                break;

            case QUEUE_GET_CONFIG_REPLY:
                // not doing queue config yet
                break;

            case STATS_REPLY:
                processStatsReply((OFStatsReply) m);
                break;

            case ROLE_REPLY: // channelHandler should handle this
            case PORT_STATUS: // channelHandler should handle this
            case FEATURES_REPLY: // don't care
            case FLOW_REMOVED: // don't care
            default:
                log.debug("Received message {} during switch-driver subhandshake "
                        + "from switch {} ... Ignoring message", m, getStringId());
        }
    }

    private void processBarrierReply(OFMessage m) throws IOException {
        if (m.getXid() == barrierXidToWaitFor) {
            // Driver state-machine progresses to the next state.
            // If Barrier messages is not received, then eventually
            // the ChannelHandler state machine will timeout, and the switch
            // will be disconnected.
            nextDriverState();
        } else {
            log.error("Received incorrect barrier-message xid {} (expected: {}) in "
                            + "switch-driver state {} for switch {}", m, barrierXidToWaitFor,
                    driverState, getStringId());
        }
    }

    private void processErrorMessage(OFMessage m) {
        log.error("Switch {} Error {} in DriverState", getStringId(),
                (OFErrorMsg) m, driverState);
    }

    private void processGroupFeatures(OFGroupFeaturesStatsReply gfsr) {
        log.info("Sw: {} Group Features {}", getStringId(), gfsr);
    }

    private void processGroupDesc(OFGroupDescStatsReply gdsr) {
        log.info("Sw: {} Group Desc {}", getStringId(), gdsr);
    }

    /*
     * Utility functions
     */

    private void decodeAsyncGetReply(OFAsyncGetReply rep) {
        long frm = rep.getFlowRemovedMaskEqualMaster();
        //long frs = rep.getFlowRemovedMaskSlave();
        long pim = rep.getPacketInMaskEqualMaster();
        //long pis = rep.getPacketInMaskSlave();
        long psm = rep.getPortStatusMaskEqualMaster();
        //long pss = rep.getPortStatusMaskSlave();

        if (role == RoleState.MASTER || role == RoleState.EQUAL) { // should separate
            log.info("FRM:{}", HexString.toHexString((frm & TEST_FLOW_REMOVED_MASK)));
            log.info("PIM:{}", HexString.toHexString((pim & TEST_PACKET_IN_MASK)));
            log.info("PSM:{}", HexString.toHexString((psm & TEST_PORT_STATUS_MASK)));
        }
    }

    protected void setTableMissEntries() throws IOException {
        // set all table-miss-entries
        populateTableMissEntry(vlanTableId, true, false, false, -1);
        populateTableMissEntry(tmacTableId, true, false, false, -1);
        populateTableMissEntry(ipv4UnicastTableId, false, true, true,
                aclTableId);
        populateTableMissEntry(mplsTableId, false, true, true,
                aclTableId);
        populateTableMissEntry(aclTableId, false, false, false, -1);
        log.debug("TableMissEntries are set");
    }

    /**
     * Adds a table-miss-entry to a pipeline table.
     * <p>
     * The table-miss-entry can be added with 'write-actions' or
     * 'apply-actions'. It can also add a 'goto-table' instruction. By default
     * if none of the booleans in the call are set, then the table-miss entry is
     * added with no instructions, which means that if a packet hits the
     * table-miss-entry, pipeline execution will stop, and the action set
     * associated with the packet will be executed.
     *
     * @param tableToAdd the table to where the table-miss-entry will be added
     * @param toControllerNow as an APPLY_ACTION instruction
     * @param toControllerWrite as a WRITE_ACTION instruction
     * @param toTable as a GOTO_TABLE instruction
     * @param tableToSend the table to send as per the GOTO_TABLE instruction it
     *        needs to be set if 'toTable' is true. Ignored of 'toTable' is
     *        false.
     */
    protected void populateTableMissEntry(int tableToAdd, boolean toControllerNow,
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
            instructions = (List<OFInstruction>) Collections.EMPTY_LIST;
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

    private void populateTableVlan() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        for (OFPortDesc p : getPorts()) {
            int pnum = p.getPortNo().getPortNumber();
            if (U32.of(pnum).compareTo(U32.of(OFPort.MAX.getPortNumber())) < 1) {
                OFOxmInPort oxp = factory.oxms().inPort(p.getPortNo());
                OFOxmVlanVid oxv = factory.oxms()
                        .vlanVid(OFVlanVidMatch.UNTAGGED);
                OFOxmList oxmList = OFOxmList.of(oxp, oxv);
                OFMatchV3 match = factory.buildMatchV3()
                        .setOxmList(oxmList).build();

                // TODO: match on vlan-tagged packets for vlans configured on
                // subnet ports and strip-vlan

                OFInstruction gotoTbl = factory.instructions().buildGotoTable()
                        .setTableId(TableId.of(tmacTableId)).build();
                List<OFInstruction> instructions = new ArrayList<OFInstruction>();
                instructions.add(gotoTbl);
                OFMessage flowEntry = factory.buildFlowAdd()
                        .setTableId(TableId.of(vlanTableId))
                        .setMatch(match)
                        .setInstructions(instructions)
                        .setPriority(1000) // does not matter - all rules
                                // exclusive
                        .setBufferId(OFBufferId.NO_BUFFER)
                        .setIdleTimeout(0)
                        .setHardTimeout(0)
                        .setXid(getNextTransactionId())
                        .build();
                msglist.add(flowEntry);
            }
        }
        write(msglist);
        log.debug("Adding {} port/vlan-rules in sw {}", msglist.size(), getStringId());
    }

    private void populateTableTMac() throws IOException {
        // match for router-mac and ip-packets
        OFOxmEthType oxe = factory.oxms().ethType(EthType.IPv4);

        /* TODO: need to read network config and need to allow only
         the packets with DMAC as the correspondent router MAC address
         Until network configuration is implemented, all packets are allowed

        OFOxmEthDst dmac = factory.oxms().ethDst(getRouterMacAddr());
        OFOxmList oxmListIp = OFOxmList.of(dmac, oxe);
        OFMatchV3 matchIp = factory.buildMatchV3()
                .setOxmList(oxmListIp).build();
        */
        OFOxmList oxmList = OFOxmList.EMPTY;
        OFMatchV3 matchIp = factory.buildMatchV3()
                .setOxmList(oxmList)
                .build();

        OFInstruction gotoTblIp = factory.instructions().buildGotoTable()
                .setTableId(TableId.of(ipv4UnicastTableId)).build();
        List<OFInstruction> instructionsIp = Collections.singletonList(gotoTblIp);
        OFMessage ipEntry = factory.buildFlowAdd()
                .setTableId(TableId.of(tmacTableId))
                .setMatch(matchIp)
                .setInstructions(instructionsIp)
                .setPriority(1000) // strict priority required lower than
                        // multicastMac
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                .setXid(getNextTransactionId())
                .build();

        // match for router-mac and mpls packets
        OFOxmEthType oxmpls = factory.oxms().ethType(EthType.MPLS_UNICAST);
        /* TODO: need to read network config and need to allow only
         the packets with DMAC as the correspondent router MAC address
        OFOxmList oxmListMpls = OFOxmList.of(dmac, oxmpls);
        OFMatchV3 matchMpls = factory.buildMatchV3()
                .setOxmList(oxmListMpls).build();
        */
        OFOxmList oxmListMpls = OFOxmList.EMPTY;
        OFMatchV3 matchMpls = factory.buildMatchV3()
                .setOxmList(oxmList)
                .build();

        OFInstruction gotoTblMpls = factory.instructions().buildGotoTable()
                .setTableId(TableId.of(mplsTableId)).build();
        List<OFInstruction> instructionsMpls = Collections.singletonList(gotoTblMpls);
        OFMessage mplsEntry = factory.buildFlowAdd()
                .setTableId(TableId.of(tmacTableId))
                .setMatch(matchMpls)
                .setInstructions(instructionsMpls)
                .setPriority(1001) // strict priority required lower than
                        // multicastMac
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                .setXid(getNextTransactionId())
                .build();

        log.debug("Adding termination-mac-rules in sw {}", getStringId());
        List<OFMessage> msglist = new ArrayList<OFMessage>(2);
        msglist.add(ipEntry);
        msglist.add(mplsEntry);
        write(msglist);
    }

    private MacAddress getRouterMacAddr() {
        // TODO: need to read network config : RouterIp
        return MacAddress.of("00:00:00:00:00:00");
    }

    private TableId getTableId(TableType tableType) {
        switch (tableType) {
            case IP:
                return TableId.of(ipv4UnicastTableId);
            case MPLS:
                return TableId.of(mplsTableId);
            case ACL:
                return TableId.of(aclTableId);
            default: {
                log.error("Table type {} is not supported in the driver", tableType);
                return TableId.NONE;
            }
        }
    }

    private void sendHandshakeBarrier() throws IOException {
        long xid = getNextTransactionId();
        barrierXidToWaitFor = xid;
        OFBarrierRequest br = factory()
                .buildBarrierRequest()
                .setXid(xid)
                .build();
        write(br);
    }
}
