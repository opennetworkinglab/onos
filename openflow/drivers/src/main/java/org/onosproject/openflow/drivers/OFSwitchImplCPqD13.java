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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.RoleState;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFAsyncGetReply;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFGroupDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFGroupFeaturesStatsReply;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.OFMatchV3;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthDst;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthSrc;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthType;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmInPort;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv4DstMasked;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmMetadataMasked;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmMplsLabel;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanVid;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFMetadata;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.util.HexString;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Stanford University,
 * Ericsson Research and CPqD Research. Make (Hardware Desc.) : OpenFlow 1.3
 * Reference Userspace Switch Model (Datapath Desc.) : None Software : Serial :
 * None
 */
public class OFSwitchImplCPqD13 extends AbstractOpenFlowSwitch {

    private static final int VLAN_ID_OFFSET = 16;
    private final AtomicBoolean driverHandshakeComplete;
    private OFFactory factory;
    private static final int OFPCML_NO_BUFFER = 0xffff;
    // Configuration of asynch messages to controller. We need different
    // asynch messages depending on role-equal or role-master.
    // We don't want to get anything if we are slave.
    private static final long SET_FLOW_REMOVED_MASK_MASTER = 0xf;
    private static final long SET_PACKET_IN_MASK_MASTER = 0x7;
    private static final long SET_PORT_STATUS_MASK_MASTER = 0x7;
    private static final long SET_FLOW_REMOVED_MASK_EQUAL = 0x0;
    private static final long SET_PACKET_IN_MASK_EQUAL = 0x0;
    private static final long SET_PORT_STATUS_MASK_EQUAL = 0x7;
    private static final long SET_ALL_SLAVE = 0x0;

    private static final long TEST_FLOW_REMOVED_MASK = 0xf;
    private static final long TEST_PACKET_IN_MASK = 0x7;
    private static final long TEST_PORT_STATUS_MASK = 0x7;
    private long barrierXidToWaitFor = -1;

    private static final int TABLE_VLAN = 0;
    private static final int TABLE_TMAC = 1;
    private static final int TABLE_IPV4_UNICAST = 2;
    private static final int TABLE_MPLS = 3;
    private static final int TABLE_META = 4;
    private static final int TABLE_ACL = 5;

    private static final short MAX_PRIORITY = (short) 0xffff;
    private static final short SLASH_24_PRIORITY = (short) 0xfff0;
    private static final short MIN_PRIORITY = 0x0;
    private static final U64 METADATA_MASK = U64.of(Long.MAX_VALUE << 1 | 0x1);

    private final Map<Integer, OFGroup> l2groups;

    public OFSwitchImplCPqD13(Dpid dpid, OFDescStatsReply desc) {
        super(dpid);
        driverHandshakeComplete = new AtomicBoolean(false);
        l2groups = new ConcurrentHashMap<Integer, OFGroup>();
        setSwitchDescription(desc);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OFSwitchImplCPqD13 [" + ((channel != null)
                ? channel.getRemoteAddress() : "?")
                + " DPID[" + ((this.getStringId() != null) ? this.getStringId() : "?") + "]]";
    }

