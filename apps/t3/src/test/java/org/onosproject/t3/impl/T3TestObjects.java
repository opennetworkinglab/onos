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
import org.onlab.packet.VlanId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.ConnectPoint;
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

    private T3TestObjects(){
        //banning construction
    }

    private static final String HOST_ONE_MAC = "00:00:00:00:00:01";
    private static final String HOST_TWO_MAC = "00:00:00:00:00:02";
    private static final String HOST_ONE_VLAN = "None";
    private static final String HOST_TWO_VLAN = "None";
    private static final String HOST_ONE = HOST_ONE_MAC + "/" + HOST_ONE_VLAN;
    private static final String HOST_TWO = HOST_TWO_MAC + "/" + HOST_TWO_VLAN;

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

    private static final GroupBucket BUCKET = DefaultGroupBucket.createSelectGroupBucket(OUTPUT_FLOW_TREATMENT);

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




    //helper elements

    static final Host H1 = new DefaultHost(ProviderId.NONE, HostId.hostId(HOST_ONE), MacAddress.valueOf(100),
            VlanId.NONE, new HostLocation(SINGLE_FLOW_DEVICE, PortNumber.portNumber(2), 0),
            ImmutableSet.of(IpAddress.valueOf("127.0.0.2")));

    static final Host H2 = new DefaultHost(ProviderId.NONE, HostId.hostId(HOST_TWO), MacAddress.valueOf(100),
            VlanId.NONE, new HostLocation(TOPO_FLOW_3_DEVICE, PortNumber.portNumber(2), 0),
            ImmutableSet.of(IpAddress.valueOf("127.0.0.3")));

    static final TrafficSelector PACKET_OK = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.2/32"))
            .build();

    static final TrafficSelector PACKET_OK_TOPO = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.3/32"))
            .build();

    static final TrafficSelector PACKET_FAIL = DefaultTrafficSelector.builder()
            .matchInPort(PortNumber.portNumber(1))
            .matchIPSrc(IpPrefix.valueOf("127.0.0.1/32"))
            .matchIPDst(IpPrefix.valueOf("127.0.0.99/32"))
            .build();
}
