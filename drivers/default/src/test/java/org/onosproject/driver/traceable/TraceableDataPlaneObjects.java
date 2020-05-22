/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.driver.traceable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onlab.packet.VlanId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.driver.extensions.Ofdpa3MplsType;
import org.onosproject.driver.extensions.Ofdpa3SetMplsType;
import org.onosproject.driver.pipeline.ofdpa.OfdpaPipelineUtility;
import org.onosproject.driver.pipeline.ofdpa.OvsOfdpaPipeline;
import org.onosproject.net.DataPlaneEntity;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;

import java.util.List;

import static org.onlab.packet.EthType.EtherType.ARP;
import static org.onlab.packet.EthType.EtherType.IPV4;
import static org.onlab.packet.EthType.EtherType.LLDP;
import static org.onlab.packet.EthType.EtherType.MPLS_UNICAST;
import static org.onosproject.driver.traceable.TraceableTestObjects.*;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.ARP_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.ARP_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BRIDG_NOT_ORDERED_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BRIDG_NOT_ORDERED_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BRIDG_UNTAG_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BRIDG_UNTAG_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BROAD_EMPTY_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BROAD_EMPTY_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BROAD_UNTAG_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L2_BROAD_UNTAG_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L3_ECMP_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L3_ECMP_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L3_UCAST_UNTAG_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.L3_UCAST_UNTAG_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.MPLS_ECMP_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.MPLS_ECMP_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.PUNT_IP_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.PUNT_IP_OVS_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.PUNT_LLDP_OFDPA;
import static org.onosproject.driver.traceable.TraceableTestObjects.TraceableTest.PUNT_LLDP_OVS_OFDPA;

/**
 * Helper class for dataplane objects related to the Traceable tests.
 */
final class TraceableDataPlaneObjects {

    private TraceableDataPlaneObjects() {
        // Banning construction
    }

    // Groups
    private static final GroupId L2_FLOOD_GROUP_ID = GroupId.valueOf(0x40140000);

    private static final GroupId PUNT_GROUP_ID = GroupId.valueOf(OvsOfdpaPipeline.POP_VLAN_PUNT_GROUP_ID);
    private static final TrafficTreatment PUNT_BUCKET_TREATMENT = DefaultTrafficTreatment.builder()
            .popVlan()
            .punt()
            .build();
    private static final GroupBucket PUNT_BUCKET = DefaultGroupBucket.createIndirectGroupBucket(
            PUNT_BUCKET_TREATMENT);
    private static final GroupBuckets PUNT_BUCKETS = new GroupBuckets(ImmutableList.of(PUNT_BUCKET));
    private static final Group PUNT_GROUP = new DefaultGroup(PUNT_GROUP_ID, OFDPA_DEVICE,
            Group.Type.INDIRECT, PUNT_BUCKETS);

    private static final GroupId L2_IFACE_GROUP_ID = GroupId.valueOf(0x140000 | (int) OUT_PORT.toLong());
    private static final TrafficTreatment L2_IFACE_BUCKET_TREATMENT = DefaultTrafficTreatment.builder()
            .popVlan()
            .setOutput(OUT_PORT)
            .build();
    private static final GroupBucket L2_IFACE_BUCKET = DefaultGroupBucket.createIndirectGroupBucket(
            L2_IFACE_BUCKET_TREATMENT);
    private static final GroupBuckets L2_IFACE_BUCKETS = new GroupBuckets(ImmutableList.of(L2_IFACE_BUCKET));
    private static final Group L2_IFACE_GROUP = new DefaultGroup(L2_IFACE_GROUP_ID, OFDPA_DEVICE,
            Group.Type.INDIRECT, L2_IFACE_BUCKETS);

    private static final GroupId L2_IFACE_GROUP_ID_1 = GroupId.valueOf(0x140000 | (int) PORT.toLong());
    private static final TrafficTreatment L2_IFACE_BUCKET_TREATMENT_1 = DefaultTrafficTreatment.builder()
            .popVlan()
            .setOutput(PORT)
            .build();
    private static final GroupBucket L2_IFACE_BUCKET_1 = DefaultGroupBucket.createIndirectGroupBucket(
            L2_IFACE_BUCKET_TREATMENT_1);
    private static final GroupBuckets L2_IFACE_BUCKETS_1 = new GroupBuckets(ImmutableList.of(L2_IFACE_BUCKET_1));
    private static final Group L2_IFACE_GROUP_1 = new DefaultGroup(L2_IFACE_GROUP_ID_1, OFDPA_DEVICE,
            Group.Type.INDIRECT, L2_IFACE_BUCKETS_1);

    private static final GroupId L2_IFACE_GROUP_ID_2 = GroupId.valueOf(0xffe000 | (int) UP_PORT.toLong());
    private static final TrafficTreatment L2_IFACE_BUCKET_TREATMENT_2 = DefaultTrafficTreatment.builder()
            .popVlan()
            .setOutput(UP_PORT)
            .build();
    private static final GroupBucket L2_IFACE_BUCKET_2 = DefaultGroupBucket.createIndirectGroupBucket(
            L2_IFACE_BUCKET_TREATMENT_2);
    private static final GroupBuckets L2_IFACE_BUCKETS_2 = new GroupBuckets(ImmutableList.of(L2_IFACE_BUCKET_2));
    private static final Group L2_IFACE_GROUP_2 = new DefaultGroup(L2_IFACE_GROUP_ID_2, OFDPA_DEVICE,
            Group.Type.INDIRECT, L2_IFACE_BUCKETS_2);

    private static final GroupId L2_IFACE_GROUP_ID_3 = GroupId.valueOf(0xffe000 | (int) UP_PORT_1.toLong());
    private static final TrafficTreatment L2_IFACE_BUCKET_TREATMENT_3 = DefaultTrafficTreatment.builder()
            .popVlan()
            .setOutput(UP_PORT_1)
            .build();
    private static final GroupBucket L2_IFACE_BUCKET_3 = DefaultGroupBucket.createIndirectGroupBucket(
            L2_IFACE_BUCKET_TREATMENT_3);
    private static final GroupBuckets L2_IFACE_BUCKETS_3 = new GroupBuckets(ImmutableList.of(L2_IFACE_BUCKET_3));
    private static final Group L2_IFACE_GROUP_3 = new DefaultGroup(L2_IFACE_GROUP_ID_3, OFDPA_DEVICE,
            Group.Type.INDIRECT, L2_IFACE_BUCKETS_3);

    private static final GroupId L2_IFACE_GROUP_ID_NOT_ORDERED = GroupId.valueOf(0x140000 | (int) OUT_PORT.toLong());
    private static final TrafficTreatment L2_IFACE_BUCKET_TREATMENT_NOT_ORDERED = DefaultTrafficTreatment.builder()
            .setOutput(OUT_PORT)
            .popVlan()
            .build();
    private static final GroupBucket L2_IFACE_BUCKET_NOT_ORDERED = DefaultGroupBucket.createIndirectGroupBucket(
            L2_IFACE_BUCKET_TREATMENT_NOT_ORDERED);
    private static final GroupBuckets L2_IFACE_BUCKETS_NOT_ORDERED = new GroupBuckets(ImmutableList.of(
            L2_IFACE_BUCKET_NOT_ORDERED));
    private static final Group L2_IFACE_GROUP_NOT_ORDERED = new DefaultGroup(L2_IFACE_GROUP_ID_NOT_ORDERED,
            OFDPA_DEVICE, Group.Type.INDIRECT, L2_IFACE_BUCKETS_NOT_ORDERED);

