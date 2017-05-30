/*
 * Copyright 2014-present Open Networking Laboratory
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

import com.google.common.collect.HashMultiset;
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
import org.onlab.util.ClosedOpenRange;
import org.onlab.util.Frequency;
import org.onlab.util.ImmutableByteSequence;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Match;
import org.onosproject.app.ApplicationState;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.Leader;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.core.ApplicationRole;
import org.onosproject.core.DefaultApplication;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.core.Version;
import org.onosproject.event.Change;
import org.onosproject.incubator.net.domain.IntentDomainId;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.net.Annotations;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.CltSignalType;
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
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.GridType;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.MarkerResource;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.OtuSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.TributarySlot;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointDescription;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.domain.DomainIntent;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTableStatisticsEntry;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleBatchRequest;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleExtPayLoad;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.net.flow.criteria.ArpHaCriterion;
import org.onosproject.net.flow.criteria.ArpOpCriterion;
import org.onosproject.net.flow.criteria.ArpPaCriterion;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.ExtensionCriterion;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
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
import org.onosproject.net.flow.criteria.LambdaCriterion;
import org.onosproject.net.flow.criteria.MetadataCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.OchSignalTypeCriterion;
import org.onosproject.net.flow.criteria.OduSignalIdCriterion;
import org.onosproject.net.flow.criteria.OduSignalTypeCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.SctpPortCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.criteria.VlanPcpCriterion;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L1ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.FlowObjectiveIntent;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentOperation;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.OpticalOduIntent;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.ProtectedTransportIntent;
import org.onosproject.net.intent.ProtectionEndpointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.net.intent.constraint.AnnotationConstraint;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.BooleanConstraint;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.net.intent.constraint.HashedPathSelectionConstraint;
import org.onosproject.net.intent.constraint.LatencyConstraint;
import org.onosproject.net.intent.constraint.LinkTypeConstraint;
import org.onosproject.net.intent.constraint.ObstacleConstraint;
import org.onosproject.net.intent.constraint.PartialFailureConstraint;
import org.onosproject.net.intent.constraint.ProtectionConstraint;
import org.onosproject.net.intent.constraint.WaypointConstraint;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.DefaultPacketRequest;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.region.DefaultRegion;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.ContinuousResourceId;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceCodec;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumerId;
import org.onosproject.security.Permission;
import org.onosproject.store.Timestamp;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.MultimapEvent;
import org.onosproject.store.service.SetEvent;
import org.onosproject.store.service.Task;
import org.onosproject.store.service.Versioned;
import org.onosproject.store.service.WorkQueueStats;
import org.onosproject.ui.model.topo.UiTopoLayoutId;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class KryoNamespaces {

    public static final int BASIC_MAX_SIZE = 50;
    public static final KryoNamespace BASIC = KryoNamespace.newBuilder()
            .nextId(KryoNamespace.FLOATING_ID)
            .register(byte[].class)
            .register(AtomicBoolean.class)
            .register(AtomicInteger.class)
            .register(AtomicLong.class)
            .register(new ImmutableListSerializer(),
                      ImmutableList.class,
                      ImmutableList.of(1).getClass(),
                      ImmutableList.of(1, 2).getClass(),
                      ImmutableList.of(1, 2, 3).subList(1, 3).getClass())
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
            .register(Collections.unmodifiableSet(Collections.emptySet()).getClass())
            .register(HashMap.class)
            .register(ConcurrentHashMap.class)
            .register(CopyOnWriteArraySet.class)
            .register(ArrayList.class,
                      LinkedList.class,
                      HashSet.class,
                      LinkedHashSet.class
            )
            .register(HashMultiset.class)
            .register(Maps.immutableEntry("a", "b").getClass())
            .register(new ArraysAsListSerializer(), Arrays.asList().getClass())
            .register(Collections.singletonList(1).getClass())
            .register(Duration.class)
            .register(Collections.emptySet().getClass())
            .register(Optional.class)
            .register(Collections.emptyList().getClass())
            .register(Collections.singleton(Object.class).getClass())
            .register(int[].class)
            .register(long[].class)
            .register(short[].class)
            .register(double[].class)
            .register(float[].class)
            .register(char[].class)
            .register(String[].class)
            .register(boolean[].class)
            .build("BASIC");

    /**
     * KryoNamespace which can serialize ON.lab misc classes.
     */
    public static final int MISC_MAX_SIZE = 30;
    public static final KryoNamespace MISC = KryoNamespace.newBuilder()
            .nextId(KryoNamespace.FLOATING_ID)
            .register(new IpPrefixSerializer(), IpPrefix.class)
            .register(new Ip4PrefixSerializer(), Ip4Prefix.class)
            .register(new Ip6PrefixSerializer(), Ip6Prefix.class)
            .register(new IpAddressSerializer(), IpAddress.class)
            .register(new Ip4AddressSerializer(), Ip4Address.class)
            .register(new Ip6AddressSerializer(), Ip6Address.class)
            .register(new MacAddressSerializer(), MacAddress.class)
            .register(Match.class)
            .register(VlanId.class)
            .register(Frequency.class)
            .register(Bandwidth.class)
            .register(Bandwidth.bps(1L).getClass())
            .register(Bandwidth.bps(1.0).getClass())
            .build("MISC");

    /**
     * KryoNamespace which can serialize API bundle classes.
     */
    public static final int API_MAX_SIZE = 499;
    public static final KryoNamespace API = KryoNamespace.newBuilder()
            .nextId(KryoNamespace.INITIAL_ID)
            .register(BASIC)
            .nextId(KryoNamespace.INITIAL_ID + BASIC_MAX_SIZE)
            .register(MISC)
            .nextId(KryoNamespace.INITIAL_ID + BASIC_MAX_SIZE + MISC_MAX_SIZE)
            .register(
                    Instructions.MeterInstruction.class,
                    MeterId.class,
                    Version.class,
                    ControllerNode.State.class,
                    ApplicationState.class,
                    ApplicationRole.class,
                    DefaultApplication.class,
                    Permission.class,
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
                    Change.class,
                    Leader.class,
                    Leadership.class,
                    LeadershipEvent.class,
                    LeadershipEvent.Type.class,
                    Task.class,
                    WorkQueueStats.class,
                    HostId.class,
                    HostDescription.class,
                    DefaultHostDescription.class,
                    DefaultFlowEntry.class,
                    StoredFlowEntry.class,
                    DefaultFlowRule.class,
                    FlowRule.FlowRemoveReason.class,
                    DefaultPacketRequest.class,
                    PacketPriority.class,
                    FlowEntry.FlowEntryState.class,
                    FlowEntry.FlowLiveType.class,
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
                    MplsBosCriterion.class,
                    TunnelIdCriterion.class,
                    IPv6ExthdrFlagsCriterion.class,
                    LambdaCriterion.class,
                    OchSignalCriterion.class,
                    OchSignalTypeCriterion.class,
                    OduSignalIdCriterion.class,
                    OduSignalTypeCriterion.class,
                    ArpOpCriterion.class,
                    ArpHaCriterion.class,
                    ArpPaCriterion.class,
                    Criterion.class,
                    Criterion.Type.class,
                    DefaultTrafficTreatment.class,
                    Instructions.NoActionInstruction.class,
                    Instructions.OutputInstruction.class,
                    Instructions.GroupInstruction.class,
                    Instructions.SetQueueInstruction.class,
                    Instructions.TableTypeTransition.class,
                    L0ModificationInstruction.class,
                    L0ModificationInstruction.L0SubType.class,
                    L0ModificationInstruction.ModOchSignalInstruction.class,
                    L1ModificationInstruction.class,
                    L1ModificationInstruction.L1SubType.class,
                    L1ModificationInstruction.ModOduSignalIdInstruction.class,
                    L2ModificationInstruction.class,
                    L2ModificationInstruction.L2SubType.class,
                    L2ModificationInstruction.ModEtherInstruction.class,
                    L2ModificationInstruction.ModMplsHeaderInstruction.class,
                    L2ModificationInstruction.ModVlanIdInstruction.class,
                    L2ModificationInstruction.ModVlanPcpInstruction.class,
                    L2ModificationInstruction.ModVlanHeaderInstruction.class,
                    L2ModificationInstruction.ModMplsLabelInstruction.class,
                    L2ModificationInstruction.ModMplsBosInstruction.class,
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
                    OpticalOduIntent.class,
                    FlowObjectiveIntent.class,
                    DiscreteResource.class,
                    ContinuousResource.class,
                    DiscreteResourceId.class,
                    ContinuousResourceId.class,
                    ResourceAllocation.class,
                    ResourceConsumerId.class,
                    ResourceGroup.class,
                    // Constraints
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
                    DefaultAnnotations.class,
                    PortStatistics.class,
                    DefaultPortStatistics.class,
                    IntentDomainId.class,
                    TableStatisticsEntry.class,
                    DefaultTableStatisticsEntry.class,
                    EncapsulationConstraint.class,
                    EncapsulationType.class,
                    HashedPathSelectionConstraint.class,
                    // Flow Objectives
                    DefaultForwardingObjective.class,
                    ForwardingObjective.Flag.class,
                    DefaultFilteringObjective.class,
                    FilteringObjective.Type.class,
                    DefaultNextObjective.class,
                    NextObjective.Type.class,
                    Objective.Operation.class
            )
            .register(new DefaultApplicationIdSerializer(), DefaultApplicationId.class)
            .register(new UriSerializer(), URI.class)
            .register(new NodeIdSerializer(), NodeId.class)
            .register(new ProviderIdSerializer(), ProviderId.class)
            .register(new DeviceIdSerializer(), DeviceId.class)
            .register(new PortNumberSerializer(), PortNumber.class)
            .register(new DefaultPortSerializer(), DefaultPort.class)
            .register(new LinkKeySerializer(), LinkKey.class)
            .register(new ConnectPointSerializer(), ConnectPoint.class)
            .register(new FilteredConnectPointSerializer(), FilteredConnectPoint.class)
            .register(new DefaultLinkSerializer(), DefaultLink.class)
            .register(new MastershipTermSerializer(), MastershipTerm.class)
            .register(new HostLocationSerializer(), HostLocation.class)
            .register(new DefaultOutboundPacketSerializer(), DefaultOutboundPacket.class)
            .register(new AnnotationsSerializer(), DefaultAnnotations.class)
            .register(new ExtensionInstructionSerializer(), Instructions.ExtensionInstructionWrapper.class)
            .register(new ExtensionCriterionSerializer(), ExtensionCriterion.class)
            .register(Region.class)
            .register(Region.Type.class)
            .register(RegionId.class)
            .register(DefaultRegion.class)
            .register(UiTopoLayoutId.class)
            .register(ExtensionSelectorType.class)
            .register(ExtensionTreatmentType.class)
            .register(TransactionId.class)
            .register(TransactionLog.class)
            .register(MapUpdate.class)
            .register(MapUpdate.Type.class)
            .register(Versioned.class)
            .register(MapEvent.class)
            .register(MapEvent.Type.class)
            .register(MultimapEvent.class)
            .register(MultimapEvent.Type.class)
            .register(SetEvent.class)
            .register(SetEvent.Type.class)
            .register(GroupId.class)
            .register(Annotations.class)
            .register(OduSignalType.class)
            .register(OchSignalType.class)
            .register(GridType.class)
            .register(ChannelSpacing.class)
            .register(CltSignalType.class)
            .register(OchSignal.class)
            .register(OduSignalId.class)
            .register(TributarySlot.class)
            .register(OtuSignalType.class)
            .register(
                    org.onlab.packet.MplsLabel.class,
                    org.onlab.packet.MPLS.class
            )
            .register(ClosedOpenRange.class)
            .register(DiscreteResourceCodec.class)
            .register(new ImmutableByteSequenceSerializer(), ImmutableByteSequence.class)
            .register(PathIntent.ProtectionType.class)
            .register(ProtectionConstraint.class)
            .register(ProtectedTransportEndpointDescription.class)
            .register(ProtectionEndpointIntent.class)
            .register(ProtectedTransportIntent.class)
            .register(MarkerResource.class)
            .register(new BitSetSerializer(), BitSet.class)
            .register(DomainIntent.class)
            .build("API");

    /**
     * Kryo registration Id for user custom registration.
     */
    public static final int BEGIN_USER_CUSTOM_ID = API_MAX_SIZE + 1;

    // not to be instantiated
    private KryoNamespaces() {
    }
}
