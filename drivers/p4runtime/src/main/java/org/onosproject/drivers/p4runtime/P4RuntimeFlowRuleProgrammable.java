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
import org.onlab.util.SharedExecutors;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeTableMirror;
import org.onosproject.drivers.p4runtime.mirror.TimedEntry;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.model.PiTableModel;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableEntryHandle;
import org.onosproject.net.pi.service.PiFlowRuleTranslator;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.net.pi.service.PiTranslationException;
import org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singleton;
import static org.onosproject.drivers.p4runtime.P4RuntimeFlowRuleProgrammable.Operation.APPLY;
import static org.onosproject.drivers.p4runtime.P4RuntimeFlowRuleProgrammable.Operation.REMOVE;
import static org.onosproject.net.flow.FlowEntry.FlowEntryState.ADDED;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.DELETE;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.INSERT;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.MODIFY;

/**
 * Implementation of the flow rule programmable behaviour for P4Runtime.
 */
public class P4RuntimeFlowRuleProgrammable
        extends AbstractP4RuntimeHandlerBehaviour
        implements FlowRuleProgrammable {

    // When updating an existing rule, if true, we issue a DELETE operation
    // before inserting the new one, otherwise we issue a MODIFY operation. This
    // is useful fore devices that do not support MODIFY operations for table
    // entries.
    // TODO: make this attribute configurable by child drivers (e.g. BMv2 or Tofino)
    private boolean deleteEntryBeforeUpdate = true;

    // If true, we ignore re-installing rules that are already exists the
    // device, i.e. same match key and action.
    // FIXME: can remove this check as soon as the multi-apply-per-same-flow rule bug is fixed.
    private boolean checkStoreBeforeUpdate = true;

    // If true, we avoid querying the device and return what's already known by
    // the ONOS store.
    private boolean ignoreDeviceWhenGet = true;

    /* If true, we read all direct counters of a table with one request.
    Otherwise, we send as many requests as the number of table entries. */
    // FIXME: set to true as soon as the feature is implemented in P4Runtime.
    private boolean readAllDirectCounters = false;

    // Needed to synchronize operations over the same table entry.
    // FIXME: locks should be removed when unused (hint use cache with timeout)
    private static final ConcurrentMap<PiTableEntryHandle, Lock>
            ENTRY_LOCKS = Maps.newConcurrentMap();

    private PiPipelineModel pipelineModel;
    private PiPipelineInterpreter interpreter;
    private P4RuntimeTableMirror tableMirror;
    private PiFlowRuleTranslator translator;

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
        tableMirror = handler().get(P4RuntimeTableMirror.class);
        translator = piTranslationService.flowRuleTranslator();
        return true;
    }

    @Override
    public Collection<FlowEntry> getFlowEntries() {

        if (!setupBehaviour()) {
            return Collections.emptyList();
        }

        if (ignoreDeviceWhenGet) {
            return getFlowEntriesFromMirror();
        }

        final ImmutableList.Builder<FlowEntry> result = ImmutableList.builder();
        final List<PiTableEntry> inconsistentEntries = Lists.newArrayList();

        for (PiTableModel tableModel : pipelineModel.tables()) {

            final PiTableId piTableId = tableModel.id();

            // Read table entries.
            final Collection<PiTableEntry> installedEntries;
            try {
                // TODO: optimize by dumping entries and counters in parallel
                // From ALL tables with the same request.
                installedEntries = client.dumpTable(piTableId, pipeconf).get();
            } catch (InterruptedException | ExecutionException e) {
                if (!(e.getCause() instanceof StatusRuntimeException)) {
                    // gRPC errors are logged in the client.
                    log.error("Exception while dumping table {} of {}",
                              piTableId, deviceId, e);
                }
                continue; // next table
            }

            if (installedEntries.size() == 0) {
                continue; // next table
            }

            // Read table direct counters (if any).
            final Map<PiTableEntry, PiCounterCellData> counterCellMap;
            if (interpreter.mapTableCounter(piTableId).isPresent()) {
                PiCounterId piCounterId = interpreter.mapTableCounter(piTableId).get();
                counterCellMap = readEntryCounters(piCounterId, installedEntries);
            } else {
                counterCellMap = Collections.emptyMap();
            }

            // Forge flow entries with counter values.
            for (PiTableEntry installedEntry : installedEntries) {

                final FlowEntry flowEntry = forgeFlowEntry(
                        installedEntry, counterCellMap.get(installedEntry));

                if (flowEntry == null) {
                    // Entry is on device but unknown to translation service or
                    // device mirror. Inconsistent. Mark for removal.
                    // TODO: make this behaviour configurable
                    // In some cases it's fine for the device to have rules
                    // that were not installed by us.
                    inconsistentEntries.add(installedEntry);
                } else {
                    result.add(flowEntry);
                }
            }
        }

        if (inconsistentEntries.size() > 0) {
            // Async clean up inconsistent entries.
            SharedExecutors.getSingleThreadExecutor().execute(
                    () -> cleanUpInconsistentEntries(inconsistentEntries));
        }

        return result.build();
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        return processFlowRules(rules, APPLY);
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        return processFlowRules(rules, REMOVE);
    }

    private FlowEntry forgeFlowEntry(PiTableEntry entry,
                                     PiCounterCellData cellData) {
        final PiTableEntryHandle handle = PiTableEntryHandle
                .of(deviceId, entry);
        final Optional<PiTranslatedEntity<FlowRule, PiTableEntry>>
                translatedEntity = translator.lookup(handle);
        final TimedEntry<PiTableEntry> timedEntry = tableMirror.get(handle);

        if (!translatedEntity.isPresent()) {
            log.debug("Handle not found in store: {}", handle);
            return null;
        }

        if (timedEntry == null) {
            log.debug("Handle not found in device mirror: {}", handle);
            return null;
        }

        if (cellData != null) {
            return new DefaultFlowEntry(translatedEntity.get().original(),
                                        ADDED, timedEntry.lifeSec(), cellData.bytes(),
                                        cellData.bytes());
        } else {
            return new DefaultFlowEntry(translatedEntity.get().original(),
                                        ADDED, timedEntry.lifeSec(), 0, 0);
        }
    }

    private Collection<FlowEntry> getFlowEntriesFromMirror() {
        return tableMirror.getAll(deviceId).stream()
                .map(timedEntry -> forgeFlowEntry(
                        timedEntry.entry(), null))
                .collect(Collectors.toList());
    }

    private void cleanUpInconsistentEntries(Collection<PiTableEntry> piEntries) {
        log.warn("Found {} entries from {} not on translation store, removing them...",
                 piEntries.size(), deviceId);
        piEntries.forEach(entry -> {
            log.debug(entry.toString());
            applyEntry(PiTableEntryHandle.of(deviceId, entry),
                       entry, null, REMOVE);
        });
    }

    private Collection<FlowRule> processFlowRules(Collection<FlowRule> rules,
                                                  Operation driverOperation) {

        if (!setupBehaviour()) {
            return Collections.emptyList();
        }

        final ImmutableList.Builder<FlowRule> result = ImmutableList.builder();

        // TODO: send writes in bulk (e.g. all entries to insert, modify or delete).
        // Instead of calling the client for each one of them.

        for (FlowRule ruleToApply : rules) {

            final PiTableEntry piEntryToApply;
            try {
                piEntryToApply = translator.translate(ruleToApply, pipeconf);
            } catch (PiTranslationException e) {
                log.warn("Unable to translate flow rule for pipeconf '{}': {} - {}",
                         pipeconf.id(), e.getMessage(), ruleToApply);
                // Next rule.
                continue;
            }

            final PiTableEntryHandle handle = PiTableEntryHandle
                    .of(deviceId, piEntryToApply);

            // Serialize operations over the same match key/table/device ID.
            final Lock lock = ENTRY_LOCKS.computeIfAbsent(handle, k -> new ReentrantLock());
            lock.lock();
            try {
                if (applyEntry(handle, piEntryToApply,
                               ruleToApply, driverOperation)) {
                    result.add(ruleToApply);
                }
            } finally {
                lock.unlock();
            }
        }

        return result.build();
    }

    /**
     * Applies the given entry to the device, and returns true if the operation
     * was successful, false otherwise.
     */
    private boolean applyEntry(PiTableEntryHandle handle,
                               PiTableEntry piEntryToApply,
                               FlowRule ruleToApply,
                               Operation driverOperation) {
        // Depending on the driver operation, and if a matching rule exists on
        // the device, decide which P4 Runtime write operation to perform for
        // this entry.
        final TimedEntry<PiTableEntry> piEntryOnDevice = tableMirror.get(handle);
        final WriteOperationType p4Operation;
        if (driverOperation == APPLY) {
            if (piEntryOnDevice == null) {
                // Entry is first-timer.
                p4Operation = INSERT;
            } else {
                if (checkStoreBeforeUpdate
                        && piEntryToApply.action().equals(piEntryOnDevice.entry().action())) {
                    log.debug("Ignoring re-apply of existing entry: {}", piEntryToApply);
                    p4Operation = null;
                } else if (deleteEntryBeforeUpdate) {
                    // Some devices return error when updating existing
                    // entries. If requested, remove entry before
                    // re-inserting the modified one.
                    applyEntry(handle, piEntryOnDevice.entry(), null, REMOVE);
                    p4Operation = INSERT;
                } else {
                    p4Operation = MODIFY;
                }
            }
        } else {
            p4Operation = DELETE;
        }

        if (p4Operation != null) {
            if (writeEntry(piEntryToApply, p4Operation)) {
                updateStores(handle, piEntryToApply, ruleToApply, p4Operation);
                return true;
            } else {
                return false;
            }
        } else {
            // If no operation, let's pretend we applied the rule to the device.
            return true;
        }
    }

    /**
     * Performs a write operation on the device.
     */
    private boolean writeEntry(PiTableEntry entry,
                               WriteOperationType p4Operation) {
        try {
            if (client.writeTableEntries(
                    newArrayList(entry), p4Operation, pipeconf).get()) {
                return true;
            } else {
                log.warn("Unable to {} table entry in {}: {}",
                         p4Operation.name(), deviceId, entry);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Exception while performing {} table entry operation:",
                     p4Operation, e);
        }
        return false;
    }

    private void updateStores(PiTableEntryHandle handle,
                              PiTableEntry entry,
                              FlowRule rule,
                              WriteOperationType p4Operation) {
        switch (p4Operation) {
            case INSERT:
            case MODIFY:
                tableMirror.put(handle, entry);
                translator.learn(handle, new PiTranslatedEntity<>(rule, entry, handle));
                break;
            case DELETE:
                tableMirror.remove(handle);
                translator.forget(handle);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown operation " + p4Operation.name());
        }
    }

    private Map<PiTableEntry, PiCounterCellData> readEntryCounters(
            PiCounterId counterId, Collection<PiTableEntry> tableEntries) {
        Collection<PiCounterCellData> cellDatas;
        try {
            if (readAllDirectCounters) {
                cellDatas = client.readAllCounterCells(
                        singleton(counterId), pipeconf).get();
            } else {
                Set<PiCounterCellId> cellIds = tableEntries.stream()
                        .map(entry -> PiCounterCellId.ofDirect(counterId, entry))
                        .collect(Collectors.toSet());
                cellDatas = client.readCounterCells(cellIds, pipeconf).get();
            }
            return cellDatas.stream()
                    .collect(Collectors.toMap(c -> c.cellId().tableEntry(), c -> c));
        } catch (InterruptedException | ExecutionException e) {
            if (!(e.getCause() instanceof StatusRuntimeException)) {
                // gRPC errors are logged in the client.
                log.error("Exception while reading counter '{}' from {}: {}",
                          counterId, deviceId, e);
            }
            return Collections.emptyMap();
        }
    }

    enum Operation {
        APPLY, REMOVE
    }
}