    private static final TrafficTreatment L2_FLOOD_BUCKET_TREATMENT = DefaultTrafficTreatment.builder()
            .group(L2_IFACE_GROUP_ID)
            .build();
    private static final GroupBucket L2_FLOOD_BUCKET = DefaultGroupBucket.createAllGroupBucket(
            L2_FLOOD_BUCKET_TREATMENT);
    private static final TrafficTreatment L2_FLOOD_BUCKET_TREATMENT_1 = DefaultTrafficTreatment.builder()
            .group(L2_IFACE_GROUP_ID_1)
            .build();
    private static final GroupBucket L2_FLOOD_BUCKET_1 = DefaultGroupBucket.createAllGroupBucket(
            L2_FLOOD_BUCKET_TREATMENT_1);
    private static final GroupBuckets L2_FLOOD_BUCKETS = new GroupBuckets(ImmutableList.of(
            L2_FLOOD_BUCKET, L2_FLOOD_BUCKET_1));
    private static final Group L2_FLOOD_GROUP = new DefaultGroup(L2_FLOOD_GROUP_ID, OFDPA_DEVICE,
            Group.Type.ALL, L2_FLOOD_BUCKETS);

    private static final GroupBuckets L2_FLOOD_EMPTY_BUCKETS = new GroupBuckets(ImmutableList.of());
    private static final Group L2_FLOOD_EMPTY_GROUP = new DefaultGroup(L2_FLOOD_GROUP_ID, OFDPA_DEVICE,
            Group.Type.ALL, L2_FLOOD_EMPTY_BUCKETS);

    private static final GroupId L3_UCAST_GROUP_ID = GroupId.valueOf(0x20000026);
    private static final TrafficTreatment L3_UCAST_BUCKET_TREATMENT = DefaultTrafficTreatment.builder()
            .setEthSrc(LEAF_MAC)
            .setEthDst(HOST_MAC)
            .setVlanId(HOST_VLAN)
            .group(L2_IFACE_GROUP_ID)
            .build();
    private static final GroupBucket L3_UCAST_BUCKET = DefaultGroupBucket.createIndirectGroupBucket(
            L3_UCAST_BUCKET_TREATMENT);
    private static final GroupBuckets L3_UCAST_BUCKETS = new GroupBuckets(ImmutableList.of(L3_UCAST_BUCKET));
    private static final Group L3_UCAST_GROUP = new DefaultGroup(L3_UCAST_GROUP_ID, OFDPA_DEVICE,
            Group.Type.INDIRECT, L3_UCAST_BUCKETS);

    private static final GroupId L3_UCAST_GROUP_ID_1 = GroupId.valueOf(0x20000027);
    private static final TrafficTreatment L3_UCAST_BUCKET_TREATMENT_1 = DefaultTrafficTreatment.builder()
            .setEthSrc(LEAF_MAC)
            .setEthDst(SPINE_MAC)
            .setVlanId(DEFAULT_VLAN)
            .group(L2_IFACE_GROUP_ID_3)
            .build();
    private static final GroupBucket L3_UCAST_BUCKET_1 = DefaultGroupBucket.createIndirectGroupBucket(
            L3_UCAST_BUCKET_TREATMENT_1);
    private static final GroupBuckets L3_UCAST_BUCKETS_1 = new GroupBuckets(ImmutableList.of(L3_UCAST_BUCKET_1));
    private static final Group L3_UCAST_GROUP_1 = new DefaultGroup(L3_UCAST_GROUP_ID_1, OFDPA_DEVICE,
            Group.Type.INDIRECT, L3_UCAST_BUCKETS_1);

    private static final GroupId MPLS_IFACE_GROUP_ID = GroupId.valueOf(0x9000000c);
    private static final TrafficTreatment MPLS_IFACE_BUCKET_TREATMENT = DefaultTrafficTreatment.builder()
            .setEthSrc(LEAF_MAC)
            .setEthDst(SPINE_MAC)
            .setVlanId(DEFAULT_VLAN)
            .group(L2_IFACE_GROUP_ID_2)
            .build();
    private static final GroupBucket MPLS_IFACE_BUCKET = DefaultGroupBucket.createIndirectGroupBucket(
            MPLS_IFACE_BUCKET_TREATMENT);
    private static final GroupBuckets MPLS_IFACE_BUCKETS = new GroupBuckets(ImmutableList.of(MPLS_IFACE_BUCKET));
    private static final Group MPLS_IFACE_GROUP = new DefaultGroup(MPLS_IFACE_GROUP_ID, OFDPA_DEVICE,
            Group.Type.INDIRECT, MPLS_IFACE_BUCKETS);

    private static final GroupId MPLS_IFACE_GROUP_ID_1 = GroupId.valueOf(0x9000000d);
    private static final TrafficTreatment MPLS_IFACE_BUCKET_TREATMENT_1 = DefaultTrafficTreatment.builder()
            .setEthSrc(LEAF_MAC)
            .setEthDst(SPINE_MAC_1)
            .setVlanId(DEFAULT_VLAN)
            .group(L2_IFACE_GROUP_ID_3)
            .build();
    private static final GroupBucket MPLS_IFACE_BUCKET_1 = DefaultGroupBucket.createIndirectGroupBucket(
            MPLS_IFACE_BUCKET_TREATMENT_1);
    private static final GroupBuckets MPLS_IFACE_BUCKETS_1 = new GroupBuckets(ImmutableList.of(MPLS_IFACE_BUCKET_1));
    private static final Group MPLS_IFACE_GROUP_1 = new DefaultGroup(MPLS_IFACE_GROUP_ID_1, OFDPA_DEVICE,
            Group.Type.INDIRECT, MPLS_IFACE_BUCKETS_1);

    private static final GroupId MPLS_L3VPN_GROUP_ID = GroupId.valueOf(0x9200000d);
    private static final TrafficTreatment MPLS_L3VPN_BUCKET_TREATMENT = DefaultTrafficTreatment.builder()
            .popVlan()
            .pushMpls()
            .setMpls(MPLS_LABEL)
            .group(MPLS_IFACE_GROUP_ID)
            .pushVlan()
            .setVlanId(VlanId.vlanId(VlanId.RESERVED))
            .build();
    private static final GroupBucket MPLS_L3VPN_BUCKET = DefaultGroupBucket.createIndirectGroupBucket(
            MPLS_L3VPN_BUCKET_TREATMENT);
    private static final GroupBuckets MPLS_L3VPN_BUCKETS = new GroupBuckets(ImmutableList.of(MPLS_L3VPN_BUCKET));
    private static final Group MPLS_L3VPN_GROUP = new DefaultGroup(MPLS_L3VPN_GROUP_ID, OFDPA_DEVICE,
            Group.Type.INDIRECT, MPLS_L3VPN_BUCKETS);

