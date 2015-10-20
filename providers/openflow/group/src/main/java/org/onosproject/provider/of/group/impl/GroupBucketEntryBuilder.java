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
package org.onosproject.provider.of.group.impl;

import com.google.common.collect.Lists;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.Lambda;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionCircuit;
import org.projectfloodlight.openflow.protocol.action.OFActionCopyTtlIn;
import org.projectfloodlight.openflow.protocol.action.OFActionCopyTtlOut;
import org.projectfloodlight.openflow.protocol.action.OFActionDecMplsTtl;
import org.projectfloodlight.openflow.protocol.action.OFActionDecNwTtl;
import org.projectfloodlight.openflow.protocol.action.OFActionExperimenter;
import org.projectfloodlight.openflow.protocol.action.OFActionGroup;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionPopMpls;
import org.projectfloodlight.openflow.protocol.action.OFActionPushMpls;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetVlanPcp;
import org.projectfloodlight.openflow.protocol.action.OFActionSetVlanVid;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOchSigidBasic;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U8;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/*
 * Builder for GroupBucketEntry.
 */
public class GroupBucketEntryBuilder {

    private List<OFBucket> ofBuckets;
    private OFGroupType type;

    private final Logger log = getLogger(getClass());

    /**
     * Creates a builder.
     *
     * @param ofBuckets list of OFBucket
     * @param type Group type
     */
    public GroupBucketEntryBuilder(List<OFBucket> ofBuckets, OFGroupType type) {
        this.ofBuckets = ofBuckets;
        this.type = type;
    }

    /**
     * Builds a GroupBuckets.
     *
     * @return GroupBuckets object, a list of GroupBuckets
     */
    public GroupBuckets build() {
        List<GroupBucket> bucketList = Lists.newArrayList();

        for (OFBucket bucket: ofBuckets) {
            TrafficTreatment treatment = buildTreatment(bucket.getActions());
            // TODO: Use GroupBucketEntry
            GroupBucket groupBucket = null;
            switch (type) {
                case INDIRECT:
                    groupBucket =
                            DefaultGroupBucket.createIndirectGroupBucket(treatment);
                    break;
                case SELECT:
                    groupBucket =
                            DefaultGroupBucket.createSelectGroupBucket(treatment);
                    break;
                case FF:
                    PortNumber port =
                            PortNumber.portNumber(bucket.getWatchPort().getPortNumber());
                    GroupId groupId =
                            new DefaultGroupId(bucket.getWatchGroup().getGroupNumber());
                    groupBucket =
                            DefaultGroupBucket.createFailoverGroupBucket(treatment,
                                    port, groupId);
                    break;
                case ALL:
                    groupBucket =
                            DefaultGroupBucket.createAllGroupBucket(treatment);
                    break;
                default:
                    log.error("Unsupported Group type : {}", type);
            }
            if (groupBucket != null) {
                bucketList.add(groupBucket);
            }
        }
        return new GroupBuckets(bucketList);
    }


