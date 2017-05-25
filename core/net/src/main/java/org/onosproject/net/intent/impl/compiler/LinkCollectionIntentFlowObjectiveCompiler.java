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
package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.HashMultimap;
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
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.intent.FlowObjectiveIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.impl.LabelAllocator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Compiler to produce flow objectives from link collections.
 * @deprecated 1.10 Kingfisher
 */
@Deprecated
@Component(immediate = true)
public class LinkCollectionIntentFlowObjectiveCompiler
        extends LinkCollectionCompiler<Objective>
        implements IntentCompiler<LinkCollectionIntent> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentConfigurableRegistrator registrator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        registrator.registerCompiler(LinkCollectionIntent.class, this, true);
        if (labelAllocator == null) {
            labelAllocator = new LabelAllocator(resourceService);
        }

    }

    @Deactivate
    public void deactivate() {
        registrator.unregisterCompiler(LinkCollectionIntent.class, true);
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

        List<Objective> objectives = new ArrayList<>();
        List<DeviceId> devices = new ArrayList<>();
        for (DeviceId deviceId: outputPorts.keys()) {
            List<Objective> deviceObjectives =
                    createRules(intent,
                                deviceId,
                                inputPorts.get(deviceId),
                                outputPorts.get(deviceId),
                                labels);
            deviceObjectives.forEach(objective -> {
                objectives.add(objective);
                devices.add(deviceId);
            });
        }
        return Collections.singletonList(
                new FlowObjectiveIntent(appId, intent.key(), devices,
                                        objectives,
                                        intent.resources(),
                                        intent.resourceGroup()));
    }

    @Override
    boolean optimizeTreatments() {
        return false;
    }

    @Override
    protected List<Objective> createRules(LinkCollectionIntent intent,
                                          DeviceId deviceId,
                                          Set<PortNumber> inPorts,
                                          Set<PortNumber> outPorts,
                                          Map<ConnectPoint, Identifier<?>> labels) {

        List<Objective> objectives = new ArrayList<>(inPorts.size());

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

            NextObjective nextObjective = DefaultNextObjective.builder()
                    .withId(flowObjectiveService.allocateNextId())
                    .addTreatment(instructions.treatment())
                    .withType(NextObjective.Type.SIMPLE)
                    .fromApp(appId)
                    .makePermanent().add();
            objectives.add(nextObjective);

            objectives.add(DefaultForwardingObjective.builder()
                    .withSelector(instructions.selector())
                    .nextStep(nextObjective.id())
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.SPECIFIC)
                    .add());
            }
        );

        return objectives;
    }

}
