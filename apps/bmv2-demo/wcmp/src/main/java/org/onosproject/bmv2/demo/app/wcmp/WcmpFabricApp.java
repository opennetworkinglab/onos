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

package org.onosproject.bmv2.demo.app.wcmp;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2DefaultConfiguration;
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.runtime.Bmv2DeviceAgent;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionTreatment;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.bmv2.demo.app.common.AbstractUpgradableFabricApp;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Path;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.onlab.packet.EthType.EtherType.IPV4;
import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.roundToBytes;
import static org.onosproject.bmv2.demo.app.wcmp.WcmpInterpreter.*;

/**
 * Implementation of an upgradable fabric app for the WCMP configuration.
 */
@Component(immediate = true)
public class WcmpFabricApp extends AbstractUpgradableFabricApp {

    private static final String APP_NAME = "org.onosproject.bmv2-wcmp-fabric";
    private static final String MODEL_NAME = "WCMP";
    private static final String JSON_CONFIG_PATH = "/wcmp.json";

    private static final double MULTI_PORT_WEIGHT_COEFFICIENT = 0.85;

    private static final Bmv2Configuration WCMP_CONFIGURATION = loadConfiguration();
    private static final WcmpInterpreter WCMP_INTERPRETER = new WcmpInterpreter();
    protected static final Bmv2DeviceContext WCMP_CONTEXT = new Bmv2DeviceContext(WCMP_CONFIGURATION, WCMP_INTERPRETER);

    private static final Map<DeviceId, Map<Map<PortNumber, Double>, Integer>> DEVICE_GROUP_ID_MAP = Maps.newHashMap();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Bmv2Controller bmv2Controller;

    /**
     * TODO.
     */
    public WcmpFabricApp() {
        super(APP_NAME, MODEL_NAME, WCMP_CONTEXT);
    }


    @Override
    public boolean initDevice(DeviceId deviceId) {
        try {
            Bmv2DeviceAgent agent = bmv2Controller.getAgent(deviceId);
            for (Map.Entry<String, Bmv2Action> entry : WCMP_INTERPRETER.defaultActionsMap().entrySet()) {
                agent.setTableDefaultAction(entry.getKey(), entry.getValue());
            }
            return true;
        } catch (Bmv2RuntimeException e) {
            log.debug("Exception while initializing device {}: {}", deviceId, e.explain());
            return false;
        }
    }

