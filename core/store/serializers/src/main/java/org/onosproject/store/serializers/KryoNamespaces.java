/*
 * Copyright 2014-2015 Open Networking Laboratory
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
import com.google.common.collect.Maps;
import org.onlab.packet.ChassisId;
import org.onlab.packet.EthType;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onlab.util.Frequency;
import org.onlab.util.KryoNamespace;
import org.onosproject.app.ApplicationState;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.core.ApplicationRole;
import org.onosproject.core.DefaultApplication;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.Version;
import org.onosproject.incubator.net.domain.IntentDomainId;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.net.Annotations;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Element;
import org.onosproject.net.GridType;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.IndexedLambda;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.OchPort;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.OduCltPort;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.OmsPort;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.OchPortDescription;
import org.onosproject.net.device.OduCltPortDescription;
import org.onosproject.net.device.OmsPortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTableStatisticsEntry;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleBatchRequest;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleExtPayLoad;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPDscpCriterion;
import org.onosproject.net.flow.criteria.IPEcnCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.IPv6ExthdrFlagsCriterion;
import org.onosproject.net.flow.criteria.IPv6FlowLabelCriterion;
import org.onosproject.net.flow.criteria.IPv6NDLinkLayerAddressCriterion;
import org.onosproject.net.flow.criteria.IPv6NDTargetAddressCriterion;
import org.onosproject.net.flow.criteria.IcmpCodeCriterion;
import org.onosproject.net.flow.criteria.IcmpTypeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6CodeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6TypeCriterion;
import org.onosproject.net.flow.criteria.IndexedLambdaCriterion;
import org.onosproject.net.flow.criteria.LambdaCriterion;
import org.onosproject.net.flow.criteria.MetadataCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.OchSignalTypeCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.SctpPortCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.criteria.VlanPcpCriterion;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentOperation;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MplsIntent;
import org.onosproject.net.intent.MplsPathIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.OpticalCircuitIntent;
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
import org.onosproject.net.intent.constraint.PartialFailureConstraint;
import org.onosproject.net.intent.constraint.WaypointConstraint;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.newresource.ResourceAllocation;
import org.onosproject.net.newresource.ResourcePath;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.DefaultPacketRequest;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.link.BandwidthResource;
import org.onosproject.net.resource.link.BandwidthResourceAllocation;
import org.onosproject.net.resource.link.BandwidthResourceRequest;
import org.onosproject.net.resource.link.DefaultLinkResourceAllocations;
import org.onosproject.net.resource.link.DefaultLinkResourceRequest;
import org.onosproject.net.resource.link.LambdaResource;
import org.onosproject.net.resource.link.LambdaResourceAllocation;
import org.onosproject.net.resource.link.LambdaResourceRequest;
import org.onosproject.net.resource.link.LinkResourceRequest;
import org.onosproject.net.resource.link.MplsLabel;
import org.onosproject.net.resource.link.MplsLabelResourceAllocation;
import org.onosproject.net.resource.link.MplsLabelResourceRequest;
import org.onosproject.store.Timestamp;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.SetEvent;
import org.onosproject.store.service.Versioned;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class KryoNamespaces {

    public static final KryoNamespace BASIC = KryoNamespace.newBuilder()
            .nextId(KryoNamespace.FLOATING_ID)
            .register(byte[].class)
            .register(AtomicBoolean.class)
            .register(AtomicInteger.class)
            .register(AtomicLong.class)
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
            .register(ConcurrentHashMap.class)
            .register(CopyOnWriteArraySet.class)
            .register(ArrayList.class,
                      LinkedList.class,
                      HashSet.class
                      )
            .register(Maps.immutableEntry("a", "b").getClass())
            .register(new ArraysAsListSerializer(), Arrays.asList().getClass())
            .register(Collections.singletonList(1).getClass())
            .register(Duration.class)
            .register(Collections.emptySet().getClass())
            .register(Optional.class)
            .register(Collections.emptyList().getClass())
            .register(Collections.unmodifiableSet(Collections.emptySet()).getClass())
            .register(Collections.singleton(Object.class).getClass())
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
            .register(Frequency.class)
            .register(Bandwidth.class)
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
                    Instructions.MeterInstruction.class,
                    MeterId.class,
                    Version.class,
                    ControllerNode.State.class,
                    ApplicationState.class,
                    ApplicationRole.class,
                    DefaultApplication.class,
                    Device.Type.class,
                    Port.Type.class,
                    ChassisId.class,
                    DefaultControllerNode.class,
                    DefaultDevice.class,
                    DefaultDeviceDescription.class,
                    DefaultHost.class,
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
                    DefaultPacketRequest.class,
                    PacketPriority.class,
                    FlowEntry.FlowEntryState.class,
                    FlowId.class,
                    DefaultTrafficSelector.class,
                    PortCriterion.class,
                    MetadataCriterion.class,
                    EthCriterion.class,
                    EthType.class,
                    EthTypeCriterion.class,
                    VlanIdCriterion.class,
                    VlanPcpCriterion.class,
                    IPDscpCriterion.class,
                    IPEcnCriterion.class,
                    IPProtocolCriterion.class,
                    IPCriterion.class,
                    TpPort.class,
                    TcpPortCriterion.class,
                    UdpPortCriterion.class,
                    SctpPortCriterion.class,
                    IcmpTypeCriterion.class,
                    IcmpCodeCriterion.class,
                    IPv6FlowLabelCriterion.class,
                    Icmpv6TypeCriterion.class,
                    Icmpv6CodeCriterion.class,
                    IPv6NDTargetAddressCriterion.class,
                    IPv6NDLinkLayerAddressCriterion.class,
                    MplsCriterion.class,
                    TunnelIdCriterion.class,
                    IPv6ExthdrFlagsCriterion.class,
                    LambdaCriterion.class,
                    IndexedLambdaCriterion.class,
                    OchSignalCriterion.class,
                    OchSignalTypeCriterion.class,
                    Criterion.class,
                    Criterion.Type.class,
                    DefaultTrafficTreatment.class,
                    Instructions.DropInstruction.class,
                    Instructions.NoActionInstruction.class,
                    Instructions.OutputInstruction.class,
                    Instructions.GroupInstruction.class,
                    Instructions.TableTypeTransition.class,
                    L0ModificationInstruction.class,
                    L0ModificationInstruction.L0SubType.class,
                    L0ModificationInstruction.ModLambdaInstruction.class,
                    L0ModificationInstruction.ModOchSignalInstruction.class,
                    L2ModificationInstruction.class,
                    L2ModificationInstruction.L2SubType.class,
                    L2ModificationInstruction.ModEtherInstruction.class,
                    L2ModificationInstruction.PushHeaderInstructions.class,
                    L2ModificationInstruction.ModVlanIdInstruction.class,
                    L2ModificationInstruction.ModVlanPcpInstruction.class,
                    L2ModificationInstruction.PopVlanInstruction.class,
                    L2ModificationInstruction.ModMplsLabelInstruction.class,
                    L2ModificationInstruction.ModMplsTtlInstruction.class,
                    L2ModificationInstruction.ModTunnelIdInstruction.class,
                    L3ModificationInstruction.class,
                    L3ModificationInstruction.L3SubType.class,
                    L3ModificationInstruction.ModIPInstruction.class,
                    L3ModificationInstruction.ModIPv6FlowLabelInstruction.class,
                    L3ModificationInstruction.ModTtlInstruction.class,
                    L4ModificationInstruction.class,
                    L4ModificationInstruction.L4SubType.class,
                    L4ModificationInstruction.ModTransportPortInstruction.class,
                    RoleInfo.class,
                    FlowRuleBatchEvent.class,
                    FlowRuleBatchEvent.Type.class,
                    FlowRuleBatchRequest.class,
                    FlowRuleBatchOperation.class,
                    FlowRuleEvent.class,
                    FlowRuleEvent.Type.class,
                    CompletedBatchOperation.class,
                    FlowRuleBatchEntry.class,
                    FlowRuleBatchEntry.FlowRuleOperation.class,
                    IntentId.class,
                    IntentState.class,
                    //Key.class, is abstract
                    Key.of(1L, new DefaultApplicationId(0, "bar")).getClass(), //LongKey.class
                    Key.of("foo", new DefaultApplicationId(0, "bar")).getClass(), //StringKey.class
                    Intent.class,
                    ConnectivityIntent.class,
                    PathIntent.class,
                    DefaultPath.class,
                    DefaultEdgeLink.class,
                    HostToHostIntent.class,
                    PointToPointIntent.class,
                    MultiPointToSinglePointIntent.class,
                    SinglePointToMultiPointIntent.class,
                    FlowRuleIntent.class,
                    LinkCollectionIntent.class,
                    OpticalConnectivityIntent.class,
                    OpticalPathIntent.class,
                    OpticalCircuitIntent.class,
                    LinkResourceRequest.class,
                    DefaultLinkResourceRequest.class,
                    BandwidthResourceRequest.class,
                    LambdaResourceRequest.class,
                    LambdaResource.class,
                    BandwidthResource.class,
                    DefaultLinkResourceAllocations.class,
                    BandwidthResourceAllocation.class,
                    LambdaResourceAllocation.class,
                    ResourcePath.class,
                    ResourceAllocation.class,
                    // Constraints
                    LambdaConstraint.class,
                    BandwidthConstraint.class,
                    LinkTypeConstraint.class,
                    LatencyConstraint.class,
                    WaypointConstraint.class,
                    ObstacleConstraint.class,
                    AnnotationConstraint.class,
                    BooleanConstraint.class,
                    PartialFailureConstraint.class,
                    IntentOperation.class,
                    FlowRuleExtPayLoad.class,
                    Frequency.class,
                    DefaultAnnotations.class,
                    PortStatistics.class,
                    DefaultPortStatistics.class,
                    IntentDomainId.class,
                    TableStatisticsEntry.class,
                    DefaultTableStatisticsEntry.class
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
            .register(new AnnotationsSerializer(), DefaultAnnotations.class)
            .register(Versioned.class)
            .register(MapEvent.class)
            .register(MapEvent.Type.class)
            .register(SetEvent.class)
            .register(SetEvent.Type.class)
            .register(DefaultGroupId.class)
            .register(Annotations.class)
            .register(OmsPort.class)
            .register(OchPort.class)
            .register(OduSignalType.class)
            .register(OchSignalType.class)
            .register(GridType.class)
            .register(ChannelSpacing.class)
            .register(OduCltPort.class)
            .register(OduCltPort.SignalType.class)
            .register(IndexedLambda.class)
            .register(OchSignal.class)
            .register(OduCltPortDescription.class)
            .register(OchPortDescription.class)
            .register(OmsPortDescription.class)
            .register(
                    MplsIntent.class,
                    MplsPathIntent.class,
                    MplsLabelResourceAllocation.class,
                    MplsLabelResourceRequest.class,
                    MplsLabel.class,
                    org.onlab.packet.MplsLabel.class,
                    org.onlab.packet.MPLS.class
            )

            .build();


    // not to be instantiated
    private KryoNamespaces() {}
}
