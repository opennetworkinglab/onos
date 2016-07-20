/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.demo.app.ecmp;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2DefaultConfiguration;
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionTreatment;
import org.onosproject.bmv2.demo.app.common.AbstractUpgradableFabricApp;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyGraph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.onlab.packet.EthType.EtherType.IPV4;
import static org.onosproject.bmv2.demo.app.ecmp.EcmpInterpreter.*;

/**
 * Implementation of an upgradable fabric app for the ECMP configuration.
 */
@Component(immediate = true)
public class EcmpFabricApp extends AbstractUpgradableFabricApp {

    private static final String APP_NAME = "org.onosproject.bmv2-ecmp-fabric";
    private static final String MODEL_NAME = "ECMP";
    private static final String JSON_CONFIG_PATH = "/ecmp.json";

    private static final Bmv2Configuration ECMP_CONFIGURATION = loadConfiguration();
    private static final EcmpInterpreter ECMP_INTERPRETER = new EcmpInterpreter();
    protected static final Bmv2DeviceContext ECMP_CONTEXT = new Bmv2DeviceContext(ECMP_CONFIGURATION, ECMP_INTERPRETER);

    private static final Map<DeviceId, Map<Set<PortNumber>, Short>> DEVICE_GROUP_ID_MAP = Maps.newHashMap();

    public EcmpFabricApp() {
        super(APP_NAME, MODEL_NAME, ECMP_CONTEXT);
    }

    @Override
    public boolean initDevice(DeviceId deviceId) {
        // Nothing to do.
        return true;
    }

