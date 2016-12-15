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

package org.onosproject.patchpanel.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cli.net.ConnectPointCompleter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.packet.PacketPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class acts as a software patch panel application.
 * The user specifies 2 connectpoint on the same device that he/she would like to patch.
 * Using a flow rule, the 2 connectpoints are patched.
 */
@Component(immediate = true)
@Service
public class PatchPanel implements PatchPanelService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // OSGI: help bundle plugin discover runtime package dependency.
    @SuppressWarnings("unused")
    private ConnectPointCompleter connectPointCompleter;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private Map<PatchId, Patch> patches;

    private ApplicationId appId;

    private AtomicInteger ids = new AtomicInteger();


    @Activate
    protected void activate() throws NullPointerException {
        patches = new HashMap<>();

        appId = coreService.registerApplication("org.onosproject.patchpanel");
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean addPatch(ConnectPoint cp1, ConnectPoint cp2) {
        checkNotNull(cp1);
        checkNotNull(cp2);

        checkArgument(cp1.deviceId().equals(cp2.deviceId()), "Ports must be on the same device");
        checkArgument(!cp1.equals(cp2), "Ports cannot be the same");

        if (patches.values().stream()
                .filter(patch -> patch.port1().equals(cp1) || patch.port1().equals(cp2)
                        || patch.port2().equals(cp1) || patch.port2().equals(cp2))
                .findAny().isPresent()) {
            log.info("One or both of these ports are already in use, NO FLOW");
            return false;
        }

        Patch patch = new Patch(PatchId.patchId(ids.incrementAndGet()), cp1, cp2);

        patches.put(patch.id(), patch);

        setFlowRuleService(patch);

        return true;
    }

    @Override
    public boolean removePatch(PatchId id) {
        Patch patch = patches.remove(id);
        if (patch == null) {
            return false;
        }

        removePatchFlows(patch);
        return true;
    }

    @Override
    public Set<Patch> getPatches() {
        return ImmutableSet.copyOf(patches.values());
    }

    public void setFlowRuleService(Patch patch) {
        createFlowRules(patch).forEach(flowRuleService::applyFlowRules);
    }

    private void removePatchFlows(Patch patch) {
        createFlowRules(patch).forEach(flowRuleService::removeFlowRules);
    }

    private Collection<FlowRule> createFlowRules(Patch patch) {
        DeviceId deviceId = patch.port1().deviceId();
        PortNumber outPort = patch.port2().port();
        PortNumber inPort = patch.port1().port();
        FlowRule fr = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(DefaultTrafficSelector.builder().matchInPort(inPort).build())
                .withTreatment(DefaultTrafficTreatment.builder().setOutput(outPort).build())
                .withPriority(PacketPriority.REACTIVE.priorityValue())
                .makePermanent()
                .fromApp(appId).build();

        FlowRule fr2 = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(DefaultTrafficSelector.builder().matchInPort(outPort).build())
                .withTreatment(DefaultTrafficTreatment.builder().setOutput(inPort).build())
                .withPriority(PacketPriority.REACTIVE.priorityValue())
                .makePermanent()
                .fromApp(appId).build();

        return ImmutableList.of(fr, fr2);
    }
}
