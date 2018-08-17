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
package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.ImmutableList;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.intent.FlowObjectiveIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.impl.LabelAllocator;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
public class PathIntentFlowObjectiveCompiler
        extends PathCompiler<Objective>
        implements IntentCompiler<PathIntent>,
                   PathCompiler.PathCompilerCreateFlow<Objective> {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentConfigurableRegistrator registrator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ResourceService resourceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        registrator.registerCompiler(PathIntent.class, this, true);
        labelAllocator = new LabelAllocator(resourceService);
    }

    @Deactivate
    public void deactivate() {
        registrator.unregisterCompiler(PathIntent.class, true);
    }

    @Override
    public List<Intent> compile(PathIntent intent, List<Intent> installable) {

        List<Objective> objectives = new LinkedList<>();
        List<DeviceId> devices = new LinkedList<>();
        compile(this, intent, objectives, devices);

        return ImmutableList.of(new FlowObjectiveIntent(appId,
                                                        intent.key(),
                                                        devices,
                                                        objectives,
                                                        intent.resources(),
                                                        intent.resourceGroup()
        ));
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    public ResourceService resourceService() {
        return resourceService;
    }

    @Override
    public void createFlow(TrafficSelector originalSelector, TrafficTreatment originalTreatment,
                                          ConnectPoint ingress, ConnectPoint egress,
                                          int priority, boolean applyTreatment,
                                          List<Objective> objectives,
                                          List<DeviceId> devices) {
        TrafficSelector selector = DefaultTrafficSelector.builder(originalSelector)
                .matchInPort(ingress.port())
                .build();

        TrafficTreatment.Builder treatmentBuilder;
        if (applyTreatment) {
            treatmentBuilder = DefaultTrafficTreatment.builder(originalTreatment);
        } else {
            treatmentBuilder = DefaultTrafficTreatment.builder();
        }

        TrafficTreatment treatment = treatmentBuilder.setOutput(egress.port()).build();

        NextObjective nextObjective = DefaultNextObjective.builder()
                .withId(flowObjectiveService.allocateNextId())
                .addTreatment(treatment)
                .withType(NextObjective.Type.SIMPLE)
                .fromApp(appId)
                .makePermanent().add();
        objectives.add(nextObjective);
        devices.add(ingress.deviceId());

        objectives.add(DefaultForwardingObjective.builder()
                .withSelector(selector)
                .nextStep(nextObjective.id())
                .withPriority(priority)
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .add());
        devices.add(ingress.deviceId());
    }
}
