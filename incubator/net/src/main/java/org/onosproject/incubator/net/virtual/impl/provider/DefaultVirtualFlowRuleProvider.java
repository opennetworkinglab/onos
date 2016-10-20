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

package org.onosproject.incubator.net.virtual.impl.provider;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import javafx.util.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProvider;
import org.onosproject.incubator.net.virtual.provider.InternalRoutingAlgorithm;
import org.onosproject.incubator.net.virtual.provider.VirtualFlowRuleProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualFlowRuleProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider that translate virtual flow rules into physical rules.
 * Current implementation is based on FlowRules.
 * This virtualize and de-virtualize virtual flow rules into physical flow rules.
 * {@link org.onosproject.net.flow.FlowRule}
 */
@Component(immediate = true)
@Service
public class DefaultVirtualFlowRuleProvider extends AbstractVirtualProvider
        implements VirtualFlowRuleProvider {

    private static final int FLOW_RULE_PRIORITY = 10;

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualNetworkAdminService virtualNetworkAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualProviderRegistryService providerRegistryService;

    InternalRoutingAlgorithm internalRoutingAlgorithm;
    InternalVirtualFlowRuleManager frm;
    ApplicationId appId;
    FlowRuleListener flowRuleListener;

    /**
     * Creates a provider with the supplied identifier.
     */
    public DefaultVirtualFlowRuleProvider() {
        super(new ProviderId("vnet-flow", "org.onosproject.virtual.vnet-flow"));
    }


    @Activate
    public void activate() {
        appId = coreService.registerApplication(
                "org.onosproject.virtual.vnet-flow");

        providerRegistryService.registerProvider(this);

        flowRuleListener = new InternalFlowRuleListener();
        flowRuleService.addListener(flowRuleListener);

        internalRoutingAlgorithm = new DefaultInternalRoutingAlgorithm();
        frm = new InternalVirtualFlowRuleManager();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        flowRuleService.removeListener(flowRuleListener);
        providerRegistryService.unregisterProvider(this);
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
    }

    @Override
    public void applyFlowRule(NetworkId networkId, FlowRule... flowRules) {
        for (FlowRule flowRule : flowRules) {
            devirtualize(networkId, flowRule).forEach(
                    r -> {
                        flowRuleService.applyFlowRules(r);
                    }
            );
        }
    }

    @Override
    public void removeFlowRule(NetworkId networkId, FlowRule... flowRules) {
        for (FlowRule flowRule : flowRules) {
            devirtualize(networkId, flowRule).forEach(
                    r -> {
                        flowRuleService.removeFlowRules(r);
                    }
            );
        }
    }

    @Override
    public void executeBatch(NetworkId networkId, FlowRuleBatchOperation batch) {
        checkNotNull(batch);

        //TODO: execute batch mechanism
    }

    public void setEmbeddingAlgorithm(InternalRoutingAlgorithm
                                              internalRoutingAlgorithm) {
        this.internalRoutingAlgorithm = internalRoutingAlgorithm;
    }

    /**
     * Translate the requested physical flow rules into virtual flow rules.
     *
     * @param flowRule A virtual flow rule to be translated
     * @return A flow rule for a specific virtual network
     */
    private FlowRule virtualize(FlowRule flowRule) {
        return frm.getVirtualRule(flowRule);
    }

    private FlowEntry virtualize(FlowEntry flowEntry) {
        FlowRule vRule = virtualize(flowEntry);
        FlowEntry vEntry = new DefaultFlowEntry(vRule, flowEntry.state(),
                                                flowEntry.life(),
                                                flowEntry.packets(),
                                                flowEntry.bytes());

        return vEntry;
    }

    /**
     * Translate the requested virtual flow rules into physical flow rules.
     * The translation could be one to many.
     *
     * @param flowRule A flow rule from underlying data plane to be translated
     * @return A set of flow rules for physical network
     */
    private Set<FlowRule> devirtualize(NetworkId networkId, FlowRule flowRule) {

        Set<FlowRule> outRules = new HashSet<>();

        Set<VirtualPort> vPorts = virtualNetworkAdminService
                .getVirtualPorts(networkId, flowRule.deviceId());

        PortCriterion portCriterion = ((PortCriterion) flowRule.selector()
                .getCriterion(Criterion.Type.IN_PORT));

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        if (portCriterion != null) {
            PortNumber vInPortNum = portCriterion.port();

            Optional<ConnectPoint> optionalCp =  vPorts.stream()
                    .filter(v -> v.number().equals(vInPortNum))
                    .map(v -> v.realizedBy()).findFirst();
            if (!optionalCp.isPresent()) {
                log.info("Port {} is not realized yet, in Network {}, Device {}",
                         vInPortNum, networkId, flowRule.deviceId());
                return outRules;
            }
            ingressPoints.add(optionalCp.get());
        } else {
            for (VirtualPort vPort : vPorts) {
                if (vPort.realizedBy() != null) {
                    ingressPoints.add(vPort.realizedBy());
                } else {
                    log.info("Port {} is not realized yet, in Network {}, " +
                                     "Device {}",
                             vPort, networkId, flowRule.deviceId());
                    return outRules;
                }
            }
        }

        PortNumber vOutPortNum = flowRule.treatment().allInstructions().stream()
                .filter(i -> i.type() == Instruction.Type.OUTPUT)
                .map(i -> ((Instructions.OutputInstruction) i).port())
                .findFirst().get();

        Optional<ConnectPoint> optionalCpOut = vPorts.stream()
                .filter(v -> v.number().equals(vOutPortNum))
                .map(v -> v.realizedBy())
                .findFirst();
        if (!optionalCpOut.isPresent()) {
            log.info("Port {} is not realized yet, in Network {}, Device {}",
                     vOutPortNum, networkId, flowRule.deviceId());
            return outRules;
        }
        ConnectPoint egressPoint = optionalCpOut.get();

        TrafficSelector.Builder commonSelectorBuilder
                = DefaultTrafficSelector.builder();
        flowRule.selector().criteria().stream()
                .filter(c -> c.type() != Criterion.Type.IN_PORT)
                .forEach(c -> commonSelectorBuilder.add(c));
        TrafficSelector commonSelector = commonSelectorBuilder.build();

        TrafficTreatment.Builder commonTreatmentBuilder
                = DefaultTrafficTreatment.builder();
        flowRule.treatment().allInstructions().stream()
                .filter(i -> i.type() != Instruction.Type.OUTPUT)
                .forEach(i -> commonTreatmentBuilder.add(i));
        TrafficTreatment commonTreatment = commonTreatmentBuilder.build();

        for (ConnectPoint ingressPoint : ingressPoints) {
            outRules.addAll(generateRules(networkId, ingressPoint, egressPoint,
                          commonSelector, commonTreatment, flowRule));
        }

        return outRules;
    }

    private Set<FlowRule> generateRules(NetworkId networkId,
                                        ConnectPoint ingressPoint,
                                        ConnectPoint egressPoint,
                                    TrafficSelector commonSelector,
                                    TrafficTreatment commonTreatment,
                                        FlowRule flowRule) {

        Set<FlowRule> outRules = new HashSet<>();

        if (ingressPoint.deviceId().equals(egressPoint.deviceId())) {
            //Traffic is handled inside a single physical switch
            //No tunnel is needed.

            TrafficSelector.Builder selectorBuilder =
                    DefaultTrafficSelector.builder(commonSelector);
            selectorBuilder.matchInPort(ingressPoint.port());

            TrafficTreatment.Builder treatmentBuilder =
                    DefaultTrafficTreatment.builder(commonTreatment);
            treatmentBuilder.setOutput(egressPoint.port());

            FlowRule.Builder ruleBuilder = DefaultFlowRule.builder();
            ruleBuilder.fromApp(appId);
            ruleBuilder.forDevice(ingressPoint.deviceId());
            ruleBuilder.withSelector(selectorBuilder.build());
            ruleBuilder.withTreatment(treatmentBuilder.build());
            ruleBuilder.withPriority(FLOW_RULE_PRIORITY);
            if (flowRule.isPermanent()) {
                ruleBuilder.makePermanent();
            } else {
                ruleBuilder.makeTemporary(flowRule.timeout());
            }

            FlowRule rule = ruleBuilder.build();
            frm.addIngressRule(flowRule, rule, networkId);
            outRules.add(ruleBuilder.build());
        } else {
            //Traffic is handled by multiple physical switches
            //A tunnel is needed.

            Path internalPath = internalRoutingAlgorithm
                    .findPath(ingressPoint, egressPoint);
            checkNotNull(internalPath, "No path between " +
                    ingressPoint.toString() + " " + egressPoint.toString());
            ConnectPoint inCp = ingressPoint;
            ConnectPoint outCp = internalPath.links().get(0).src();

            //ingress point of tunnel
            TrafficSelector.Builder selectorBuilder =
                    DefaultTrafficSelector.builder(commonSelector);
            selectorBuilder.matchInPort(ingressPoint.port());

            TrafficTreatment.Builder treatmentBuilder =
                    DefaultTrafficTreatment.builder(commonTreatment);
            treatmentBuilder.pushVlan()
                    .setVlanId(VlanId.vlanId(networkId.id().shortValue()));
            treatmentBuilder.setOutput(outCp.port());

            FlowRule.Builder ruleBuilder = DefaultFlowRule.builder();
            ruleBuilder.fromApp(appId);
            ruleBuilder.forDevice(ingressPoint.deviceId());
            ruleBuilder.withSelector(selectorBuilder.build());
            ruleBuilder.withTreatment(treatmentBuilder.build());
            ruleBuilder.withPriority(FLOW_RULE_PRIORITY);
            if (flowRule.isPermanent()) {
                ruleBuilder.makePermanent();
            } else {
                ruleBuilder.makeTemporary(flowRule.timeout());
            }

            FlowRule rule = ruleBuilder.build();
            frm.addIngressRule(flowRule, rule, networkId);
            outRules.add(ruleBuilder.build());

            //routing inside tunnel
            inCp = internalPath.links().get(0).dst();

            if (internalPath.links().size() > 1) {
                for (Link l : internalPath.links()
                        .subList(1, internalPath.links().size() - 1)) {

                    outCp = l.src();

                    selectorBuilder = DefaultTrafficSelector
                            .builder(commonSelector);
                    selectorBuilder.matchVlanId(
                            VlanId.vlanId(networkId.id().shortValue()));
                    selectorBuilder.matchInPort(inCp.port());

                    treatmentBuilder = DefaultTrafficTreatment
                            .builder(commonTreatment);
                    treatmentBuilder.setOutput(outCp.port());

                    ruleBuilder = DefaultFlowRule.builder();
                    ruleBuilder.fromApp(appId);
                    ruleBuilder.forDevice(inCp.deviceId());
                    ruleBuilder.withSelector(selectorBuilder.build());
                    ruleBuilder.withTreatment(treatmentBuilder.build());
                    if (flowRule.isPermanent()) {
                        ruleBuilder.makePermanent();
                    } else {
                        ruleBuilder.makeTemporary(flowRule.timeout());
                    }

                    outRules.add(ruleBuilder.build());
                    inCp = l.dst();
                }
            }

            //egress point of tunnel
            selectorBuilder = DefaultTrafficSelector.builder(commonSelector);
            selectorBuilder.matchVlanId(
                    VlanId.vlanId(networkId.id().shortValue()));
            selectorBuilder.matchInPort(ingressPoint.port());

            treatmentBuilder = DefaultTrafficTreatment.builder(commonTreatment);
            treatmentBuilder.popVlan();
            treatmentBuilder.setOutput(egressPoint.port());

            ruleBuilder = DefaultFlowRule.builder();
            ruleBuilder.fromApp(appId);
            ruleBuilder.forDevice(egressPoint.deviceId());
            ruleBuilder.withSelector(selectorBuilder.build());
            ruleBuilder.withTreatment(treatmentBuilder.build());
            ruleBuilder.withPriority(FLOW_RULE_PRIORITY);
            if (flowRule.isPermanent()) {
                ruleBuilder.makePermanent();
            } else {
                ruleBuilder.makeTemporary(flowRule.timeout());
            }

            outRules.add(ruleBuilder.build());
        }

        return outRules;
    }

    private class InternalFlowRuleListener implements FlowRuleListener {
        @Override
        public void event(FlowRuleEvent event) {

            if ((event.type() == FlowRuleEvent.Type.RULE_ADDED) ||
                    (event.type() == FlowRuleEvent.Type.RULE_UPDATED)) {
                if (frm.isVirtualIngressRule(event.subject())) {
                    NetworkId networkId = frm.getVirtualNetworkId(event.subject());
                    FlowEntry vEntry = getVirtualFlowEntry(event.subject());
                    ImmutableList.Builder<FlowEntry> builder = ImmutableList.builder();
                    builder.add(vEntry);

                    VirtualFlowRuleProviderService providerService =
                            (VirtualFlowRuleProviderService) providerRegistryService
                                    .getProviderService(networkId,
                                                        VirtualFlowRuleProvider.class);

                    providerService.pushFlowMetrics(vEntry.deviceId(), builder.build());
                }
            } else if (event.type() == FlowRuleEvent.Type.RULE_REMOVED) {
                if (frm.isVirtualIngressRule(event.subject())) {
                    //FIXME confirm all physical rules are removed
                    NetworkId networkId = frm.getVirtualNetworkId(event.subject());
                    FlowEntry vEntry = getVirtualFlowEntry(event.subject());

                    VirtualFlowRuleProviderService providerService =
                            (VirtualFlowRuleProviderService) providerRegistryService
                                    .getProviderService(networkId,
                                                        VirtualFlowRuleProvider.class);
                    providerService.flowRemoved(vEntry);

                }
            }
        }

        private FlowEntry getVirtualFlowEntry(FlowRule rule) {
            FlowEntry entry = null;
            for (FlowEntry fe :
                    flowRuleService.getFlowEntries(rule.deviceId())) {
                if (rule.exactMatch(fe)) {
                    entry = fe;
                }
            }

            FlowRule vRule = virtualize(entry);
            FlowEntry vEntry = new DefaultFlowEntry(vRule, entry.state(),
                                                    entry.life(), entry.packets(),
                                                    entry.bytes());

            return vEntry;
        }
    }

    private class InternalVirtualFlowRuleManager {
        /** <Virtual Network ID, Virtual Device ID, Virtual Flow Rules>.*/
        final Table<NetworkId, DeviceId, Set<FlowRule>> flowRuleTable
                = HashBasedTable.create();

        /** <Virtual Network ID, Virtual Device ID, Virtual Flow Rules>.*/
        final Table<NetworkId, DeviceId, Set<FlowRule>> missingFlowRuleTable
                = HashBasedTable.create();

        /** <Virtual Network ID, Virtual Device ID, Virtual Flow Entries>.*/
        final Table<NetworkId, DeviceId, Set<FlowEntry>> flowEntryTable
                = HashBasedTable.create();

        /** <Physical Flow Rule, Virtual Network ID>.*/
        final Map<FlowRule, NetworkId> ingressRuleMap = Maps.newConcurrentMap();

        /** <Physical Flow Rule, Virtual Virtual Flow Rule>.*/
        final Map<FlowRule, FlowRule> virtualizationMap = Maps.newConcurrentMap();

        private int getFlowRuleCount(NetworkId networkId, DeviceId deviceId) {
            return flowRuleTable.get(networkId, deviceId).size();
        }

        private int getMissingFlowCount(NetworkId networkId, DeviceId deviceId) {
            return missingFlowRuleTable.get(networkId, deviceId).size();
        }

        private int getFlowEntryCount(NetworkId networkId, DeviceId deviceId) {
            return flowEntryTable.get(networkId, deviceId).size();
        }

        private Iterable<FlowRule> getFlowRules(NetworkId networkId,
                                                DeviceId deviceId) {
            return flowRuleTable.get(networkId, deviceId);
        }

        private Iterable<FlowEntry> getFlowEntries(NetworkId networkId,
                                                   DeviceId deviceId) {
            return flowEntryTable.get(networkId, deviceId);
        }

        private void addFlowRule(NetworkId networkId, DeviceId deviceId,
                                 FlowRule flowRule) {
            Set<FlowRule> set = flowRuleTable.get(networkId, deviceId);
            if (set == null) {
                set = Sets.newHashSet();
                flowRuleTable.put(networkId, deviceId, set);
            }
            set.add(flowRule);
        }

        private void removeFlowRule(NetworkId networkId, DeviceId deviceId,
                                    FlowRule flowRule) {
            Set<FlowRule> set = flowRuleTable.get(networkId, deviceId);
            if (set == null) {
                return;
            }
            set.remove(flowRule);
        }

        private Set<FlowRule> getMissingRules(NetworkId networkId,
                                                   DeviceId deviceId) {
            return missingFlowRuleTable.get(networkId, deviceId);
        }

        private void addMissingFlowRule(NetworkId networkId, DeviceId deviceId,
                                 FlowRule flowRule) {
            Set<FlowRule> set = missingFlowRuleTable.get(networkId, deviceId);
            if (set == null) {
                set = Sets.newHashSet();
                missingFlowRuleTable.put(networkId, deviceId, set);
            }
            set.add(flowRule);
        }

        private void removeMissingFlowRule(NetworkId networkId, DeviceId deviceId,
                                    FlowRule flowRule) {
            Set<FlowRule> set = missingFlowRuleTable.get(networkId, deviceId);
            if (set == null) {
                return;
            }
            set.remove(flowRule);
        }

        private void addFlowEntry(NetworkId networkId, DeviceId deviceId,
                                  FlowEntry flowEntry) {
            Set<FlowEntry> set = flowEntryTable.get(networkId, deviceId);
            if (set == null) {
                set = Sets.newHashSet();
                flowEntryTable.put(networkId, deviceId, set);
            }

            //Replace old entry with new one
            set.stream().filter(fe -> fe.exactMatch(flowEntry))
                    .forEach(set::remove);
            set.add(flowEntry);

            //Remove old entry from missing flow
            getMissingRules(networkId, deviceId).stream()
                    .filter(fr -> fr.exactMatch(flowEntry))
                    .forEach(fr -> removeMissingFlowRule(networkId, deviceId, fr));
        }

        private void removeFlowEntry(NetworkId networkId, DeviceId deviceId,
                                     FlowEntry flowEntry) {
            Set<FlowEntry> set = flowEntryTable.get(networkId, deviceId);
            if (set == null) {
                return;
            }
            set.remove(flowEntry);
        }

        private void addIngressRule(FlowRule virtualRule, FlowRule physicalRule,
                                    NetworkId networkId) {
            ingressRuleMap.put(physicalRule, networkId);
            virtualizationMap.put(physicalRule, virtualRule);
        }

        private FlowRule getVirtualRule(FlowRule physicalRule) {
            return virtualizationMap.get(physicalRule);
        }

        private Set<FlowRule> getAllPhysicalRule() {
            return ImmutableSet.copyOf(virtualizationMap.keySet());
        }

        private NetworkId getVirtualNetworkId(FlowRule physicalRule) {
            return ingressRuleMap.get(physicalRule);
        }

        /**
         * Test the rule is the ingress rule for virtual rules.
         *
         * @param flowRule A flow rule from underlying data plane to be translated
         * @return True when the rule is for ingress point for a virtual switch
         */
        private boolean isVirtualIngressRule(FlowRule flowRule) {
            return ingressRuleMap.containsKey(flowRule);
        }

        private Set<Pair<NetworkId, DeviceId>> getCompletedDevice(boolean
                                                                  withMissing) {

            Set<Pair<NetworkId, DeviceId>> completed = new HashSet<>();

            for (Table.Cell<NetworkId, DeviceId, Set<FlowRule>> cell
                    : flowRuleTable.cellSet()) {

                int ruleCount = getFlowRuleCount(cell.getRowKey(),
                                                 cell.getColumnKey());
                int missingFlowCount = getMissingFlowCount(cell.getRowKey(),
                                                           cell.getColumnKey());
                int entryCount = getFlowEntryCount(cell.getRowKey(),
                                                   cell.getColumnKey());

                if (withMissing && (ruleCount == missingFlowCount + entryCount)) {
                    if (ruleCount < entryCount) {
                        completed.add(new Pair<>(cell.getRowKey(),
                                                 cell.getColumnKey()));
                    }
                } else if (ruleCount == entryCount) {
                    completed.add(new Pair<>(cell.getRowKey(),
                                             cell.getColumnKey()));
                }
            }
            return completed;
        }
    }

    private class DefaultInternalRoutingAlgorithm
            implements InternalRoutingAlgorithm {

        @Override
        public Path findPath(ConnectPoint src, ConnectPoint dst) {
            Set<Path> paths =
                    topologyService.getPaths(topologyService.currentTopology(),
                                             src.deviceId(),
                                             dst.deviceId());

            if (paths.isEmpty()) {
                return null;
            }

            //TODO the logic find the best path
            return (Path) paths.toArray()[0];
        }
    }
}
