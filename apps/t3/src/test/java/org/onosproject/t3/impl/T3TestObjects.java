/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.t3.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
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
import org.onosproject.net.provider.ProviderId;

/**
 * Helper class for objects related to the Troubleshoot Manager Test.
 */
final class T3TestObjects {

    private T3TestObjects() {
        //banning construction
    }

    private static final String HOST_ONE_MAC = "00:00:00:00:00:01";
    private static final String HOST_TWO_MAC = "00:00:00:00:00:02";
    private static final String HOST_ONE_VLAN = "None";
    private static final String HOST_TWO_VLAN = "None";
    private static final String HOST_ONE = HOST_ONE_MAC + "/" + HOST_ONE_VLAN;
    private static final String HOST_TWO = HOST_TWO_MAC + "/" + HOST_TWO_VLAN;
    private static final String HOST_DUAL_HOMED_MAC = "00:00:00:00:00:03";
    private static final String HOST_DUAL_HOMED_VLAN = "None";
    private static final String HOST_DUAL_HOMED = HOST_DUAL_HOMED_MAC + "/" + HOST_DUAL_HOMED_VLAN;

    //offline device
    static final DeviceId OFFLINE_DEVICE = DeviceId.deviceId("offlineDevice");

    //Single Flow Test
    static final DeviceId SINGLE_FLOW_DEVICE = DeviceId.deviceId("SingleFlowDevice");
    private static final TrafficSelector SINGLE_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.2/32"))
            .build();