    private TrafficTreatment buildTreatment(List<OFAction> actions) {
        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
        // If this is a drop rule
        if (actions.size() == 0) {
            builder.drop();
            return builder.build();
        }
        for (OFAction act : actions) {
            switch (act.getType()) {
                case OUTPUT:
                    OFActionOutput out = (OFActionOutput) act;
                    builder.setOutput(
                            PortNumber.portNumber(out.getPort().getPortNumber()));
                    break;
                case SET_VLAN_VID:
                    OFActionSetVlanVid vlan = (OFActionSetVlanVid) act;
                    builder.setVlanId(VlanId.vlanId(vlan.getVlanVid().getVlan()));
                    break;
                case SET_VLAN_PCP:
                    OFActionSetVlanPcp pcp = (OFActionSetVlanPcp) act;
                    builder.setVlanPcp(pcp.getVlanPcp().getValue());
                    break;
                case POP_VLAN:
                    builder.popVlan();
                    break;
                case PUSH_VLAN:
                    builder.pushVlan();
                    break;
                case SET_DL_DST:
                    OFActionSetDlDst dldst = (OFActionSetDlDst) act;
                    builder.setEthDst(
                            MacAddress.valueOf(dldst.getDlAddr().getLong()));
                    break;
                case SET_DL_SRC:
                    OFActionSetDlSrc dlsrc = (OFActionSetDlSrc) act;
                    builder.setEthSrc(
                            MacAddress.valueOf(dlsrc.getDlAddr().getLong()));

                    break;
                case SET_NW_DST:
                    OFActionSetNwDst nwdst = (OFActionSetNwDst) act;
                    IPv4Address di = nwdst.getNwAddr();
                    builder.setIpDst(Ip4Address.valueOf(di.getInt()));
                    break;
                case SET_NW_SRC:
                    OFActionSetNwSrc nwsrc = (OFActionSetNwSrc) act;
                    IPv4Address si = nwsrc.getNwAddr();
                    builder.setIpSrc(Ip4Address.valueOf(si.getInt()));
                    break;
                case EXPERIMENTER:
                    OFActionExperimenter exp = (OFActionExperimenter) act;
                    if (exp.getExperimenter() == 0x80005A06 ||
                            exp.getExperimenter() == 0x748771) {
                        OFActionCircuit ct = (OFActionCircuit) exp;
                        short lambda = ((OFOxmOchSigidBasic) ct.getField()).getValue().getChannelNumber();
                        builder.add(Instructions.modL0Lambda(Lambda.indexedLambda(lambda)));
                    } else {
                        log.warn("Unsupported OFActionExperimenter {}", exp.getExperimenter());
                    }
                    break;
                case SET_FIELD:
                    OFActionSetField setField = (OFActionSetField) act;
                    handleSetField(builder, setField.getField());
                    break;
                case POP_MPLS:
                    OFActionPopMpls popMpls = (OFActionPopMpls) act;
                    builder.popMpls((short) popMpls.getEthertype().getValue());
                    break;
                case PUSH_MPLS:
                    OFActionPushMpls pushMpls = (OFActionPushMpls) act;
                    builder.pushMpls();
                    break;
                case COPY_TTL_IN:
                    OFActionCopyTtlIn copyTtlIn = (OFActionCopyTtlIn) act;
                    builder.copyTtlIn();
                    break;
                case COPY_TTL_OUT:
                    OFActionCopyTtlOut copyTtlOut = (OFActionCopyTtlOut) act;
                    builder.copyTtlOut();
                    break;
                case DEC_MPLS_TTL:
                    OFActionDecMplsTtl decMplsTtl = (OFActionDecMplsTtl) act;
                    builder.decMplsTtl();
                    break;
                case DEC_NW_TTL:
                    OFActionDecNwTtl decNwTtl = (OFActionDecNwTtl) act;
                    builder.decNwTtl();
                    break;
                case GROUP:
                    OFActionGroup grp = (OFActionGroup) act;
                    builder.group(new DefaultGroupId(grp.getGroup().getGroupNumber()));
                    break;
                case SET_TP_DST:
                case SET_TP_SRC:
                case POP_PBB:
                case PUSH_PBB:
                case SET_MPLS_LABEL:
                case SET_MPLS_TC:
                case SET_MPLS_TTL:
                case SET_NW_ECN:
                case SET_NW_TOS:
                case SET_NW_TTL:
                case SET_QUEUE:
                case STRIP_VLAN:
                case ENQUEUE:
                default:
                    log.warn("Action type {} not yet implemented.", act.getType());
            }
        }

        return builder.build();
    }