    @Override
    public void startDriverHandshake() {
        log.debug("Starting driver handshake for sw {}", getStringId());
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;
        factory = this.factory();

        sendBarrier(true);
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

        case GET_ASYNC_REPLY:
            OFAsyncGetReply asrep = (OFAsyncGetReply) m;
            decodeAsyncGetReply(asrep);
            break;
        case STATS_REPLY:
            processStatsReply((OFStatsReply) m);
            break;
        case PACKET_IN:
        case PORT_STATUS:
        case QUEUE_GET_CONFIG_REPLY:
        case ROLE_REPLY:
        case FEATURES_REPLY:
        case FLOW_REMOVED:
            break;

        default:
            log.debug("Received message {} during switch-driver subhandshake "
                    + "from switch {} ... Ignoring message", m, getStringId());

        }
    }

    private void configureSwitch() throws IOException {
        // setAsyncConfig();
        // getTableFeatures();
        sendGroupFeaturesRequest();
        setL2Groups();
        sendBarrier(false);
        setL3Groups();
        setL25Groups();
        sendGroupDescRequest();
        populateTableVlan();
        populateTableTMac();
        populateIpTable();
        populateMplsTable();
        populateTableMissEntry(TABLE_ACL, false, false, false, -1);
        sendBarrier(true);
    }

    private void setAsyncConfig() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>(3);
        OFMessage setAC = null;

        if (role == RoleState.MASTER) {
            setAC = factory.buildAsyncSet()
                    .setFlowRemovedMaskEqualMaster(SET_FLOW_REMOVED_MASK_MASTER)
                    .setPacketInMaskEqualMaster(SET_PACKET_IN_MASK_MASTER)
                    .setPortStatusMaskEqualMaster(SET_PORT_STATUS_MASK_MASTER)
                    .setFlowRemovedMaskSlave(SET_ALL_SLAVE)
                    .setPacketInMaskSlave(SET_ALL_SLAVE)
                    .setPortStatusMaskSlave(SET_ALL_SLAVE)
                    .setXid(getNextTransactionId())
                    .build();
        } else if (role == RoleState.EQUAL) {
            setAC = factory.buildAsyncSet()
                    .setFlowRemovedMaskEqualMaster(SET_FLOW_REMOVED_MASK_EQUAL)
                    .setPacketInMaskEqualMaster(SET_PACKET_IN_MASK_EQUAL)
                    .setPortStatusMaskEqualMaster(SET_PORT_STATUS_MASK_EQUAL)
                    .setFlowRemovedMaskSlave(SET_ALL_SLAVE)
                    .setPacketInMaskSlave(SET_ALL_SLAVE)
                    .setPortStatusMaskSlave(SET_ALL_SLAVE)
                    .setXid(getNextTransactionId())
                    .build();
        }
        msglist.add(setAC);

        OFMessage br = factory.buildBarrierRequest()
                .setXid(getNextTransactionId())
                .build();
        msglist.add(br);

        OFMessage getAC = factory.buildAsyncGetRequest()
                .setXid(getNextTransactionId())
                .build();
        msglist.add(getAC);

        sendMsg(msglist);
    }

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

    private void getTableFeatures() throws IOException {
        OFMessage gtf = factory.buildTableFeaturesStatsRequest()
                .setXid(getNextTransactionId())
                .build();
        sendMsg(gtf);
    }

    private void sendGroupFeaturesRequest() throws IOException {
        OFMessage gfr = factory.buildGroupFeaturesStatsRequest()
                .setXid(getNextTransactionId())
                .build();
        sendMsg(gfr);
    }

    private void sendGroupDescRequest() throws IOException {
        OFMessage gdr = factory.buildGroupDescStatsRequest()
                .setXid(getNextTransactionId())
                .build();
        sendMsg(gdr);
    }

    /*Create L2 interface groups for all physical ports
     Naming convention followed is the same as OF-DPA spec
     eg. port 1 with allowed vlan 10, is enveloped in group with id,
         0x0 00a 0001, where the uppermost 4 bits identify an L2 interface,
         the next 12 bits identify the vlan-id, and the lowermost 16 bits
         identify the port number.*/
    private void setL2Groups() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        for (OFPortDesc p : getPorts()) {
            int pnum = p.getPortNo().getPortNumber();
            int portVlan = getVlanConfig(pnum);
            if (U32.of(pnum).compareTo(U32.of(OFPort.MAX.getPortNumber())) < 1) {
                OFGroup gl2 = OFGroup.of(pnum | (portVlan << VLAN_ID_OFFSET));
                OFAction out = factory.actions().buildOutput()
                        .setPort(p.getPortNo()).build();
                OFAction popVlan = factory.actions().popVlan();
                List<OFAction> actions = new ArrayList<OFAction>();
                actions.add(popVlan);
                actions.add(out);
                OFBucket bucket = factory.buildBucket()
                        .setActions(actions).build();
                List<OFBucket> buckets = Collections.singletonList(bucket);
                OFMessage gmAdd = factory.buildGroupAdd()
                        .setGroup(gl2)
                        .setBuckets(buckets)
                        .setGroupType(OFGroupType.INDIRECT)
                        .setXid(getNextTransactionId())
                        .build();
                msglist.add(gmAdd);
                l2groups.put(pnum, gl2);
            }
        }
        log.debug("Creating {} L2 groups in sw {}", msglist.size(), getStringId());
        sendMsg(msglist);
    }

    private int getVlanConfig(int portnum) {
        int portVlan = 10 * portnum;
        if ((getId() == 0x1 && portnum == 6) ||
                (getId() == 0x2) ||
                (getId() == 0x3 && portnum == 2)) {
            portVlan = 192; // 0xc0
        }
        return portVlan;
    }

    private MacAddress getRouterMacAddr() {
        if (getId() == 0x3) {
            return MacAddress.of("00:00:07:07:07:80"); // router mac
        }
        if (getId() == 0x1) {
            return MacAddress.of("00:00:01:01:01:80");
        }
        // switch 0x2
        return MacAddress.of("00:00:02:02:02:80");
    }

    // only for ports connected to other routers
    private OFAction getDestAction(int portnum) {
        OFAction setDA = null;
        MacAddress dAddr = null;
        if (getId() == 0x1 && portnum == 6) { // connected to switch 2
            dAddr = MacAddress.of("00:00:02:02:02:80");
        }
        if (getId() == 0x2) {
            if (portnum == 1) { // connected to sw 1
                dAddr = MacAddress.of("00:00:01:01:01:80");
            } else if (portnum == 2) { // connected to sw 3
                dAddr = MacAddress.of("00:00:07:07:07:80");
            }
        }
        if (getId() == 0x3) {
            if (portnum == 2) { // connected to switch 2
                dAddr = MacAddress.of("00:00:02:02:02:80");
            }
        }

        if (dAddr != null) {
            OFOxmEthDst dstAddr = factory.oxms().ethDst(dAddr);
            setDA = factory.actions().buildSetField()
                    .setField(dstAddr).build();
        }
        return setDA;
    }

    /*
     * L3 groups are created for all router ports and they all point to corresponding
     * L2 groups. Only the ports that connect to other routers will have the
     * DA set.
     */
    private void setL3Groups() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        for (OFGroup gl2 : l2groups.values()) {
            int gnum = gl2.getGroupNumber();
            int portnum = gnum & 0x0000ffff;
            int vlanid = ((gnum & 0x0fff0000) >> VLAN_ID_OFFSET);
            MacAddress sAddr = getRouterMacAddr();

            OFGroup gl3 = OFGroup.of(0x20000000 | portnum);
            OFAction group = factory.actions().buildGroup()
                    .setGroup(gl2).build();
            OFOxmEthSrc srcAddr = factory.oxms().ethSrc(sAddr);
            OFAction setSA = factory.actions().buildSetField()
                    .setField(srcAddr).build();
            OFOxmVlanVid vid = factory.oxms().vlanVid(OFVlanVidMatch.ofVlan(vlanid));
            OFAction setVlan = factory.actions().buildSetField()
                    .setField(vid).build();
            OFAction decTtl = factory.actions().decNwTtl();

            List<OFAction> actions = new ArrayList<OFAction>();
            actions.add(decTtl); // decrement the IP TTL/do-checksum/check TTL
            // and MTU
            actions.add(setVlan); // set the vlan-id of the exit-port (and
            // l2group)
            actions.add(setSA); // set this routers mac address
            // make L3Unicast group setDA for known (configured) ports
            // that connect to other routers
            OFAction setDA = getDestAction(portnum);
            if (setDA != null) {
                actions.add(setDA);
            }
            actions.add(group);

            OFBucket bucket = factory.buildBucket()
                    .setActions(actions).build();
            List<OFBucket> buckets = Collections.singletonList(bucket);
            OFMessage gmAdd = factory.buildGroupAdd()
                    .setGroup(gl3)
                    .setBuckets(buckets)
                    .setGroupType(OFGroupType.INDIRECT)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(gmAdd);
        }
        sendMsg(msglist);
        log.debug("Creating {} L3 groups in sw {}", msglist.size(), getStringId());
    }

    /*
     * L2.5 or mpls-unicast groups are only created for those router ports
     * connected to other router ports. They differ from the corresponding
     * L3-unicast group only by the fact that they decrement the MPLS TTL
     * instead of the IP ttl
     */
    private void setL25Groups() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        for (OFGroup gl2 : l2groups.values()) {
            int gnum = gl2.getGroupNumber();
            int portnum = gnum & 0x0000ffff;
            int vlanid = ((gnum & 0x0fff0000) >> VLAN_ID_OFFSET);
            MacAddress sAddr = getRouterMacAddr();
            OFAction setDA = getDestAction(portnum);
            // setDA will only be non-null for ports connected to routers
            if (setDA != null) {
                OFGroup gl3 = OFGroup.of(0xa0000000 | portnum); // different id
                // for mpls
                // group
                OFAction group = factory.actions().buildGroup()
                        .setGroup(gl2).build();
                OFOxmEthSrc srcAddr = factory.oxms().ethSrc(sAddr);
                OFAction setSA = factory.actions().buildSetField()
                        .setField(srcAddr).build();
                OFOxmVlanVid vid = factory.oxms().vlanVid(OFVlanVidMatch.ofVlan(vlanid));
                OFAction setVlan = factory.actions().buildSetField()
                        .setField(vid).build();
                OFAction decMplsTtl = factory.actions().decMplsTtl();
                List<OFAction> actions = new ArrayList<OFAction>();
                actions.add(decMplsTtl); // decrement the MPLS
                // TTL/do-checksum/check TTL and MTU
                actions.add(setVlan); // set the vlan-id of the exit-port (and
                // l2group)
                actions.add(setSA); // set this routers mac address
                actions.add(setDA);
                actions.add(group);
                OFBucket bucket = factory.buildBucket()
                        .setActions(actions).build();
                List<OFBucket> buckets = Collections.singletonList(bucket);
                OFMessage gmAdd = factory.buildGroupAdd()
                        .setGroup(gl3)
                        .setBuckets(buckets)
                        .setGroupType(OFGroupType.INDIRECT)
                        .setXid(getNextTransactionId())
                        .build();
                msglist.add(gmAdd);
            }
        }
        sendMsg(msglist);
        log.debug("Creating {} MPLS groups in sw {}", msglist.size(), getStringId());
    }

    /* Using ECMP groups
     *
     * OFGroup group47 = OFGroup.of(47);
        OFAction outgroup1 = factory.actions()
                        .buildGroup()
                        .setGroup(group61)
                        .build();
        OFBucket buc47_1 = factory.buildBucket()
                        .setWeight(1)
                        .setActions(Collections.singletonList(outgroup1))
                        .build();
        OFAction outgroup2 = factory.actions()
                        .buildGroup()
                        .setGroup(group62)
                        .build();
        OFBucket buc47_2 = factory.buildBucket()
                        .setWeight(1)
                        .setActions(Collections.singletonList(outgroup2))
                        .build();
        List<OFBucket> buckets47 = new ArrayList<OFBucket>();
        buckets47.add(buc47_1);
        buckets47.add(buc47_2);
        OFMessage gmS12 = factory.buildGroupAdd()
                        .setGroup(group47)
                        .setBuckets(buckets47)
                        .setGroupType(OFGroupType.SELECT)
                        .setXid(getNextTransactionId())
                        .build();
        write(gmS12, null);     */

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

    private void processGroupFeatures(OFGroupFeaturesStatsReply gfsr) {
        log.info("Sw: {} Group Features {}", getStringId(), gfsr);
    }

    private void processGroupDesc(OFGroupDescStatsReply gdsr) {
        log.info("Sw: {} Group Desc {}", getStringId(), gdsr);
    }

    private void populateTableVlan() throws IOException {
        // for all incoming ports assign configured port-vlans
        // currently assign portnum*10 -> vlanid to access ports
        // and vlan 192 to router to router ports
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        for (OFPortDesc p : getPorts()) {
            int pnum = p.getPortNo().getPortNumber();
            if (U32.of(pnum).compareTo(U32.of(OFPort.MAX.getPortNumber())) < 1) {
                int vlanid = getVlanConfig(pnum);
                OFOxmInPort oxp = factory.oxms().inPort(p.getPortNo());
                OFOxmVlanVid oxv = factory.oxms()
                        .vlanVid(OFVlanVidMatch.UNTAGGED);
                OFOxmList oxmList = OFOxmList.of(oxp, oxv);
                OFMatchV3 match = factory.buildMatchV3()
                        .setOxmList(oxmList).build();
                OFOxmVlanVid vidToSet = factory.oxms()
                        .vlanVid(OFVlanVidMatch.ofVlan(vlanid));
                OFAction pushVlan = factory.actions().pushVlan(EthType.VLAN_FRAME);
                OFAction setVlan = factory.actions().setField(vidToSet);
                List<OFAction> actionlist = new ArrayList<OFAction>();
                actionlist.add(pushVlan);
                actionlist.add(setVlan);
                OFInstruction appAction = factory.instructions().buildApplyActions()
                        .setActions(actionlist).build();
                OFInstruction gotoTbl = factory.instructions().buildGotoTable()
                        .setTableId(TableId.of(TABLE_TMAC)).build();
                List<OFInstruction> instructions = new ArrayList<OFInstruction>();
                instructions.add(appAction);
                instructions.add(gotoTbl);
                OFMessage flowEntry = factory.buildFlowAdd()
                        .setTableId(TableId.of(TABLE_VLAN))
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
        // table-vlan has no table-miss entry, and so packets that miss are
        // essentially dropped
        sendMsg(msglist);
        log.debug("Adding {} vlan-rules in sw {}", msglist.size(), getStringId());
    }

    private void populateTableTMac() throws IOException {
        // match for ip packets
        OFOxmEthType oxe = factory.oxms().ethType(EthType.IPv4);
        OFOxmList oxmListIp = OFOxmList.of(oxe);
        OFMatchV3 matchIp = factory.buildMatchV3()
                .setOxmList(oxmListIp).build();
        OFInstruction gotoTblIp = factory.instructions().buildGotoTable()
                .setTableId(TableId.of(TABLE_IPV4_UNICAST)).build();
        List<OFInstruction> instructionsIp = Collections.singletonList(gotoTblIp);
        OFMessage ipEntry = factory.buildFlowAdd()
                .setTableId(TableId.of(TABLE_TMAC))
                .setMatch(matchIp)
                .setInstructions(instructionsIp)
                .setPriority(1000) // strict priority required lower than
                // multicastMac
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                .setXid(getNextTransactionId())
                .build();

        // match for mpls packets
        OFOxmEthType oxmpls = factory.oxms().ethType(EthType.MPLS_UNICAST);
        OFOxmList oxmListMpls = OFOxmList.of(oxmpls);
        OFMatchV3 matchMpls = factory.buildMatchV3()
                .setOxmList(oxmListMpls).build();
        OFInstruction gotoTblMpls = factory.instructions().buildGotoTable()
                .setTableId(TableId.of(TABLE_MPLS)).build();
        List<OFInstruction> instructionsMpls = Collections.singletonList(gotoTblMpls);
        OFMessage mplsEntry = factory.buildFlowAdd()
                .setTableId(TableId.of(TABLE_TMAC))
                .setMatch(matchMpls)
                .setInstructions(instructionsMpls)
                .setPriority(1001) // strict priority required lower than
                // multicastMac
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                .setXid(getNextTransactionId())
                .build();

        // match for everything else to send to controller. Essentially
        // the table miss flow entry
        populateTableMissEntry(TABLE_TMAC, true, false, false, -1);
        log.debug("Adding termination-mac-rules in sw {}", getStringId());
        List<OFMessage> msglist = new ArrayList<OFMessage>(2);
        msglist.add(ipEntry);
        msglist.add(mplsEntry);
        sendMsg(msglist);
    }

    private List<String> getMyIps() { // send to controller
        List<String> myIps = new ArrayList<String>();
        if (getId() == 0x1) {
            myIps.add("10.0.2.128");
            myIps.add("10.0.3.128");
            myIps.add("10.0.1.128");
            myIps.add("192.168.0.1");
        }
        if (getId() == 0x2) {
            myIps.add("192.168.0.2");
        }
        if (getId() == 0x3) {
            myIps.add("192.168.0.3");
            myIps.add("7.7.7.128");
        }
        return myIps;
    }

    private List<String> getMySubnetIps() { // send to controller
        List<String> subnetIps = new ArrayList<String>();
        if (getId() == 0x1) {
            subnetIps.add("10.0.2.0");
            subnetIps.add("10.0.3.0");
            subnetIps.add("10.0.1.0");
        }

        if (getId() == 0x3) {
            subnetIps.add("7.7.7.0");
        }
        return subnetIps;
    }

    private static class RouteEntry {
        String prefix;
        String mask;
        int nextHopPort;
        String dstMac;
        int label;

        public RouteEntry(String prefix, String mask, int nextHopPort, int label) {
            this.prefix = prefix;
            this.mask = mask;
            this.nextHopPort = nextHopPort;
            this.label = label;
        }

        public RouteEntry(String prefix, int nextHopPort, String dstMac) {
            this.prefix = prefix;
            this.nextHopPort = nextHopPort;
            this.dstMac = dstMac;
        }
    }

    // send out of mpls-group where the next-hop mac-da is already set
    private List<RouteEntry> getRouterNextHopIps() {
        List<RouteEntry> routerNextHopIps = new ArrayList<RouteEntry>();
        if (getId() == 0x1) {
            routerNextHopIps
            .add(new RouteEntry("192.168.0.2", "255.255.255.255", 6, 102));
            routerNextHopIps
            .add(new RouteEntry("192.168.0.3", "255.255.255.255", 6, 103));
            routerNextHopIps.add(new RouteEntry("7.7.7.0", "255.255.255.0", 6, 103));
        }
        //if (getId() == 0x2) {
        /* These are required for normal IP routing without labels.
            routerNextHopIps.add(new RouteEntry("192.168.0.1","255.255.255.255",1));
            routerNextHopIps.add(new RouteEntry("192.168.0.3","255.255.255.255",2));
            routerNextHopIps.add(new RouteEntry("10.0.1.0","255.255.255.0",1));
            routerNextHopIps.add(new RouteEntry("10.0.2.0","255.255.255.0",1));
            routerNextHopIps.add(new RouteEntry("10.0.3.0","255.255.255.0",1));
            routerNextHopIps.add(new RouteEntry("7.7.7.0","255.255.255.0",2));*/
        //}
        if (getId() == 0x3) {
            routerNextHopIps
            .add(new RouteEntry("192.168.0.2", "255.255.255.255", 2, 102));
            routerNextHopIps
            .add(new RouteEntry("192.168.0.1", "255.255.255.255", 2, 101));
            routerNextHopIps.add(new RouteEntry("10.0.1.0", "255.255.255.0", 2, 101));
            routerNextHopIps.add(new RouteEntry("10.0.2.0", "255.255.255.0", 2, 101));
            routerNextHopIps.add(new RouteEntry("10.0.3.0", "255.255.255.0", 2, 101));
        }
        return routerNextHopIps;
    }

    // known host mac-addr, setDA/send out of l3group
    private List<RouteEntry> getHostNextHopIps() {
        List<RouteEntry> hostNextHopIps = new ArrayList<RouteEntry>();
        if (getId() == 0x1) {
            hostNextHopIps.add(new RouteEntry("10.0.2.1", 4, "00:00:00:00:02:01"));
            hostNextHopIps.add(new RouteEntry("10.0.3.1", 5, "00:00:00:00:03:01"));
        }
        if (getId() == 0x3) {
            hostNextHopIps.add(new RouteEntry("7.7.7.7", 1, "00:00:07:07:07:07"));
        }
        return hostNextHopIps;
    }

    private void populateIpTable() throws IOException {
        populateMyIps();
        populateMySubnets();
        populateRoutes();
        populateHostRoutes();

        // match for everything else to send to ACL table. Essentially
        // the table miss flow entry
        populateTableMissEntry(TABLE_IPV4_UNICAST, false, true,
                true, TABLE_ACL);
    }

    private void populateMyIps() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        // first all my ip's as exact-matches
        // write-action instruction to send to controller
        List<String> myIps = getMyIps();
        for (int i = 0; i < myIps.size(); i++) {
            OFOxmEthType ethTypeIp = factory.oxms()
                    .ethType(EthType.IPv4);
            OFOxmIpv4DstMasked ipPrefix = factory.oxms()
                    .ipv4DstMasked(IPv4Address.of(myIps.get(i)), IPv4Address.NO_MASK);
            OFOxmList oxmListSlash32 = OFOxmList.of(ethTypeIp, ipPrefix);
            OFMatchV3 match = factory.buildMatchV3()
                    .setOxmList(oxmListSlash32).build();
            OFAction outc = factory.actions().buildOutput()
                    .setPort(OFPort.CONTROLLER).setMaxLen(OFPCML_NO_BUFFER)
                    .build();
            OFInstruction writeInstr = factory.instructions().buildWriteActions()
                    .setActions(Collections.singletonList(outc)).build();
            OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                    .setTableId(TableId.of(TABLE_ACL)).build();
            List<OFInstruction> instructions = new ArrayList<OFInstruction>();
            instructions.add(writeInstr);
            instructions.add(gotoInstr);
            OFMessage myIpEntry = factory.buildFlowAdd()
                    .setTableId(TableId.of(TABLE_IPV4_UNICAST))
                    .setMatch(match)
                    .setInstructions(instructions)
                    .setPriority(MAX_PRIORITY) // highest priority for exact
                    // match
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(myIpEntry);
        }
        sendMsg(msglist);
        log.debug("Adding {} my-ip-rules in sw {}", msglist.size(), getStringId());
    }

    private void populateMySubnets() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        // next prefix-based subnet-IP's configured on my interfaces
        // need to ARP for exact-IP, so write-action instruction to send to
        // controller
        // this has different mask and priority than earlier case
        List<String> subnetIps = getMySubnetIps();
        for (int i = 0; i < subnetIps.size(); i++) {
            OFOxmEthType ethTypeIp = factory.oxms()
                    .ethType(EthType.IPv4);
            OFOxmIpv4DstMasked ipPrefix = factory.oxms().ipv4DstMasked(
                    IPv4Address.of(subnetIps.get(i)),
                    IPv4Address.of(0xffffff00)); // '/24' mask
            OFOxmList oxmListSlash24 = OFOxmList.of(ethTypeIp, ipPrefix);
            OFMatchV3 match = factory.buildMatchV3()
                    .setOxmList(oxmListSlash24).build();
            OFAction outc = factory.actions().buildOutput()
                    .setPort(OFPort.CONTROLLER).setMaxLen(OFPCML_NO_BUFFER)
                    .build();
            OFInstruction writeInstr = factory.instructions().buildWriteActions()
                    .setActions(Collections.singletonList(outc)).build();
            OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                    .setTableId(TableId.of(TABLE_ACL)).build();
            List<OFInstruction> instructions = new ArrayList<OFInstruction>();
            instructions.add(writeInstr);
            instructions.add(gotoInstr);
            OFMessage myIpEntry = factory.buildFlowAdd()
                    .setTableId(TableId.of(TABLE_IPV4_UNICAST))
                    .setMatch(match)
                    .setInstructions(instructions)
                    .setPriority(SLASH_24_PRIORITY)
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(myIpEntry);
        }
        sendMsg(msglist);
        log.debug("Adding {} subnet-ip-rules in sw {}", msglist.size(), getStringId());
        msglist.clear();
    }

    private void populateRoutes() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        // addresses where I know the next-hop's mac-address because it is a
        // router port - so I have an L3 interface to it (and an MPLS interface)
        List<RouteEntry> routerNextHopIps = getRouterNextHopIps();
        for (int i = 0; i < routerNextHopIps.size(); i++) {
            OFOxmEthType ethTypeIp = factory.oxms()
                    .ethType(EthType.IPv4);
            OFOxmIpv4DstMasked ipPrefix = factory.oxms()
                    .ipv4DstMasked(
                            IPv4Address.of(routerNextHopIps.get(i).prefix),
                            IPv4Address.of(routerNextHopIps.get(i).mask)
                            );
            OFOxmList oxmListSlash32 = OFOxmList.of(ethTypeIp, ipPrefix);
            OFMatchV3 match = factory.buildMatchV3()
                    .setOxmList(oxmListSlash32).build();
            OFAction outg = factory.actions().buildGroup()
                    .setGroup(OFGroup.of(0xa0000000 | // mpls group id
                            routerNextHopIps.get(i).nextHopPort))
                            .build();
            // lots of actions before forwarding to mpls group, and
            // unfortunately
            // they need to be apply-actions

            OFAction pushlabel = factory.actions().pushMpls(EthType.MPLS_UNICAST);
            OFOxmMplsLabel l = factory.oxms()
                    .mplsLabel(U32.of(routerNextHopIps.get(i).label));
            OFAction setlabelid = factory.actions().buildSetField()
                    .setField(l).build();
            OFAction copyTtlOut = factory.actions().copyTtlOut();
            // OFAction setBos =
            // factory.actions().buildSetField().setField(bos).build();

            /*
            writeActions.add(pushlabel);  // need to be apply actions so can be
            writeActions.add(copyTtlOut); // matched in pseudo-table
            //writeActions.add(setlabelid); // bad support in cpqd
            //writeActions.add(setBos); no support in loxigen
             */

            List<OFAction> applyActions = new ArrayList<OFAction>();
            applyActions.add(pushlabel);
            applyActions.add(copyTtlOut);
            OFInstruction applyInstr = factory.instructions().buildApplyActions()
                    .setActions(applyActions).build();
            List<OFAction> writeActions = new ArrayList<OFAction>();
            writeActions.add(outg); // group will decr mpls-ttl, set mac-sa/da,
            // vlan
            OFInstruction writeInstr = factory.instructions().buildWriteActions()
                    .setActions(writeActions).build();

            // necessary to match in pseudo-table to overcome cpqd 1.3 flaw
            OFInstruction writeMeta = factory.instructions().buildWriteMetadata()
                    .setMetadata(U64.of(routerNextHopIps.get(i).label))
                    .setMetadataMask(METADATA_MASK).build();
            /*OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                        .setTableId(TableId.of(TABLE_ACL)).build();*/
            OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                    .setTableId(TableId.of(TABLE_META)).build();
            List<OFInstruction> instructions = new ArrayList<OFInstruction>();
            instructions.add(applyInstr);
            // instructions.add(writeInstr);// cannot write here - causes switch
            // to crash
            instructions.add(writeMeta);
            instructions.add(gotoInstr);

            int priority = -1;
            if (routerNextHopIps.get(i).mask.equals("255.255.255.255")) {
                priority = MAX_PRIORITY;
            } else {
                priority = SLASH_24_PRIORITY;
            }
            OFMessage myIpEntry = factory.buildFlowAdd()
                    .setTableId(TableId.of(TABLE_IPV4_UNICAST))
                    .setMatch(match)
                    .setInstructions(instructions)
                    .setPriority(priority)
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(myIpEntry);

            // need to also handle psuedo-table entries to match-metadata and
            // set mpls
            // label-id
            OFOxmEthType ethTypeMpls = factory.oxms()
                    .ethType(EthType.MPLS_UNICAST);
            OFOxmMetadataMasked meta = factory.oxms()
                    .metadataMasked(
                            OFMetadata.ofRaw(routerNextHopIps.get(i).label),
                            OFMetadata.NO_MASK);
            OFOxmList oxmListMeta = OFOxmList.of(ethTypeMpls, meta);
            OFMatchV3 matchMeta = factory.buildMatchV3()
                    .setOxmList(oxmListMeta).build();
            List<OFAction> writeActions2 = new ArrayList<OFAction>();
            writeActions2.add(setlabelid);
            OFAction outg2 = factory.actions().buildGroup()
                    .setGroup(OFGroup.of(routerNextHopIps.get(i).nextHopPort |
                            (192 << VLAN_ID_OFFSET)))
                            .build();
            writeActions2.add(outg2);
            OFInstruction writeInstr2 = factory.instructions().buildWriteActions()
                    .setActions(writeActions2).build();
            OFInstruction gotoInstr2 = factory.instructions().buildGotoTable()
                    .setTableId(TableId.of(TABLE_ACL)).build();
            List<OFInstruction> instructions2 = new ArrayList<OFInstruction>();
            // unfortunately have to apply this action too
            OFInstruction applyInstr2 = factory.instructions().buildApplyActions()
                    .setActions(writeActions2).build();
            instructions2.add(applyInstr2);
            // instructions2.add(writeInstr2);
            // instructions2.add(gotoInstr2);

            /*OFMatchV3 match3 = factory.buildMatchV3()
                        .setOxmList(OFOxmList.of(meta)).build();
            OFInstruction clearInstruction = factory.instructions().clearActions();
            List<OFInstruction> instructions3 = new ArrayList<OFInstruction>();
            OFAction outc = factory.actions().buildOutput()
                        .setPort(OFPort.CONTROLLER).setMaxLen(OFPCML_NO_BUFFER)
                        .build();
            OFInstruction writec = factory.instructions()
                        .writeActions(Collections.singletonList(outc));
            instructions3.add(clearInstruction);
            instructions3.add(writec);
            instructions3.add(gotoInstr2); */
            OFMessage myMetaEntry = factory.buildFlowAdd()
                    .setTableId(TableId.of(TABLE_META))
                    .setMatch(matchMeta)
                    .setInstructions(instructions2)
                    .setPriority(MAX_PRIORITY)
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(myMetaEntry);

        }
        sendMsg(msglist);
        log.debug("Adding {} next-hop-router-rules in sw {}", msglist.size(),
                getStringId());

        // add a table-miss entry to table 4 for debugging - leave it out
        // unclear packet state - causes switch to crash
        // populateTableMissEntry(TABLE_META, false, true,
        // true, TABLE_ACL);
    }

    private void populateHostRoutes() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        // addresses where I know the next hop's mac-address and I can set the
        // destination mac in the match-instruction.write-action
        // either I sent out arp-request or I got an arp-request from this host
        List<RouteEntry> hostNextHopIps = getHostNextHopIps();
        for (int i = 0; i < hostNextHopIps.size(); i++) {
            OFOxmEthType ethTypeIp = factory.oxms()
                    .ethType(EthType.IPv4);
            OFOxmIpv4DstMasked ipPrefix = factory.oxms()
                    .ipv4DstMasked(
                            IPv4Address.of(hostNextHopIps.get(i).prefix),
                            IPv4Address.NO_MASK); // host addr should be /32
            OFOxmList oxmListSlash32 = OFOxmList.of(ethTypeIp, ipPrefix);
            OFMatchV3 match = factory.buildMatchV3()
                    .setOxmList(oxmListSlash32).build();
            OFAction setDmac = null, outg = null;
            OFOxmEthDst dmac = factory.oxms()
                    .ethDst(MacAddress.of(hostNextHopIps.get(i).dstMac));
            setDmac = factory.actions().buildSetField()
                    .setField(dmac).build();
            outg = factory.actions().buildGroup()
                    .setGroup(OFGroup.of(0x20000000 | hostNextHopIps.get(i).nextHopPort)) // l3group
                    // id
                    .build();
            List<OFAction> writeActions = new ArrayList<OFAction>();
            writeActions.add(setDmac);
            writeActions.add(outg);
            OFInstruction writeInstr = factory.instructions().buildWriteActions()
                    .setActions(writeActions).build();
            OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                    .setTableId(TableId.of(TABLE_ACL)).build();
            List<OFInstruction> instructions = new ArrayList<OFInstruction>();
            instructions.add(writeInstr);
            instructions.add(gotoInstr);
            OFMessage myIpEntry = factory.buildFlowAdd()
                    .setTableId(TableId.of(TABLE_IPV4_UNICAST))
                    .setMatch(match)
                    .setInstructions(instructions)
                    .setPriority(MAX_PRIORITY) // highest priority for exact
                    // match
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(myIpEntry);
        }
        sendMsg(msglist);
        log.debug("Adding {} next-hop-host-rules in sw {}", msglist.size(), getStringId());
    }

    private static class MplsEntry {
        int labelid;
        int portnum;

        public MplsEntry(int labelid, int portnum) {
            this.labelid = labelid;
            this.portnum = portnum;
        }
    }

    private List<MplsEntry> getMplsEntries() {
        List<MplsEntry> myLabels = new ArrayList<MplsEntry>();
        if (getId() == 0x1) {
            myLabels.add(new MplsEntry(101, OFPort.CONTROLLER.getPortNumber()));
            myLabels.add(new MplsEntry(103, 6));
        }
        if (getId() == 0x2) {
            myLabels.add(new MplsEntry(103, 2));
            myLabels.add(new MplsEntry(102, OFPort.CONTROLLER.getPortNumber()));
            myLabels.add(new MplsEntry(101, 1));
        }
        if (getId() == 0x3) {
            myLabels.add(new MplsEntry(103, OFPort.CONTROLLER.getPortNumber()));
            myLabels.add(new MplsEntry(101, 2));
        }
        return myLabels;
    }

    private void populateMplsTable() throws IOException {
        List<OFMessage> msglist = new ArrayList<OFMessage>();
        List<MplsEntry> lfibEntries = getMplsEntries();
        for (int i = 0; i < lfibEntries.size(); i++) {
            OFOxmEthType ethTypeMpls = factory.oxms()
                    .ethType(EthType.MPLS_UNICAST);
            OFOxmMplsLabel labelid = factory.oxms()
                    .mplsLabel(U32.of(lfibEntries.get(i).labelid));
            OFOxmList oxmList = OFOxmList.of(ethTypeMpls, labelid);
            OFMatchV3 matchlabel = factory.buildMatchV3()
                    .setOxmList(oxmList).build();
            OFAction poplabel = factory.actions().popMpls(EthType.IPv4);
            OFAction sendTo = null;
            if (lfibEntries.get(i).portnum == OFPort.CONTROLLER.getPortNumber()) {
                sendTo = factory.actions().output(OFPort.CONTROLLER,
                        OFPCML_NO_BUFFER);
            } else {
                sendTo = factory.actions().group(OFGroup.of(
                        0xa0000000 | lfibEntries.get(i).portnum));
            }
            List<OFAction> writeActions = new ArrayList<OFAction>();
            writeActions.add(poplabel);
            writeActions.add(sendTo);
            OFInstruction writeInstr = factory.instructions().buildWriteActions()
                    .setActions(writeActions).build();
            OFInstruction gotoInstr = factory.instructions().buildGotoTable()
                    .setTableId(TableId.of(TABLE_ACL)).build();
            List<OFInstruction> instructions = new ArrayList<OFInstruction>();
            instructions.add(writeInstr);
            instructions.add(gotoInstr);
            OFMessage myMplsEntry = factory.buildFlowAdd()
                    .setTableId(TableId.of(TABLE_MPLS))
                    .setMatch(matchlabel)
                    .setInstructions(instructions)
                    .setPriority(MAX_PRIORITY) // exact match and exclusive
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setXid(getNextTransactionId())
                    .build();
            msglist.add(myMplsEntry);
        }
        sendMsg(msglist);
        log.debug("Adding {} mpls-forwarding-rules in sw {}", msglist.size(),
                getStringId());

        // match for everything else to send to ACL table. Essentially
        // the table miss flow entry
        populateTableMissEntry(TABLE_MPLS, false, true,
                true, TABLE_ACL);

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
     * @throws java.io.IOException
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
        this.channel.write(Collections.singletonList(msg));

    }

    @Override
    public void write(List<OFMessage> msgs) {
        this.channel.write(msgs);
    }

}
