/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.drivers.bmv2;

import com.google.common.collect.Maps;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.runtime.PiFlowRuleTranslationService;
import org.onosproject.net.pi.runtime.PiPipeconfService;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the flow rule programmable behaviour for BMv2.
 */
public class Bmv2FlowRuleProgrammable extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final Map<DeviceId, Collection<Bmv2FlowRuleWrapper>> INSTALLED_FLOWENTRIES = Maps.newHashMap();

    P4RuntimeClient client;
    PiPipeconf pipeconf;
    PiPipelineInterpreter interpreter;
    DeviceId deviceId;
    PiFlowRuleTranslationService piFlowRuleTranslationService;

    private boolean init() {
        deviceId = handler().data().deviceId();

        P4RuntimeController controller = handler().get(P4RuntimeController.class);
        if (!controller.hasClient(deviceId)) {
            log.warn("Unable to find client for {}, aborting flow rule operation", deviceId);
            return false;
        }

        client = controller.getClient(deviceId);
        piFlowRuleTranslationService = handler().get(PiFlowRuleTranslationService.class);
        PiPipeconfService piPipeconfService = handler().get(PiPipeconfService.class);

        if (piPipeconfService.ofDevice(deviceId).isPresent() &&
                piPipeconfService.getPipeconf(piPipeconfService.ofDevice(deviceId).get()).isPresent()) {
            pipeconf = piPipeconfService.getPipeconf(piPipeconfService.ofDevice(deviceId).get()).get();
        } else {
            log.warn("Unable to get the pipeconf of {}", deviceId);
            return false;
        }

        DeviceService deviceService = handler().get(DeviceService.class);
        Device device = deviceService.getDevice(deviceId);
        interpreter = device.is(PiPipelineInterpreter.class) ? device.as(PiPipelineInterpreter.class) : null;
        if (device.is(PiPipelineInterpreter.class)) {
            log.warn("Device {} unable to instantiate interpreter of pipeconf {}", deviceId, pipeconf.id());
            return false;
        }

        return true;
    }

    @Override
    public Collection<FlowEntry> getFlowEntries() {

        if (!init()) {
            return Collections.emptyList();
        }

        //TD DO: retrieve statistics.
        long packets = 0;
        long bytes = 0;
        Collection<FlowEntry> flowEntries = INSTALLED_FLOWENTRIES
                .get(deviceId).stream().map(wrapper -> new DefaultFlowEntry(wrapper.rule(),
                                                                            FlowEntry.FlowEntryState.ADDED,
                                                                            wrapper.lifeInSeconds(),
                                                                            packets,
                                                                            bytes))
                .collect(Collectors.toList());

        return Collections.unmodifiableCollection(flowEntries);
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {

        Collection<FlowRule> insertFlowRules = rules.stream()
                .filter(r -> INSTALLED_FLOWENTRIES.get(deviceId) == null ||
                        INSTALLED_FLOWENTRIES.get(deviceId).stream().noneMatch(x -> x.rule().id() == r.id()))
                .collect(Collectors.toList());

        Collection<FlowRule> updateFlowRules = rules.stream()
                .filter(r -> INSTALLED_FLOWENTRIES.get(deviceId) != null &&
                        INSTALLED_FLOWENTRIES.get(deviceId).stream().anyMatch(x -> x.rule().id() == r.id()))
                .collect(Collectors.toList());

        Collection<FlowRule> flowRules = processFlowRules(insertFlowRules, WriteOperationType.INSERT);
        flowRules.addAll(processFlowRules(updateFlowRules, WriteOperationType.MODIFY));
        return flowRules;
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {

        return processFlowRules(rules, WriteOperationType.DELETE);
    }

    private Collection<FlowRule> processFlowRules(Collection<FlowRule> rules, WriteOperationType opType) {

        if (!init()) {
            return Collections.emptyList();
        }

        Collection<PiTableEntry> piTableEntries = rules.stream().map(r -> {
            PiTableEntry piTableEntry = null;
            try {
                piTableEntry = piFlowRuleTranslationService.translate(r, pipeconf);
            } catch (PiFlowRuleTranslationService.PiFlowRuleTranslationException e) {
                log.error("Flow rule {} can not translte to PiTableEntry {}", r.toString(), e.getMessage());
            }
            return piTableEntry;
        }).collect(Collectors.toList());

        Collection<FlowRule> installedEntries = Collections.emptyList();
        client.writeTableEntries(piTableEntries, opType, pipeconf).whenComplete((r, e) -> {
            if (r) {

                Collection<Bmv2FlowRuleWrapper> bmv2FlowRuleWrappers = rules.stream()
                        .map(rule -> new Bmv2FlowRuleWrapper(rule, System.currentTimeMillis()))
                        .collect(Collectors.toList());

                if (opType == WriteOperationType.INSERT) {
                    INSTALLED_FLOWENTRIES.put(deviceId, bmv2FlowRuleWrappers);
                } else if (opType == WriteOperationType.MODIFY) {
                    rules.stream().forEach(rule -> INSTALLED_FLOWENTRIES
                            .get(deviceId).removeIf(x -> x.rule().id() == rule.id()));
                    INSTALLED_FLOWENTRIES.put(deviceId, bmv2FlowRuleWrappers);
                } else if (opType == WriteOperationType.DELETE) {
                    INSTALLED_FLOWENTRIES.get(deviceId).removeAll(bmv2FlowRuleWrappers);
                }
                installedEntries.addAll(rules);
            }
        });

        return installedEntries;
    }
}