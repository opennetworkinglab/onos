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
package org.onlab.onos.store.serializers;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.Leadership;
import org.onlab.onos.cluster.LeadershipEvent;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.cluster.RoleInfo;
import org.onlab.onos.core.DefaultApplicationId;
import org.onlab.onos.core.DefaultGroupId;
import org.onlab.onos.mastership.MastershipTerm;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultAnnotations;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.DefaultEdgeLink;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.DefaultPath;
import org.onlab.onos.net.DefaultPort;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Element;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.LinkKey;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DefaultDeviceDescription;
import org.onlab.onos.net.device.DefaultPortDescription;
import org.onlab.onos.net.flow.CompletedBatchOperation;
import org.onlab.onos.net.flow.DefaultFlowEntry;
import org.onlab.onos.net.flow.DefaultFlowRule;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowId;
import org.onlab.onos.net.flow.FlowRuleBatchEntry;
import org.onlab.onos.net.flow.FlowRuleBatchOperation;
import org.onlab.onos.net.flow.StoredFlowEntry;
import org.onlab.onos.net.flow.criteria.Criteria;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.onos.net.flow.instructions.Instructions;
import org.onlab.onos.net.flow.instructions.L0ModificationInstruction;
import org.onlab.onos.net.flow.instructions.L2ModificationInstruction;
import org.onlab.onos.net.flow.instructions.L3ModificationInstruction;
import org.onlab.onos.net.host.DefaultHostDescription;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.intent.ConnectivityIntent;
import org.onlab.onos.net.intent.HostToHostIntent;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentOperation;
import org.onlab.onos.net.intent.IntentOperations;
import org.onlab.onos.net.intent.IntentState;
import org.onlab.onos.net.intent.LinkCollectionIntent;
import org.onlab.onos.net.intent.MultiPointToSinglePointIntent;
import org.onlab.onos.net.intent.OpticalConnectivityIntent;
import org.onlab.onos.net.intent.OpticalPathIntent;
import org.onlab.onos.net.intent.PathIntent;
import org.onlab.onos.net.intent.PointToPointIntent;
import org.onlab.onos.net.intent.constraint.AnnotationConstraint;
import org.onlab.onos.net.intent.SinglePointToMultiPointIntent;
import org.onlab.onos.net.intent.constraint.BandwidthConstraint;
import org.onlab.onos.net.intent.constraint.BooleanConstraint;
import org.onlab.onos.net.intent.constraint.LambdaConstraint;
import org.onlab.onos.net.intent.constraint.LatencyConstraint;
import org.onlab.onos.net.intent.constraint.LinkTypeConstraint;
import org.onlab.onos.net.intent.constraint.ObstacleConstraint;
import org.onlab.onos.net.intent.constraint.WaypointConstraint;
import org.onlab.onos.net.link.DefaultLinkDescription;
import org.onlab.onos.net.packet.DefaultOutboundPacket;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.resource.Bandwidth;
import org.onlab.onos.net.resource.BandwidthResourceAllocation;
import org.onlab.onos.net.resource.BandwidthResourceRequest;
import org.onlab.onos.net.resource.DefaultLinkResourceAllocations;
import org.onlab.onos.net.resource.DefaultLinkResourceRequest;
import org.onlab.onos.net.resource.Lambda;
import org.onlab.onos.net.resource.LambdaResourceAllocation;
import org.onlab.onos.net.resource.LambdaResourceRequest;
import org.onlab.onos.net.resource.LinkResourceRequest;
import org.onlab.onos.store.Timestamp;
import org.onlab.onos.store.service.BatchReadRequest;
import org.onlab.onos.store.service.BatchWriteRequest;
import org.onlab.onos.store.service.ReadRequest;
import org.onlab.onos.store.service.ReadResult;
import org.onlab.onos.store.service.ReadStatus;
import org.onlab.onos.store.service.VersionedValue;
import org.onlab.onos.store.service.WriteRequest;
import org.onlab.onos.store.service.WriteResult;
import org.onlab.onos.store.service.WriteStatus;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public final class KryoNamespaces {

    public static final KryoNamespace BASIC = KryoNamespace.newBuilder()
            .nextId(KryoNamespace.FLOATING_ID)
            .register(byte[].class)
            .register(new ImmutableListSerializer(),
                      ImmutableList.class,
                      ImmutableList.of(1).getClass(),
                      ImmutableList.of(1, 2).getClass())
            .register(new ImmutableSetSerializer(),
                      ImmutableSet.class,
                      ImmutableSet.of().getClass(),
                      ImmutableSet.of(1).getClass(),
                      ImmutableSet.of(1, 2).getClass())
            .register(new ImmutableMapSerializer(),
                      ImmutableMap.class,
                      ImmutableMap.of().getClass(),
                      ImmutableMap.of("a", 1).getClass(),
                      ImmutableMap.of("R", 2, "D", 2).getClass())
            .register(HashMap.class)
            .register(ArrayList.class,
                      LinkedList.class,
                      HashSet.class
                      )
            .register(new ArraysAsListSerializer(), Arrays.asList().getClass())
            .register(Collections.singletonList(1).getClass())
            .register(Duration.class)
            .register(Collections.emptySet().getClass())
            .build();

    /**
     * KryoNamespace which can serialize ON.lab misc classes.
     */
    public static final KryoNamespace MISC = KryoNamespace.newBuilder()
            .nextId(KryoNamespace.FLOATING_ID)
            .register(new IpPrefixSerializer(), IpPrefix.class)
            .register(new Ip4PrefixSerializer(), Ip4Prefix.class)
            .register(new Ip6PrefixSerializer(), Ip6Prefix.class)
            .register(new IpAddressSerializer(), IpAddress.class)
            .register(new Ip4AddressSerializer(), Ip4Address.class)
            .register(new Ip6AddressSerializer(), Ip6Address.class)
            .register(new MacAddressSerializer(), MacAddress.class)
            .register(VlanId.class)
            .build();

    /**
     * Kryo registration Id for user custom registration.
     */
    public static final int BEGIN_USER_CUSTOM_ID = 300;

    // TODO: Populate other classes
    /**
     * KryoNamespace which can serialize API bundle classes.
     */
    public static final KryoNamespace API = KryoNamespace.newBuilder()
            .nextId(KryoNamespace.INITIAL_ID)
            .register(BASIC)
            .nextId(KryoNamespace.INITIAL_ID + 30)
            .register(MISC)
            .nextId(KryoNamespace.INITIAL_ID + 30 + 10)
            .register(
                    ControllerNode.State.class,
                    Device.Type.class,
                    Port.Type.class,
                    ChassisId.class,
                    DefaultAnnotations.class,
                    DefaultControllerNode.class,
                    DefaultDevice.class,
                    DefaultDeviceDescription.class,
                    DefaultLinkDescription.class,
                    Port.class,
                    DefaultPortDescription.class,
                    Element.class,
                    Link.Type.class,
                    Link.State.class,
                    Timestamp.class,
                    Leadership.class,
                    LeadershipEvent.class,
                    LeadershipEvent.Type.class,
                    HostId.class,
                    HostDescription.class,
                    DefaultHostDescription.class,
                    DefaultFlowEntry.class,
                    StoredFlowEntry.class,
                    DefaultFlowRule.class,
                    DefaultFlowEntry.class,
                    FlowEntry.FlowEntryState.class,
                    FlowId.class,
                    DefaultTrafficSelector.class,
                    Criteria.PortCriterion.class,
                    Criteria.EthCriterion.class,
                    Criteria.EthTypeCriterion.class,
                    Criteria.IPCriterion.class,
                    Criteria.IPProtocolCriterion.class,
                    Criteria.VlanIdCriterion.class,
                    Criteria.VlanPcpCriterion.class,
                    Criteria.TcpPortCriterion.class,
                    Criterion.class,
                    Criterion.Type.class,
                    DefaultTrafficTreatment.class,
                    Instructions.DropInstruction.class,
                    Instructions.OutputInstruction.class,
                    L0ModificationInstruction.class,
                    L0ModificationInstruction.L0SubType.class,
                    L0ModificationInstruction.ModLambdaInstruction.class,
                    L2ModificationInstruction.class,
                    L2ModificationInstruction.L2SubType.class,
                    L2ModificationInstruction.ModEtherInstruction.class,
                    L2ModificationInstruction.ModVlanIdInstruction.class,
                    L2ModificationInstruction.ModVlanPcpInstruction.class,
                    L3ModificationInstruction.class,
                    L3ModificationInstruction.L3SubType.class,
                    L3ModificationInstruction.ModIPInstruction.class,
                    RoleInfo.class,
                    FlowRuleBatchOperation.class,
                    CompletedBatchOperation.class,
                    FlowRuleBatchEntry.class,
                    FlowRuleBatchEntry.FlowRuleOperation.class,
                    IntentId.class,
                    IntentState.class,
                    Intent.class,
                    ConnectivityIntent.class,
                    PathIntent.class,
                    DefaultPath.class,
                    DefaultEdgeLink.class,
                    HostToHostIntent.class,
                    PointToPointIntent.class,
                    MultiPointToSinglePointIntent.class,
                    SinglePointToMultiPointIntent.class,
                    LinkCollectionIntent.class,
                    OpticalConnectivityIntent.class,
                    OpticalPathIntent.class,
                    LinkResourceRequest.class,
                    DefaultLinkResourceRequest.class,
                    BandwidthResourceRequest.class,
                    LambdaResourceRequest.class,
                    Lambda.class,
                    Bandwidth.class,
                    DefaultLinkResourceAllocations.class,
                    BandwidthResourceAllocation.class,
                    LambdaResourceAllocation.class,
                    // Constraints
                    LambdaConstraint.class,
                    BandwidthConstraint.class,
                    LinkTypeConstraint.class,
                    LatencyConstraint.class,
                    WaypointConstraint.class,
                    ObstacleConstraint.class,
                    AnnotationConstraint.class,
                    BooleanConstraint.class,
                    IntentOperation.class,
                    IntentOperations.class
                    )
            .register(new DefaultApplicationIdSerializer(), DefaultApplicationId.class)
            .register(new URISerializer(), URI.class)
            .register(new NodeIdSerializer(), NodeId.class)
            .register(new ProviderIdSerializer(), ProviderId.class)
            .register(new DeviceIdSerializer(), DeviceId.class)
            .register(new PortNumberSerializer(), PortNumber.class)
            .register(new DefaultPortSerializer(), DefaultPort.class)
            .register(new LinkKeySerializer(), LinkKey.class)
            .register(new ConnectPointSerializer(), ConnectPoint.class)
            .register(new DefaultLinkSerializer(), DefaultLink.class)
            .register(new MastershipTermSerializer(), MastershipTerm.class)
            .register(new HostLocationSerializer(), HostLocation.class)
            .register(new DefaultOutboundPacketSerializer(), DefaultOutboundPacket.class)
            .register(ReadRequest.class)
            .register(WriteRequest.class)
            .register(WriteRequest.Type.class)
            .register(WriteResult.class)
            .register(ReadResult.class)
            .register(BatchReadRequest.class)
            .register(BatchWriteRequest.class)
            .register(ReadStatus.class)
            .register(WriteStatus.class)
            .register(VersionedValue.class)
            .register(DefaultGroupId.class)

            .build();


    // not to be instantiated
    private KryoNamespaces() {}
}
