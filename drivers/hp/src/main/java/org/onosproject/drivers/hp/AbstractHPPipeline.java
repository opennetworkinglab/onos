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

package org.onosproject.drivers.hp;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.meter.MeterService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;

import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.onosproject.net.flow.FlowRule.Builder;
import static org.onosproject.net.flowobjective.Objective.Operation.ADD;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstraction of the HP pipeline handler.
 * Possibly compliant with all HP switches: tested with HP3800 (v2 module) and HP3500 (v1 module).
 *
 * These switches supports multiple OpenFlow instances.
 * Each instance can be created with different pipeline models.
 * This driver refers to OpenFlow instances created with "pipeline-model standard-match"
 *
 * With this model Table 100 is used for all entries that can be processed in hardware,
 * whereas table 200 is used for entries processed in software.
 *
 * Trying to install a flow entries only supported in software in table 100 generates
 * an OpenFlow error message from the switch.
 *
 * Installation of a flow entry supported in hardware in table 200 is allowed. But it strongly
 * degrades forwarding performance.
 *
 * ---------------------------------------------
 * --- SELECTION OF PROPER TABLE ID ---
 * --- from device manual OpenFlow v1.3 for firmware 16.04 - (Appendix A)
 * ---------------------------------------------
 * --- Hardware differences between v1, v2 and v3 modules affect which features are supported
 * in hardware/software. In this driver "TableIdForForwardingObjective()" function selects the
 * proper table ID considering the hardware features of the device and the actual FlowObjective.
 *
 * ---------------------------------------------
 * --- HPE switches support OpenFlow version 1.3.1 with following limitations.
 * --- from device manual OpenFlow v1.3 for firmware 16.04 - (Appendix A)
 *
 *** UNSUPPORTED FLOW MATCHES --- (implemented using unsupported_criteria)
 * ---------------------------------------------
 * - METADATA - DONE
 * - IP_ECN - DONE
 * - SCTP_SRC, SCTP_DST - DONE
 * - IPV6_ND_SLL, IPV6_ND_TLL - DONE
 * - MPLS_LABEL, MPLS_TC, MPLS_BOS - DONE
 * - PBB_ISID - DONE
 * - TUNNEL_ID - DONE
 * - IPV6_EXTHDR - DONE
 *
 *** UNSUPPORTED ACTIONS ---
 * ---------------------------------------------
 * - METADATA - DONE
 * - QUEUE - DONE
 * - OFPP_TABLE action - TODO
 * - MPLS actions: Push-MPLS, Pop-MPLS, Set-MPLS TTL, Decrement MPLS TTL - DONE
 * - Push-PBB, Pop-PBB actions - TODO
 * - Copy TTL inwards/outwards actions - DONE
 * - Decrement IP TTL - DONE
 *
 * ---------------------------------------------
 * --- OTHER UNSUPPORTED FEATURES ---
 * --- from device manual OpenFlow v1.3 for firmware 16.04
 * ---------------------------------------------
 * - Port commands: OFPPC_NO_STP, OFPPC_NO_RECV, OFPPC_NO_RECV_STP, OFPPC_NO_FWD - TODO
 * - Handling of IP Fragments: OFPC_IP_REASM, OFPC_FRAG_REASM - TODO
 *
 * TODO MINOR: include above actions in the lists of unsupported features
 * TODO MINOR: check pre-requites in flow match
 *
 *
 * With current implementation, in case of unsupported features a WARNING message in generated
 * in the ONOS log, but FlowRule is sent anyway to the device.
 * The device will reply with an OFP_ERROR message.
 * Use "debug openflow events" and "debug openflow errors" on the device to locally
 * visualize detailed information on the specific error.
 *
 * TODO MAJOR: use OFP_TABLE_FEATURE messages to automate learning of unsupported features
 *
 */

public abstract class AbstractHPPipeline extends AbstractHandlerBehaviour implements Pipeliner {

    protected static final String APPLICATION_ID = "org.onosproject.drivers.hp.HPPipeline";

    protected static final int HP_TABLE_ZERO = 0;
    protected static final int HP_HARDWARE_TABLE = 100;
    protected static final int HP_SOFTWARE_TABLE = 200;

    public static final int CACHE_ENTRY_EXPIRATION_PERIOD = 20;

