/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.openflow.controller.ExtensionTreatmentInterpreter;
import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFGroupAdd;
import org.projectfloodlight.openflow.protocol.OFGroupDelete;
import org.projectfloodlight.openflow.protocol.OFGroupMod;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionGroup;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IPv6FlowLabel;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBooleanValue;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/*
 * Builder for GroupMod.
 */
public final class GroupModBuilder {

    private GroupBuckets buckets;
    private GroupId groupId;
    private GroupDescription.Type type;
    private OFFactory factory;
    private Long xid;
    private Optional<DriverService> driverService;

    private final Logger log = getLogger(getClass());

    private static final int OFPCML_NO_BUFFER = 0xffff;

    private GroupModBuilder(GroupBuckets buckets, GroupId groupId,
                             GroupDescription.Type type, OFFactory factory,
                             Optional<Long> xid) {
        this.buckets = buckets;
        this.groupId = groupId;
        this.type = type;
        this.factory = factory;
        this.xid = xid.orElse((long) 0);
    }

    private GroupModBuilder(GroupBuckets buckets, GroupId groupId,
                            GroupDescription.Type type, OFFactory factory,
                            Optional<Long> xid, Optional<DriverService> driverService) {
       this.buckets = buckets;
       this.groupId = groupId;
       this.type = type;
       this.factory = factory;
       this.xid = xid.orElse((long) 0);
       this.driverService = driverService;
   }
    /**
     * Creates a builder for GroupMod.
     *
     * @param buckets GroupBuckets object
     * @param groupId Group Id to create
     * @param type Group type
     * @param factory OFFactory object
     * @param xid transaction ID
     * @return GroupModBuilder object
     */
    public static GroupModBuilder builder(GroupBuckets buckets, GroupId groupId,
                                          GroupDescription.Type type, OFFactory factory,
                                          Optional<Long> xid) {

        return new GroupModBuilder(buckets, groupId, type, factory, xid);
    }

    /**
     * Creates a builder for GroupMod.
     *
     * @param buckets GroupBuckets object
     * @param groupId Group Id to create
     * @param type Group type
     * @param factory OFFactory object
     * @param xid transaction ID
     * @param driverService driver Service
     * @return GroupModBuilder object
     */
    public static GroupModBuilder builder(GroupBuckets buckets, GroupId groupId,
                                          GroupDescription.Type type, OFFactory factory,
                                          Optional<Long> xid, Optional<DriverService> driverService) {

        return new GroupModBuilder(buckets, groupId, type, factory, xid, driverService);
    }

    /**
     * Builds the GroupAdd OF message.
     *
     * @return GroupAdd OF message
     */
    public OFGroupAdd buildGroupAdd() {

        List<OFBucket> ofBuckets = new ArrayList<OFBucket>();
        for (GroupBucket bucket: buckets.buckets()) {
            List<OFAction> actions = buildActions(bucket.treatment());

            OFBucket.Builder bucketBuilder = factory.buildBucket();
            bucketBuilder.setActions(actions);
            if (type == GroupDescription.Type.SELECT) {
                bucketBuilder.setWeight(bucket.weight());
            }

            if (type == GroupDescription.Type.FAILOVER && bucket.watchPort() != null) {
                bucketBuilder.setWatchPort(OFPort.of((int) bucket.watchPort().toLong()));
            } else {
                bucketBuilder.setWatchPort(OFPort.ANY);
            }
            if (type == GroupDescription.Type.FAILOVER &&  bucket.watchGroup() != null) {
                bucketBuilder.setWatchGroup(OFGroup.of(bucket.watchGroup().id()));
            } else {
                bucketBuilder.setWatchGroup(OFGroup.ANY);
            }
            OFBucket ofBucket = bucketBuilder.build();
            ofBuckets.add(ofBucket);
        }

        OFGroupAdd groupMsg = factory.buildGroupAdd()
                .setGroup(OFGroup.of(groupId.id()))
                .setBuckets(ofBuckets)
                .setGroupType(getOFGroupType(type))
                .setXid(xid)
                .build();

        return groupMsg;
    }

    /**
     * Builds the GroupMod OF message.
     *
     * @return GroupMod OF message
     */
    public OFGroupMod buildGroupMod() {
        List<OFBucket> ofBuckets = new ArrayList<OFBucket>();
        for (GroupBucket bucket: buckets.buckets()) {
            List<OFAction> actions = buildActions(bucket.treatment());

            OFBucket.Builder bucketBuilder = factory.buildBucket();
            bucketBuilder.setActions(actions);
            if (type == GroupDescription.Type.SELECT) {
                bucketBuilder.setWeight(bucket.weight());
            }
            if (type == GroupDescription.Type.FAILOVER && bucket.watchPort() != null) {
                bucketBuilder.setWatchPort(OFPort.of((int) bucket.watchPort().toLong()));
            } else {
                bucketBuilder.setWatchPort(OFPort.ANY);
            }
            if (type == GroupDescription.Type.FAILOVER &&  bucket.watchGroup() != null) {
                bucketBuilder.setWatchGroup(OFGroup.of(bucket.watchGroup().id()));
            } else {
                bucketBuilder.setWatchGroup(OFGroup.ANY);
            }

            OFBucket ofBucket = bucketBuilder.build();
            ofBuckets.add(ofBucket);
        }

        OFGroupMod groupMsg = factory.buildGroupModify()
                .setGroup(OFGroup.of(groupId.id()))
                .setBuckets(ofBuckets)
                .setGroupType(getOFGroupType(type))
                .setXid(xid)
                .build();

        return groupMsg;
    }