    private static final TrafficTreatment OUTPUT_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .setOutput(PortNumber.portNumber(2)).build();
    private static final FlowRule SINGLE_FLOW = DefaultFlowEntry.builder().forDevice(SINGLE_FLOW_DEVICE)
            .forTable(0)
            .withPriority(100)
            .withSelector(SINGLE_FLOW_SELECTOR)
            .withTreatment(OUTPUT_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry SINGLE_FLOW_ENTRY = new DefaultFlowEntry(SINGLE_FLOW);

    static final ConnectPoint SINGLE_FLOW_IN_CP = ConnectPoint.deviceConnectPoint(SINGLE_FLOW_DEVICE + "/" + 1);

    static final ConnectPoint SINGLE_FLOW_OUT_CP = ConnectPoint.deviceConnectPoint(SINGLE_FLOW_DEVICE + "/" + 2);

    //same output as input
    static final DeviceId SAME_OUTPUT_FLOW_DEVICE = DeviceId.deviceId("sameOutputDevice");

    private static final TrafficTreatment SAME_OUTPUT_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .setOutput(PortNumber.portNumber(1)).build();
    private static final FlowRule SAME_OUTPUT_FLOW = DefaultFlowEntry.builder().forDevice(SAME_OUTPUT_FLOW_DEVICE)
            .forTable(0)
            .withPriority(100)
            .withSelector(SINGLE_FLOW_SELECTOR)
            .withTreatment(SAME_OUTPUT_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry SAME_OUTPUT_FLOW_ENTRY = new DefaultFlowEntry(SAME_OUTPUT_FLOW);

    static final ConnectPoint SAME_OUTPUT_FLOW_CP = ConnectPoint.deviceConnectPoint(SAME_OUTPUT_FLOW_DEVICE + "/" + 1);

    //ARP
    static final DeviceId ARP_FLOW_DEVICE = DeviceId.deviceId("ArpDevice");

    private static final TrafficSelector ARP_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchEthType(EthType.EtherType.ARP.ethType().toShort())
            .build();

    private static final TrafficTreatment ARP_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .setOutput(PortNumber.CONTROLLER).build();
    private static final FlowRule ARP_FLOW = DefaultFlowEntry.builder().forDevice(ARP_FLOW_DEVICE)
            .forTable(0)
            .withPriority(100)
            .withSelector(ARP_FLOW_SELECTOR)
            .withTreatment(ARP_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry ARP_FLOW_ENTRY = new DefaultFlowEntry(ARP_FLOW);

    static final ConnectPoint ARP_FLOW_CP = ConnectPoint.deviceConnectPoint(ARP_FLOW_DEVICE + "/" + 1);


    //Dual Flow Test
    static final DeviceId DUAL_FLOW_DEVICE = DeviceId.deviceId("DualFlowDevice");
    private static final TrafficTreatment TRANSITION_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .setVlanId(VlanId.vlanId((short) 100))
            .transition(10)
            .build();
    private static final TrafficSelector VLAN_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchVlanId(VlanId.vlanId((short) 100))
            .build();
    private static final FlowRule FIRST_FLOW = DefaultFlowEntry.builder().forDevice(DUAL_FLOW_DEVICE)
            .forTable(0)
            .withPriority(100)
            .withSelector(SINGLE_FLOW_SELECTOR)
            .withTreatment(TRANSITION_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry FIRST_FLOW_ENTRY = new DefaultFlowEntry(FIRST_FLOW);
    private static final FlowRule SECOND_FLOW = DefaultFlowEntry.builder().forDevice(DUAL_FLOW_DEVICE)
            .forTable(10)
            .withPriority(100)
            .withSelector(VLAN_FLOW_SELECTOR)
            .withTreatment(OUTPUT_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry SECOND_FLOW_ENTRY = new DefaultFlowEntry(SECOND_FLOW);

    static final ConnectPoint DUAL_FLOW_IN_CP = ConnectPoint.deviceConnectPoint(DUAL_FLOW_DEVICE + "/" + 1);

    static final ConnectPoint DUAL_FLOW_OUT_CP = ConnectPoint.deviceConnectPoint(DUAL_FLOW_DEVICE + "/" + 2);

    //Flow and Group Test
    static final DeviceId GROUP_FLOW_DEVICE = DeviceId.deviceId("GroupFlowDevice");

    private static final GroupId GROUP_ID = GroupId.valueOf(1);

    private static final TrafficTreatment GROUP_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .pushMpls()
            .setMpls(MplsLabel.mplsLabel(100))
            .group(GROUP_ID)
            .build();
    private static final FlowRule GROUP_FLOW = DefaultFlowEntry.builder().forDevice(GROUP_FLOW_DEVICE)
            .forTable(0)
            .withPriority(100)
            .withSelector(SINGLE_FLOW_SELECTOR)
            .withTreatment(GROUP_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry GROUP_FLOW_ENTRY = new DefaultFlowEntry(GROUP_FLOW);

    private static final TrafficTreatment OUTPUT_GROUP_TREATMENT = DefaultTrafficTreatment.builder()
            .popMpls(EthType.EtherType.IPV4.ethType())
            .setOutput(PortNumber.portNumber(2)).build();

    private static final GroupBucket BUCKET = DefaultGroupBucket.createSelectGroupBucket(OUTPUT_GROUP_TREATMENT);

    private static final GroupBuckets BUCKETS = new GroupBuckets(ImmutableList.of(BUCKET));

    static final Group GROUP = new DefaultGroup(GROUP_ID, GROUP_FLOW_DEVICE, Group.Type.SELECT, BUCKETS);

    static final ConnectPoint GROUP_FLOW_IN_CP = ConnectPoint.deviceConnectPoint(GROUP_FLOW_DEVICE + "/" + 1);

    static final ConnectPoint GROUP_FLOW_OUT_CP = ConnectPoint.deviceConnectPoint(GROUP_FLOW_DEVICE + "/" + 2);

    //topology

    static final DeviceId TOPO_FLOW_DEVICE = DeviceId.deviceId("SingleFlowDevice1");

    static final DeviceId TOPO_FLOW_2_DEVICE = DeviceId.deviceId("SingleFlowDevice2");

    static final DeviceId TOPO_FLOW_3_DEVICE = DeviceId.deviceId("SingleFlowDevice3");

    private static final TrafficSelector TOPO_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.3/32"))
            .build();

    private static final FlowRule TOPO_SINGLE_FLOW = DefaultFlowEntry.builder().forDevice(TOPO_FLOW_DEVICE)
            .forTable(0)
            .withPriority(100)
            .withSelector(TOPO_FLOW_SELECTOR)
            .withTreatment(OUTPUT_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();

    static final FlowEntry TOPO_SINGLE_FLOW_ENTRY = new DefaultFlowEntry(TOPO_SINGLE_FLOW);

    static final ConnectPoint TOPO_FLOW_1_IN_CP = ConnectPoint.deviceConnectPoint(TOPO_FLOW_DEVICE + "/" + 1);

    static final ConnectPoint TOPO_FLOW_1_OUT_CP = ConnectPoint.deviceConnectPoint(TOPO_FLOW_DEVICE + "/" + 2);

    static final ConnectPoint TOPO_FLOW_2_IN_CP = ConnectPoint.deviceConnectPoint(TOPO_FLOW_2_DEVICE + "/" + 1);

    static final ConnectPoint TOPO_FLOW_2_OUT_CP = ConnectPoint.deviceConnectPoint(TOPO_FLOW_2_DEVICE + "/" + 2);

    static final ConnectPoint TOPO_FLOW_3_IN_CP = ConnectPoint.deviceConnectPoint(TOPO_FLOW_3_DEVICE + "/" + 1);

    static final ConnectPoint TOPO_FLOW_3_OUT_CP = ConnectPoint.deviceConnectPoint(TOPO_FLOW_3_DEVICE + "/" + 2);


    //Topology with Groups

    static final DeviceId TOPO_GROUP_FLOW_DEVICE = DeviceId.deviceId("TopoGroupFlowDevice");

    private static final TrafficSelector TOPO_SECOND_INPUT_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(3))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.3/32"))
            .build();

    private static final FlowRule TOPO_SECOND_INPUT_FLOW = DefaultFlowEntry.builder().forDevice(TOPO_FLOW_3_DEVICE)
            .forTable(0)
            .withPriority(100)
            .withSelector(TOPO_SECOND_INPUT_FLOW_SELECTOR)
            .withTreatment(OUTPUT_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();

    private static final TrafficTreatment OUTPUT_2_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .setOutput(PortNumber.portNumber(3)).build();


    private static final GroupId TOPO_GROUP_ID = GroupId.valueOf(1);

    private static final TrafficTreatment TOPO_GROUP_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .group(TOPO_GROUP_ID)
            .build();
    private static final FlowRule TOPO_GROUP_FLOW = DefaultFlowEntry.builder().forDevice(TOPO_GROUP_FLOW_DEVICE)
            .forTable(0)
            .withPriority(100)
            .withSelector(TOPO_FLOW_SELECTOR)
            .withTreatment(TOPO_GROUP_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry TOPO_GROUP_FLOW_ENTRY = new DefaultFlowEntry(TOPO_GROUP_FLOW);

    private static final GroupBucket BUCKET_2 = DefaultGroupBucket.createSelectGroupBucket(OUTPUT_2_FLOW_TREATMENT);

    private static final GroupBuckets BUCKETS_MULTIPLE = new GroupBuckets(ImmutableList.of(BUCKET, BUCKET_2));

    static final Group TOPO_GROUP = new DefaultGroup(TOPO_GROUP_ID, TOPO_GROUP_FLOW_DEVICE,
            Group.Type.SELECT, BUCKETS_MULTIPLE);

    static final FlowEntry TOPO_SECOND_INPUT_FLOW_ENTRY = new DefaultFlowEntry(TOPO_SECOND_INPUT_FLOW);

    static final DeviceId TOPO_FLOW_4_DEVICE = DeviceId.deviceId("SingleFlowDevice4");

    static final ConnectPoint TOPO_FLOW_IN_CP = ConnectPoint.deviceConnectPoint(TOPO_GROUP_FLOW_DEVICE + "/" + 1);

    static final ConnectPoint TOPO_FLOW_OUT_CP_1 = ConnectPoint.deviceConnectPoint(TOPO_GROUP_FLOW_DEVICE + "/" + 2);

    protected static final ConnectPoint TOPO_FLOW_OUT_CP_2 =
            ConnectPoint.deviceConnectPoint(TOPO_GROUP_FLOW_DEVICE + "/" + 3);

    static final ConnectPoint TOPO_FLOW_4_IN_CP = ConnectPoint.deviceConnectPoint(TOPO_FLOW_4_DEVICE + "/" + 1);

    static final ConnectPoint TOPO_FLOW_3_IN_2_CP = ConnectPoint.deviceConnectPoint(TOPO_FLOW_3_DEVICE + "/" + 3);

    static final ConnectPoint TOPO_FLOW_4_OUT_CP = ConnectPoint.deviceConnectPoint(TOPO_FLOW_4_DEVICE + "/" + 2);


    //hardware

    static final DeviceId HARDWARE_DEVICE = DeviceId.deviceId("HardwareDevice");

    static final ConnectPoint HARDWARE_DEVICE_IN_CP = ConnectPoint.deviceConnectPoint(HARDWARE_DEVICE + "/" + 1);

    static final ConnectPoint HARDWARE_DEVICE_OUT_CP = ConnectPoint.deviceConnectPoint(HARDWARE_DEVICE + "/" + 2);

    private static final TrafficSelector HARDWARE_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.2/32"))
            .build();

    private static final TrafficTreatment HW_TRANSITION_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .pushMpls()
            .transition(27)
            .build();

    private static final FlowRule HARDWARE_FLOW = DefaultFlowEntry.builder().forDevice(TOPO_FLOW_3_DEVICE)
            .forTable(0)
            .withPriority(100)
            .withSelector(HARDWARE_FLOW_SELECTOR)
            .withTreatment(HW_TRANSITION_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();

    static final FlowEntry HARDWARE_FLOW_ENTRY = new DefaultFlowEntry(HARDWARE_FLOW);

    private static final TrafficSelector HARDWARE_ETH_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.2/32"))
            .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
            .build();

    private static final FlowRule HARDWARE_ETH_FLOW = DefaultFlowEntry.builder().forDevice(TOPO_FLOW_3_DEVICE)
            .forTable(30)
            .withPriority(100)
            .withSelector(HARDWARE_ETH_FLOW_SELECTOR)
            .withTreatment(OUTPUT_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();

    static final FlowEntry HARDWARE_ETH_FLOW_ENTRY = new DefaultFlowEntry(HARDWARE_ETH_FLOW);

    //HW Double Rule on 10

    static final DeviceId HARDWARE_DEVICE_10 = DeviceId.deviceId("HardwareDevice10");

    static final ConnectPoint HARDWARE_DEVICE_10_IN_CP = ConnectPoint.deviceConnectPoint(HARDWARE_DEVICE_10 + "/" + 1);

    static final ConnectPoint HARDWARE_DEVICE_10_OUT_CP = ConnectPoint.deviceConnectPoint(HARDWARE_DEVICE_10 + "/" + 2);

    private static final TrafficSelector HARDWARE_10_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.2/32"))
            .matchVlanId(VlanId.NONE)
            .build();

    private static final TrafficTreatment HARDWARE_10_TRANSITION_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .setVlanId(VlanId.vlanId("10"))
            .transition(20)
            .build();

    private static final FlowRule HARDWARE_DEVICE_10_FLOW = DefaultFlowEntry.builder().forDevice(HARDWARE_DEVICE_10)
            .forTable(10)
            .withPriority(100)
            .withSelector(HARDWARE_10_FLOW_SELECTOR)
            .withTreatment(HARDWARE_10_TRANSITION_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();

    static final FlowEntry HARDWARE_10_FLOW_ENTRY = new DefaultFlowEntry(HARDWARE_DEVICE_10_FLOW);

    private static final TrafficSelector HARDWARE_10_SECOND_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchVlanId(VlanId.vlanId("10"))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.2/32"))
            .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
            .build();

    private static final FlowRule HARDWARE_10_SECOND_FLOW = DefaultFlowEntry.builder().forDevice(HARDWARE_DEVICE_10)
            .forTable(10)
            .withPriority(100)
            .withSelector(HARDWARE_10_SECOND_SELECTOR)
            .withTreatment(HARDWARE_10_TRANSITION_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();

    static final FlowEntry HARDWARE_10_SECOND_FLOW_ENTRY = new DefaultFlowEntry(HARDWARE_10_SECOND_FLOW);

    private static final FlowRule HARDWARE_10_OUTPUT_FLOW = DefaultFlowEntry.builder().forDevice(HARDWARE_DEVICE_10)
            .forTable(20)
            .withPriority(100)
            .withSelector(SINGLE_FLOW_SELECTOR)
            .withTreatment(OUTPUT_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();

    static final FlowEntry HARDWARE_10_OUTPUT_FLOW_ENTRY = new DefaultFlowEntry(HARDWARE_10_OUTPUT_FLOW);

    //Dual Links
    // - (1) Device 1 (2-3) - (1-4) Device 2 (2-3) - (1-2) Device 3 (3) -
    static final DeviceId DUAL_LINK_1 = DeviceId.deviceId("DualLink1");
    static final DeviceId DUAL_LINK_2 = DeviceId.deviceId("DualLink2");
    static final DeviceId DUAL_LINK_3 = DeviceId.deviceId("DualLink3");

    static final ConnectPoint DUAL_LINK_1_CP_1_IN = ConnectPoint.deviceConnectPoint(DUAL_LINK_1 + "/" + 1);
    static final ConnectPoint DUAL_LINK_1_CP_2_OUT = ConnectPoint.deviceConnectPoint(DUAL_LINK_1 + "/" + 2);
    static final ConnectPoint DUAL_LINK_1_CP_3_OUT = ConnectPoint.deviceConnectPoint(DUAL_LINK_1 + "/" + 3);
    static final ConnectPoint DUAL_LINK_2_CP_1_IN = ConnectPoint.deviceConnectPoint(DUAL_LINK_2 + "/" + 1);
    static final ConnectPoint DUAL_LINK_2_CP_4_IN = ConnectPoint.deviceConnectPoint(DUAL_LINK_2 + "/" + 4);
    static final ConnectPoint DUAL_LINK_2_CP_2_OUT = ConnectPoint.deviceConnectPoint(DUAL_LINK_2 + "/" + 2);
    static final ConnectPoint DUAL_LINK_2_CP_3_OUT = ConnectPoint.deviceConnectPoint(DUAL_LINK_2 + "/" + 3);
    static final ConnectPoint DUAL_LINK_3_CP_1_IN = ConnectPoint.deviceConnectPoint(DUAL_LINK_3 + "/" + 1);
    static final ConnectPoint DUAL_LINK_3_CP_2_IN = ConnectPoint.deviceConnectPoint(DUAL_LINK_3 + "/" + 2);
    static final ConnectPoint DUAL_LINK_3_CP_3_OUT = ConnectPoint.deviceConnectPoint(DUAL_LINK_3 + "/" + 3);

    //match on port 1 and point to group for device 1 and 2
    private static final TrafficTreatment DUAL_LINK_1_GROUP_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .pushMpls()
            .setMpls(MplsLabel.mplsLabel(100))
            .group(GROUP_ID)
            .build();
    private static final FlowRule DUAL_LINK_1_GROUP_FLOW = DefaultFlowEntry.builder().forDevice(DUAL_LINK_1)
            .forTable(0)
            .withPriority(100)
            .withSelector(SINGLE_FLOW_SELECTOR)
            .withTreatment(DUAL_LINK_1_GROUP_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry DUAL_LINK_1_GROUP_FLOW_ENTRY = new DefaultFlowEntry(DUAL_LINK_1_GROUP_FLOW);

    //Match on port 4 and point to group for device 2
    private static final TrafficSelector DUAL_LINK_2_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(4))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.2/32"))
            .build();

    private static final FlowRule DUAL_LINK_2_GROUP_FLOW = DefaultFlowEntry.builder().forDevice(DUAL_LINK_2)
            .forTable(0)
            .withPriority(100)
            .withSelector(DUAL_LINK_2_FLOW_SELECTOR)
            .withTreatment(DUAL_LINK_1_GROUP_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry DUAL_LINK_2_GROUP_FLOW_ENTRY = new DefaultFlowEntry(DUAL_LINK_2_GROUP_FLOW);

    //Flows for device 3 to ouput on port 3
    private static final TrafficTreatment DUAL_LINK_1_OUTPUT_TREATMENT = DefaultTrafficTreatment.builder()
            .popMpls(EthType.EtherType.IPV4.ethType())
            .setOutput(PortNumber.portNumber(3)).build();

    private static final TrafficSelector DUAL_LINK_3_FLOW_SELECTOR_1 = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.2/32"))
            .build();
    private static final FlowRule DUAL_LINK_3_FLOW_1 = DefaultFlowEntry.builder().forDevice(DUAL_LINK_3)
            .forTable(0)
            .withPriority(100)
            .withSelector(DUAL_LINK_3_FLOW_SELECTOR_1)
            .withTreatment(DUAL_LINK_1_OUTPUT_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();

    static final FlowEntry DUAL_LINK_3_FLOW_ENTRY = new DefaultFlowEntry(DUAL_LINK_3_FLOW_1);

    private static final TrafficSelector DUAL_LINK_3_FLOW_SELECTOR_2 = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(2))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.2/32"))
            .build();
    private static final FlowRule DUAL_LINK_3_FLOW_2 = DefaultFlowEntry.builder().forDevice(DUAL_LINK_3)
            .forTable(0)
            .withPriority(100)
            .withSelector(DUAL_LINK_3_FLOW_SELECTOR_2)
            .withTreatment(DUAL_LINK_1_OUTPUT_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();

    static final FlowEntry DUAL_LINK_3_FLOW_ENTRY_2 = new DefaultFlowEntry(DUAL_LINK_3_FLOW_2);

    //Group with two buckets to output on port 2 and 3 of device 1 and 2

    private static final GroupBucket BUCKET_2_DUAL =
            DefaultGroupBucket.createSelectGroupBucket(DUAL_LINK_1_OUTPUT_TREATMENT);

    private static final GroupBuckets BUCKETS_DUAL = new GroupBuckets(ImmutableList.of(BUCKET, BUCKET_2_DUAL));

    static final Group DUAL_LINK_GROUP = new DefaultGroup(GROUP_ID, DUAL_LINK_1, Group.Type.SELECT, BUCKETS_DUAL);

    //Clear Deferred
    static final DeviceId DEFERRED_1 = DeviceId.deviceId("Deferred");

    static final ConnectPoint DEFERRED_CP_1_IN = ConnectPoint.deviceConnectPoint(DEFERRED_1 + "/" + 1);
    static final ConnectPoint DEFERRED_CP_2_OUT = ConnectPoint.deviceConnectPoint(DEFERRED_1 + "/" + 2);

    //match on port 1 and apply deferred actions
    private static final TrafficTreatment DEFERRED_1_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .transition(10)
            .deferred()
            .pushMpls()
            .setMpls(MplsLabel.mplsLabel(100))
            .build();
    private static final FlowRule DEFERRED_FLOW = DefaultFlowEntry.builder().forDevice(DEFERRED_1)
            .forTable(0)
            .withPriority(100)
            .withSelector(SINGLE_FLOW_SELECTOR)
            .withTreatment(DEFERRED_1_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry DEFERRED_FLOW_ENTRY = new DefaultFlowEntry(DEFERRED_FLOW);

    //Multicast Flow and Group Test
    static final DeviceId MULTICAST_GROUP_FLOW_DEVICE = DeviceId.deviceId("MulticastGroupFlowDevice");

    private static final TrafficSelector MULTICAST_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchIPDst(IpPrefix.valueOf("224.0.0.1/32"))
            .matchEthDst(MacAddress.valueOf("01:00:5e:00:00:01"))
            .build();

    private static final FlowRule MULTICAST_GROUP_FLOW =
            DefaultFlowEntry.builder().forDevice(MULTICAST_GROUP_FLOW_DEVICE)
                    .forTable(0)
                    .withPriority(100)
                    .withSelector(MULTICAST_FLOW_SELECTOR)
                    .withTreatment(GROUP_FLOW_TREATMENT)
                    .fromApp(new DefaultApplicationId(0, "TestApp"))
                    .makePermanent()
                    .build();

    static final FlowEntry MULTICAST_GROUP_FLOW_ENTRY = new DefaultFlowEntry(MULTICAST_GROUP_FLOW);

    static final Group MULTICAST_GROUP = new DefaultGroup(GROUP_ID, MULTICAST_GROUP_FLOW_DEVICE,
            Group.Type.SELECT, BUCKETS_MULTIPLE);

    static final ConnectPoint MULTICAST_IN_CP = ConnectPoint.deviceConnectPoint(MULTICAST_GROUP_FLOW_DEVICE + "/" + 1);

    static final ConnectPoint MULTICAST_OUT_CP = ConnectPoint.deviceConnectPoint(MULTICAST_GROUP_FLOW_DEVICE + "/" + 3);

    static final ConnectPoint MULTICAST_OUT_CP_2 =
            ConnectPoint.deviceConnectPoint(MULTICAST_GROUP_FLOW_DEVICE + "/" + 2);

    //match on port 1, clear deferred actions and output
    private static final TrafficTreatment DEFERRED_CLEAR_1_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .wipeDeferred()
            .setOutput(PortNumber.portNumber(2))
            .build();
    private static final FlowRule DEFERRED_CLEAR_FLOW = DefaultFlowEntry.builder().forDevice(DEFERRED_1)
            .forTable(10)
            .withPriority(100)
            .withSelector(SINGLE_FLOW_SELECTOR)
            .withTreatment(DEFERRED_CLEAR_1_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry DEFERRED_CLEAR_FLOW_ENTRY = new DefaultFlowEntry(DEFERRED_CLEAR_FLOW);

    //LLDP

    static final DeviceId LLDP_FLOW_DEVICE = DeviceId.deviceId("LldpDevice");

    private static final TrafficSelector LLDP_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchEthType(EthType.EtherType.LLDP.ethType().toShort())
            .build();

    private static final TrafficTreatment LLDP_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .setOutput(PortNumber.CONTROLLER).build();
    private static final FlowRule LLDP_FLOW = DefaultFlowEntry.builder().forDevice(LLDP_FLOW_DEVICE)
            .forTable(0)
            .withPriority(100)
            .withSelector(LLDP_FLOW_SELECTOR)
            .withTreatment(LLDP_FLOW_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry LLDP_FLOW_ENTRY = new DefaultFlowEntry(LLDP_FLOW);

    static final ConnectPoint LLDP_FLOW_CP = ConnectPoint.deviceConnectPoint(LLDP_FLOW_DEVICE + "/" + 1);

    //No Buckets

    static final DeviceId NO_BUCKET_DEVICE = DeviceId.deviceId("nobucket");

    private static final TrafficSelector NO_BUCKET_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .build();

    private static final GroupId NO_BUCKET_GROUP_ID = GroupId.valueOf(1);

    private static final TrafficTreatment NO_BUCKET_TREATMENT = DefaultTrafficTreatment.builder()
            .group(NO_BUCKET_GROUP_ID)
            .build();
    private static final FlowRule NO_BUCKET_FLOW = DefaultFlowEntry.builder().forDevice(NO_BUCKET_DEVICE)
            .forTable(0)
            .withPriority(100)
            .withSelector(NO_BUCKET_SELECTOR)
            .withTreatment(NO_BUCKET_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry NO_BUCKET_ENTRY = new DefaultFlowEntry(NO_BUCKET_FLOW);

    private static final GroupBuckets NO_BUCKETS = new GroupBuckets(ImmutableList.of());

    static final Group NO_BUCKET_GROUP =
            new DefaultGroup(NO_BUCKET_GROUP_ID, NO_BUCKET_DEVICE, Group.Type.SELECT, NO_BUCKETS);

    static final ConnectPoint NO_BUCKET_CP = ConnectPoint.deviceConnectPoint(NO_BUCKET_DEVICE + "/" + 1);

    //Dual Homing

    static final DeviceId DUAL_HOME_DEVICE_1 = DeviceId.deviceId("DualHomeDevice1");

    static final DeviceId DUAL_HOME_DEVICE_2 = DeviceId.deviceId("DualHomeDevice2");

    static final DeviceId DUAL_HOME_DEVICE_3 = DeviceId.deviceId("DualHomeDevice3");

    static final ConnectPoint DUAL_HOME_CP_1_1 = ConnectPoint.deviceConnectPoint(DUAL_HOME_DEVICE_1 + "/" + 1);
    static final ConnectPoint DUAL_HOME_CP_1_2 = ConnectPoint.deviceConnectPoint(DUAL_HOME_DEVICE_1 + "/" + 2);
    static final ConnectPoint DUAL_HOME_CP_1_3 = ConnectPoint.deviceConnectPoint(DUAL_HOME_DEVICE_1 + "/" + 3);

    static final ConnectPoint DUAL_HOME_CP_2_1 = ConnectPoint.deviceConnectPoint(DUAL_HOME_DEVICE_2 + "/" + 1);
    static final ConnectPoint DUAL_HOME_CP_2_2 = ConnectPoint.deviceConnectPoint(DUAL_HOME_DEVICE_2 + "/" + 2);

    static final ConnectPoint DUAL_HOME_CP_3_1 = ConnectPoint.deviceConnectPoint(DUAL_HOME_DEVICE_3 + "/" + 1);
    static final ConnectPoint DUAL_HOME_CP_3_2 = ConnectPoint.deviceConnectPoint(DUAL_HOME_DEVICE_3 + "/" + 2);


    private static final TrafficSelector  DUAL_HOME_INPUT_FLOW_SELECTOR = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .build();

    private static final GroupId DUAL_HOME_GROUP_ID = GroupId.valueOf(1);

    private static final TrafficTreatment DUAL_HOME_GROUP_TREATMENT = DefaultTrafficTreatment.builder()
            .group(DUAL_HOME_GROUP_ID)
            .build();
    private static final FlowRule DUAL_HOME_INPUT_FLOW = DefaultFlowEntry.builder().forDevice(DUAL_HOME_DEVICE_1)
            .forTable(0)
            .withPriority(100)
            .withSelector(DUAL_HOME_INPUT_FLOW_SELECTOR)
            .withTreatment(DUAL_HOME_GROUP_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry DUAL_HOME_FLOW_ENTRY = new DefaultFlowEntry(DUAL_HOME_INPUT_FLOW);

    private static final TrafficTreatment DUAL_HOME_OUTPUT_1_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .setOutput(PortNumber.portNumber(2)).build();
    private static final TrafficTreatment DUAL_HOME_OUTPUT_2_FLOW_TREATMENT = DefaultTrafficTreatment.builder()
            .setOutput(PortNumber.portNumber(3)).build();

    private static final GroupBucket BUCKET_1_DUAL_HOMED =
            DefaultGroupBucket.createSelectGroupBucket(DUAL_HOME_OUTPUT_1_FLOW_TREATMENT);

    private static final GroupBucket BUCKET_2_DUAL_HOMED =
            DefaultGroupBucket.createSelectGroupBucket(DUAL_HOME_OUTPUT_2_FLOW_TREATMENT);

    private static final GroupBuckets BUCKETS_MULTIPLE_DUAL = new GroupBuckets(ImmutableList.of(BUCKET_1_DUAL_HOMED,
            BUCKET_2_DUAL_HOMED));

    static final Group DUAL_HOME_GROUP = new DefaultGroup(DUAL_HOME_GROUP_ID, DUAL_HOME_DEVICE_1,
            Group.Type.SELECT, BUCKETS_MULTIPLE_DUAL);

    private static final TrafficTreatment DUAL_HOME_TREATMENT = DefaultTrafficTreatment.builder()
            .setOutput(PortNumber.portNumber("2"))
            .build();
    private static final FlowRule DUAL_HOME_OUT_FLOW = DefaultFlowEntry.builder().forDevice(DUAL_HOME_DEVICE_2)
            .forTable(0)
            .withPriority(100)
            .withSelector(DUAL_HOME_INPUT_FLOW_SELECTOR)
            .withTreatment(DUAL_HOME_TREATMENT)
            .fromApp(new DefaultApplicationId(0, "TestApp"))
            .makePermanent()
            .build();
    static final FlowEntry DUAL_HOME_OUT_FLOW_ENTRY = new DefaultFlowEntry(DUAL_HOME_OUT_FLOW);

    //helper elements

    static final String MASTER_1 = "Master1";

    static final Host H1 = new DefaultHost(ProviderId.NONE, HostId.hostId(HOST_ONE), MacAddress.valueOf(100),
            VlanId.NONE, new HostLocation(SINGLE_FLOW_DEVICE, PortNumber.portNumber(2), 0),
            ImmutableSet.of(IpAddress.valueOf("127.0.0.2")));

    static final Host H2 = new DefaultHost(ProviderId.NONE, HostId.hostId(HOST_TWO), MacAddress.valueOf(100),
            VlanId.NONE, new HostLocation(TOPO_FLOW_3_DEVICE, PortNumber.portNumber(2), 0),
            ImmutableSet.of(IpAddress.valueOf("127.0.0.3")));

    static final Host DUAL_HOME_H = new DefaultHost(ProviderId.NONE, HostId.hostId(HOST_DUAL_HOMED),
            MacAddress.valueOf(HOST_DUAL_HOMED_MAC),
            VlanId.NONE, ImmutableSet.of(new HostLocation(DUAL_HOME_DEVICE_2, PortNumber.portNumber(2), 0),
            new HostLocation(DUAL_HOME_DEVICE_3, PortNumber.portNumber(2), 0)),
            ImmutableSet.of(IpAddress.valueOf("127.0.0.4")), true, DefaultAnnotations.builder().build());

    static final TrafficSelector PACKET_OK = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.2/32"))
            .matchVlanId(VlanId.NONE)
            .build();

    static final TrafficSelector PACKET_OK_TOPO = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.3/32"))
            .build();

    static final TrafficSelector PACKET_ARP = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchIPDst(IpPrefix.valueOf("255.255.255.255/32"))
            .matchEthType(EthType.EtherType.ARP.ethType().toShort())
            .build();

    static final TrafficSelector PACKET_LLDP = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchEthType(EthType.EtherType.LLDP.ethType().toShort())
            .build();

    static final TrafficSelector PACKET_OK_MULTICAST = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
            .matchEthDst(MacAddress.valueOf("01:00:5e:00:00:01"))
            .matchIPDst(IpPrefix.valueOf("224.0.0.1/32"))
            .build();

    static final TrafficSelector PACKET_DUAL_HOME = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.4/32"))
            .build();

    static final TrafficSelector PACKET_FAIL = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.99/32"))
            .build();
}
