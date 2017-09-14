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

package org.onosproject.drivers.p4runtime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.grpc.StatusRuntimeException;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiTableModel;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiCounterId;
import org.onosproject.net.pi.runtime.PiDirectCounterCellId;
import org.onosproject.net.pi.runtime.PiFlowRuleTranslationService;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableId;
import org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType;
import org.onosproject.p4runtime.api.P4RuntimeFlowRuleWrapper;
import org.onosproject.p4runtime.api.P4RuntimeTableEntryReference;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static org.onosproject.drivers.p4runtime.P4RuntimeFlowRuleProgrammable.Operation.APPLY;
import static org.onosproject.drivers.p4runtime.P4RuntimeFlowRuleProgrammable.Operation.REMOVE;
import static org.onosproject.net.flow.FlowEntry.FlowEntryState.ADDED;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.*;

/**
 * Implementation of the flow rule programmable behaviour for P4Runtime.
 */
public class P4RuntimeFlowRuleProgrammable extends AbstractP4RuntimeHandlerBehaviour implements FlowRuleProgrammable {

    /*
    When updating an existing rule, if true, we issue a DELETE operation before inserting the new one, otherwise we
    issue a MODIFY operation. This is useful fore devices that do not support MODIFY operations for table entries.
     */
    // TODO: make this attribute configurable by child drivers (e.g. BMv2 or Tofino)
    private boolean deleteEntryBeforeUpdate = true;

    /*
    If true, we ignore re-installing rules that are already known in the ENTRY_STORE, i.e. same match key and action.
     */
    // TODO: can remove this check as soon as the multi-apply-per-same-flow rule bug is fixed.
    private boolean checkEntryStoreBeforeUpdate = true;

    /*
    If true, we avoid querying the device and return the content of the ENTRY_STORE.
     */
    private boolean ignoreDeviceWhenGet = false;

    /*
    If true, we read all direct counters of a table with one request. Otherwise, send as many request as the number of
    table entries.
     */
    // TODO: set to true as soon as the feature is implemented in P4Runtime.
    private boolean readAllDirectCounters = false;

    // Needed to synchronize operations over the same table entry.
    private static final ConcurrentMap<P4RuntimeTableEntryReference, Lock> ENTRY_LOCKS = Maps.newConcurrentMap();

    // TODO: replace with distributed store.
    // Can reuse old BMv2TableEntryService from ONOS 1.6
    private static final ConcurrentMap<P4RuntimeTableEntryReference, P4RuntimeFlowRuleWrapper> ENTRY_STORE =
            Maps.newConcurrentMap();

    private PiPipelineModel pipelineModel;
    private PiPipelineInterpreter interpreter;
    private PiFlowRuleTranslationService piFlowRuleTranslationService;

    @Override
    protected boolean setupBehaviour() {

        if (!super.setupBehaviour()) {
            return false;
        }

        if (!device.is(PiPipelineInterpreter.class)) {
            log.warn("Unable to get interpreter of {}", deviceId);
            return false;
        }
        interpreter = device.as(PiPipelineInterpreter.class);
        pipelineModel = pipeconf.pipelineModel();
        piFlowRuleTranslationService = handler().get(PiFlowRuleTranslationService.class);
        return true;
    }

    @Override
    public Collection<FlowEntry> getFlowEntries() {

        if (!setupBehaviour()) {
            return Collections.emptyList();
        }

        if (ignoreDeviceWhenGet) {
            return ENTRY_STORE.values().stream()
                    .filter(frWrapper -> frWrapper.rule().deviceId().equals(this.deviceId))
                    .map(frWrapper -> new DefaultFlowEntry(frWrapper.rule(), ADDED, frWrapper.lifeInSeconds(),
                                                           0, 0))
                    .collect(Collectors.toList());
        }

        ImmutableList.Builder<FlowEntry> resultBuilder = ImmutableList.builder();
        List<PiTableEntry> inconsistentEntries = Lists.newArrayList();

        for (PiTableModel tableModel : pipelineModel.tables()) {

            PiTableId piTableId = PiTableId.of(tableModel.name());

            // Only dump tables that are exposed by the interpreter.
            // The reason is that some P4 targets (e.g. BMv2's simple_switch) use more table than those defined in the
            // P4 program, to implement other capabilities, e.g. action execution in control flow.
            if (!interpreter.mapPiTableId(piTableId).isPresent()) {
                continue; // next table
            }

            Collection<PiTableEntry> installedEntries;
            try {
                // TODO: optimize by dumping entries and counters in parallel, from ALL tables with the same request.
                installedEntries = client.dumpTable(piTableId, pipeconf).get();
            } catch (InterruptedException | ExecutionException e) {
                if (!(e.getCause() instanceof StatusRuntimeException)) {
                    // gRPC errors are logged in the client.
                    log.error("Exception while dumping table {} of {}", piTableId, deviceId, e);
                }
                return Collections.emptyList();
            }

            Map<PiTableEntry, PiCounterCellData> counterCellMap;
            try {
                if (interpreter.mapTableCounter(piTableId).isPresent()) {
                    PiCounterId piCounterId = interpreter.mapTableCounter(piTableId).get();
                    Collection<PiCounterCellData> cellDatas;
                    if (readAllDirectCounters) {
                        cellDatas = client.readAllCounterCells(Collections.singleton(piCounterId), pipeconf).get();
                    } else {
                        Set<PiCounterCellId> cellIds = installedEntries.stream()
                                .map(entry -> PiDirectCounterCellId.of(piCounterId, entry))
                                .collect(Collectors.toSet());
                        cellDatas = client.readCounterCells(cellIds, pipeconf).get();
                    }
                    counterCellMap = cellDatas.stream()
                            .collect(Collectors.toMap(c -> ((PiDirectCounterCellId) c.cellId()).tableEntry(), c -> c));
                } else {
                    counterCellMap = Collections.emptyMap();
                }
                installedEntries = client.dumpTable(piTableId, pipeconf).get();
            } catch (InterruptedException | ExecutionException e) {
                if (!(e.getCause() instanceof StatusRuntimeException)) {
                    // gRPC errors are logged in the client.
                    log.error("Exception while reading counters of table {} of {}", piTableId, deviceId, e);
                }
                counterCellMap = Collections.emptyMap();
            }

            for (PiTableEntry installedEntry : installedEntries) {

                P4RuntimeTableEntryReference entryRef = new P4RuntimeTableEntryReference(deviceId,
                                                                                         piTableId,
                                                                                         installedEntry.matchKey());

                if (!ENTRY_STORE.containsKey(entryRef)) {
                    // Inconsistent entry
                    inconsistentEntries.add(installedEntry);
                    continue; // next one.
                }

                P4RuntimeFlowRuleWrapper frWrapper = ENTRY_STORE.get(entryRef);

                long bytes = 0L;
                long packets = 0L;
                if (counterCellMap.containsKey(installedEntry)) {
                    PiCounterCellData counterCellData = counterCellMap.get(installedEntry);
                    bytes = counterCellData.bytes();
                    packets = counterCellData.packets();
                }

                resultBuilder.add(new DefaultFlowEntry(frWrapper.rule(),
                                                       ADDED,
                                                       frWrapper.lifeInSeconds(),
                                                       packets,
                                                       bytes));
            }
        }

        if (inconsistentEntries.size() > 0) {
            log.warn("Found {} entries in {} that are not known by table entry service," +
                             " removing them", inconsistentEntries.size(), deviceId);
            inconsistentEntries.forEach(entry -> log.debug(entry.toString()));
            // Async remove them.
            client.writeTableEntries(inconsistentEntries, DELETE, pipeconf);
        }

        return resultBuilder.build();
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        return processFlowRules(rules, APPLY);
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        return processFlowRules(rules, REMOVE);
    }