    private static final GroupId MPLS_L3VPN_GROUP_ID_1 = GroupId.valueOf(0x9200000e);
    private static final TrafficTreatment MPLS_L3VPN_BUCKET_TREATMENT_1 = DefaultTrafficTreatment.builder()
            .popVlan()
            .pushMpls()
            .setMpls(MPLS_LABEL)
            .group(MPLS_IFACE_GROUP_ID_1)
            .pushVlan()
            .setVlanId(VlanId.vlanId(VlanId.RESERVED))
            .build();
    private static final GroupBucket MPLS_L3VPN_BUCKET_1 = DefaultGroupBucket.createIndirectGroupBucket(
            MPLS_L3VPN_BUCKET_TREATMENT_1);
    private static final GroupBuckets MPLS_L3VPN_BUCKETS_1 = new GroupBuckets(ImmutableList.of(MPLS_L3VPN_BUCKET_1));
    private static final Group MPLS_L3VPN_GROUP_1 = new DefaultGroup(MPLS_L3VPN_GROUP_ID_1, OFDPA_DEVICE,
            Group.Type.INDIRECT, MPLS_L3VPN_BUCKETS_1);

    private static final TrafficTreatment MPLS_L3VPN_OFDPA_BUCKET_TREATMENT = DefaultTrafficTreatment.builder()
            .pushMpls()
            .setMpls(MPLS_LABEL)
            .group(MPLS_IFACE_GROUP_ID)
            .copyTtlOut()
            .setMplsBos(true)
            .build();
    private static final GroupBucket MPLS_L3VPN_OFDPA_BUCKET = DefaultGroupBucket.createIndirectGroupBucket(
            MPLS_L3VPN_OFDPA_BUCKET_TREATMENT);
    private static final GroupBuckets MPLS_L3VPN_OFDPA_BUCKETS = new GroupBuckets(ImmutableList.of(
            MPLS_L3VPN_OFDPA_BUCKET));
    private static final Group MPLS_L3VPN_OFDPA_GROUP = new DefaultGroup(MPLS_L3VPN_GROUP_ID, OFDPA_DEVICE,
            Group.Type.INDIRECT, MPLS_L3VPN_OFDPA_BUCKETS);

    private static final TrafficTreatment MPLS_L3VPN_OFDPA_BUCKET_TREATMENT_1 = DefaultTrafficTreatment.builder()
            .pushMpls()
            .setMpls(MPLS_LABEL)
            .group(MPLS_IFACE_GROUP_ID_1)
            .copyTtlOut()
            .setMplsBos(true)
            .build();
    private static final GroupBucket MPLS_L3VPN_OFDPA_BUCKET_1 = DefaultGroupBucket.createIndirectGroupBucket(
            MPLS_L3VPN_OFDPA_BUCKET_TREATMENT_1);
    private static final GroupBuckets MPLS_L3VPN_OFDPA_BUCKETS_1 = new GroupBuckets(ImmutableList.of(
            MPLS_L3VPN_OFDPA_BUCKET_1));
    private static final Group MPLS_L3VPN_OFDPA_GROUP_1 = new DefaultGroup(MPLS_L3VPN_GROUP_ID_1, OFDPA_DEVICE,
            Group.Type.INDIRECT, MPLS_L3VPN_OFDPA_BUCKETS_1);

    private static final GroupId L3_ECMP_GROUP_ID = GroupId.valueOf(0x7000000e);
    private static final TrafficTreatment L3_ECMP_BUCKET_TREATMENT = DefaultTrafficTreatment.builder()
            .group(MPLS_L3VPN_GROUP_ID)
            .build();
    private static final GroupBucket L3_ECMP_BUCKET = DefaultGroupBucket.createSelectGroupBucket(
            L3_ECMP_BUCKET_TREATMENT);
    private static final TrafficTreatment L3_ECMP_BUCKET_TREATMENT_1 = DefaultTrafficTreatment.builder()
            .group(MPLS_L3VPN_GROUP_ID_1)
            .build();
    private static final GroupBucket L3_ECMP_BUCKET_1 = DefaultGroupBucket.createSelectGroupBucket(
            L3_ECMP_BUCKET_TREATMENT_1);
    private static final GroupBuckets L3_ECMP_BUCKETS = new GroupBuckets(ImmutableList.of(L3_ECMP_BUCKET,
            L3_ECMP_BUCKET_1));
    private static final Group L3_ECMP_GROUP = new DefaultGroup(L3_ECMP_GROUP_ID, OFDPA_DEVICE,
            Group.Type.SELECT, L3_ECMP_BUCKETS);

    private static final TrafficTreatment L3_ECMP_OFDPA_BUCKET_TREATMENT = DefaultTrafficTreatment.builder()
            .group(MPLS_L3VPN_GROUP_ID)
            .build();
    private static final GroupBucket L3_ECMP_OFDPA_BUCKET = DefaultGroupBucket.createSelectGroupBucket(
            L3_ECMP_OFDPA_BUCKET_TREATMENT);
    private static final TrafficTreatment L3_ECMP_OFDPA_BUCKET_TREATMENT_1 = DefaultTrafficTreatment.builder()
            .group(MPLS_L3VPN_GROUP_ID_1)
            .build();
    private static final GroupBucket L3_ECMP_OFDPA_BUCKET_1 = DefaultGroupBucket.createSelectGroupBucket(
            L3_ECMP_OFDPA_BUCKET_TREATMENT_1);
    private static final GroupBuckets L3_ECMP_OFDPA_BUCKETS = new GroupBuckets(ImmutableList.of(L3_ECMP_OFDPA_BUCKET,
            L3_ECMP_OFDPA_BUCKET_1));
    private static final Group L3_ECMP_OFDPA_GROUP = new DefaultGroup(L3_ECMP_GROUP_ID, OFDPA_DEVICE,
            Group.Type.SELECT, L3_ECMP_OFDPA_BUCKETS);

    private static final GroupId MPLS_ECMP_GROUP_ID = GroupId.valueOf(0x7000000f);
    private static final TrafficTreatment MPLS_ECMP_BUCKET_TREATMENT = DefaultTrafficTreatment.builder()
            .group(L3_UCAST_GROUP_ID_1)
            .build();
    private static final GroupBucket MPLS_ECMP_BUCKET = DefaultGroupBucket.createSelectGroupBucket(
            MPLS_ECMP_BUCKET_TREATMENT);
    private static final GroupBuckets MPLS_ECMP_BUCKETS = new GroupBuckets(ImmutableList.of(MPLS_ECMP_BUCKET));
    private static final Group MPLS_ECMP_GROUP = new DefaultGroup(MPLS_ECMP_GROUP_ID, OFDPA_DEVICE,
            Group.Type.SELECT, MPLS_ECMP_BUCKETS);

    // Flows
    private static final TrafficSelector EMPTY_SELECTOR = DefaultTrafficSelector.emptySelector();
    private static final TrafficTreatment EMPTY_TREATMENT = DefaultTrafficTreatment.emptyTreatment();

