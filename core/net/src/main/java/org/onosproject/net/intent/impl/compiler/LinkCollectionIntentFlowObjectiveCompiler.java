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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Compiler to produce flow objectives from link collections.
 */
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

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        registrator.registerCompiler(LinkCollectionIntent.class, this, true);
    }

    @Deactivate
    public void deactivate() {
        registrator.unregisterCompiler(LinkCollectionIntent.class, true);
    }

    @Override
    public List<Intent> compile(LinkCollectionIntent intent, List<Intent> installable) {

        SetMultimap<DeviceId, PortNumber> inputPorts = HashMultimap.create();
        SetMultimap<DeviceId, PortNumber> outputPorts = HashMultimap.create();

        computePorts(intent, inputPorts, outputPorts);

        List<Objective> objectives = new ArrayList<>();
        List<DeviceId> devices = new ArrayList<>();
        for (DeviceId deviceId: outputPorts.keys()) {
            List<Objective> deviceObjectives =
                    createRules(intent,
                                deviceId,
                                inputPorts.get(deviceId),
                                outputPorts.get(deviceId));
            deviceObjectives.forEach(objective -> {
                objectives.add(objective);
                devices.add(deviceId);
            });
        }
        return Collections.singletonList(
                new FlowObjectiveIntent(appId, devices, objectives, intent.resources()));
    }

    @Override
    protected List<Objective> createRules(LinkCollectionIntent intent, DeviceId deviceId,
                                       Set<PortNumber> inPorts, Set<PortNumber> outPorts) {

        Set<PortNumber> ingressPorts = Sets.newHashSet();
        Set<PortNumber> egressPorts = Sets.newHashSet();

        computePorts(intent, deviceId, ingressPorts, egressPorts);

        List<Objective> objectives = new ArrayList<>(inPorts.size());
        Set<PortNumber> copyIngressPorts = ImmutableSet.copyOf(ingressPorts);
        Set<PortNumber> copyEgressPorts = ImmutableSet.copyOf(egressPorts);

        inPorts.forEach(inport -> {
            ForwardingInstructions instructions = this.createForwardingInstructions(intent,
                                                                                    inport,
                                                                                    deviceId,
                                                                                    outPorts,
                                                                                    copyIngressPorts,
                                                                                    copyEgressPorts);

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
