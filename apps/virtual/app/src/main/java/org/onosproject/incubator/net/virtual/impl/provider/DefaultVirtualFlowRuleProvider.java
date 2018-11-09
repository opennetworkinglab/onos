/*
 * Copyright 2016-present Open Networking Foundation
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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
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
import org.onosproject.net.flow.BatchOperationEntry;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEntry;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchOperation;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.copyOf;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider that translate virtual flow rules into physical rules.
 * Current implementation is based on FlowRules.
 * This virtualize and de-virtualize virtual flow rules into physical flow rules.
 * {@link org.onosproject.net.flow.FlowRule}
 */
@Component(service = VirtualFlowRuleProvider.class)
public class DefaultVirtualFlowRuleProvider extends AbstractVirtualProvider
        implements VirtualFlowRuleProvider {

    private static final String APP_ID_STR = "org.onosproject.virtual.vnet-flow_";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VirtualNetworkService vnService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VirtualProviderRegistryService providerRegistryService;

    private InternalRoutingAlgorithm internalRoutingAlgorithm;
    private InternalVirtualFlowRuleManager frm;
    private ApplicationId appId;
    private FlowRuleListener flowRuleListener;

    /**
     * Creates a provider with the identifier.
     */
    public DefaultVirtualFlowRuleProvider() {
        super(new ProviderId("vnet-flow", "org.onosproject.virtual.vnet-flow"));
    }

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID_STR);

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
        flowRuleService.removeFlowRulesById(appId);
        providerRegistryService.unregisterProvider(this);
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
    }

    @Override
    public void applyFlowRule(NetworkId networkId, FlowRule... flowRules) {
        for (FlowRule flowRule : flowRules) {
            devirtualize(networkId, flowRule).forEach(
                    r -> flowRuleService.applyFlowRules(r));
        }
    }

    @Override
    public void removeFlowRule(NetworkId networkId, FlowRule... flowRules) {
        for (FlowRule flowRule : flowRules) {
            devirtualize(networkId, flowRule).forEach(
                    r -> flowRuleService.removeFlowRules(r));
        }
    }

    @Override
    public void executeBatch(NetworkId networkId, FlowRuleBatchOperation batch) {
        checkNotNull(batch);

        for (FlowRuleBatchEntry fop : batch.getOperations()) {
            FlowRuleOperations.Builder builder = FlowRuleOperations.builder();

            switch (fop.operator()) {
                case ADD:
                    devirtualize(networkId, fop.target()).forEach(builder::add);
                    break;
                case REMOVE:
                    devirtualize(networkId, fop.target()).forEach(builder::remove);
                    break;
                case MODIFY:
                    devirtualize(networkId, fop.target()).forEach(builder::modify);
                    break;
                default:
                    break;
            }

            flowRuleService.apply(builder.build(new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    CompletedBatchOperation status =
                            new CompletedBatchOperation(true,
                                                        Sets.newConcurrentHashSet(),
                                                        batch.deviceId());

                    VirtualFlowRuleProviderService providerService =
                            (VirtualFlowRuleProviderService) providerRegistryService
                                    .getProviderService(networkId,
                                                        VirtualFlowRuleProvider.class);
                    providerService.batchOperationCompleted(batch.id(), status);
                }

                @Override
                public void onError(FlowRuleOperations ops) {
                    Set<FlowRule> failures = ImmutableSet.copyOf(
                            Lists.transform(batch.getOperations(),
                                            BatchOperationEntry::target));

                    CompletedBatchOperation status =
                            new CompletedBatchOperation(false,
                                                        failures,
                                                        batch.deviceId());

                    VirtualFlowRuleProviderService providerService =
                            (VirtualFlowRuleProviderService) providerRegistryService
                                    .getProviderService(networkId,
                                                        VirtualFlowRuleProvider.class);
                    providerService.batchOperationCompleted(batch.id(), status);
                }
            }));
        }
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
    private FlowRule virtualizeFlowRule(FlowRule flowRule) {

        FlowRule storedrule = frm.getVirtualRule(flowRule);

        if (flowRule.reason() == FlowRule.FlowRemoveReason.NO_REASON) {
            return storedrule;
        } else {
            return DefaultFlowRule.builder()
                    .withReason(flowRule.reason())
                    .withPriority(storedrule.priority())
                    .forDevice(storedrule.deviceId())
                    .forTable(storedrule.tableId())
                    .fromApp(new DefaultApplicationId(storedrule.appId(), null))
                    .withIdleTimeout(storedrule.timeout())
                    .withHardTimeout(storedrule.hardTimeout())
                    .withSelector(storedrule.selector())
                    .withTreatment(storedrule.treatment())
                    .build();
        }
    }

    private FlowEntry virtualize(FlowEntry flowEntry) {
        FlowRule vRule = virtualizeFlowRule(flowEntry);

        if (vRule == null) {
            return null;
        }

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

        Set<ConnectPoint> ingressPoints = extractIngressPoints(networkId,
                                                               flowRule.deviceId(),
                                                               flowRule.selector());

        ConnectPoint egressPoint = extractEgressPoints(networkId,
                                                         flowRule.deviceId(),
                                                         flowRule.treatment());

        if (egressPoint == null) {
            return outRules;
        }

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
            if (egressPoint.port() == PortNumber.FLOOD) {
                Set<ConnectPoint> outPoints = vnService
                        .getVirtualPorts(networkId, flowRule.deviceId())
                        .stream()
                        .map(VirtualPort::realizedBy)
                        .filter(p -> !p.equals(ingressPoint))
                        .collect(Collectors.toSet());

                for (ConnectPoint outPoint : outPoints) {
                    outRules.addAll(generateRules(networkId, ingressPoint, outPoint,
                                                  commonSelector, commonTreatment, flowRule));
                }
            } else {
                outRules.addAll(generateRules(networkId, ingressPoint, egressPoint,
                                              commonSelector, commonTreatment, flowRule));
            }
        }

        return outRules;
    }

    /**
     * Extract ingress connect points of the physical network
     * from the requested traffic selector.
     *
     * @param networkId the virtual network identifier
     * @param deviceId the virtual device identifier
     * @param selector the traffic selector to extract ingress point
     * @return the set of ingress connect points of the physical network
     */
    private Set<ConnectPoint> extractIngressPoints(NetworkId networkId,
                                                   DeviceId deviceId,
                                                   TrafficSelector selector) {

        Set<ConnectPoint> ingressPoints = new HashSet<>();

        Set<VirtualPort> vPorts = vnService
                .getVirtualPorts(networkId, deviceId);

        PortCriterion portCriterion = ((PortCriterion) selector
                .getCriterion(Criterion.Type.IN_PORT));

        if (portCriterion != null) {
            PortNumber vInPortNum = portCriterion.port();

            Optional<ConnectPoint> optionalCp =  vPorts.stream()
                    .filter(v -> v.number().equals(vInPortNum))
                    .map(VirtualPort::realizedBy).findFirst();
            if (!optionalCp.isPresent()) {
                log.warn("Port {} is not realized yet, in Network {}, Device {}",
                         vInPortNum, networkId, deviceId);
                return ingressPoints;
            }

            ingressPoints.add(optionalCp.get());
        } else {
            for (VirtualPort vPort : vPorts) {
                if (vPort.realizedBy() != null) {
                    ingressPoints.add(vPort.realizedBy());
                } else {
                    log.warn("Port {} is not realized yet, in Network {}, " +
                                     "Device {}",
                             vPort, networkId, deviceId);
                }
            }
        }

        return ingressPoints;
    }

    /**
     * Extract egress connect point of the physical network
     * from the requested traffic treatment.
     *
     * @param networkId the virtual network identifier
     * @param deviceId the virtual device identifier
     * @param treatment the traffic treatment to extract ingress point
     * @return the egress connect point of the physical network
     */
    private ConnectPoint extractEgressPoints(NetworkId networkId,
                                                  DeviceId deviceId,
                                                  TrafficTreatment treatment) {

        Set<VirtualPort> vPorts = vnService
                .getVirtualPorts(networkId, deviceId);

        PortNumber vOutPortNum = treatment.allInstructions().stream()
                .filter(i -> i.type() == Instruction.Type.OUTPUT)
                .map(i -> ((Instructions.OutputInstruction) i).port())
                .findFirst().get();

        Optional<ConnectPoint> optionalCpOut = vPorts.stream()
                .filter(v -> v.number().equals(vOutPortNum))
                .map(VirtualPort::realizedBy)
                .findFirst();

        if (!optionalCpOut.isPresent()) {
            if (vOutPortNum.isLogical()) {
                return new ConnectPoint(DeviceId.deviceId("vNet"), vOutPortNum);
            }

            log.warn("Port {} is not realized yet, in Network {}, Device {}",
                     vOutPortNum, networkId, deviceId);
            return null;
        }

        return optionalCpOut.get();
    }


    /**
     * Generates the corresponding flow rules for the physical network.
     *
     * @param networkId The virtual network identifier
     * @param ingressPoint The ingress point of the physical network
     * @param egressPoint The egress point of the physical network
     * @param commonSelector A common traffic selector between the virtual
     *                       and physical flow rules
     * @param commonTreatment A common traffic treatment between the virtual
     *                        and physical flow rules
     * @param flowRule The virtual flow rule to be translated
     * @return A set of flow rules for the physical network
     */
    private Set<FlowRule> generateRules(NetworkId networkId,
                                        ConnectPoint ingressPoint,
                                        ConnectPoint egressPoint,
                                        TrafficSelector commonSelector,
                                        TrafficTreatment commonTreatment,
                                        FlowRule flowRule) {

        if (ingressPoint.deviceId().equals(egressPoint.deviceId()) ||
                egressPoint.port().isLogical()) {
            return generateRuleForSingle(networkId, ingressPoint, egressPoint,
                                         commonSelector, commonTreatment, flowRule);
        } else {
            return generateRuleForMulti(networkId, ingressPoint, egressPoint,
                                         commonSelector, commonTreatment, flowRule);
        }
    }

    /**
     * Generate physical rules when a virtual flow rule can be handled inside
     * a single physical switch.
     *
     * @param networkId The virtual network identifier
     * @param ingressPoint The ingress point of the physical network
     * @param egressPoint The egress point of the physical network
     * @param commonSelector A common traffic selector between the virtual
     *                       and physical flow rules
     * @param commonTreatment A common traffic treatment between the virtual
     *                        and physical flow rules
     * @param flowRule The virtual flow rule to be translated
     * @return A set of flow rules for the physical network
     */
    private Set<FlowRule> generateRuleForSingle(NetworkId networkId,
            ConnectPoint ingressPoint,
            ConnectPoint egressPoint,
            TrafficSelector commonSelector,
            TrafficTreatment commonTreatment,
            FlowRule flowRule) {

        Set<FlowRule> outRules = new HashSet<>();

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector
                .builder(commonSelector)
                .matchInPort(ingressPoint.port());

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment
                .builder(commonTreatment)
                .setOutput(egressPoint.port());

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(vnService.getVirtualNetworkApplicationId(networkId))
                .forDevice(ingressPoint.deviceId())
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.build())
                .withIdleTimeout(flowRule.timeout())
                .withPriority(flowRule.priority());

        FlowRule rule = ruleBuilder.build();
        frm.addIngressRule(flowRule, rule, networkId);
        outRules.add(rule);

        return outRules;
    }

    /**
     * Generate physical rules when a virtual flow rule can be handled with
     * multiple physical switches.
     *
     * @param networkId The virtual network identifier
     * @param ingressPoint The ingress point of the physical network
     * @param egressPoint The egress point of the physical network
     * @param commonSelector A common traffic selector between the virtual
     *                       and physical flow rules
     * @param commonTreatment A common traffic treatment between the virtual
     *                        and physical flow rules
     * @param flowRule The virtual flow rule to be translated
     * @return A set of flow rules for the physical network
     */
    private Set<FlowRule> generateRuleForMulti(NetworkId networkId,
                                                ConnectPoint ingressPoint,
                                                ConnectPoint egressPoint,
                                                TrafficSelector commonSelector,
                                                TrafficTreatment commonTreatment,
                                                FlowRule flowRule) {
        Set<FlowRule> outRules = new HashSet<>();

        Path internalPath = internalRoutingAlgorithm
                .findPath(ingressPoint, egressPoint);
        checkNotNull(internalPath, "No path between " +
                ingressPoint.toString() + " " + egressPoint.toString());

        ConnectPoint outCp = internalPath.links().get(0).src();

        //ingress point of tunnel
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder(commonSelector);
        selectorBuilder.matchInPort(ingressPoint.port());

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder(commonTreatment);
        //TODO: add the logic to check host location
        treatmentBuilder.pushVlan()
                .setVlanId(VlanId.vlanId(networkId.id().shortValue()));
        treatmentBuilder.setOutput(outCp.port());

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(vnService.getVirtualNetworkApplicationId(networkId))
                .forDevice(ingressPoint.deviceId())
                .withSelector(selectorBuilder.build())
                .withIdleTimeout(flowRule.timeout())
                .withTreatment(treatmentBuilder.build())
                .withPriority(flowRule.priority());

        FlowRule rule = ruleBuilder.build();
        frm.addIngressRule(flowRule, rule, networkId);
        outRules.add(rule);

        //routing inside tunnel
        ConnectPoint inCp = internalPath.links().get(0).dst();

        if (internalPath.links().size() > 1) {
            for (Link l : internalPath.links()
                    .subList(1, internalPath.links().size())) {

                outCp = l.src();

                selectorBuilder = DefaultTrafficSelector
                        .builder(commonSelector)
                        .matchVlanId(VlanId.vlanId(networkId.id().shortValue()))
                        .matchInPort(inCp.port());

                treatmentBuilder = DefaultTrafficTreatment
                        .builder(commonTreatment)
                        .setOutput(outCp.port());

                ruleBuilder = DefaultFlowRule.builder()
                        .fromApp(vnService.getVirtualNetworkApplicationId(networkId))
                        .forDevice(inCp.deviceId())
                        .withSelector(selectorBuilder.build())
                        .withTreatment(treatmentBuilder.build())
                        .withIdleTimeout(flowRule.timeout())
                        .withPriority(flowRule.priority());

                outRules.add(ruleBuilder.build());
                inCp = l.dst();
            }
        }

        //egress point of tunnel
        selectorBuilder = DefaultTrafficSelector.builder(commonSelector)
                .matchVlanId(VlanId.vlanId(networkId.id().shortValue()))
                .matchInPort(inCp.port());

        treatmentBuilder = DefaultTrafficTreatment.builder(commonTreatment)
                .popVlan()
                .setOutput(egressPoint.port());

        ruleBuilder = DefaultFlowRule.builder()
                .fromApp(appId)
                .forDevice(egressPoint.deviceId())
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.build())
                .withIdleTimeout(flowRule.timeout())
                .withPriority(flowRule.priority());

        outRules.add(ruleBuilder.build());

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

                    if (vEntry == null) {
                        return;
                    }

                    frm.addOrUpdateFlowEntry(networkId, vEntry.deviceId(), vEntry);

                    VirtualFlowRuleProviderService providerService =
                            (VirtualFlowRuleProviderService) providerRegistryService
                                    .getProviderService(networkId,
                                                        VirtualFlowRuleProvider.class);

                    ImmutableList.Builder<FlowEntry> builder = ImmutableList.builder();
                    builder.addAll(frm.getFlowEntries(networkId, vEntry.deviceId()));

                    providerService.pushFlowMetrics(vEntry.deviceId(), builder.build());
                }
            } else if (event.type() == FlowRuleEvent.Type.RULE_REMOVED) {
                if (frm.isVirtualIngressRule(event.subject())) {
                    //FIXME confirm all physical rules are removed
                    NetworkId networkId = frm.getVirtualNetworkId(event.subject());
                    FlowEntry vEntry = getVirtualFlowEntry(event.subject());

                    if (vEntry == null) {
                        return;
                    }

                    frm.removeFlowEntry(networkId, vEntry.deviceId(), vEntry);
                    frm.removeFlowRule(networkId, vEntry.deviceId(), vEntry);

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

            if (entry != null) {
                return virtualize(entry);
            } else  {
                return virtualize(new DefaultFlowEntry(rule,
                                                       FlowEntry.FlowEntryState.PENDING_REMOVE));
            }
        }
    }

    private class InternalVirtualFlowRuleManager {
        /** <Virtual Network ID, Virtual Device ID, Virtual Flow Rules>.*/
        final Table<NetworkId, DeviceId, Set<FlowRule>> flowRuleTable
                = HashBasedTable.create();

        /** <Virtual Network ID, Virtual Device ID, Virtual Flow Entries>.*/
        final Table<NetworkId, DeviceId, Set<FlowEntry>> flowEntryTable
                = HashBasedTable.create();

        /** <Physical Flow Rule, Virtual Network ID>.*/
        final Map<FlowRule, NetworkId> ingressRuleMap = Maps.newHashMap();

        /** <Physical Flow Rule, Virtual Virtual Flow Rule>.*/
        final Map<FlowRule, FlowRule> virtualizationMap = Maps.newHashMap();

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

        private void addOrUpdateFlowEntry(NetworkId networkId, DeviceId deviceId,
                                  FlowEntry flowEntry) {
            Set<FlowEntry> set = flowEntryTable.get(networkId, deviceId);
            if (set == null) {
                set = Sets.newConcurrentHashSet();
                flowEntryTable.put(networkId, deviceId, set);
            }

            //Replace old entry with new one
            set.stream().filter(fe -> fe.exactMatch(flowEntry))
                    .forEach(set::remove);
            set.add(flowEntry);
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

        private void removeIngressRule(FlowRule physicalRule) {
            ingressRuleMap.remove(physicalRule);
            virtualizationMap.remove(physicalRule);
        }

        private Set<FlowRule> getAllPhysicalRule() {
            return copyOf(virtualizationMap.keySet());
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
