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
import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.context.Bmv2FlowRuleTranslator;
import org.onosproject.bmv2.api.context.Bmv2FlowRuleTranslatorException;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.bmv2.api.context.Bmv2TableModel;
import org.onosproject.bmv2.api.runtime.Bmv2DeviceAgent;
import org.onosproject.bmv2.api.runtime.Bmv2FlowRuleWrapper;
import org.onosproject.bmv2.api.runtime.Bmv2MatchKey;
import org.onosproject.bmv2.api.runtime.Bmv2ParsedTableEntry;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntry;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntryReference;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.bmv2.api.service.Bmv2DeviceContextService;
import org.onosproject.bmv2.api.service.Bmv2TableEntryService;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.onosproject.bmv2.api.runtime.Bmv2RuntimeException.Code.*;
import static org.onosproject.net.flow.FlowEntry.FlowEntryState.ADDED;

/**
 * Implementation of the flow rule programmable behaviour for BMv2.
 */
public class Bmv2FlowRuleProgrammable extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // Needed to synchronize operations over the same table entry.
    private static final ConcurrentMap<Bmv2TableEntryReference, Lock> ENTRY_LOCKS = Maps.newConcurrentMap();

    private Bmv2Controller controller;
    private Bmv2TableEntryService tableEntryService;
    private Bmv2DeviceContextService contextService;

    private boolean init() {
        try {
            controller = handler().get(Bmv2Controller.class);
            tableEntryService = handler().get(Bmv2TableEntryService.class);
            contextService = handler().get(Bmv2DeviceContextService.class);
            return true;
        } catch (ServiceNotFoundException e) {
            log.warn(e.getMessage());
            return false;
        }
    }

    @Override
    public Collection<FlowEntry> getFlowEntries() {

        if (!init()) {
            return Collections.emptyList();
        }

        DeviceId deviceId = handler().data().deviceId();

        Bmv2DeviceAgent deviceAgent;
        try {
            deviceAgent = controller.getAgent(deviceId);
        } catch (Bmv2RuntimeException e) {
            log.error("Failed to get BMv2 device agent: {}", e.explain());
            return Collections.emptyList();
        }

        Bmv2DeviceContext context = contextService.getContext(deviceId);
        if (context == null) {
            log.warn("Unable to get device context for {}", deviceId);
        }

        Bmv2Interpreter interpreter = context.interpreter();
        Bmv2Configuration configuration = context.configuration();

        List<FlowEntry> entryList = Lists.newArrayList();

        for (Bmv2TableModel table : configuration.tables()) {
            // For each table in the configuration AND exposed by the interpreter.
            if (!interpreter.tableIdMap().inverse().containsKey(table.name())) {
                continue; // next table
            }

            List<Bmv2ParsedTableEntry> installedEntries;
            try {
                installedEntries = deviceAgent.getTableEntries(table.name());
            } catch (Bmv2RuntimeException e) {
                log.warn("Failed to get table entries of table {} of {}: {}", table.name(), deviceId, e.explain());
                continue; // next table
            }

            for (Bmv2ParsedTableEntry parsedEntry : installedEntries) {
                Bmv2TableEntryReference entryRef = new Bmv2TableEntryReference(deviceId, table.name(),
                                                                               parsedEntry.matchKey());

                Lock lock = ENTRY_LOCKS.computeIfAbsent(entryRef, key -> new ReentrantLock());
                lock.lock();

                try {
                    Bmv2FlowRuleWrapper frWrapper = tableEntryService.lookup(entryRef);

                    if (frWrapper == null) {
                        log.debug("Missing reference from table entry service. Deleting it. BUG? " +
                                         "deviceId={}, tableName={}, matchKey={}",
                                 deviceId, table.name(), entryRef.matchKey());
                        try {
                            doRemove(deviceAgent, table.name(), parsedEntry.entryId(), parsedEntry.matchKey());
                        } catch (Bmv2RuntimeException e) {
                            log.warn("Unable to remove inconsistent flow rule: {}", e.explain());
                        }
                        continue; // next entry
                    }

                    long remoteEntryId = parsedEntry.entryId();
                    long localEntryId = frWrapper.entryId();

                    if (remoteEntryId != localEntryId) {
                        log.debug("getFlowEntries(): inconsistent entry id! BUG? Updating it... remote={}, local={}",
                                 remoteEntryId, localEntryId);
                        frWrapper = new Bmv2FlowRuleWrapper(frWrapper.rule(), remoteEntryId,
                                                            frWrapper.installedOnMillis());
                        tableEntryService.bind(entryRef, frWrapper);
                    }

                    long bytes = 0L;
                    long packets = 0L;

                    if (table.hasCounters()) {
                        // Read counter values from device.
                        try {
                            Pair<Long, Long> counterValue = deviceAgent.readTableEntryCounter(table.name(),
                                                                                              remoteEntryId);
                            bytes = counterValue.getLeft();
                            packets = counterValue.getRight();
                        } catch (Bmv2RuntimeException e) {
                            log.warn("Unable to get counters for entry {}/{} of device {}: {}",
                                     table.name(), remoteEntryId, deviceId, e.explain());
                        }
                    }

                    FlowEntry entry = new DefaultFlowEntry(frWrapper.rule(), ADDED, frWrapper.lifeInSeconds(),
                                                           packets, bytes);
                    entryList.add(entry);

                } finally {
                    lock.unlock();
                }
            }
        }

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

        if (!init()) {
            return Collections.emptyList();
        }

        DeviceId deviceId = handler().data().deviceId();

        Bmv2DeviceAgent deviceAgent;
        try {
            deviceAgent = controller.getAgent(deviceId);
        } catch (Bmv2RuntimeException e) {
            log.error("Failed to get BMv2 device agent: {}", e.explain());
            return Collections.emptyList();
        }

        Bmv2DeviceContext context = contextService.getContext(deviceId);
        if (context == null) {
            log.error("Unable to get device context for {}", deviceId);
            return Collections.emptyList();
        }

        Bmv2FlowRuleTranslator translator = tableEntryService.getFlowRuleTranslator();

        List<FlowRule> processedFlowRules = Lists.newArrayList();

        for (FlowRule rule : rules) {

            Bmv2TableEntry bmv2Entry;

            try {
                bmv2Entry = translator.translate(rule, context);
            } catch (Bmv2FlowRuleTranslatorException e) {
                log.warn("Unable to translate flow rule: {} - {}", e.getMessage(), rule);
                continue; // next rule
            }

            String tableName = bmv2Entry.tableName();
            Bmv2TableEntryReference entryRef = new Bmv2TableEntryReference(deviceId, tableName, bmv2Entry.matchKey());

            Lock lock = ENTRY_LOCKS.computeIfAbsent(entryRef, k -> new ReentrantLock());
            lock.lock();
            try {
                // Get from store
                Bmv2FlowRuleWrapper frWrapper = tableEntryService.lookup(entryRef);
                try {
                    if (operation == Operation.APPLY) {
                        // Apply entry
                        long entryId;
                        if (frWrapper != null) {
                            // Existing entry.
                            entryId = frWrapper.entryId();
                            // Tentatively delete entry before re-adding.
                            // It might not exist on device due to inconsistencies.
                            silentlyRemove(deviceAgent, entryRef.tableName(), entryId);
                        }
                        // Add entry.
                        entryId = doAddEntry(deviceAgent, bmv2Entry);
                        frWrapper = new Bmv2FlowRuleWrapper(rule, entryId, System.currentTimeMillis());
                    } else {
                        // Remove entry
                        if (frWrapper == null) {
                            // Entry not found in map, how come?
                            forceRemove(deviceAgent, entryRef.tableName(), entryRef.matchKey());
                        } else {
                            long entryId = frWrapper.entryId();
                            doRemove(deviceAgent, entryRef.tableName(), entryId, entryRef.matchKey());
                        }
                        frWrapper = null;
                    }
                    // If here, no exceptions... things went well :)
                    processedFlowRules.add(rule);
                } catch (Bmv2RuntimeException e) {
                    log.warn("Unable to {} flow rule: {}", operation.name(), e.explain());
                }

                // Update entryRef binding in table entry service.
                if (frWrapper != null) {
                    tableEntryService.bind(entryRef, frWrapper);
                } else {
                    tableEntryService.unbind(entryRef);
                }
            } finally {
                lock.unlock();
            }
        }

        return processedFlowRules;
    }

    private long doAddEntry(Bmv2DeviceAgent agent, Bmv2TableEntry entry) throws Bmv2RuntimeException {
        try {
            return agent.addTableEntry(entry);
        } catch (Bmv2RuntimeException e) {
            if (e.getCode().equals(TABLE_DUPLICATE_ENTRY)) {
                forceRemove(agent, entry.tableName(), entry.matchKey());
                return agent.addTableEntry(entry);
            } else {
                throw e;
            }
        }
    }

    private void doRemove(Bmv2DeviceAgent agent, String tableName, long entryId, Bmv2MatchKey matchKey)
            throws Bmv2RuntimeException {
        try {
            agent.deleteTableEntry(tableName, entryId);
        } catch (Bmv2RuntimeException e) {
            if (e.getCode().equals(TABLE_INVALID_HANDLE) || e.getCode().equals(TABLE_EXPIRED_HANDLE)) {
                // entry is not there with the declared ID, try with a forced remove.
                forceRemove(agent, tableName, matchKey);
            } else {
                throw e;
            }
        }
    }

    private void forceRemove(Bmv2DeviceAgent agent, String tableName, Bmv2MatchKey matchKey)
            throws Bmv2RuntimeException {
        // Find the entryID (expensive call!)
        for (Bmv2ParsedTableEntry pEntry : agent.getTableEntries(tableName)) {
            if (pEntry.matchKey().equals(matchKey)) {
                // Remove entry and drop exceptions.
                silentlyRemove(agent, tableName, pEntry.entryId());
                break;
            }
        }
    }

    private void silentlyRemove(Bmv2DeviceAgent agent, String tableName, long entryId) {
        try {
            agent.deleteTableEntry(tableName, entryId);
        } catch (Bmv2RuntimeException e) {
            // do nothing
        }
    }

    private enum Operation {
        APPLY, REMOVE
    }
}