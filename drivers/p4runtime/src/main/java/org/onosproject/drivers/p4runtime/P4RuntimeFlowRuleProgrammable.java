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
import com.google.common.util.concurrent.Striped;
import org.onlab.util.SharedExecutors;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeTableMirror;
import org.onosproject.drivers.p4runtime.mirror.TimedEntry;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.model.PiTableModel;
import org.onosproject.net.pi.runtime.PiCounterCell;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
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
    private static final String DELETE_BEFORE_UPDATE = "tableDeleteBeforeUpdate";
    private static final boolean DEFAULT_DELETE_BEFORE_UPDATE = false;

    // If true, we ignore re-installing rules that already exist in the
    // device mirror, i.e. same match key and action.
    private static final String IGNORE_SAME_ENTRY_UPDATE = "tableIgnoreSameEntryUpdate";
    private static final boolean DEFAULT_IGNORE_SAME_ENTRY_UPDATE = false;

    // If true, we avoid querying the device and return what's already known by
    // the ONOS store.
    private static final String READ_FROM_MIRROR = "tableReadFromMirror";
    private static final boolean DEFAULT_READ_FROM_MIRROR = false;

    // If true, we read counters when reading table entries (if table has
    // counters). Otherwise, we don't.
    private static final String SUPPORT_TABLE_COUNTERS = "supportTableCounters";
    private static final boolean DEFAULT_SUPPORT_TABLE_COUNTERS = true;

    // If true, assumes that the device returns table entry message populated
    // with direct counter values. If false, we issue a second P4Runtime request
    // to read the direct counter values.
    private static final String READ_COUNTERS_WITH_TABLE_ENTRIES = "tableReadCountersWithTableEntries";
    private static final boolean DEFAULT_READ_COUNTERS_WITH_TABLE_ENTRIES = true;

    // For default entries, P4Runtime mandates that only MODIFY messages are
    // allowed. If true, treats default entries as normal table entries,
    // e.g. inserting them first.
    private static final String TABLE_DEFAULT_AS_ENTRY = "tableDefaultAsEntry";
    private static final boolean DEFAULT_TABLE_DEFAULT_AS_ENTRY = false;

    // Needed to synchronize operations over the same table entry.
    private static final Striped<Lock> ENTRY_LOCKS = Striped.lock(30);

    private PiPipelineModel pipelineModel;
    private P4RuntimeTableMirror tableMirror;
    private PiFlowRuleTranslator translator;

    @Override
    protected boolean setupBehaviour() {

        if (!super.setupBehaviour()) {
            return false;
        }

        pipelineModel = pipeconf.pipelineModel();
        tableMirror = handler().get(P4RuntimeTableMirror.class);
        translator = translationService.flowRuleTranslator();
        return true;
    }

    @Override
    public Collection<FlowEntry> getFlowEntries() {

        if (!setupBehaviour()) {
            return Collections.emptyList();
        }

        if (driverBoolProperty(READ_FROM_MIRROR, DEFAULT_READ_FROM_MIRROR)) {
            return getFlowEntriesFromMirror();
        }

        final ImmutableList.Builder<FlowEntry> result = ImmutableList.builder();
        final List<PiTableEntry> inconsistentEntries = Lists.newArrayList();

        // Read table entries, including default ones.
        final Collection<PiTableEntry> deviceEntries = Stream.concat(
                streamEntries(), streamDefaultEntries())
                // Ignore entries from constant tables.
                .filter(e -> !tableIsConstant(e.table()))
                // Device implementation might return duplicate entries. For
                // example if reading only default ones is not supported and
                // non-default entries are returned, by using distinct() we are
                // robust against that possibility.
                .distinct()
                .collect(Collectors.toList());

        if (deviceEntries.isEmpty()) {
            return Collections.emptyList();
        }

        // Synchronize mirror with the device state.
        syncMirror(deviceEntries);
        final Map<PiTableEntry, PiCounterCellData> counterCellMap =
                readEntryCounters(deviceEntries);
        // Forge flow entries with counter values.
        for (PiTableEntry entry : deviceEntries) {
            final FlowEntry flowEntry = forgeFlowEntry(
                    entry, counterCellMap.get(entry));
            if (flowEntry == null) {
                // Entry is on device but unknown to translation service or
                // device mirror. Inconsistent. Mark for removal.
                // TODO: make this behaviour configurable
                // In some cases it's fine for the device to have rules
                // that were not installed by us, e.g. original default entry.
                if (!isOriginalDefaultEntry(entry)) {
                    inconsistentEntries.add(entry);
                }
            } else {
                result.add(flowEntry);
            }
        }

        if (inconsistentEntries.size() > 0) {
            // Trigger clean up of inconsistent entries.
            SharedExecutors.getSingleThreadExecutor().execute(
                    () -> cleanUpInconsistentEntries(inconsistentEntries));
        }

        return result.build();
    }

    private Stream<PiTableEntry> streamEntries() {
        return getFutureWithDeadline(
                client.dumpAllTables(pipeconf), "dumping all tables",
                Collections.emptyList())
                .stream();
    }

    private Stream<PiTableEntry> streamDefaultEntries() {
        // Ignore tables with constant default action.
        final Set<PiTableId> defaultTables = pipelineModel.tables()
                .stream()
                .filter(table -> !table.constDefaultAction().isPresent())
                .map(PiTableModel::id)
                .collect(Collectors.toSet());
        return defaultTables.isEmpty() ? Stream.empty()
                : getFutureWithDeadline(
                client.dumpTables(defaultTables, true, pipeconf),
                "dumping default table entries",
                Collections.emptyList())
                .stream();
    }

    private void syncMirror(Collection<PiTableEntry> entries) {
        Map<PiTableEntryHandle, PiTableEntry> handleMap = Maps.newHashMap();
        entries.forEach(e -> handleMap.put(PiTableEntryHandle.of(deviceId, e), e));
        tableMirror.sync(deviceId, handleMap);
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
            log.warn("Table entry handle not found in translation store: {}", handle);
            return null;
        }
        if (!translatedEntity.get().translated().equals(entry)) {
            log.warn("Table entry obtained from device {} is different from " +
                             "one in in translation store: device={}, store={}",
                     deviceId, entry, translatedEntity.get().translated());
            return null;
        }
        if (timedEntry == null) {
            log.warn("Table entry handle not found in device mirror: {}", handle);
            return null;
        }

        if (cellData != null) {
            return new DefaultFlowEntry(translatedEntity.get().original(),
                                        ADDED, timedEntry.lifeSec(), cellData.packets(),
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
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void cleanUpInconsistentEntries(Collection<PiTableEntry> piEntries) {
        log.warn("Found {} inconsistent table entries on {}, removing them...",
                 piEntries.size(), deviceId);
        piEntries.forEach(entry -> {
            log.debug(entry.toString());
            final PiTableEntryHandle handle = PiTableEntryHandle.of(deviceId, entry);
            ENTRY_LOCKS.get(handle).lock();
            try {
                applyEntry(handle, entry, null, REMOVE);
            } finally {
                ENTRY_LOCKS.get(handle).unlock();
            }
        });
    }

    private Collection<FlowRule> processFlowRules(Collection<FlowRule> rules,
                                                  Operation driverOperation) {

        if (!setupBehaviour() || rules.isEmpty()) {
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
            ENTRY_LOCKS.get(handle).lock();
            try {
                if (applyEntry(handle, piEntryToApply,
                               ruleToApply, driverOperation)) {
                    result.add(ruleToApply);
                }
            } finally {
                ENTRY_LOCKS.get(handle).unlock();
            }
        }

        return result.build();
    }

    /**
     * Applies the given entry to the device, and returns true if the operation
     * was successful, false otherwise.
     */
    private boolean applyEntry(final PiTableEntryHandle handle,
                               PiTableEntry piEntryToApply,
                               final FlowRule ruleToApply,
                               final Operation driverOperation) {
        // Depending on the driver operation, and if a matching rule exists on
        // the device, decide which P4 Runtime write operation to perform for
        // this entry.
        final TimedEntry<PiTableEntry> piEntryOnDevice = tableMirror.get(handle);
        final WriteOperationType p4Operation;
        final WriteOperationType storeOperation;

        final boolean defaultAsEntry = driverBoolProperty(
                TABLE_DEFAULT_AS_ENTRY, DEFAULT_TABLE_DEFAULT_AS_ENTRY);
        final boolean ignoreSameEntryUpdate = driverBoolProperty(
                IGNORE_SAME_ENTRY_UPDATE, DEFAULT_IGNORE_SAME_ENTRY_UPDATE);
        final boolean deleteBeforeUpdate = driverBoolProperty(
                DELETE_BEFORE_UPDATE, DEFAULT_DELETE_BEFORE_UPDATE);
        if (driverOperation == APPLY) {
            if (piEntryOnDevice == null) {
                // Entry is first-timer, INSERT or MODIFY if default action.
                p4Operation = !piEntryToApply.isDefaultAction() || defaultAsEntry
                        ? INSERT : MODIFY;
                storeOperation = p4Operation;
            } else {
                if (ignoreSameEntryUpdate &&
                        piEntryToApply.action().equals(piEntryOnDevice.entry().action())) {
                    log.debug("Ignoring re-apply of existing entry: {}", piEntryToApply);
                    p4Operation = null;
                } else if (deleteBeforeUpdate && !piEntryToApply.isDefaultAction()) {
                    // Some devices return error when updating existing
                    // entries. If requested, remove entry before
                    // re-inserting the modified one, except the default action
                    // entry, that cannot be removed.
                    applyEntry(handle, piEntryOnDevice.entry(), null, REMOVE);
                    p4Operation = INSERT;
                } else {
                    p4Operation = MODIFY;
                }
                storeOperation = p4Operation;
            }
        } else {
            if (piEntryToApply.isDefaultAction()) {
                // Cannot remove default action. Instead we should use the
                // original defined by the interpreter (if any).
                piEntryToApply = getOriginalDefaultEntry(piEntryToApply.table());
                if (piEntryToApply == null) {
                    return false;
                }
                p4Operation = MODIFY;
            } else {
                p4Operation = DELETE;
            }
            // Still want to delete the default entry from the mirror and
            // translation store.
            storeOperation = DELETE;
        }

        if (p4Operation != null) {
            if (writeEntry(piEntryToApply, p4Operation)) {
                updateStores(handle, piEntryToApply, ruleToApply, storeOperation);
                return true;
            } else {
                return false;
            }
        } else {
            // If no operation, let's pretend we applied the rule to the device.
            return true;
        }
    }

    private PiTableEntry getOriginalDefaultEntry(PiTableId tableId) {
        final PiPipelineInterpreter interpreter = getInterpreter();
        if (interpreter == null) {
            log.warn("Missing interpreter for {}, cannot get default action",
                     deviceId);
            return null;
        }
        if (!interpreter.getOriginalDefaultAction(tableId).isPresent()) {
            log.warn("Interpreter of {} doesn't define a default action for " +
                             "table {}, cannot produce default action entry",
                     deviceId, tableId);
            return null;
        }
        return PiTableEntry.builder()
                .forTable(tableId)
                .withAction(interpreter.getOriginalDefaultAction(tableId).get())
                .build();
    }

    private boolean isOriginalDefaultEntry(PiTableEntry entry) {
        if (!entry.isDefaultAction()) {
            return false;
        }
        final PiTableEntry originalDefaultEntry = getOriginalDefaultEntry(entry.table());
        return originalDefaultEntry != null &&
                originalDefaultEntry.action().equals(entry.action());
    }

    /**
     * Performs a write operation on the device.
     */
    private boolean writeEntry(PiTableEntry entry,
                               WriteOperationType p4Operation) {
        final CompletableFuture<Boolean> future = client.writeTableEntries(
                newArrayList(entry), p4Operation, pipeconf);
        // If false, errors logged by internal calls.
        return getFutureWithDeadline(
                future, "performing table " + p4Operation.name(), false);
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
            Collection<PiTableEntry> tableEntries) {
        if (!driverBoolProperty(SUPPORT_TABLE_COUNTERS,
                                DEFAULT_SUPPORT_TABLE_COUNTERS)
                || tableEntries.isEmpty()) {
            return Collections.emptyMap();
        }

        if (driverBoolProperty(READ_COUNTERS_WITH_TABLE_ENTRIES,
                               DEFAULT_READ_COUNTERS_WITH_TABLE_ENTRIES)) {
            return tableEntries.stream().collect(Collectors.toMap(c -> c, PiTableEntry::counter));
        } else {
            Collection<PiCounterCell> cells;
            Set<PiCounterCellId> cellIds = tableEntries.stream()
                    // Ignore counter for default entry.
                    .filter(e -> !e.isDefaultAction())
                    .filter(e -> tableHasCounter(e.table()))
                    .map(PiCounterCellId::ofDirect)
                    .collect(Collectors.toSet());
            cells = getFutureWithDeadline(client.readCounterCells(cellIds, pipeconf),
                                              "reading table counters", Collections.emptyList());
            return cells.stream()
                    .collect(Collectors.toMap(c -> c.cellId().tableEntry(), PiCounterCell::data));
        }
    }

    private boolean tableHasCounter(PiTableId tableId) {
        return pipelineModel.table(tableId).isPresent() &&
                !pipelineModel.table(tableId).get().counters().isEmpty();
    }

    private boolean tableIsConstant(PiTableId tableId) {
        return pipelineModel.table(tableId).isPresent() &&
                pipelineModel.table(tableId).get().isConstantTable();
    }

    enum Operation {
        APPLY, REMOVE
    }
}
