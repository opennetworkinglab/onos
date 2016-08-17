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
package org.onosproject.codec.impl;

import com.codahale.metrics.Metric;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualHost;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Path;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.Port;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.region.Region;
import org.onosproject.net.statistic.Load;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.*;

/**
 * Implementation of the JSON codec brokering service.
 */
@Component(immediate = true)
@Service
public class CodecManager implements CodecService {

    private static Logger log = LoggerFactory.getLogger(CodecManager.class);

    private final Map<Class<?>, JsonCodec> codecs = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        codecs.clear();
        registerCodec(Application.class, new ApplicationCodec());
        registerCodec(ApplicationId.class, new ApplicationIdCodec());
        registerCodec(ControllerNode.class, new ControllerNodeCodec());
        registerCodec(Annotations.class, new AnnotationsCodec());
        registerCodec(Device.class, new DeviceCodec());
        registerCodec(Port.class, new PortCodec());
        registerCodec(ConnectPoint.class, new ConnectPointCodec());
        registerCodec(Link.class, new LinkCodec());
        registerCodec(Host.class, new HostCodec());
        registerCodec(HostLocation.class, new HostLocationCodec());
        registerCodec(HostToHostIntent.class, new HostToHostIntentCodec());
        registerCodec(PointToPointIntent.class, new PointToPointIntentCodec());
        registerCodec(SinglePointToMultiPointIntent.class, new SinglePointToMultiPointIntentCodec());
        registerCodec(Intent.class, new IntentCodec());
        registerCodec(ConnectivityIntent.class, new ConnectivityIntentCodec());
        registerCodec(FlowEntry.class, new FlowEntryCodec());
        registerCodec(FlowRule.class, new FlowRuleCodec());
        registerCodec(TrafficTreatment.class, new TrafficTreatmentCodec());
        registerCodec(TrafficSelector.class, new TrafficSelectorCodec());
        registerCodec(Instruction.class, new InstructionCodec());
        registerCodec(Criterion.class, new CriterionCodec());
        registerCodec(Ethernet.class, new EthernetCodec());
        registerCodec(Constraint.class, new ConstraintCodec());
        registerCodec(Topology.class, new TopologyCodec());
        registerCodec(TopologyCluster.class, new TopologyClusterCodec());
        registerCodec(Path.class, new PathCodec());
        registerCodec(DisjointPath.class, new DisjointPathCodec());
        registerCodec(Group.class, new GroupCodec());
        registerCodec(Driver.class, new DriverCodec());
        registerCodec(GroupBucket.class, new GroupBucketCodec());
        registerCodec(Load.class, new LoadCodec());
        registerCodec(MeterRequest.class, new MeterRequestCodec());
        registerCodec(Meter.class, new MeterCodec());
        registerCodec(Band.class, new MeterBandCodec());
        registerCodec(TableStatisticsEntry.class, new TableStatisticsEntryCodec());
        registerCodec(PortStatistics.class, new PortStatisticsCodec());
        registerCodec(Metric.class, new MetricCodec());
        registerCodec(FilteringObjective.class, new FilteringObjectiveCodec());
        registerCodec(ForwardingObjective.class, new ForwardingObjectiveCodec());
        registerCodec(NextObjective.class, new NextObjectiveCodec());
        registerCodec(McastRoute.class, new McastRouteCodec());
        registerCodec(DeviceKey.class, new DeviceKeyCodec());
        registerCodec(Region.class, new RegionCodec());
        registerCodec(TenantId.class, new TenantIdCodec());
        registerCodec(VirtualNetwork.class, new VirtualNetworkCodec());
        registerCodec(VirtualDevice.class, new VirtualDeviceCodec());
        registerCodec(VirtualPort.class, new VirtualPortCodec());
        registerCodec(VirtualLink.class, new VirtualLinkCodec());
        registerCodec(VirtualHost.class, new VirtualHostCodec());
        registerCodec(MastershipTerm.class, new MastershipTermCodec());
        registerCodec(MastershipRole.class, new MastershipRoleCodec());
        registerCodec(RoleInfo.class, new RoleInfoCodec());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        codecs.clear();
        log.info("Stopped");
    }

    @Override
    public Set<Class<?>> getCodecs() {
        checkPermission(CODEC_READ);
        return ImmutableSet.copyOf(codecs.keySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> JsonCodec<T> getCodec(Class<T> entityClass) {
        checkPermission(CODEC_READ);
        return codecs.get(entityClass);
    }

    @Override
    public <T> void registerCodec(Class<T> entityClass, JsonCodec<T> codec) {
        checkPermission(CODEC_WRITE);
        codecs.putIfAbsent(entityClass, codec);
    }

    @Override
    public void unregisterCodec(Class<?> entityClass) {
        checkPermission(CODEC_WRITE);
        codecs.remove(entityClass);
    }

}
