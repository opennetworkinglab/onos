/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.pi.demo.app.ecmp;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionGroupId;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiGroupKey;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.pi.demo.app.common.AbstractUpgradableFabricApp;
import org.onosproject.pipelines.basic.PipeconfLoader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.pipelines.basic.BasicConstants.ACT_PRF_WCMP_SELECTOR_ID;
import static org.onosproject.pipelines.basic.BasicConstants.ACT_PRM_NEXT_HOP_ID;
import static org.onosproject.pipelines.basic.BasicConstants.ACT_SET_NEXT_HOP_ID;
import static org.onosproject.pipelines.basic.BasicConstants.HDR_NEXT_HOP_ID;
import static org.onosproject.pipelines.basic.BasicConstants.TBL_TABLE0_ID;
import static org.onosproject.pipelines.basic.BasicConstants.TBL_WCMP_TABLE_ID;

/**
 * Implementation of an upgradable fabric app for the Basic pipeconf (basic.p4)
 * with ECMP support.
 */
@Component(immediate = true)
public class EcmpFabricApp extends AbstractUpgradableFabricApp {

    private static final String APP_NAME = "org.onosproject.pi-ecmp";

    private static final Map<DeviceId, Map<Set<PortNumber>, Short>>
            DEVICE_GROUP_ID_MAP = Maps.newHashMap();

    private final Set<Pair<DeviceId, GroupKey>> groupKeys = Sets.newHashSet();

    public EcmpFabricApp() {
        super(APP_NAME, singleton(PipeconfLoader.BASIC_PIPECONF));
    }

    @Deactivate
    public void deactivate() {
        groupKeys.forEach(pair -> groupService.removeGroup(
                pair.getLeft(), pair.getRight(), appId));
        super.deactivate();
    }

    @Override
    public boolean initDevice(DeviceId deviceId) {
        // Nothing to do.
        return true;
    }

    @Override
    public List<FlowRule> generateLeafRules(DeviceId leaf, Host localHost,
                                            Set<Host> remoteHosts,
                                            Set<DeviceId> availableSpines,
                                            Topology topo)
            throws FlowRuleGeneratorException {

        // Get ports which connect this leaf switch to hosts.
        Set<PortNumber> hostPorts = deviceService.getPorts(leaf)
                .stream()
                .filter(port -> !isFabricPort(port, topo))
                .map(Port::number)
                .collect(toSet());

        // Get ports which connect this leaf to the given available spines.
        TopologyGraph graph = topologyService.getGraph(topo);
        Set<PortNumber> fabricPorts = graph
                .getEdgesFrom(new DefaultTopologyVertex(leaf))
                .stream()
                .filter(e -> availableSpines.contains(e.dst().deviceId()))
                .map(e -> e.link().src().port())
                .collect(toSet());

        if (hostPorts.size() != 1 || fabricPorts.size() == 0) {
            log.error("Leaf has invalid port configuration: hostPorts={}, fabricPorts={}",
                      hostPorts.size(), fabricPorts.size());
            throw new FlowRuleGeneratorException();
        }
        PortNumber hostPort = hostPorts.iterator().next();

        List<FlowRule> rules = Lists.newArrayList();

        // From local host to remote ones.
        for (Host remoteHost : remoteHosts) {
            int groupId = provisionGroup(leaf, fabricPorts);

            rules.add(groupFlowRule(leaf, groupId));

            PiTableAction piTableAction = PiAction.builder()
                    .withId(ACT_SET_NEXT_HOP_ID)
                    .withParameter(new PiActionParam(
                            ACT_PRM_NEXT_HOP_ID,
                            copyFrom(groupId)))
                    .build();

            for (IpAddress ipAddr : remoteHost.ipAddresses()) {
                FlowRule rule = flowRuleBuilder(leaf, TBL_TABLE0_ID)
                        .withSelector(
                                DefaultTrafficSelector.builder()
                                        .matchIPDst(ipAddr.toIpPrefix())
                                        .build())
                        .withTreatment(
                                DefaultTrafficTreatment.builder()
                                        .piTableAction(piTableAction)
                                        .build())
                        .build();
                rules.add(rule);
            }
        }

        // From remote hosts to the local one
        for (IpAddress dstIpAddr : localHost.ipAddresses()) {
            FlowRule rule = flowRuleBuilder(leaf, TBL_TABLE0_ID)
                    .withSelector(
                            DefaultTrafficSelector.builder()
                                    .matchIPDst(dstIpAddr.toIpPrefix())
                                    .build())
                    .withTreatment(
                            DefaultTrafficTreatment.builder()
                                    .setOutput(hostPort)
                                    .build())
                    .build();
            rules.add(rule);
        }

        return rules;
    }

