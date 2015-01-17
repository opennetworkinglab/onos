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
package org.onosproject.store.serializers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.ChassisId;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.app.ApplicationState;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.core.DefaultApplication;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.Version;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Element;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleBatchRequest;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentOperation;
import org.onosproject.net.intent.IntentOperations;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.net.intent.constraint.AnnotationConstraint;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.BooleanConstraint;
import org.onosproject.net.intent.constraint.LambdaConstraint;
import org.onosproject.net.intent.constraint.LatencyConstraint;
import org.onosproject.net.intent.constraint.LinkTypeConstraint;
import org.onosproject.net.intent.constraint.ObstacleConstraint;
import org.onosproject.net.intent.constraint.WaypointConstraint;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.Bandwidth;
import org.onosproject.net.resource.BandwidthResourceAllocation;
import org.onosproject.net.resource.BandwidthResourceRequest;
import org.onosproject.net.resource.DefaultLinkResourceAllocations;
import org.onosproject.net.resource.DefaultLinkResourceRequest;
import org.onosproject.net.resource.Lambda;
import org.onosproject.net.resource.LambdaResourceAllocation;
import org.onosproject.net.resource.LambdaResourceRequest;
import org.onosproject.net.resource.LinkResourceRequest;
import org.onosproject.store.Timestamp;
import org.onosproject.store.service.BatchReadRequest;
import org.onosproject.store.service.BatchWriteRequest;
import org.onosproject.store.service.ReadRequest;
import org.onosproject.store.service.ReadResult;
import org.onosproject.store.service.ReadStatus;
import org.onosproject.store.service.VersionedValue;
import org.onosproject.store.service.WriteRequest;
import org.onosproject.store.service.WriteResult;
import org.onosproject.store.service.WriteStatus;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;

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
            .register(Optional.class)
            .register(Collections.emptyList().getClass())
            .register(Collections.unmodifiableSet(Collections.emptySet()).getClass())
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
                    Version.class,
                    ControllerNode.State.class,
                    ApplicationState.class,
                    DefaultApplication.class,
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
                    FlowRule.Type.class,
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
                    Criteria.OpticalSignalTypeCriterion.class,
                    Criteria.LambdaCriterion.class,
                    Criteria.MplsCriterion.class,
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
                    FlowRuleBatchEvent.class,
                    FlowRuleBatchEvent.Type.class,
                    FlowRuleBatchRequest.class,
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