    @Override
    public List<FlowRule> generateLeafRules(DeviceId leaf, Host srcHost, Collection<Host> dstHosts,
                                            Collection<DeviceId> availableSpines, Topology topo)
            throws FlowRuleGeneratorException {

        // Get ports which connect this leaf switch to hosts.
        Set<PortNumber> hostPorts = deviceService.getPorts(leaf)
                .stream()
                .filter(port -> !isFabricPort(port, topo))
                .map(Port::number)
                .collect(Collectors.toSet());

        // Get ports which connect this leaf to the given available spines.
        TopologyGraph graph = topologyService.getGraph(topo);
        Set<PortNumber> fabricPorts = graph.getEdgesFrom(new DefaultTopologyVertex(leaf))
                .stream()
                .filter(e -> availableSpines.contains(e.dst().deviceId()))
                .map(e -> e.link().src().port())
                .collect(Collectors.toSet());

        if (hostPorts.size() != 1 || fabricPorts.size() == 0) {
            log.error("Leaf switch has invalid port configuration: hostPorts={}, fabricPorts={}",
                      hostPorts.size(), fabricPorts.size());
            throw new FlowRuleGeneratorException();
        }
        PortNumber hostPort = hostPorts.iterator().next();

        List<FlowRule> rules = Lists.newArrayList();

        TrafficTreatment treatment;
        if (fabricPorts.size() > 1) {
            // Do ECMP.
            Pair<ExtensionTreatment, List<FlowRule>> result = provisionEcmpTreatment(leaf, fabricPorts);
            rules.addAll(result.getRight());
            ExtensionTreatment extTreatment = result.getLeft();
            treatment = DefaultTrafficTreatment.builder().extension(extTreatment, leaf).build();
        } else {
            // Output on port.
            PortNumber outPort = fabricPorts.iterator().next();
            treatment = DefaultTrafficTreatment.builder().setOutput(outPort).build();
        }

        // From srHost to dstHosts.
        for (Host dstHost : dstHosts) {
            FlowRule rule = flowRuleBuilder(leaf, TABLE0)
                    .withSelector(
                            DefaultTrafficSelector.builder()
                                    .matchInPort(hostPort)
                                    .matchEthType(IPV4.ethType().toShort())
                                    .matchEthSrc(srcHost.mac())
                                    .matchEthDst(dstHost.mac())
                                    .build())
                    .withTreatment(treatment)
                    .build();
            rules.add(rule);
        }

        // From fabric ports to this leaf host.
        for (PortNumber port : fabricPorts) {
            FlowRule rule = flowRuleBuilder(leaf, TABLE0)
                    .withSelector(
                            DefaultTrafficSelector.builder()
                                    .matchInPort(port)
                                    .matchEthType(IPV4.ethType().toShort())
                                    .matchEthDst(srcHost.mac())
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
    public List<FlowRule> generateSpineRules(DeviceId deviceId, Collection<Host> dstHosts, Topology topo)
            throws FlowRuleGeneratorException {

        List<FlowRule> rules = Lists.newArrayList();

        // for each host
        for (Host dstHost : dstHosts) {

            Set<Path> paths = topologyService.getPaths(topo, deviceId, dstHost.location().deviceId());

            if (paths.size() == 0) {
                log.warn("Can't find any path between spine {} and host {}", deviceId, dstHost);
                throw new FlowRuleGeneratorException();
            }

            TrafficTreatment treatment;

            if (paths.size() == 1) {
                // Only one path, do output on port.
                PortNumber port = paths.iterator().next().src().port();
                treatment = DefaultTrafficTreatment.builder().setOutput(port).build();
            } else {
                // Multiple paths, do ECMP.
                Set<PortNumber> portNumbers = paths.stream().map(p -> p.src().port()).collect(toSet());
                Pair<ExtensionTreatment, List<FlowRule>> result = provisionEcmpTreatment(deviceId, portNumbers);
                rules.addAll(result.getRight());
                treatment = DefaultTrafficTreatment.builder().extension(result.getLeft(), deviceId).build();
            }

            FlowRule rule = flowRuleBuilder(deviceId, TABLE0)
                    .withSelector(
                            DefaultTrafficSelector.builder()
                                    .matchEthType(IPV4.ethType().toShort())
                                    .matchEthDst(dstHost.mac())
                                    .build())
                    .withTreatment(treatment)
                    .build();

            rules.add(rule);
        }

        return rules;
    }

    private Pair<ExtensionTreatment, List<FlowRule>> provisionEcmpTreatment(DeviceId deviceId,
                                                                            Set<PortNumber> fabricPorts)
            throws FlowRuleGeneratorException {

        // Install ECMP group table entries that map from hash values to actual fabric ports...
        int groupId = groupIdOf(deviceId, fabricPorts);
        int groupSize = fabricPorts.size();
        Iterator<PortNumber> portIterator = fabricPorts.iterator();
        List<FlowRule> rules = Lists.newArrayList();
        for (short i = 0; i < groupSize; i++) {
            ExtensionSelector extSelector = buildEcmpSelector(groupId, i);
            FlowRule rule = flowRuleBuilder(deviceId, ECMP_GROUP_TABLE)
                    .withSelector(
                            DefaultTrafficSelector.builder()
                                    .extension(extSelector, deviceId)
                                    .build())
                    .withTreatment(
                            DefaultTrafficTreatment.builder()
                                    .setOutput(portIterator.next())
                                    .build())
                    .build();
            rules.add(rule);
        }

        ExtensionTreatment extTreatment = buildEcmpTreatment(groupId, groupSize);

        return Pair.of(extTreatment, rules);
    }

    private Bmv2ExtensionTreatment buildEcmpTreatment(int groupId, int groupSize) {
        return Bmv2ExtensionTreatment.builder()
                .forConfiguration(ECMP_CONTEXT.configuration())
                .setActionName(ECMP_GROUP)
                .addParameter(GROUP_ID, groupId)
                .addParameter(GROUP_SIZE, groupSize)
                .build();
    }

    private Bmv2ExtensionSelector buildEcmpSelector(int groupId, int selector) {
        return Bmv2ExtensionSelector.builder()
                .forConfiguration(ECMP_CONTEXT.configuration())
                .matchExact(ECMP_METADATA, GROUP_ID, groupId)
                .matchExact(ECMP_METADATA, SELECTOR, selector)
                .build();
    }


    public int groupIdOf(DeviceId deviceId, Set<PortNumber> ports) {
        DEVICE_GROUP_ID_MAP.putIfAbsent(deviceId, Maps.newHashMap());
        // Counts the number of unique portNumber sets for each deviceId.
        // Each distinct set of portNumbers will have a unique ID.
        return DEVICE_GROUP_ID_MAP.get(deviceId).computeIfAbsent(ports, (pp) ->
                (short) (DEVICE_GROUP_ID_MAP.get(deviceId).size() + 1));
    }

    private static Bmv2Configuration loadConfiguration() {
        try {
            JsonObject json = Json.parse(new BufferedReader(new InputStreamReader(
                    EcmpFabricApp.class.getResourceAsStream(JSON_CONFIG_PATH)))).asObject();
            return Bmv2DefaultConfiguration.parse(json);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration", e);
        }
    }
}