    private final Logger log = getLogger(getClass());
    protected FlowRuleService flowRuleService;
    protected GroupService groupService;
    protected MeterService meterService;
    protected FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    protected ApplicationId appId;
    protected DeviceService deviceService;
    protected Device device;
    protected String deviceHwVersion;
    protected KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(KryoNamespaces.API)
            .build("AbstractHPPipeline");
    private ServiceDirectory serviceDirectory;
    private CoreService coreService;
    private Cache<Integer, NextObjective> pendingAddNext = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_ENTRY_EXPIRATION_PERIOD, TimeUnit.SECONDS)
            .removalListener((RemovalNotification<Integer, NextObjective> notification) -> {
                if (notification.getCause() == RemovalCause.EXPIRED) {
                    notification.getValue().context()
                            .ifPresent(c -> c.onError(notification.getValue(),
                                                      ObjectiveError.FLOWINSTALLATIONFAILED));
                }
            }).build();

    /** Lists of unsupported features (firmware version K 16.04)
     * If a FlowObjective uses one of these features a warning log message is generated.
     */
    protected Set<Criterion.Type> unsupportedCriteria = new HashSet<>();
    protected Set<Instruction.Type> unsupportedInstructions = new HashSet<>();
    protected Set<L2ModificationInstruction.L2SubType> unsupportedL2mod = new HashSet<>();
    protected Set<L3ModificationInstruction.L3SubType> unsupportedL3mod = new HashSet<>();

    /** Lists of Criteria and Instructions supported in hardware
     * If a FlowObjective uses one of these features the FlowRule is intalled in HP_SOFTWARE_TABLE.
     */
    protected Set<Criterion.Type> hardwareCriteria = new HashSet<>();
    protected Set<Instruction.Type> hardwareInstructions = new HashSet<>();
    protected Set<L2ModificationInstruction.L2SubType> hardwareInstructionsL2mod = new HashSet<>();
    protected Set<L3ModificationInstruction.L3SubType> hardwareInstructionsL3mod = new HashSet<>();
    protected Set<L4ModificationInstruction.L4SubType> hardwareInstructionsL4mod = new HashSet<>();
    protected Set<Group.Type> hardwareGroups = new HashSet<>();

    /**
     * Sets default table id.
     * Using this solution all flow rules are installed on the "default" table
     *
     * @param ruleBuilder flow rule builder to be set table id
     * @return flow rule builder with set table id for flow
     */
    protected abstract FlowRule.Builder setDefaultTableIdForFlowObjective(Builder ruleBuilder);

    /**
     * Return the proper table ID depending on the specific ForwardingObjective.
     *
     * HP switches supporting openflow have 3 tables (Pipeline Model: Standard Match)
     * Table 0 is just a shortcut to table 100
     * Table 100/200 are respectively used for rules processed in HARDWARE/SOFTWARE
     *
     * @param selector TrafficSelector including flow match
     * @param treatment TrafficTreatment including instructions/actions
     * @return table id
     */
    protected abstract int tableIdForForwardingObjective(TrafficSelector selector, TrafficTreatment treatment);

    /**
     * Return TRUE if ForwardingObjective fwd includes unsupported features.
     *
     * @param selector TrafficSelector including flow match
     * @param treatment TrafficTreatment including instructions/actions
     * @return boolean
     */
    protected abstract boolean checkUnSupportedFeatures(TrafficSelector selector, TrafficTreatment treatment);

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.deviceId = deviceId;

        serviceDirectory = context.directory();
        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        groupService = serviceDirectory.get(GroupService.class);
        meterService = serviceDirectory.get(MeterService.class);
        deviceService = serviceDirectory.get(DeviceService.class);
        flowObjectiveStore = context.store();

        appId = coreService.registerApplication(APPLICATION_ID);

        device = deviceService.getDevice(deviceId);
        deviceHwVersion = device.hwVersion();

        //Initialization of model specific features
        log.debug("HP Driver - Initializing unsupported features for switch {}", deviceHwVersion);
        initUnSupportedFeatures();

        log.debug("HP Driver - Initializing features supported in hardware");
        initHardwareCriteria();
        initHardwareInstructions();

        log.debug("HP Driver - Initializing pipeline");
        installHPTableZero();
        installHPHardwareTable();
        installHPSoftwareTable();
    }

    /**
     * UnSupported features are specific of each model.
     */
    protected abstract void initUnSupportedFeatures();

    /**
     * Criteria supported in hardware are specific of each model.
     */
    protected abstract void initHardwareCriteria();

    /**
     * Instructions supported in hardware are specific of each model.
     */
    protected abstract void initHardwareInstructions();

    /**
     * HP Table 0 initialization.
     * Installs rule goto HP_HARDWARE_TABLE in HP_TABLE_ZERO
     */
    private void installHPTableZero() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.transition(HP_HARDWARE_TABLE);

        FlowRule rule = DefaultFlowRule.builder().forDevice(this.deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(0)
                .fromApp(appId)
                .makePermanent()
                .forTable(HP_TABLE_ZERO)
                .build();

        this.applyRules(true, rule);
    }

    /**
     * HP hardware table initialization.
     * Installs rule goto HP_SOFTWARE_TABLE in HP_HARDWARE_TABLE
     */
    private void installHPHardwareTable() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        //treatment.setOutput(PortNumber.NORMAL);
        treatment.transition(HP_SOFTWARE_TABLE);

        FlowRule rule = DefaultFlowRule.builder().forDevice(this.deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(0)
                .fromApp(appId)
                .makePermanent()
                .forTable(HP_HARDWARE_TABLE)
                .build();

        this.applyRules(true, rule);
    }

    /**
     * Applies FlowRule.
     * Installs or removes FlowRule.
     *
     * @param install - whether to install or remove rule
     * @param rule    - the rule to be installed or removed
     */
    private void applyRules(boolean install, FlowRule rule) {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();

        ops = install ? ops.add(rule) : ops.remove(rule);
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.trace("HP Driver: - applyRules onSuccess rule {}", rule);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.trace("HP Driver: applyRules onError rule: " + rule);
            }
        }));
    }

    /**
     * HP software table initialization.
     * No rules required.
     */
    private void installHPSoftwareTable() {}

    protected void pass(Objective obj) {
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }

    protected void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }

    @Override
    public void forward(ForwardingObjective fwd) {

        if (fwd.treatment() != null) {
            /* If UNSUPPORTED features included in ForwardingObjective a warning message is generated.
             * FlowRule is anyway sent to the device, device will reply with an OFP_ERROR.
             * Moreover, checkUnSupportedFeatures function generates further warnings specifying
             * each unsupported feature.
             */
            if (checkUnSupportedFeatures(fwd.selector(), fwd.treatment())) {
                log.warn("HP Driver - specified ForwardingObjective contains UNSUPPORTED FEATURES");
            }

            // Deal with SPECIFIC and VERSATILE in the same manner.
            // Create the FlowRule starting from the ForwardingObjective
            FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(fwd.selector())
                    .withTreatment(fwd.treatment())
                    .withPriority(fwd.priority())
                    .fromApp(fwd.appId());

            // Determine the table to be used depends on selector, treatment and specific switch hardware
            ruleBuilder.forTable(tableIdForForwardingObjective(fwd.selector(), fwd.treatment()));

            if (fwd.permanent()) {
                ruleBuilder.makePermanent();
            } else {
                ruleBuilder.makeTemporary(fwd.timeout());
            }

            log.debug("HP Driver - installing ForwadingObjective arrived with treatment {}", fwd);
            installObjective(ruleBuilder, fwd);

        } else {
            NextObjective nextObjective;
            NextGroup next;
            TrafficTreatment treatment;
            if (fwd.op() == ADD) {
                // Give a try to the cache. Doing an operation
                // on the store seems to be very expensive.
                nextObjective = pendingAddNext.getIfPresent(fwd.nextId());
                // If the next objective is not present
                // We will try with the store
                if (nextObjective == null) {
                    next = flowObjectiveStore.getNextGroup(fwd.nextId());
                    // We verify that next was in the store and then de-serialize
                    // the treatment in order to re-build the flow rule.
                    if (next == null) {
                        fwd.context().ifPresent(c -> c.onError(fwd, ObjectiveError.GROUPMISSING));
                        return;
                    }
                    treatment = appKryo.deserialize(next.data());
                } else {
                    pendingAddNext.invalidate(fwd.nextId());
                    treatment = getTreatment(nextObjective);
                    if (treatment == null) {
                        fwd.context().ifPresent(c -> c.onError(fwd, ObjectiveError.UNSUPPORTED));
                        return;
                    }
                }
            } else {
                // We get the NextGroup from the remove operation.
                // Doing an operation on the store seems to be very expensive.
                next = flowObjectiveStore.getNextGroup(fwd.nextId());
                treatment = (next != null) ? appKryo.deserialize(next.data()) : null;
            }

            // If the treatment is null we cannot re-build the original flow
            if (treatment == null) {
                fwd.context().ifPresent(c -> c.onError(fwd, ObjectiveError.GROUPMISSING));
                return;
            }
            // Finally we build the flow rule and push to the flowrule subsystem.
            FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(fwd.selector())
                    .fromApp(fwd.appId())
                    .withPriority(fwd.priority())
                    .withTreatment(treatment);

            /* If UNSUPPORTED features included in ForwardingObjective a warning message is generated.
             * FlowRule is anyway sent to the device, device will reply with an OFP_ERROR.
             * Moreover, checkUnSupportedFeatures function generates further warnings specifying
             * each unsupported feature.
             */
            if (checkUnSupportedFeatures(fwd.selector(), treatment)) {
                log.warn("HP Driver - specified ForwardingObjective contains UNSUPPORTED FEATURES");
            }

            //Table to be used depends on the specific switch hardware and ForwardingObjective
            ruleBuilder.forTable(tableIdForForwardingObjective(fwd.selector(), treatment));

            if (fwd.permanent()) {
                ruleBuilder.makePermanent();
            } else {
                ruleBuilder.makeTemporary(fwd.timeout());
            }

            log.debug("HP Driver - installing ForwadingObjective arrived with NULL treatment (intent)");
            installObjective(ruleBuilder, fwd);
        }
    }

    /**
     * Installs objective.
     *
     * @param ruleBuilder flow rule builder used to build rule from objective
     * @param objective   objective to be installed
     */
    protected void installObjective(FlowRule.Builder ruleBuilder, Objective objective) {
        FlowRuleOperations.Builder flowBuilder = FlowRuleOperations.builder();

        switch (objective.op()) {
            case ADD:
                log.trace("HP Driver - Requested ADD of objective to device " + deviceId);
                flowBuilder.add(ruleBuilder.build());
                break;
            case REMOVE:
                log.trace("HP Driver - Requested REMOVE of objective to device " + deviceId);
                flowBuilder.remove(ruleBuilder.build());
                break;
            default:
                log.debug("HP Driver - Unknown operation {}", objective.op());
        }

        flowRuleService.apply(flowBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                objective.context().ifPresent(context -> context.onSuccess(objective));
                log.trace("HP Driver - Installed objective " + objective.toString());
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                objective.context()
                        .ifPresent(context -> context.onError(objective, ObjectiveError.FLOWINSTALLATIONFAILED));
                log.trace("HP Driver - Objective installation failed" + objective.toString());
            }
        }));
    }

    @Override
    public void next(NextObjective nextObjective) {
        switch (nextObjective.op()) {
            case ADD:
                log.debug("HP driver: NextObjective ADDED");
                // We insert the value in the cache
                pendingAddNext.put(nextObjective.id(), nextObjective);
                // Then in the store, this will unblock the queued fwd obj
                flowObjectiveStore.putNextGroup(
                        nextObjective.id(),
                        new SingleGroup(nextObjective.next().iterator().next())
                );
                break;
            case REMOVE:
                log.debug("HP driver: NextObjective REMOVED");
                NextGroup next = flowObjectiveStore.removeNextGroup(nextObjective.id());
                if (next == null) {
                    nextObjective.context().ifPresent(context -> context.onError(nextObjective,
                            ObjectiveError.GROUPMISSING));
                    return;
                }
                break;
            default:
                log.debug("Unsupported operation {}", nextObjective.op());
        }
        nextObjective.context().ifPresent(context -> context.onSuccess(nextObjective));
    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        //TODO: to be implemented
        return Collections.emptyList();
    }

    @Override
    public void filter(FilteringObjective filteringObjective) {
        if (filteringObjective.type() == FilteringObjective.Type.PERMIT) {
            processFilter(filteringObjective,
                          filteringObjective.op() == Objective.Operation.ADD,
                          filteringObjective.appId());
        } else {
            fail(filteringObjective, ObjectiveError.UNSUPPORTED);
        }
    }

    @Override
    public void purgeAll(ApplicationId appId) {
        flowRuleService.purgeFlowRules(deviceId, appId);
        groupService.purgeGroupEntries(deviceId, appId);
        meterService.purgeMeters(deviceId, appId);
    }

    /**
     * Gets traffic treatment from a next objective.
     * Merge traffic treatments from next objective if the next objective is
     * BROADCAST type and contains multiple traffic treatments.
     * Returns first treatment from next objective if the next objective is
     * SIMPLE type and it contains only one treatment.
     *
     * @param nextObjective the next objective
     * @return the treatment from next objective; null if not supported
     */
    private TrafficTreatment getTreatment(NextObjective nextObjective) {
        Collection<TrafficTreatment> treatments = nextObjective.next();
        switch (nextObjective.type()) {
            case SIMPLE:
                if (treatments.size() != 1) {
                    log.error("Next Objectives of type SIMPLE should have only " +
                                    "one traffic treatment. NexObjective: {}",
                            nextObjective.toString());
                    return null;
                }
                return treatments.iterator().next();
            case BROADCAST:
                TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
                treatments.forEach(builder::addTreatment);
                return builder.build();
            default:
                log.error("Unsupported next objective type {}.", nextObjective.type());
                return null;
        }
    }

    /**
     * Filter processing and installation.
     * Processes and installs filtering rules.
     *
     * @param filt
     * @param install
     * @param applicationId
     */
    private void processFilter(FilteringObjective filt, boolean install,
                               ApplicationId applicationId) {
        // This driver only processes filtering criteria defined with switch
        // ports as the key
        PortCriterion port;
        if (!filt.key().equals(Criteria.dummy()) &&
                filt.key().type() == Criterion.Type.IN_PORT) {
            port = (PortCriterion) filt.key();
        } else {
            log.warn("No key defined in filtering objective from app: {}. Not"
                             + "processing filtering objective", applicationId);
            fail(filt, ObjectiveError.UNKNOWN);
            return;
        }
        // convert filtering conditions for switch-intfs into flowrules
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        for (Criterion c : filt.conditions()) {
            if (c.type() == Criterion.Type.ETH_DST) {
                EthCriterion eth = (EthCriterion) c;
                FlowRule.Builder rule = processEthFilter(filt, eth, port);
                rule.forDevice(deviceId)
                        .fromApp(applicationId);
                ops = install ? ops.add(rule.build()) : ops.remove(rule.build());

            } else if (c.type() == Criterion.Type.VLAN_VID) {
                VlanIdCriterion vlan = (VlanIdCriterion) c;
                FlowRule.Builder rule = processVlanFilter(filt, vlan, port);
                rule.forDevice(deviceId)
                        .fromApp(applicationId);
                ops = install ? ops.add(rule.build()) : ops.remove(rule.build());

            } else if (c.type() == Criterion.Type.IPV4_DST) {
                IPCriterion ip = (IPCriterion) c;
                FlowRule.Builder rule = processIpFilter(filt, ip, port);
                rule.forDevice(deviceId)
                        .fromApp(applicationId);
                ops = install ? ops.add(rule.build()) : ops.remove(rule.build());

            } else {
                log.warn("Driver does not currently process filtering condition"
                                 + " of type: {}", c.type());
                fail(filt, ObjectiveError.UNSUPPORTED);
            }
        }
        // apply filtering flow rules
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                pass(filt);
                log.trace("HP Driver - Applied filtering rules");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(filt, ObjectiveError.FLOWINSTALLATIONFAILED);
                log.trace("HP Driver - Failed to apply filtering rules");
            }
        }));
    }

    protected abstract Builder processEthFilter(FilteringObjective filt,
                                               EthCriterion eth, PortCriterion port);

    protected abstract Builder processVlanFilter(FilteringObjective filt,
                                                VlanIdCriterion vlan, PortCriterion port);

    protected abstract Builder processIpFilter(FilteringObjective filt,
                                               IPCriterion ip, PortCriterion port);

    private class SingleGroup implements NextGroup {

        private TrafficTreatment nextActions;

        SingleGroup(TrafficTreatment next) {
            this.nextActions = next;
        }

        @Override
        public byte[] data() {
            return appKryo.serialize(nextActions);
        }

        public TrafficTreatment treatment() {
            return nextActions;
        }

    }


}