    private static final TrafficTreatment TABLE_0_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .transition(OfdpaPipelineUtility.VLAN_TABLE)
            .build();
    private static final FlowRule TABLE_0_MISS_OVS = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.PORT_TABLE)
            .withPriority(0)
            .withSelector(EMPTY_SELECTOR)
            .withTreatment(TABLE_0_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_0_MISS_FLOW_ENTRY_OVS = new DefaultFlowEntry(TABLE_0_MISS_OVS);

    private static final TrafficSelector TABLE_10_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PORT)
            .matchVlanId(VlanId.NONE)
            .build();
    private static final TrafficTreatment TABLE_10_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .pushVlan()
            .setVlanId(HOST_VLAN)
            .transition(OfdpaPipelineUtility.TMAC_TABLE)
            .build();
    private static final FlowRule TABLE_10_FLOW = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.VLAN_TABLE)
            .withPriority(32768)
            .withSelector(TABLE_10_FLOW_SELECTOR)
            .withTreatment(TABLE_10_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_10_FLOW_ENTRY = new DefaultFlowEntry(TABLE_10_FLOW);

    private static final TrafficTreatment TABLE_10_FLOW_TREATMENT_1 = DefaultTrafficTreatment.builder()
            .setVlanId(HOST_VLAN)
            .transition(OfdpaPipelineUtility.TMAC_TABLE)
            .build();
    private static final FlowRule TABLE_10_FLOW_1 = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.VLAN_TABLE)
            .withPriority(32768)
            .withSelector(TABLE_10_FLOW_SELECTOR)
            .withTreatment(TABLE_10_FLOW_TREATMENT_1)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_10_FLOW_ENTRY_1 = new DefaultFlowEntry(TABLE_10_FLOW_1);

    private static final TrafficSelector TABLE_10_FLOW_SELECTOR_2 = DefaultTrafficSelector.builder()
            .matchInPort(PORT)
            .matchVlanId(HOST_VLAN)
            .build();
    private static final TrafficTreatment TABLE_10_FLOW_TREATMENT_2 = DefaultTrafficTreatment.builder()
            .transition(OfdpaPipelineUtility.TMAC_TABLE)
            .build();
    private static final FlowRule TABLE_10_FLOW_2 = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.VLAN_TABLE)
            .withPriority(32768)
            .withSelector(TABLE_10_FLOW_SELECTOR_2)
            .withTreatment(TABLE_10_FLOW_TREATMENT_2)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_10_FLOW_ENTRY_2 = new DefaultFlowEntry(TABLE_10_FLOW_2);

    private static final TrafficSelector TABLE_10_DEFAULT_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(UP_PORT)
            .matchVlanId(VlanId.NONE)
            .build();
    private static final TrafficTreatment TABLE_10_DEFAULT_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .pushVlan()
            .setVlanId(DEFAULT_VLAN)
            .transition(OfdpaPipelineUtility.TMAC_TABLE)
            .build();
    private static final FlowRule TABLE_10_DEFAULT_FLOW = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.VLAN_TABLE)
            .withPriority(32768)
            .withSelector(TABLE_10_DEFAULT_FLOW_SELECTOR)
            .withTreatment(TABLE_10_DEFAULT_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_10_DEFAULT_FLOW_ENTRY = new DefaultFlowEntry(TABLE_10_DEFAULT_FLOW);

    private static final TrafficSelector TABLE_10_DEFAULT_FLOW_SELECTOR_1 = DefaultTrafficSelector.builder()
            .matchInPort(UP_PORT)
            .matchVlanId(VlanId.NONE)
            .build();
    private static final TrafficTreatment TABLE_10_DEFAULT_FLOW_TREATMENT_1 = DefaultTrafficTreatment.builder()
            .setVlanId(DEFAULT_VLAN)
            .transition(OfdpaPipelineUtility.TMAC_TABLE)
            .build();
    private static final FlowRule TABLE_10_DEFAULT_FLOW_1 = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.VLAN_TABLE)
            .withPriority(32768)
            .withSelector(TABLE_10_DEFAULT_FLOW_SELECTOR_1)
            .withTreatment(TABLE_10_DEFAULT_FLOW_TREATMENT_1)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_10_DEFAULT_FLOW_ENTRY_1 = new DefaultFlowEntry(TABLE_10_DEFAULT_FLOW_1);

    private static final TrafficSelector TABLE_10_DEFAULT_FLOW_SELECTOR_2 = DefaultTrafficSelector.builder()
            .matchInPort(UP_PORT)
            .matchVlanId(DEFAULT_VLAN)
            .build();
    private static final TrafficTreatment TABLE_10_DEFAULT_FLOW_TREATMENT_2 = DefaultTrafficTreatment.builder()
            .transition(OfdpaPipelineUtility.TMAC_TABLE)
            .build();
    private static final FlowRule TABLE_10_DEFAULT_FLOW_2 = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.VLAN_TABLE)
            .withPriority(32768)
            .withSelector(TABLE_10_DEFAULT_FLOW_SELECTOR_2)
            .withTreatment(TABLE_10_DEFAULT_FLOW_TREATMENT_2)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_10_DEFAULT_FLOW_ENTRY_2 = new DefaultFlowEntry(TABLE_10_DEFAULT_FLOW_2);

    private static final TrafficTreatment TABLE_20_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .transition(OfdpaPipelineUtility.BRIDGING_TABLE)
            .build();
    private static final FlowRule TABLE_20_MISS_OVS = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.TMAC_TABLE)
            .withPriority(0)
            .withSelector(EMPTY_SELECTOR)
            .withTreatment(TABLE_20_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_20_MISS_FLOW_ENTRY_OVS = new DefaultFlowEntry(TABLE_20_MISS_OVS);

    private static final TrafficSelector TABLE_20_IPV4_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(UP_PORT)
            .matchEthDst(LEAF_MAC)
            .matchEthType(IPV4.ethType().toShort())
            .matchVlanId(DEFAULT_VLAN)
            .build();
    private static final TrafficTreatment TABLE_20_IPV4_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .transition(OfdpaPipelineUtility.UNICAST_ROUTING_TABLE)
            .build();
    private static final FlowRule TABLE_20_IPV4_FLOW = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.TMAC_TABLE)
            .withPriority(32768)
            .withSelector(TABLE_20_IPV4_FLOW_SELECTOR)
            .withTreatment(TABLE_20_IPV4_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_20_IPV4_FLOW_ENTRY = new DefaultFlowEntry(TABLE_20_IPV4_FLOW);

    private static final TrafficSelector TABLE_20_IPV4_FLOW_SELECTOR_1 = DefaultTrafficSelector.builder()
            .matchInPort(PORT)
            .matchEthDst(LEAF_MAC)
            .matchEthType(IPV4.ethType().toShort())
            .matchVlanId(HOST_VLAN)
            .build();
    private static final TrafficTreatment TABLE_20_IPV4_FLOW_TREATMENT_1 = DefaultTrafficTreatment.builder()
            .transition(OfdpaPipelineUtility.UNICAST_ROUTING_TABLE)
            .build();
    private static final FlowRule TABLE_20_IPV4_FLOW_1 = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.TMAC_TABLE)
            .withPriority(32768)
            .withSelector(TABLE_20_IPV4_FLOW_SELECTOR_1)
            .withTreatment(TABLE_20_IPV4_FLOW_TREATMENT_1)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_20_IPV4_FLOW_ENTRY_1 = new DefaultFlowEntry(TABLE_20_IPV4_FLOW_1);

    private static final TrafficSelector TABLE_20_MPLS_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(UP_PORT)
            .matchEthDst(LEAF_MAC)
            .matchEthType(MPLS_UNICAST.ethType().toShort())
            .matchVlanId(DEFAULT_VLAN)
            .build();
    private static final TrafficTreatment TABLE_20_MPLS_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .transition(OfdpaPipelineUtility.MPLS_TABLE_0)
            .build();
    private static final FlowRule TABLE_20_MPLS_FLOW = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.TMAC_TABLE)
            .withPriority(32768)
            .withSelector(TABLE_20_MPLS_FLOW_SELECTOR)
            .withTreatment(TABLE_20_MPLS_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_20_MPLS_FLOW_ENTRY = new DefaultFlowEntry(TABLE_20_MPLS_FLOW);

    private static final TrafficTreatment TABLE_23_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .transition(OfdpaPipelineUtility.MPLS_TABLE_1)
            .build();
    private static final FlowRule TABLE_23_MISS_OVS = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.MPLS_TABLE_0)
            .withPriority(0)
            .withSelector(EMPTY_SELECTOR)
            .withTreatment(TABLE_23_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_23_MISS_FLOW_ENTRY_OVS = new DefaultFlowEntry(TABLE_23_MISS_OVS);

    private static final TrafficSelector TABLE_24_MPLS_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchEthType(MPLS_UNICAST.ethType().toShort())
            .matchMplsLabel(MPLS_LABEL)
            .matchMplsBos(true)
            .build();
    private static final TrafficTreatment TABLE_24_MPLS_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .transition(OfdpaPipelineUtility.ACL_TABLE)
            .deferred()
            .popMpls(IPV4.ethType())
            .decMplsTtl()
            .group(MPLS_ECMP_GROUP_ID)
            .build();
    private static final FlowRule TABLE_24_MPLS_FLOW = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.MPLS_TABLE_1)
            .withPriority(100)
            .withSelector(TABLE_24_MPLS_FLOW_SELECTOR)
            .withTreatment(TABLE_24_MPLS_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_24_MPLS_FLOW_ENTRY = new DefaultFlowEntry(TABLE_24_MPLS_FLOW);

    private static final TrafficTreatment TABLE_24_MPLS_FLOW_TREATMENT_OFDPA = DefaultTrafficTreatment.builder()
            .transition(OfdpaPipelineUtility.MPLS_L3_TYPE_TABLE)
            .copyTtlIn()
            .decMplsTtl()
            .extension(new Ofdpa3SetMplsType(Ofdpa3MplsType.L3_PHP), OFDPA_DEVICE)
            .deferred()
            .group(MPLS_ECMP_GROUP_ID)
            .build();
    private static final FlowRule TABLE_24_MPLS_FLOW_OFDPA = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.MPLS_TABLE_1)
            .withPriority(100)
            .withSelector(TABLE_24_MPLS_FLOW_SELECTOR)
            .withTreatment(TABLE_24_MPLS_FLOW_TREATMENT_OFDPA)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_24_MPLS_FLOW_ENTRY_OFDPA = new DefaultFlowEntry(TABLE_24_MPLS_FLOW_OFDPA);

    private static final TrafficSelector TABLE_30_UNI_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchEthType(IPV4.ethType().toShort())
            .matchIPDst(IP_DST)
            .build();
    private static final TrafficTreatment TABLE_30_UNI_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .deferred()
            .group(L3_UCAST_GROUP_ID)
            .transition(OfdpaPipelineUtility.ACL_TABLE)
            .build();
    private static final FlowRule TABLE_30_UNI_FLOW = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.UNICAST_ROUTING_TABLE)
            .withPriority(64010)
            .withSelector(TABLE_30_UNI_FLOW_SELECTOR)
            .withTreatment(TABLE_30_UNI_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_30_UNI_FLOW_ENTRY = new DefaultFlowEntry(TABLE_30_UNI_FLOW);

    private static final TrafficSelector TABLE_30_ECMP_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchEthType(IPV4.ethType().toShort())
            .matchIPDst(PREFIX_DST)
            .build();
    private static final TrafficTreatment TABLE_30_ECMP_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .deferred()
            .group(L3_ECMP_GROUP_ID)
            .transition(OfdpaPipelineUtility.ACL_TABLE)
            .build();
    private static final FlowRule TABLE_30_ECMP_FLOW = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.UNICAST_ROUTING_TABLE)
            .withPriority(48010)
            .withSelector(TABLE_30_ECMP_FLOW_SELECTOR)
            .withTreatment(TABLE_30_ECMP_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_30_ECMP_FLOW_ENTRY = new DefaultFlowEntry(TABLE_30_ECMP_FLOW);

    private static final TrafficSelector TABLE_50_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchVlanId(HOST_VLAN)
            .build();
    private static final TrafficTreatment TABLE_50_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .deferred()
            .group(L2_FLOOD_GROUP_ID)
            .transition(OfdpaPipelineUtility.ACL_TABLE)
            .build();
    private static final FlowRule TABLE_50_FLOW = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.BRIDGING_TABLE)
            .withPriority(5)
            .withSelector(TABLE_50_FLOW_SELECTOR)
            .withTreatment(TABLE_50_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_50_FLOW_ENTRY = new DefaultFlowEntry(TABLE_50_FLOW);

    private static final TrafficSelector TABLE_50_FLOW_SELECTOR_BRIDG = DefaultTrafficSelector.builder()
            .matchVlanId(HOST_VLAN)
            .matchEthDst(HOST_MAC)
            .build();
    private static final TrafficTreatment TABLE_50_FLOW_TREATMENT_BRIDG = DefaultTrafficTreatment.builder()
            .deferred()
            .group(L2_IFACE_GROUP_ID)
            .transition(OfdpaPipelineUtility.ACL_TABLE)
            .build();
    private static final FlowRule TABLE_50_FLOW_BRIDG = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.BRIDGING_TABLE)
            .withPriority(100)
            .withSelector(TABLE_50_FLOW_SELECTOR_BRIDG)
            .withTreatment(TABLE_50_FLOW_TREATMENT_BRIDG)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_50_FLOW_ENTRY_BRIDG = new DefaultFlowEntry(TABLE_50_FLOW_BRIDG);

    private static final TrafficSelector TABLE_60_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchEthType(IPV4.ethType().toShort())
            .matchIPDst(IP_PUNT)
            .build();
    private static final TrafficTreatment TABLE_60_FLOW_TREATMENT_OVS = DefaultTrafficTreatment.builder()
            .wipeDeferred()
            .transition(OvsOfdpaPipeline.PUNT_TABLE)
            .build();
    private static final FlowRule TABLE_60_FLOW_OVS = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.ACL_TABLE)
            .withPriority(40000)
            .withSelector(TABLE_60_FLOW_SELECTOR)
            .withTreatment(TABLE_60_FLOW_TREATMENT_OVS)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_60_FLOW_ENTRY = new DefaultFlowEntry(TABLE_60_FLOW_OVS);

    private static final TrafficTreatment TABLE_60_FLOW_TREATMENT_OFDPA = DefaultTrafficTreatment.builder()
            .wipeDeferred()
            .punt()
            .build();
    private static final FlowRule TABLE_60_FLOW_OFDPA = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.ACL_TABLE)
            .withPriority(40000)
            .withSelector(TABLE_60_FLOW_SELECTOR)
            .withTreatment(TABLE_60_FLOW_TREATMENT_OFDPA)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_60_FLOW_ENTRY_OFDPA = new DefaultFlowEntry(TABLE_60_FLOW_OFDPA);

    private static final FlowRule TABLE_60_MISS_OVS = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.ACL_TABLE)
            .withPriority(0)
            .withSelector(EMPTY_SELECTOR)
            .withTreatment(EMPTY_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_60_MISS_FLOW_ENTRY_OVS = new DefaultFlowEntry(TABLE_60_MISS_OVS);

    private static final TrafficSelector TABLE_60_FLOW_SELECTOR_ARP = DefaultTrafficSelector.builder()
            .matchEthType(ARP.ethType().toShort())
            .build();
    private static final TrafficTreatment TABLE_60_FLOW_TREATMENT_ARP_OVS = DefaultTrafficTreatment.builder()
            .transition(OvsOfdpaPipeline.PUNT_TABLE)
            .build();
    private static final FlowRule TABLE_60_FLOW_ARP_OVS = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.ACL_TABLE)
            .withPriority(30000)
            .withSelector(TABLE_60_FLOW_SELECTOR_ARP)
            .withTreatment(TABLE_60_FLOW_TREATMENT_ARP_OVS)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_60_FLOW_ENTRY_ARP_OVS = new DefaultFlowEntry(TABLE_60_FLOW_ARP_OVS);

    private static final TrafficTreatment TABLE_60_FLOW_TREATMENT_ARP_OFDPA = DefaultTrafficTreatment.builder()
            .punt()
            .build();
    private static final FlowRule TABLE_60_FLOW_ARP_OFDPA = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.ACL_TABLE)
            .withPriority(30000)
            .withSelector(TABLE_60_FLOW_SELECTOR_ARP)
            .withTreatment(TABLE_60_FLOW_TREATMENT_ARP_OFDPA)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_60_FLOW_ENTRY_ARP_OFDPA = new DefaultFlowEntry(TABLE_60_FLOW_ARP_OFDPA);

    private static final TrafficSelector TABLE_60_FLOW_SELECTOR_LLDP = DefaultTrafficSelector.builder()
            .matchEthType(LLDP.ethType().toShort())
            .build();
    private static final TrafficTreatment TABLE_60_FLOW_TREATMENT_LLDP = DefaultTrafficTreatment.builder()
            .wipeDeferred()
            .transition(OvsOfdpaPipeline.PUNT_TABLE)
            .build();
    private static final FlowRule TABLE_60_FLOW_LLDP = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.ACL_TABLE)
            .withPriority(40000)
            .withSelector(TABLE_60_FLOW_SELECTOR_LLDP)
            .withTreatment(TABLE_60_FLOW_TREATMENT_LLDP)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_60_FLOW_ENTRY_LLDP = new DefaultFlowEntry(TABLE_60_FLOW_LLDP);

    private static final TrafficTreatment TABLE_60_FLOW_TREATMENT_LLDP_OFDPA = DefaultTrafficTreatment.builder()
            .wipeDeferred()
            .punt()
            .build();
    private static final FlowRule TABLE_60_FLOW_LLDP_OFDPA = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OfdpaPipelineUtility.ACL_TABLE)
            .withPriority(40000)
            .withSelector(TABLE_60_FLOW_SELECTOR_LLDP)
            .withTreatment(TABLE_60_FLOW_TREATMENT_LLDP_OFDPA)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_60_FLOW_ENTRY_LLDP_OFDPA = new DefaultFlowEntry(TABLE_60_FLOW_LLDP_OFDPA);

    private static final TrafficSelector TABLE_63_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(OFDPA_CP.port())
            .matchVlanId(HOST_VLAN)
            .build();
    private static final TrafficTreatment TABLE_63_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .group(GroupId.valueOf(OvsOfdpaPipeline.POP_VLAN_PUNT_GROUP_ID))
            .build();
    private static final FlowRule TABLE_63_FLOW = DefaultFlowEntry.builder().forDevice(OFDPA_DEVICE)
            .forTable(OvsOfdpaPipeline.PUNT_TABLE)
            .withPriority(40000)
            .withSelector(TABLE_63_FLOW_SELECTOR)
            .withTreatment(TABLE_63_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    private static final FlowEntry TABLE_63_FLOW_ENTRY = new DefaultFlowEntry(TABLE_63_FLOW);

    // Represents the device state
    public static List<DataPlaneEntity> getDataPlaneEntities(String driverName, TraceableTest test) {
        List<FlowEntry> flowRules = ImmutableList.of();
        List<Group> groups = ImmutableList.of();
        // Flow and groups by device
        if (driverName.equals(OFDPA_DRIVER)) {
            flowRules = ImmutableList.of(
                    // Vlan 1 table
                    TABLE_10_FLOW_ENTRY_1, TABLE_10_FLOW_ENTRY_2, TABLE_10_DEFAULT_FLOW_ENTRY_1,
                    TABLE_10_DEFAULT_FLOW_ENTRY_2,
                    // TMAC table
                    TABLE_20_IPV4_FLOW_ENTRY, TABLE_20_IPV4_FLOW_ENTRY_1, TABLE_20_MPLS_FLOW_ENTRY,
                    // MPLS 1 table
                    TABLE_24_MPLS_FLOW_ENTRY_OFDPA,
                    // Unicast table
                    TABLE_30_UNI_FLOW_ENTRY, TABLE_30_ECMP_FLOW_ENTRY,
                    // Bridging table
                    TABLE_50_FLOW_ENTRY, TABLE_50_FLOW_ENTRY_BRIDG,
                    // ACL table
                    TABLE_60_FLOW_ENTRY_OFDPA, TABLE_60_FLOW_ENTRY_ARP_OFDPA, TABLE_60_FLOW_ENTRY_LLDP_OFDPA);
            groups = Lists.newArrayList(
                    // L3 ECMP groups
                    L3_ECMP_OFDPA_GROUP,
                    // MPLS ECMP groups
                    MPLS_ECMP_GROUP,
                    // L3 groups
                    L3_UCAST_GROUP, L3_UCAST_GROUP_1,
                    // MPLS L3 VPN groups
                    MPLS_L3VPN_OFDPA_GROUP, MPLS_L3VPN_OFDPA_GROUP_1,
                    // MPLS iface groups
                    MPLS_IFACE_GROUP, MPLS_IFACE_GROUP_1,
                    // L2 groups
                    L2_FLOOD_GROUP,
                    L2_IFACE_GROUP, L2_IFACE_GROUP_1, L2_IFACE_GROUP_2, L2_IFACE_GROUP_3);
        } else if (driverName.equals(OVS_OFDPA_DRIVER)) {
            flowRules = ImmutableList.of(
                    // Port table
                    TABLE_0_MISS_FLOW_ENTRY_OVS,
                    // Vlan 1 table
                    TABLE_10_FLOW_ENTRY, TABLE_10_DEFAULT_FLOW_ENTRY,
                    // TMAC table
                    TABLE_20_MISS_FLOW_ENTRY_OVS, TABLE_20_IPV4_FLOW_ENTRY, TABLE_20_IPV4_FLOW_ENTRY_1,
                    TABLE_20_MPLS_FLOW_ENTRY,
                    // MPLS 0 table
                    TABLE_23_MISS_FLOW_ENTRY_OVS,
                    // MPLS 1 table
                    TABLE_24_MPLS_FLOW_ENTRY,
                    // Unicast table
                    TABLE_30_UNI_FLOW_ENTRY, TABLE_30_ECMP_FLOW_ENTRY,
                    // Bridging table
                    TABLE_50_FLOW_ENTRY, TABLE_50_FLOW_ENTRY_BRIDG,
                    // ACL table
                    TABLE_60_MISS_FLOW_ENTRY_OVS, TABLE_60_FLOW_ENTRY, TABLE_60_FLOW_ENTRY_ARP_OVS,
                    TABLE_60_FLOW_ENTRY_LLDP,
                    // Punt table
                    TABLE_63_FLOW_ENTRY);
            groups = Lists.newArrayList(
                    // Punt groups
                    PUNT_GROUP,
                    // L3 ECMP groups
                    L3_ECMP_GROUP,
                    // MPLS ECMP groups
                    MPLS_ECMP_GROUP,
                    // L3 groups
                    L3_UCAST_GROUP, L3_UCAST_GROUP_1,
                    // MPLS L3 VPN groups
                    MPLS_L3VPN_GROUP, MPLS_L3VPN_GROUP_1,
                    // MPLS iface groups
                    MPLS_IFACE_GROUP, MPLS_IFACE_GROUP_1,
                    // L2 groups
                    L2_FLOOD_GROUP,
                    L2_IFACE_GROUP, L2_IFACE_GROUP_1, L2_IFACE_GROUP_2, L2_IFACE_GROUP_3);
        }
        // Inject failure scenarios
        if (test.equals(L2_BROAD_EMPTY_OFDPA) || test.equals(L2_BROAD_EMPTY_OVS_OFDPA)) {
            groups.remove(L2_FLOOD_GROUP);
            groups.add(L2_FLOOD_EMPTY_GROUP);
        } else if (test.equals(L2_BRIDG_NOT_ORDERED_OFDPA) || test.equals(L2_BRIDG_NOT_ORDERED_OVS_OFDPA)) {
            groups.remove(L2_IFACE_GROUP);
            groups.add(L2_IFACE_GROUP_NOT_ORDERED);
        }
        List<DataPlaneEntity> dataPlaneEntities = Lists.newArrayList();
        flowRules.forEach(flowRule -> dataPlaneEntities.add(new DataPlaneEntity(flowRule)));
        groups.forEach(group -> dataPlaneEntities.add(new DataPlaneEntity(group)));
        return dataPlaneEntities;
    }

    // Returns the expected hit chains (order matters!)
    public static List<List<DataPlaneEntity>> getHitChains(TraceableTest test) {
        List<List<FlowEntry>> flowRules = Lists.newArrayList();
        List<List<Group>> groups = Lists.newArrayList();
        // Flows and groups by test
        if (test.equals(PUNT_IP_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_10_FLOW_ENTRY_1, TABLE_10_FLOW_ENTRY_2,
                    TABLE_50_FLOW_ENTRY, TABLE_60_FLOW_ENTRY));
        } else if (test.equals(PUNT_IP_OVS_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_FLOW_ENTRY, TABLE_20_MISS_FLOW_ENTRY_OVS,
                    TABLE_50_FLOW_ENTRY, TABLE_60_FLOW_ENTRY, TABLE_63_FLOW_ENTRY));
            groups.add(ImmutableList.of(PUNT_GROUP));
        } else if (test.equals(ARP_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_10_FLOW_ENTRY_1, TABLE_10_FLOW_ENTRY_2,
                    TABLE_50_FLOW_ENTRY, TABLE_60_FLOW_ENTRY_ARP_OFDPA));
            groups.add(ImmutableList.of());
            flowRules.add(ImmutableList.of(
                    TABLE_10_FLOW_ENTRY_1, TABLE_10_FLOW_ENTRY_2,
                    TABLE_50_FLOW_ENTRY, TABLE_60_FLOW_ENTRY_ARP_OFDPA));
            groups.add(ImmutableList.of(L2_FLOOD_GROUP, L2_IFACE_GROUP));
            flowRules.add(ImmutableList.of(
                    TABLE_10_FLOW_ENTRY_1, TABLE_10_FLOW_ENTRY_2,
                    TABLE_50_FLOW_ENTRY, TABLE_60_FLOW_ENTRY_ARP_OFDPA));
            groups.add(ImmutableList.of(L2_FLOOD_GROUP, L2_IFACE_GROUP_1));
        } else if (test.equals(ARP_OVS_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_FLOW_ENTRY, TABLE_20_MISS_FLOW_ENTRY_OVS,
                    TABLE_50_FLOW_ENTRY, TABLE_60_FLOW_ENTRY_ARP_OVS, TABLE_63_FLOW_ENTRY));
            groups.add(ImmutableList.of(PUNT_GROUP));
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_FLOW_ENTRY, TABLE_20_MISS_FLOW_ENTRY_OVS,
                    TABLE_50_FLOW_ENTRY, TABLE_60_FLOW_ENTRY_ARP_OVS, TABLE_63_FLOW_ENTRY));
            groups.add(ImmutableList.of(L2_FLOOD_GROUP, L2_IFACE_GROUP));
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_FLOW_ENTRY, TABLE_20_MISS_FLOW_ENTRY_OVS,
                    TABLE_50_FLOW_ENTRY, TABLE_60_FLOW_ENTRY_ARP_OVS, TABLE_63_FLOW_ENTRY));
            groups.add(ImmutableList.of(L2_FLOOD_GROUP, L2_IFACE_GROUP_1));
        } else if (test.equals(PUNT_LLDP_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_10_FLOW_ENTRY_1, TABLE_10_FLOW_ENTRY_2,
                    TABLE_50_FLOW_ENTRY, TABLE_60_FLOW_ENTRY_LLDP_OFDPA));
        } else if (test.equals(PUNT_LLDP_OVS_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_FLOW_ENTRY, TABLE_20_MISS_FLOW_ENTRY_OVS,
                    TABLE_50_FLOW_ENTRY, TABLE_60_FLOW_ENTRY_LLDP, TABLE_63_FLOW_ENTRY));
            groups.add(ImmutableList.of(PUNT_GROUP));
        } else if (test.equals(L2_BRIDG_UNTAG_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_10_FLOW_ENTRY_1, TABLE_10_FLOW_ENTRY_2,
                    TABLE_50_FLOW_ENTRY_BRIDG));
            groups.add(ImmutableList.of(L2_IFACE_GROUP));
        } else if (test.equals(L2_BRIDG_UNTAG_OVS_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_FLOW_ENTRY, TABLE_20_MISS_FLOW_ENTRY_OVS,
                    TABLE_50_FLOW_ENTRY_BRIDG, TABLE_60_MISS_FLOW_ENTRY_OVS));
            groups.add(ImmutableList.of(L2_IFACE_GROUP));
        } else if (test.equals(L2_BROAD_UNTAG_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_10_FLOW_ENTRY_1, TABLE_10_FLOW_ENTRY_2,
                    TABLE_50_FLOW_ENTRY));
            groups.add(ImmutableList.of(L2_FLOOD_GROUP, L2_IFACE_GROUP));
            flowRules.add(ImmutableList.of(
                    TABLE_10_FLOW_ENTRY_1, TABLE_10_FLOW_ENTRY_2,
                    TABLE_50_FLOW_ENTRY));
            groups.add(ImmutableList.of(L2_FLOOD_GROUP, L2_IFACE_GROUP_1));
        } else if (test.equals(L2_BROAD_UNTAG_OVS_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_FLOW_ENTRY, TABLE_20_MISS_FLOW_ENTRY_OVS,
                    TABLE_50_FLOW_ENTRY, TABLE_60_MISS_FLOW_ENTRY_OVS));
            groups.add(ImmutableList.of(L2_FLOOD_GROUP, L2_IFACE_GROUP));
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_FLOW_ENTRY, TABLE_20_MISS_FLOW_ENTRY_OVS,
                    TABLE_50_FLOW_ENTRY, TABLE_60_MISS_FLOW_ENTRY_OVS));
            groups.add(ImmutableList.of(L2_FLOOD_GROUP, L2_IFACE_GROUP_1));
        } else if (test.equals(L3_UCAST_UNTAG_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_10_DEFAULT_FLOW_ENTRY_1, TABLE_10_DEFAULT_FLOW_ENTRY_2, TABLE_20_IPV4_FLOW_ENTRY,
                    TABLE_30_UNI_FLOW_ENTRY));
            groups.add(ImmutableList.of(L3_UCAST_GROUP, L2_IFACE_GROUP));
        } else if (test.equals(L3_UCAST_UNTAG_OVS_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_DEFAULT_FLOW_ENTRY, TABLE_20_IPV4_FLOW_ENTRY,
                    TABLE_30_UNI_FLOW_ENTRY, TABLE_60_MISS_FLOW_ENTRY_OVS));
            groups.add(ImmutableList.of(L3_UCAST_GROUP, L2_IFACE_GROUP));
        } else if (test.equals(L3_ECMP_OVS_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_FLOW_ENTRY, TABLE_20_IPV4_FLOW_ENTRY_1,
                    TABLE_30_ECMP_FLOW_ENTRY, TABLE_60_MISS_FLOW_ENTRY_OVS));
            groups.add(ImmutableList.of(L3_ECMP_GROUP, MPLS_L3VPN_GROUP, MPLS_IFACE_GROUP, L2_IFACE_GROUP_2));
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_FLOW_ENTRY, TABLE_20_IPV4_FLOW_ENTRY_1,
                    TABLE_30_ECMP_FLOW_ENTRY, TABLE_60_MISS_FLOW_ENTRY_OVS));
            groups.add(ImmutableList.of(L3_ECMP_GROUP, MPLS_L3VPN_GROUP_1, MPLS_IFACE_GROUP_1, L2_IFACE_GROUP_3));
        } else if (test.equals(L3_ECMP_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_10_FLOW_ENTRY_1, TABLE_10_FLOW_ENTRY_2, TABLE_20_IPV4_FLOW_ENTRY_1,
                    TABLE_30_ECMP_FLOW_ENTRY));
            groups.add(ImmutableList.of(L3_ECMP_OFDPA_GROUP, MPLS_L3VPN_OFDPA_GROUP, MPLS_IFACE_GROUP,
                    L2_IFACE_GROUP_2));
            flowRules.add(ImmutableList.of(
                    TABLE_10_FLOW_ENTRY_1, TABLE_10_FLOW_ENTRY_2, TABLE_20_IPV4_FLOW_ENTRY_1,
                    TABLE_30_ECMP_FLOW_ENTRY));
            groups.add(ImmutableList.of(L3_ECMP_OFDPA_GROUP, MPLS_L3VPN_OFDPA_GROUP_1, MPLS_IFACE_GROUP_1,
                    L2_IFACE_GROUP_3));
        } else if (test.equals(MPLS_ECMP_OVS_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_DEFAULT_FLOW_ENTRY, TABLE_20_MPLS_FLOW_ENTRY,
                    TABLE_23_MISS_FLOW_ENTRY_OVS, TABLE_24_MPLS_FLOW_ENTRY, TABLE_60_MISS_FLOW_ENTRY_OVS));
            groups.add(ImmutableList.of(MPLS_ECMP_GROUP, L3_UCAST_GROUP_1, L2_IFACE_GROUP_3));
        } else if (test.equals(MPLS_ECMP_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_10_DEFAULT_FLOW_ENTRY_1, TABLE_10_DEFAULT_FLOW_ENTRY_2, TABLE_20_MPLS_FLOW_ENTRY,
                    TABLE_24_MPLS_FLOW_ENTRY_OFDPA));
            groups.add(ImmutableList.of(MPLS_ECMP_GROUP, L3_UCAST_GROUP_1, L2_IFACE_GROUP_3));
        } else if (test.equals(L2_BROAD_EMPTY_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_10_FLOW_ENTRY_1, TABLE_10_FLOW_ENTRY_2,
                    TABLE_50_FLOW_ENTRY));
            groups.add(ImmutableList.of(L2_FLOOD_EMPTY_GROUP));
        } else if (test.equals(L2_BROAD_EMPTY_OVS_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_FLOW_ENTRY, TABLE_20_MISS_FLOW_ENTRY_OVS,
                    TABLE_50_FLOW_ENTRY, TABLE_60_MISS_FLOW_ENTRY_OVS));
            groups.add(ImmutableList.of(L2_FLOOD_EMPTY_GROUP));
        } else if (test.equals(L2_BRIDG_NOT_ORDERED_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_10_FLOW_ENTRY_1, TABLE_10_FLOW_ENTRY_2,
                    TABLE_50_FLOW_ENTRY_BRIDG));
            groups.add(ImmutableList.of(L2_IFACE_GROUP_NOT_ORDERED));
        } else if (test.equals(L2_BRIDG_NOT_ORDERED_OVS_OFDPA)) {
            flowRules.add(ImmutableList.of(
                    TABLE_0_MISS_FLOW_ENTRY_OVS, TABLE_10_FLOW_ENTRY, TABLE_20_MISS_FLOW_ENTRY_OVS,
                    TABLE_50_FLOW_ENTRY_BRIDG, TABLE_60_MISS_FLOW_ENTRY_OVS));
            groups.add(ImmutableList.of(L2_IFACE_GROUP_NOT_ORDERED));
        }
        List<List<DataPlaneEntity>> chains = Lists.newArrayList();
        List<DataPlaneEntity> dataPlaneEntities = Lists.newArrayList();
        int end = Math.max(flowRules.size(), groups.size());
        int i = 0;
        while (i < end) {
            if (i < flowRules.size()) {
                flowRules.get(i).forEach(flowRule -> dataPlaneEntities.add(new DataPlaneEntity(flowRule)));
            }
            if (i < groups.size()) {
                groups.get(i).forEach(group -> dataPlaneEntities.add(new DataPlaneEntity(group)));
            }
            chains.add(ImmutableList.copyOf(dataPlaneEntities));
            dataPlaneEntities.clear();
            i = i + 1;
        }
        return chains;
    }

}