    /**
     * Builds the GroupDel OF message.
     *
     * @return GroupDel OF message
     */
    public OFGroupDelete buildGroupDel() {

        OFGroupDelete groupMsg = factory.buildGroupDelete()
                .setGroup(OFGroup.of(groupId.id()))
                .setGroupType(OFGroupType.SELECT)
                .setXid(xid)
                .build();

        return groupMsg;
    }

    private List<OFAction> buildActions(TrafficTreatment treatment) {
        if (treatment == null) {
            return Collections.emptyList();
        }

        List<OFAction> actions = new LinkedList<>();
        for (Instruction i : treatment.allInstructions()) {
            switch (i.type()) {
                case L0MODIFICATION:
                    actions.add(buildL0Modification(i));
                    break;
                case L2MODIFICATION:
                    actions.add(buildL2Modification(i));
                    break;
                case L3MODIFICATION:
                    actions.add(buildL3Modification(i));
                    break;
                case OUTPUT:
                    Instructions.OutputInstruction out =
                            (Instructions.OutputInstruction) i;
                    OFActionOutput.Builder action = factory.actions().buildOutput()
                            .setPort(OFPort.of((int) out.port().toLong()));
                    if (out.port().equals(PortNumber.CONTROLLER)) {
                        action.setMaxLen(OFPCML_NO_BUFFER);
                    }
                    actions.add(action.build());
                    break;
                case GROUP:
                    Instructions.GroupInstruction grp =
                            (Instructions.GroupInstruction) i;
                    OFActionGroup.Builder actgrp = factory.actions().buildGroup()
                            .setGroup(OFGroup.of(grp.groupId().id()));
                    actions.add(actgrp.build());
                    break;
                case EXTENSION:
                    Instructions.ExtensionInstructionWrapper wrapper =
                    (Instructions.ExtensionInstructionWrapper) i;
                    actions.add(buildExtensionAction(
                            wrapper.extensionInstruction(), wrapper.deviceId()));
                    break;
                default:
                    log.warn("Instruction type {} not yet implemented.", i.type());
            }
        }

        return actions;
    }

    private OFAction buildL0Modification(Instruction i) {
        L0ModificationInstruction l0m = (L0ModificationInstruction) i;
        switch (l0m.subtype()) {
            default:
                log.warn("Unimplemented action type {}.", l0m.subtype());
                break;
        }
        return null;
    }

    private OFAction buildL2Modification(Instruction i) {
        L2ModificationInstruction l2m = (L2ModificationInstruction) i;
        L2ModificationInstruction.ModEtherInstruction eth;
        OFOxm<?> oxm = null;
        switch (l2m.subtype()) {
            case ETH_DST:
                eth = (L2ModificationInstruction.ModEtherInstruction) l2m;
                oxm = factory.oxms().ethDst(MacAddress.of(eth.mac().toLong()));
                break;
            case ETH_SRC:
                eth = (L2ModificationInstruction.ModEtherInstruction) l2m;
                oxm = factory.oxms().ethSrc(MacAddress.of(eth.mac().toLong()));
                break;
            case VLAN_ID:
                L2ModificationInstruction.ModVlanIdInstruction vlanId =
                        (L2ModificationInstruction.ModVlanIdInstruction) l2m;
                oxm = factory.oxms().vlanVid(OFVlanVidMatch.ofVlan(vlanId.vlanId().toShort()));
                break;
            case VLAN_PCP:
                L2ModificationInstruction.ModVlanPcpInstruction vlanPcp =
                        (L2ModificationInstruction.ModVlanPcpInstruction) l2m;
                oxm = factory.oxms().vlanPcp(VlanPcp.of(vlanPcp.vlanPcp()));
                break;
            case VLAN_POP:
                return factory.actions().popVlan();
            case VLAN_PUSH:
                L2ModificationInstruction.ModVlanHeaderInstruction pushVlanInstruction
                        = (L2ModificationInstruction.ModVlanHeaderInstruction) l2m;
                return factory.actions().pushVlan(
                        EthType.of(pushVlanInstruction.ethernetType().toShort()));
            case MPLS_PUSH:
                L2ModificationInstruction.ModMplsHeaderInstruction pushHeaderInstructions =
                        (L2ModificationInstruction.ModMplsHeaderInstruction) l2m;
                return factory.actions().pushMpls(EthType.of(pushHeaderInstructions
                                                             .ethernetType().toShort()));
            case MPLS_POP:
                L2ModificationInstruction.ModMplsHeaderInstruction popHeaderInstructions =
                        (L2ModificationInstruction.ModMplsHeaderInstruction) l2m;
                return factory.actions().popMpls(EthType.of(popHeaderInstructions
                                                            .ethernetType().toShort()));
            case MPLS_LABEL:
                L2ModificationInstruction.ModMplsLabelInstruction mplsLabel =
                        (L2ModificationInstruction.ModMplsLabelInstruction) l2m;
                oxm = factory.oxms().mplsLabel(U32.of(mplsLabel.label().toInt()));
                break;
            case MPLS_BOS:
                L2ModificationInstruction.ModMplsBosInstruction mplsBos =
                        (L2ModificationInstruction.ModMplsBosInstruction) l2m;
                oxm = factory.oxms()
                        .mplsBos(mplsBos.mplsBos() ? OFBooleanValue.TRUE
                                                   : OFBooleanValue.FALSE);
                break;
            case DEC_MPLS_TTL:
                return factory.actions().decMplsTtl();
            case TUNNEL_ID:
                L2ModificationInstruction.ModTunnelIdInstruction tunnelId =
                        (L2ModificationInstruction.ModTunnelIdInstruction) l2m;
                oxm = factory.oxms().tunnelId(U64.of(tunnelId.tunnelId()));
                break;
            default:
                log.warn("Unimplemented action type {}.", l2m.subtype());
                break;
        }

        if (oxm != null) {
            return factory.actions().buildSetField().setField(oxm).build();
        }
        return null;
    }

