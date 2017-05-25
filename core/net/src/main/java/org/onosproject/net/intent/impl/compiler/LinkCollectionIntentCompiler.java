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
package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.Identifier;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.domain.DomainService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompilationException;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.impl.LabelAllocator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.onosproject.net.domain.DomainId.LOCAL;
import static org.onosproject.net.flow.instructions.Instruction.Type.NOACTION;

/**
 * Compiler to produce flow rules from link collections.
 */
@Component(immediate = true)
public class LinkCollectionIntentCompiler
        extends LinkCollectionCompiler<FlowRule>
        implements IntentCompiler<LinkCollectionIntent> {

    private static final String UNKNOWN_INSTRUCTION = "Unknown instruction type";
    private static final String UNSUPPORTED_INSTRUCTION = "Unsupported %s instruction";


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentConfigurableRegistrator registrator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DomainService domainService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        registrator.registerCompiler(LinkCollectionIntent.class, this, false);
        if (labelAllocator == null) {
            labelAllocator = new LabelAllocator(resourceService);
        }
    }

    @Deactivate
    public void deactivate() {
        registrator.unregisterCompiler(LinkCollectionIntent.class, false);
    }

    @Override
    public List<Intent> compile(LinkCollectionIntent intent, List<Intent> installable) {

        SetMultimap<DeviceId, PortNumber> inputPorts = HashMultimap.create();
        SetMultimap<DeviceId, PortNumber> outputPorts = HashMultimap.create();
        Map<ConnectPoint, Identifier<?>> labels = ImmutableMap.of();

        Optional<EncapsulationConstraint> encapConstraint = this.getIntentEncapConstraint(intent);

        computePorts(intent, inputPorts, outputPorts);

        if (encapConstraint.isPresent()) {
            labels = labelAllocator.assignLabelToPorts(intent.links(),
                                                       intent.key(),
                                                       encapConstraint.get().encapType());
        }

        ImmutableList.Builder<Intent> intentList = ImmutableList.builder();
        if (this.isDomainProcessingEnabled(intent)) {
            intentList.addAll(this.getDomainIntents(intent, domainService));
        }

        List<FlowRule> rules = new ArrayList<>();
        for (DeviceId deviceId : outputPorts.keySet()) {
            // add only flows that are not inside of a domain
            if (LOCAL.equals(domainService.getDomain(deviceId))) {
                rules.addAll(createRules(
                        intent,
                        deviceId,
                        inputPorts.get(deviceId),
                        outputPorts.get(deviceId),
                        labels)
                );
            }
        }
        // if any rules have been created
        if (!rules.isEmpty()) {
            intentList.add(new FlowRuleIntent(appId, intent.key(), rules,
                                              intent.resources()));
        }
        return intentList.build();
    }

    @Override
    boolean optimizeTreatments() {
        return true;
    }

    @Override
    protected List<FlowRule> createRules(LinkCollectionIntent intent,
                                         DeviceId deviceId,
                                         Set<PortNumber> inPorts,
                                         Set<PortNumber> outPorts,
                                         Map<ConnectPoint, Identifier<?>> labels) {

        List<FlowRule> rules = new ArrayList<>(inPorts.size());
        /*
         * Looking for the encapsulation constraint
         */
        Optional<EncapsulationConstraint> encapConstraint = this.getIntentEncapConstraint(intent);

        inPorts.forEach(inport -> {

                ForwardingInstructions instructions = this.createForwardingInstruction(
                        encapConstraint,
                        intent,
                        inport,
                        outPorts,
                        deviceId,
                        labels
                );

                if (optimizeInstructions) {
                    TrafficTreatment compactedTreatment = compactActions(instructions.treatment());
                    instructions = new ForwardingInstructions(compactedTreatment, instructions.selector());
                }

                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(instructions.selector())
                        .withTreatment(instructions.treatment())
                        .withPriority(intent.priority())
                        .fromApp(appId)
                        .makePermanent()
                        .build();
                rules.add(rule);
            }
        );

        return rules;
    }

    /**
     * This method tries to optimize the chain of actions.
     *
     * @param oldTreatment the list of instructions to optimize
     * @return the optimized set of actions
     */
    private TrafficTreatment compactActions(TrafficTreatment oldTreatment) {

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        Instruction instruction;
        Instruction newInstruction;

        for (int index = 0; index < oldTreatment.allInstructions().size(); index++) {
            instruction = oldTreatment.allInstructions().get(index);
            /*
             * if the action is not optimizable. We simply add
             * to the builder.
             */
            if (checkInstruction(instruction)) {
                treatmentBuilder.add(instruction);
                continue;
            }
            /*
             * We try to run an optimization;
             */
            newInstruction = optimizeInstruction(index, instruction, oldTreatment.allInstructions());
            if (!newInstruction.type().equals(NOACTION)) {
                treatmentBuilder.add(newInstruction);
            }
        }

        return treatmentBuilder.build();
    }

    /**
     * Verifies if the given L2 instruction can be optimized.
     *
     * @param l2instruction the l2 instruction to verify
     * @return true if the instruction cannot be optimized. False otherwise
     */
    private boolean checkL2Instructions(L2ModificationInstruction l2instruction) {
        switch (l2instruction.subtype()) {
            /*
             * These actions can be performed safely.
             */
            case ETH_SRC:
            case ETH_DST:
            case VLAN_ID:
            case VLAN_PCP:
            case MPLS_LABEL:
            case MPLS_BOS:
            case TUNNEL_ID:
            case VLAN_PUSH:
            case VLAN_POP:
            case MPLS_PUSH:
            case MPLS_POP:
                return true;
            /*
             * We should avoid dec mpls ttl multiple
             * times.
             */
            case DEC_MPLS_TTL:
                return false;

            default:
                throw new IntentCompilationException(String.format(UNSUPPORTED_INSTRUCTION, "L2"));
        }

    }

    /**
     * Verifies if the given L3 instruction can be optimized.
     *
     * @param l3instruction the l3 instruction to verify
     * @return true if the instruction cannot be optimized. False otherwise
     */
    private boolean checkL3Instructions(L3ModificationInstruction l3instruction) {
        switch (l3instruction.subtype()) {
            /*
             * These actions can be performed several times.
             */
            case IPV4_SRC:
            case IPV4_DST:
            case IPV6_SRC:
            case IPV6_DST:
            case IPV6_FLABEL:
            case ARP_SPA:
            case ARP_SHA:
            case ARP_OP:
            case TTL_OUT:
            case TTL_IN:
                return true;
            /*
             * This action should be executed one time;
             */
            case DEC_TTL:
                return false;
            default:
                throw new IntentCompilationException(String.format(UNSUPPORTED_INSTRUCTION, "L3"));
        }
    }

    /**
     * Helper method to handle the optimization of the ttl instructions.
     *
     * @param index the index of the instruction
     * @param instruction the instruction to optimize
     * @param instructions the list of instructions to optimize
     * @return no action if the action can be removed. The same instruction
     *         if we have to perform it
     */
    private Instruction optimizeTtlInstructions(int index, Instruction instruction, List<Instruction> instructions) {
        /**
         * Here we handle the optimization of decrement mpls ttl. The optimization
         * is to come back to the start of the list looking for the same
         * action. If we find the same action, we can optimize.
         */
        Instruction currentInstruction;
        for (int i = index - 1; i >= 0; i--) {
            currentInstruction = instructions.get(i);
            if (currentInstruction.equals(instruction)) {
                return Instructions.createNoAction();

            }
        }
        return instruction;
    }

    /**
     * Helper method to handle the optimization of the instructions.
     *
     * @param index the index of the instruction
     * @param instruction the instruction to optimize
     * @param instructions the list of instructions to optimize
     * @return no action if the action can be removed. The same instruction
     *         if we have to perform it
     */
    private Instruction optimizeInstruction(int index, Instruction instruction, List<Instruction> instructions) {

        switch (instruction.type()) {
            /*
             * Here we have the chance to optimize the dec mpls ttl action.
             */
            case L2MODIFICATION:
            /*
             * Here we have the chance to optimize the ttl related actions.
             */
            case L3MODIFICATION:
                return optimizeTtlInstructions(index, instruction, instructions);

            default:
                throw new IntentCompilationException(UNKNOWN_INSTRUCTION);

        }

    }

    /**
     * Helper method to verify if the instruction can be optimized.
     *
     * @param instruction the instruction to verify
     * @return true if the action can be optimized. False otherwise.
     */
    private boolean checkInstruction(Instruction instruction) {

        switch (instruction.type()) {
            /*
             * The following instructions are not supported.
             */
            case L0MODIFICATION:
            case L1MODIFICATION:
            case L4MODIFICATION:
            case NOACTION:
            case OUTPUT:
            case GROUP:
            case QUEUE:
            case TABLE:
            case METER:
            case METADATA:
            case EXTENSION:
                return true;
            /*
             * Here we have the chance to optimize actions like dec mpls ttl.
             */
            case L2MODIFICATION:
                return checkL2Instructions((L2ModificationInstruction) instruction);
            /*
             * Here we have the chance to optimize the ttl related actions.
             */
            case L3MODIFICATION:
                return checkL3Instructions((L3ModificationInstruction) instruction);

            default:
                throw new IntentCompilationException(UNKNOWN_INSTRUCTION);

        }

    }

}