    private Collection<FlowRule> processFlowRules(Collection<FlowRule> rules, Operation operation) {

        if (!setupBehaviour()) {
            return Collections.emptyList();
        }

        ImmutableList.Builder<FlowRule> processedFlowRuleListBuilder = ImmutableList.builder();

        // TODO: send write operations in bulk (e.g. all entries to insert, modify or delete).
        // Instead of calling the client for each one of them.

        for (FlowRule rule : rules) {

            PiTableEntry piTableEntry;

            try {
                piTableEntry = piFlowRuleTranslationService.translate(rule, pipeconf);
            } catch (PiFlowRuleTranslationService.PiFlowRuleTranslationException e) {
                log.warn("Unable to translate flow rule: {} - {}", e.getMessage(), rule);
                continue; // next rule
            }

            PiTableId tableId = piTableEntry.table();
            P4RuntimeTableEntryReference entryRef = new P4RuntimeTableEntryReference(deviceId,
                                                                                     tableId, piTableEntry.matchKey());

            Lock lock = ENTRY_LOCKS.computeIfAbsent(entryRef, k -> new ReentrantLock());
            lock.lock();

            try {

                P4RuntimeFlowRuleWrapper frWrapper = ENTRY_STORE.get(entryRef);
                WriteOperationType opType = null;
                boolean doApply = true;

                if (operation == APPLY) {
                    if (frWrapper == null) {
                        // Entry is first-timer.
                        opType = INSERT;
                    } else {
                        // This match key already exists in the device.
                        if (checkEntryStoreBeforeUpdate &&
                                piTableEntry.action().equals(frWrapper.piTableEntry().action())) {
                            doApply = false;
                            log.debug("Ignoring re-apply of existing entry: {}", piTableEntry);
                        }
                        if (doApply) {
                            if (deleteEntryBeforeUpdate) {
                                // We've seen some strange error when trying to modify existing flow rules.
                                // Remove before re-adding the modified one.
                                try {
                                    if (client.writeTableEntries(newArrayList(piTableEntry), DELETE, pipeconf).get()) {
                                        frWrapper = null;
                                    } else {
                                        log.warn("Unable to DELETE table entry (before re-adding) in {}: {}",
                                                 deviceId, piTableEntry);
                                    }
                                } catch (InterruptedException | ExecutionException e) {
                                    log.warn("Exception while deleting table entry:", operation.name(), e);
                                }
                                opType = INSERT;
                            } else {
                                opType = MODIFY;
                            }
                        }
                    }
                } else {
                    opType = DELETE;
                }

                if (doApply) {
                    try {
                        if (client.writeTableEntries(newArrayList(piTableEntry), opType, pipeconf).get()) {
                            processedFlowRuleListBuilder.add(rule);
                            if (operation == APPLY) {
                                frWrapper = new P4RuntimeFlowRuleWrapper(rule, piTableEntry,
                                                                         System.currentTimeMillis());
                            } else {
                                frWrapper = null;
                            }
                        } else {
                            log.warn("Unable to {} table entry in {}: {}", opType.name(), deviceId, piTableEntry);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        log.warn("Exception while performing {} table entry operation:", operation.name(), e);
                    }
                } else {
                    processedFlowRuleListBuilder.add(rule);
                }

                // Update entryRef binding in table entry service.
                if (frWrapper != null) {
                    ENTRY_STORE.put(entryRef, frWrapper);
                } else {
                    ENTRY_STORE.remove(entryRef);
                }

            } finally {
                lock.unlock();
            }
        }

        return processedFlowRuleListBuilder.build();
    }

    enum Operation {
        APPLY, REMOVE
    }
}