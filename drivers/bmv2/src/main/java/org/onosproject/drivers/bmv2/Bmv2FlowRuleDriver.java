/*
 * Copyright 2014-2016 Open Networking Laboratory
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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.bmv2.api.Bmv2ExtensionSelector;
import org.onosproject.bmv2.api.Bmv2ExtensionTreatment;
import org.onosproject.bmv2.api.Bmv2TableEntry;
import org.onosproject.bmv2.api.Bmv2Exception;
import org.onosproject.bmv2.ctl.Bmv2ThriftClient;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.ExtensionCriterion;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Bmv2FlowRuleDriver extends AbstractHandlerBehaviour
        implements FlowRuleProgrammable {

    private final Logger log =
            LoggerFactory.getLogger(this.getClass());

    // Bmv2 doesn't support proper table dump, use a local store
    // FIXME: synchronize entries with device
    private final Map<FlowRule, FlowEntry> deviceEntriesMap = Maps.newHashMap();
    private final Map<Integer, Set<FlowRule>> tableRulesMap = Maps.newHashMap();
    private final Map<FlowRule, Long> tableEntryIdsMap = Maps.newHashMap();

    @Override
    public Collection<FlowEntry> getFlowEntries() {
        return Collections.unmodifiableCollection(
                deviceEntriesMap.values());
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        Bmv2ThriftClient deviceClient;
        try {
            deviceClient = getDeviceClient();
        } catch (Bmv2Exception e) {
            return Collections.emptyList();
        }

        List<FlowRule> appliedFlowRules = Lists.newArrayList();

        for (FlowRule rule : rules) {

            Bmv2TableEntry entry;

            try {
                entry = parseFlowRule(rule);
            } catch (IllegalStateException e) {
                log.error("Unable to parse flow rule", e);
                continue;
            }

            // Instantiate flowrule set for table if it does not exist
            if (!tableRulesMap.containsKey(rule.tableId())) {
                tableRulesMap.put(rule.tableId(), Sets.newHashSet());
            }

            if (tableRulesMap.get(rule.tableId()).contains(rule)) {
                /* Rule is already installed in the table */
                long entryId = tableEntryIdsMap.get(rule);

                try {
                    deviceClient.modifyTableEntry(
                            entry.tableName(), entryId, entry.action());

                    // Replace stored rule as treatment, etc. might have changed
                    // Java Set doesn't replace on add, remove first
                    tableRulesMap.get(rule.tableId()).remove(rule);
                    tableRulesMap.get(rule.tableId()).add(rule);
                    tableEntryIdsMap.put(rule, entryId);
                    deviceEntriesMap.put(rule, new DefaultFlowEntry(
                            rule, FlowEntry.FlowEntryState.ADDED, 0, 0, 0));
                } catch (Bmv2Exception e) {
                    log.error("Unable to update flow rule", e);
                    continue;
                }

            } else {
                /* Rule is new */
                try {
                    long entryId = deviceClient.addTableEntry(entry);

                    tableRulesMap.get(rule.tableId()).add(rule);
                    tableEntryIdsMap.put(rule, entryId);
                    deviceEntriesMap.put(rule, new DefaultFlowEntry(
                            rule, FlowEntry.FlowEntryState.ADDED, 0, 0, 0));
                } catch (Bmv2Exception e) {
                    log.error("Unable to add flow rule", e);
                    continue;
                }
            }

            appliedFlowRules.add(rule);
        }

        return Collections.unmodifiableCollection(appliedFlowRules);
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        Bmv2ThriftClient deviceClient;
        try {
            deviceClient = getDeviceClient();
        } catch (Bmv2Exception e) {
            return Collections.emptyList();
        }

        List<FlowRule> removedFlowRules = Lists.newArrayList();

        for (FlowRule rule : rules) {

            if (tableEntryIdsMap.containsKey(rule)) {
                long entryId = tableEntryIdsMap.get(rule);
                String tableName = parseTableName(rule.tableId());

                try {
                    deviceClient.deleteTableEntry(tableName, entryId);
                } catch (Bmv2Exception e) {
                    log.error("Unable to delete flow rule", e);
                    continue;
                }

                /* remove from local store */
                tableEntryIdsMap.remove(rule);
                tableRulesMap.get(rule.tableId()).remove(rule);
                deviceEntriesMap.remove(rule);

                removedFlowRules.add(rule);
            }
        }

        return Collections.unmodifiableCollection(removedFlowRules);
    }

    private Bmv2TableEntry parseFlowRule(FlowRule flowRule) {

        // TODO make it pipeline dependant, i.e. implement mapping

        Bmv2TableEntry.Builder entryBuilder = Bmv2TableEntry.builder();

        // Check selector
        ExtensionCriterion ec =
                (ExtensionCriterion) flowRule
                        .selector().getCriterion(Criterion.Type.EXTENSION);
        Preconditions.checkState(
                flowRule.selector().criteria().size() == 1
                        && ec != null,
                "Selector must have only 1 criterion of type EXTENSION");
        ExtensionSelector es = ec.extensionSelector();
        Preconditions.checkState(
                es.type() == ExtensionSelectorType.ExtensionSelectorTypes.P4_BMV2_MATCH_KEY.type(),
                "ExtensionSelectorType must be P4_BMV2_MATCH_KEY");

        // Selector OK, get Bmv2MatchKey
        entryBuilder.withMatchKey(((Bmv2ExtensionSelector) es).matchKey());

        // Check treatment
        Instruction inst = flowRule.treatment().allInstructions().get(0);
        Preconditions.checkState(
                flowRule.treatment().allInstructions().size() == 1
                        && inst.type() == Instruction.Type.EXTENSION,
                "Treatment must have only 1 instruction of type EXTENSION");
        ExtensionTreatment et =
                ((Instructions.ExtensionInstructionWrapper) inst)
                        .extensionInstruction();

        Preconditions.checkState(
                et.type() == ExtensionTreatmentType.ExtensionTreatmentTypes.P4_BMV2_ACTION.type(),
                "ExtensionTreatmentType must be P4_BMV2_ACTION");

        // Treatment OK, get Bmv2Action
        entryBuilder.withAction(((Bmv2ExtensionTreatment) et).getAction());

        // Table name
        entryBuilder.withTableName(parseTableName(flowRule.tableId()));

        if (!flowRule.isPermanent()) {
            entryBuilder.withTimeout(flowRule.timeout());
        }

        entryBuilder.withPriority(flowRule.priority());

        return entryBuilder.build();
    }

    private String parseTableName(int tableId) {
        // TODO: map tableId with tableName according to P4 JSON
        return "table" + String.valueOf(tableId);
    }

    private Bmv2ThriftClient getDeviceClient() throws Bmv2Exception {
        try {
            return Bmv2ThriftClient.of(handler().data().deviceId());
        } catch (Bmv2Exception e) {
            log.error("Failed to connect to Bmv2 device", e);
            throw e;
        }
    }
}