    @Override
    public List<FlowRule> generateLeafRules(DeviceId deviceId, Host srcHost, Collection<Host> dstHosts,
                                            Collection<DeviceId> availableSpines, Topology topo)
            throws FlowRuleGeneratorException {

        Set<PortNumber> hostPortNumbers = Sets.newHashSet();
        Set<PortNumber> fabricPortNumbers = Sets.newHashSet();
        deviceService.getPorts(deviceId)
                .forEach(p -> (isFabricPort(p, topo) ? fabricPortNumbers : hostPortNumbers).add(p.number()));

        if (hostPortNumbers.size() != 1 || fabricPortNumbers.size() == 0) {
            log.error("Leaf switch has invalid port configuration: hostPorts={}, fabricPorts={}",
                      hostPortNumbers.size(), fabricPortNumbers.size());
            throw new FlowRuleGeneratorException();
        }
        PortNumber hostPort = hostPortNumbers.iterator().next();

        TopologyGraph graph = topologyService.getGraph(topo);
        // Map key: spine device id, value: leaf switch ports which connect to spine in the key.
        Map<DeviceId, Set<PortNumber>> spineToPortsMap = Maps.newHashMap();
        graph.getEdgesFrom(new DefaultTopologyVertex(deviceId)).forEach(edge -> {
            spineToPortsMap.putIfAbsent(edge.dst().deviceId(), Sets.newHashSet());
            spineToPortsMap.get(edge.dst().deviceId()).add(edge.link().src().port());
        });

        double baseWeight = 1d / spineToPortsMap.size();

        int numSinglePorts = (int) spineToPortsMap.values().stream().filter(s -> s.size() == 1).count();
        int numMultiPorts = spineToPortsMap.size() - numSinglePorts;

        // Reduce weight portion assigned to multi-ports to mitigate flow assignment imbalance (measured empirically).
        double multiPortBaseWeight = baseWeight * MULTI_PORT_WEIGHT_COEFFICIENT;
        double excess = (baseWeight - multiPortBaseWeight) * numMultiPorts;
        double singlePortBaseWeight = baseWeight + (excess / numSinglePorts);

        Map<PortNumber, Double> weighedPortNumbers = Maps.newHashMap();
        spineToPortsMap.forEach((did, portSet) -> {
            double base = (portSet.size() == 1) ? singlePortBaseWeight : multiPortBaseWeight;
            double weight = base / portSet.size();
            portSet.forEach(portNumber -> weighedPortNumbers.put(portNumber, weight));
        });

        List<FlowRule> rules = Lists.newArrayList();


        Pair<ExtensionTreatment, List<FlowRule>> result = provisionWcmpTreatment(deviceId, weighedPortNumbers);
        ExtensionTreatment wcmpTreatment = result.getLeft();
        rules.addAll(result.getRight());

        // From src host to dst hosts, WCMP to all fabric ports.
        for (Host dstHost : dstHosts) {
            FlowRule rule = flowRuleBuilder(deviceId, TABLE0)
                    .withSelector(
                            DefaultTrafficSelector.builder()
                                    .matchInPort(hostPort)
                                    .matchEthType(IPV4.ethType().toShort())
                                    .matchEthSrc(srcHost.mac())
                                    .matchEthDst(dstHost.mac())
                                    .build())
                    .withTreatment(
                            DefaultTrafficTreatment.builder()
                                    .extension(wcmpTreatment, deviceId)
                                    .build())
                    .build();
            rules.add(rule);
        }

        // From fabric ports to src host.
        for (PortNumber port : fabricPortNumbers) {
            FlowRule rule = flowRuleBuilder(deviceId, TABLE0)
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

        for (Host dstHost : dstHosts) {

            Set<Path> paths = topologyService.getPaths(topo, deviceId, dstHost.location().deviceId());

            if (paths.size() == 0) {
                log.warn("Can't find any path between spine {} and host {}", deviceId, dstHost);
                throw new FlowRuleGeneratorException();
            }

            TrafficTreatment treatment;

            if (paths.size() == 1) {
                // Only one path.
                PortNumber port = paths.iterator().next().src().port();
                treatment = DefaultTrafficTreatment.builder().setOutput(port).build();
            } else {
                // Multiple paths, do WCMP.
                Set<PortNumber> portNumbers = paths.stream().map(p -> p.src().port()).collect(toSet());
                double weight = 1d / portNumbers.size();
                // Same weight for all ports.
                Map<PortNumber, Double> weightedPortNumbers = portNumbers.stream()
                        .collect(Collectors.toMap(p -> p, p -> weight));
                Pair<ExtensionTreatment, List<FlowRule>> result = provisionWcmpTreatment(deviceId, weightedPortNumbers);
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

    private Pair<ExtensionTreatment, List<FlowRule>> provisionWcmpTreatment(DeviceId deviceId,
                                                                            Map<PortNumber, Double> weightedFabricPorts)
            throws FlowRuleGeneratorException {

        // Install WCMP group table entries that map from hash values to fabric ports.

        int groupId = groupIdOf(deviceId, weightedFabricPorts);
        List<PortNumber> portNumbers = Lists.newArrayList();
        List<Double> weights = Lists.newArrayList();
        weightedFabricPorts.forEach((p, w) -> {
            portNumbers.add(p);
            weights.add(w);
        });
        List<Integer> prefixLengths = toPrefixLengths(weights);

        List<FlowRule> rules = Lists.newArrayList();
        for (int i = 0; i < portNumbers.size(); i++) {
            ExtensionSelector extSelector = buildWcmpSelector(groupId, prefixLengths.get(i));
            FlowRule rule = flowRuleBuilder(deviceId, WCMP_GROUP_TABLE)
                    .withSelector(DefaultTrafficSelector.builder()
                                          .extension(extSelector, deviceId)
                                          .build())
                    .withTreatment(
                            DefaultTrafficTreatment.builder()
                                    .setOutput(portNumbers.get(i))
                                    .build())
                    .build();
            rules.add(rule);
        }

        ExtensionTreatment extTreatment = buildWcmpTreatment(groupId);

        return Pair.of(extTreatment, rules);
    }

    private Bmv2ExtensionSelector buildWcmpSelector(int groupId, int prefixLength) {
        byte[] ones = new byte[roundToBytes(prefixLength)];
        Arrays.fill(ones, (byte) 0xFF);
        return Bmv2ExtensionSelector.builder()
                .forConfiguration(WCMP_CONTEXT.configuration())
                .matchExact(WCMP_META, GROUP_ID, groupId)
                .matchLpm(WCMP_META, SELECTOR, ones, prefixLength)
                .build();
    }

    private Bmv2ExtensionTreatment buildWcmpTreatment(int groupId) {
        return Bmv2ExtensionTreatment.builder()
                .forConfiguration(WCMP_CONTEXT.configuration())
                .setActionName(WCMP_GROUP)
                .addParameter(GROUP_ID, groupId)
                .build();
    }

    public int groupIdOf(DeviceId did, Map<PortNumber, Double> weightedPorts) {
        DEVICE_GROUP_ID_MAP.putIfAbsent(did, Maps.newHashMap());
        // Counts the number of unique portNumber sets for each device ID.
        // Each distinct set of portNumbers will have a unique ID.
        return DEVICE_GROUP_ID_MAP.get(did).computeIfAbsent(weightedPorts,
                                                            (pp) -> DEVICE_GROUP_ID_MAP.get(did).size() + 1);
    }

    public List<Integer> toPrefixLengths(List<Double> weigths) {

        final double weightSum = weigths.stream()
                .mapToDouble(Double::doubleValue)
                .map(this::roundDouble)
                .sum();

        if (Math.abs(weightSum - 1) > 0.0001) {
            throw new RuntimeException("WCMP weights sum is expected to be 1, found was " + weightSum);
        }

        final int selectorBitWidth = WCMP_CONTEXT.configuration().headerType(WCMP_META_T).field(SELECTOR).bitWidth();
        final int availableBits = selectorBitWidth - 1;

        List<Long> prefixDiffs = weigths.stream().map(w -> Math.round(w * availableBits)).collect(toList());

        final long bitSum = prefixDiffs.stream().mapToLong(Long::longValue).sum();
        final long error = availableBits - bitSum;

        if (error != 0) {
            // Lazy intuition here is that the error can be absorbed by the longest prefixDiff with the minor impact.
            Long maxDiff = Collections.max(prefixDiffs);
            int idx = prefixDiffs.indexOf(maxDiff);
            prefixDiffs.remove(idx);
            prefixDiffs.add(idx, maxDiff + error);
        }
        List<Integer> prefixLengths = Lists.newArrayList();

        int prefix = 1;
        for (Long p : prefixDiffs) {
            prefixLengths.add(prefix);
            prefix += p;
        }
        return ImmutableList.copyOf(prefixLengths);
    }

    private double roundDouble(double n) {
        // 5 digits precision.
        return (double) Math.round(n * 100000d) / 100000d;
    }

    private static Bmv2Configuration loadConfiguration() {
        try {
            JsonObject json = Json.parse(new BufferedReader(new InputStreamReader(
                    WcmpFabricApp.class.getResourceAsStream(JSON_CONFIG_PATH)))).asObject();
            return Bmv2DefaultConfiguration.parse(json);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration", e);
        }
    }
}