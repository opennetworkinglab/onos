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

package org.onosproject.drivers.bmv2;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.onosproject.bmv2.api.runtime.Bmv2MatchKey;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntry;
import org.onosproject.bmv2.ctl.Bmv2ThriftClient;
import org.onosproject.drivers.bmv2.translators.Bmv2DefaultFlowRuleTranslator;
import org.onosproject.drivers.bmv2.translators.Bmv2FlowRuleTranslator;
import org.onosproject.drivers.bmv2.translators.Bmv2FlowRuleTranslatorException;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class Bmv2FlowRuleDriver extends AbstractHandlerBehaviour
        implements FlowRuleProgrammable {

    private static final Logger LOG =
            LoggerFactory.getLogger(Bmv2FlowRuleDriver.class);
    // There's no Bmv2 client method to poll flow entries from the device device. gitNeed a local store.
    private static final ConcurrentMap<Triple<DeviceId, String, Bmv2MatchKey>, Pair<Long, FlowEntry>>
            ENTRIES_MAP = Maps.newConcurrentMap();
    private static final Bmv2FlowRuleTranslator TRANSLATOR = new Bmv2DefaultFlowRuleTranslator();

    @Override
    public Collection<FlowEntry> getFlowEntries() {

        DeviceId deviceId = handler().data().deviceId();

        List<FlowEntry> entryList = Lists.newArrayList();

        // FIXME: improve this, e.g. might store a separate Map<DeviceId, Collection<FlowEntry>>
        ENTRIES_MAP.forEach((key, value) -> {
            if (key.getLeft() == deviceId && value != null) {
                entryList.add(value.getRight());
            }
        });

        return Collections.unmodifiableCollection(entryList);
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {

        return processFlowRules(rules, Operation.APPLY);
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {

        return processFlowRules(rules, Operation.REMOVE);
    }

    private Collection<FlowRule> processFlowRules(Collection<FlowRule> rules, Operation operation) {

        DeviceId deviceId = handler().data().deviceId();

        Bmv2ThriftClient deviceClient;
        try {
            deviceClient = Bmv2ThriftClient.of(deviceId);
        } catch (Bmv2RuntimeException e) {
            LOG.error("Failed to connect to Bmv2 device", e);
            return Collections.emptyList();
        }

        List<FlowRule> processedFlowRules = Lists.newArrayList();

        for (FlowRule rule : rules) {

            Bmv2TableEntry bmv2Entry;

            try {
                bmv2Entry = TRANSLATOR.translate(rule);
            } catch (Bmv2FlowRuleTranslatorException e) {
                LOG.error("Unable to translate flow rule: {}", e.getMessage());
                continue;
            }

            String tableName = bmv2Entry.tableName();
            Triple<DeviceId, String, Bmv2MatchKey> entryKey = Triple.of(deviceId, tableName, bmv2Entry.matchKey());

            /*
            From here on threads are synchronized over entryKey, i.e. serialize operations
            over the same matchKey of a specific table and device.
             */
            ENTRIES_MAP.compute(entryKey, (key, value) -> {
                try {
                    if (operation == Operation.APPLY) {
                        // Apply entry
                        long entryId;
                        if (value == null) {
                            // New entry
                            entryId = deviceClient.addTableEntry(bmv2Entry);
                        } else {
                            // Existing entry
                            entryId = value.getKey();
                            // FIXME: check if priority or timeout changed
                            // In this case we should to re-add the entry (not modify)
                            deviceClient.modifyTableEntry(tableName, entryId, bmv2Entry.action());
                        }
                        // TODO: evaluate flow entry life, bytes and packets
                        FlowEntry flowEntry = new DefaultFlowEntry(
                                rule, FlowEntry.FlowEntryState.ADDED, 0, 0, 0);
                        value = Pair.of(entryId, flowEntry);
                    } else {
                        // Remove entry
                        if (value == null) {
                            // Entry not found in map, how come?
                            LOG.debug("Trying to remove entry, but entry ID not found: " + entryKey);
                        } else {
                            deviceClient.deleteTableEntry(tableName, value.getKey());
                            value = null;
                        }
                    }
                    // If here, no exceptions... things went well :)
                    processedFlowRules.add(rule);
                } catch (Bmv2RuntimeException e) {
                    LOG.error("Unable to " + operation.name().toLowerCase() + " flow rule", e);
                } catch (Exception e) {
                    LOG.error("Uncaught exception while processing flow rule", e);
                }
                return value;
            });
        }

        return processedFlowRules;
    }

    private enum Operation {
        APPLY, REMOVE
    }
}