    @Override
    public List<FlowRule> generateSpineRules(DeviceId spine, Set<Host> hosts,
                                             Topology topo)
            throws FlowRuleGeneratorException {

        List<FlowRule> rules = Lists.newArrayList();

        // For each host pair (src -> dst)
        for (Host dstHost : hosts) {

            Set<Path> paths = topologyService.getPaths(
                    topo, spine, dstHost.location().deviceId());

            if (paths.size() == 0) {
                log.warn("No path between spine {} and host {}",
                         spine, dstHost);
                throw new FlowRuleGeneratorException();
            }

            Set<PortNumber> ports = paths.stream()
                    .map(p -> p.src().port())
                    .collect(toSet());

            int groupId = provisionGroup(spine, ports);

            rules.add(groupFlowRule(spine, groupId));

            PiTableAction piTableAction = PiAction.builder()
                    .withId(ACT_SET_NEXT_HOP_ID)
                    .withParameter(new PiActionParam(ACT_PRM_NEXT_HOP_ID,
                                                     copyFrom(groupId)))
                    .build();

            for (IpAddress dstIpAddr : dstHost.ipAddresses()) {
                FlowRule rule = flowRuleBuilder(spine, TBL_TABLE0_ID)
                        .withSelector(DefaultTrafficSelector.builder()
                                              .matchIPDst(dstIpAddr.toIpPrefix())
                                              .build())
                        .withTreatment(DefaultTrafficTreatment.builder()
                                               .piTableAction(piTableAction)
                                               .build())
                        .build();
                rules.add(rule);
            }
        }

        return rules;
    }

    /**
     * Provisions an ECMP group for the given device and set of ports, returns
     * the group ID.
     */
    private int provisionGroup(DeviceId deviceId, Set<PortNumber> ports)
            throws FlowRuleGeneratorException {

        int groupId = groupIdOf(deviceId, ports);

        // Group buckets
        List<GroupBucket> bucketList = ports.stream()
                .map(port -> DefaultTrafficTreatment.builder()
                        .setOutput(port)
                        .build())
                .map(DefaultGroupBucket::createSelectGroupBucket)
                .collect(Collectors.toList());

        // Group cookie (with action profile ID)
        PiGroupKey groupKey = new PiGroupKey(TBL_WCMP_TABLE_ID,
                                             ACT_PRF_WCMP_SELECTOR_ID,
                                             groupId);

        log.info("Adding group {} to {}...", groupId, deviceId);
        groupService.addGroup(
                new DefaultGroupDescription(deviceId,
                                            GroupDescription.Type.SELECT,
                                            new GroupBuckets(bucketList),
                                            groupKey,
                                            groupId,
                                            appId));

        groupKeys.add(ImmutablePair.of(deviceId, groupKey));

        return groupId;
    }

    private FlowRule groupFlowRule(DeviceId deviceId, int groupId)
            throws FlowRuleGeneratorException {
        return flowRuleBuilder(deviceId, TBL_WCMP_TABLE_ID)
                .withSelector(
                        DefaultTrafficSelector.builder()
                                .matchPi(
                                        PiCriterion.builder()
                                                .matchExact(HDR_NEXT_HOP_ID,
                                                            groupId)
                                                .build())
                                .build())
                .withTreatment(
                        DefaultTrafficTreatment.builder()
                                .piTableAction(PiActionGroupId.of(groupId))
                                .build())
                .build();
    }

    private int groupIdOf(DeviceId deviceId, Set<PortNumber> ports) {
        DEVICE_GROUP_ID_MAP.putIfAbsent(deviceId, Maps.newHashMap());
        // Counts the number of unique portNumber sets for each deviceId.
        // Each distinct set of portNumbers will have a unique ID.
        return DEVICE_GROUP_ID_MAP.get(deviceId).computeIfAbsent(ports, (pp) ->
                (short) (DEVICE_GROUP_ID_MAP.get(deviceId).size() + 1));
    }
}