    private void handleSetField(TrafficTreatment.Builder builder, OFOxm<?> oxm) {
        switch (oxm.getMatchField().id) {
            case VLAN_PCP:
                @SuppressWarnings("unchecked")
                OFOxm<VlanPcp> vlanpcp = (OFOxm<VlanPcp>) oxm;
                builder.setVlanPcp(vlanpcp.getValue().getValue());
                break;
            case VLAN_VID:
                @SuppressWarnings("unchecked")
                OFOxm<OFVlanVidMatch> vlanvid = (OFOxm<OFVlanVidMatch>) oxm;
                builder.setVlanId(VlanId.vlanId(vlanvid.getValue().getVlan()));
                break;
            case ETH_DST:
                @SuppressWarnings("unchecked")
                OFOxm<org.projectfloodlight.openflow.types.MacAddress> ethdst =
                        (OFOxm<org.projectfloodlight.openflow.types.MacAddress>) oxm;
                builder.setEthDst(MacAddress.valueOf(ethdst.getValue().getLong()));
                break;
            case ETH_SRC:
                @SuppressWarnings("unchecked")
                OFOxm<org.projectfloodlight.openflow.types.MacAddress> ethsrc =
                        (OFOxm<org.projectfloodlight.openflow.types.MacAddress>) oxm;
                builder.setEthSrc(MacAddress.valueOf(ethsrc.getValue().getLong()));
                break;
            case IPV4_DST:
                @SuppressWarnings("unchecked")
                OFOxm<IPv4Address> ip4dst = (OFOxm<IPv4Address>) oxm;
                builder.setIpDst(Ip4Address.valueOf(ip4dst.getValue().getInt()));
                break;
            case IPV4_SRC:
                @SuppressWarnings("unchecked")
                OFOxm<IPv4Address> ip4src = (OFOxm<IPv4Address>) oxm;
                builder.setIpSrc(Ip4Address.valueOf(ip4src.getValue().getInt()));
                break;
            case MPLS_LABEL:
                @SuppressWarnings("unchecked")
                OFOxm<U32> labelId = (OFOxm<U32>) oxm;
                builder.setMpls(MplsLabel.mplsLabel((int) labelId.getValue().getValue()));
                break;
            case MPLS_BOS:
                @SuppressWarnings("unchecked")
                OFOxm<U8> mplsBos = (OFOxm<U8>) oxm;
                builder.setMplsBos(mplsBos.getValue() == U8.ZERO ? false : true);
                break;
            case ARP_OP:
            case ARP_SHA:
            case ARP_SPA:
            case ARP_THA:
            case ARP_TPA:
            case BSN_EGR_PORT_GROUP_ID:
            case BSN_GLOBAL_VRF_ALLOWED:
            case BSN_IN_PORTS_128:
            case BSN_L3_DST_CLASS_ID:
            case BSN_L3_INTERFACE_CLASS_ID:
            case BSN_L3_SRC_CLASS_ID:
            case BSN_LAG_ID:
            case BSN_TCP_FLAGS:
            case BSN_UDF0:
            case BSN_UDF1:
            case BSN_UDF2:
            case BSN_UDF3:
            case BSN_UDF4:
            case BSN_UDF5:
            case BSN_UDF6:
            case BSN_UDF7:
            case BSN_VLAN_XLATE_PORT_GROUP_ID:
            case BSN_VRF:
            case ETH_TYPE:
            case ICMPV4_CODE:
            case ICMPV4_TYPE:
            case ICMPV6_CODE:
            case ICMPV6_TYPE:
            case IN_PHY_PORT:
            case IN_PORT:
            case IPV6_DST:
            case IPV6_FLABEL:
            case IPV6_ND_SLL:
            case IPV6_ND_TARGET:
            case IPV6_ND_TLL:
            case IPV6_SRC:
            case IP_DSCP:
            case IP_ECN:
            case IP_PROTO:
            case METADATA:
            case MPLS_TC:
            case OCH_SIGID:
            case OCH_SIGID_BASIC:
            case OCH_SIGTYPE:
            case OCH_SIGTYPE_BASIC:
            case SCTP_DST:
            case SCTP_SRC:
            case TCP_DST:
            case TCP_SRC:
            case TUNNEL_ID:
            case UDP_DST:
            case UDP_SRC:
            default:
                log.warn("Set field type {} not yet implemented.", oxm.getMatchField().id);
                break;
        }
    }
}
