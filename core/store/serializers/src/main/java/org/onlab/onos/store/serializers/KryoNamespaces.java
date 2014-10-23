package org.onlab.onos.store.serializers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.cluster.RoleInfo;
import org.onlab.onos.mastership.MastershipTerm;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultAnnotations;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.DefaultLink;
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
import org.onlab.onos.net.host.DefaultHostDescription;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.link.DefaultLinkDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.Timestamp;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public final class KryoNamespaces {

    /**
     * KryoNamespace which can serialize ON.lab misc classes.
     */
    public static final KryoNamespace MISC = KryoNamespace.newBuilder()
            .register(IpPrefix.class, new IpPrefixSerializer())
            .register(IpAddress.class, new IpAddressSerializer())
            .register(MacAddress.class, new MacAddressSerializer())
            .register(VlanId.class)
            .build();

    // TODO: Populate other classes
    /**
     * KryoNamespace which can serialize API bundle classes.
     */
    public static final KryoNamespace API = KryoNamespace.newBuilder()
            .register(MISC)
            .register(ImmutableMap.class, new ImmutableMapSerializer())
            .register(ImmutableList.class, new ImmutableListSerializer())
            .register(ImmutableSet.class, new ImmutableSetSerializer())
            .register(
                    //
                    ArrayList.class,
                    Arrays.asList().getClass(),
                    HashMap.class,
                    HashSet.class,
                    LinkedList.class,
                    //
                    //
                    ControllerNode.State.class,
                    Device.Type.class,
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
                    Timestamp.class,
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
                    RoleInfo.class,
                    FlowRuleBatchOperation.class,
                    CompletedBatchOperation.class,
                    FlowRuleBatchEntry.class,
                    FlowRuleBatchEntry.FlowRuleOperation.class
                    )
            .register(URI.class, new URISerializer())
            .register(NodeId.class, new NodeIdSerializer())
            .register(ProviderId.class, new ProviderIdSerializer())
            .register(DeviceId.class, new DeviceIdSerializer())
            .register(PortNumber.class, new PortNumberSerializer())
            .register(DefaultPort.class, new DefaultPortSerializer())
            .register(LinkKey.class, new LinkKeySerializer())
            .register(ConnectPoint.class, new ConnectPointSerializer())
            .register(DefaultLink.class, new DefaultLinkSerializer())
            .register(MastershipTerm.class, new MastershipTermSerializer())
            .register(HostLocation.class, new HostLocationSerializer())

            .build();


    // not to be instantiated
    private KryoNamespaces() {}
}