    private OFAction buildL3Modification(Instruction i) {
        L3ModificationInstruction l3m = (L3ModificationInstruction) i;
        L3ModificationInstruction.ModIPInstruction ip;
        Ip4Address ip4;
        Ip6Address ip6;
        OFOxm<?> oxm = null;
        switch (l3m.subtype()) {
            case IPV4_SRC:
                ip = (L3ModificationInstruction.ModIPInstruction) i;
                ip4 = ip.ip().getIp4Address();
                oxm = factory.oxms().ipv4Src(IPv4Address.of(ip4.toInt()));
                break;
            case IPV4_DST:
                ip = (L3ModificationInstruction.ModIPInstruction) i;
                ip4 = ip.ip().getIp4Address();
                oxm = factory.oxms().ipv4Dst(IPv4Address.of(ip4.toInt()));
                break;
            case IPV6_SRC:
                ip = (L3ModificationInstruction.ModIPInstruction) i;
                ip6 = ip.ip().getIp6Address();
                oxm = factory.oxms().ipv6Src(IPv6Address.of(ip6.toOctets()));
                break;
            case IPV6_DST:
                ip = (L3ModificationInstruction.ModIPInstruction) i;
                ip6 = ip.ip().getIp6Address();
                oxm = factory.oxms().ipv6Dst(IPv6Address.of(ip6.toOctets()));
                break;
            case IPV6_FLABEL:
                L3ModificationInstruction.ModIPv6FlowLabelInstruction flowLabelInstruction =
                    (L3ModificationInstruction.ModIPv6FlowLabelInstruction) i;
                int flowLabel = flowLabelInstruction.flowLabel();
                oxm = factory.oxms().ipv6Flabel(IPv6FlowLabel.of(flowLabel));
                break;
            case DEC_TTL:
                return factory.actions().decNwTtl();
            case TTL_IN:
                return factory.actions().copyTtlIn();
            case TTL_OUT:
                return factory.actions().copyTtlOut();
            default:
                log.warn("Unimplemented action type {}.", l3m.subtype());
                break;
        }

        if (oxm != null) {
            return factory.actions().buildSetField().setField(oxm).build();
        }
        return null;
    }

    private OFGroupType getOFGroupType(GroupDescription.Type groupType) {
        switch (groupType) {
            case INDIRECT:
                return OFGroupType.INDIRECT;
            case SELECT:
                return OFGroupType.SELECT;
            case FAILOVER:
                return OFGroupType.FF;
            case ALL:
                return OFGroupType.ALL;
            default:
                log.error("Unsupported group type : {}", groupType);
                break;
        }
        return null;
    }

    private OFAction buildExtensionAction(ExtensionTreatment i, DeviceId deviceId) {
        if (!driverService.isPresent()) {
            log.error("No driver service present");
            return null;
        }
        Driver driver = driverService.get().getDriver(deviceId);
        if (driver.hasBehaviour(ExtensionTreatmentInterpreter.class)) {
            DefaultDriverHandler handler =
                    new DefaultDriverHandler(new DefaultDriverData(driver, deviceId));
            ExtensionTreatmentInterpreter interpreter = handler.behaviour(ExtensionTreatmentInterpreter.class);
            return interpreter.mapInstruction(factory, i);
        }

        return null;
    }
}

