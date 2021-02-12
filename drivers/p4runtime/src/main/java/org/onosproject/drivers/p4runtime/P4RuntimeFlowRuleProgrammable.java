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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.Striped;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeDefaultEntryMirror;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeTableMirror;
import org.onosproject.drivers.p4runtime.mirror.TimedEntry;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.pi.model.PiCounterType;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiCounterCell;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellHandle;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiEntityType;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.net.pi.runtime.PiMatchKey;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableEntryHandle;
import org.onosproject.net.pi.service.PiFlowRuleTranslator;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.net.pi.service.PiTranslationException;
import org.onosproject.p4runtime.api.P4RuntimeReadClient;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient.UpdateType;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient.WriteRequest;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient.WriteResponse;

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

import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.DEFAULT_DELETE_BEFORE_UPDATE;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.DEFAULT_READ_COUNTERS_WITH_TABLE_ENTRIES;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.DEFAULT_READ_FROM_MIRROR;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.DEFAULT_SUPPORT_DEFAULT_TABLE_ENTRY;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.DEFAULT_SUPPORT_TABLE_COUNTERS;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.DEFAULT_TABLE_WILCARD_READS;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.DELETE_BEFORE_UPDATE;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.READ_COUNTERS_WITH_TABLE_ENTRIES;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.READ_FROM_MIRROR;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.SUPPORT_DEFAULT_TABLE_ENTRY;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.SUPPORT_TABLE_COUNTERS;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.TABLE_WILCARD_READS;
import static org.onosproject.drivers.p4runtime.P4RuntimeFlowRuleProgrammable.Operation.APPLY;
import static org.onosproject.drivers.p4runtime.P4RuntimeFlowRuleProgrammable.Operation.REMOVE;
import static org.onosproject.net.flow.FlowEntry.FlowEntryState.ADDED;
import static org.onosproject.p4runtime.api.P4RuntimeWriteClient.UpdateType.DELETE;
import static org.onosproject.p4runtime.api.P4RuntimeWriteClient.UpdateType.INSERT;
import static org.onosproject.p4runtime.api.P4RuntimeWriteClient.UpdateType.MODIFY;

/**
 * Implementation of the flow rule programmable behaviour for P4Runtime.
 */
public class P4RuntimeFlowRuleProgrammable
        extends AbstractP4RuntimeHandlerBehaviour
        implements FlowRuleProgrammable {

    // Used to make sure concurrent calls to write flow rules are serialized so
    // that each request gets consistent access to mirror state.
    private static final Striped<Lock> WRITE_LOCKS = Striped.lock(30);

    private PiPipelineModel pipelineModel;
    private P4RuntimeTableMirror tableMirror;
    private PiFlowRuleTranslator translator;
    private P4RuntimeDefaultEntryMirror defaultEntryMirror;

    @Override
    protected boolean setupBehaviour(String opName) {

        if (!super.setupBehaviour(opName)) {
            return false;
        }

        pipelineModel = pipeconf.pipelineModel();
        tableMirror = handler().get(P4RuntimeTableMirror.class);
        translator = translationService.flowRuleTranslator();
        defaultEntryMirror = handler().get(P4RuntimeDefaultEntryMirror.class);
        return true;
    }

    @Override
    public Collection<FlowEntry> getFlowEntries() {

        if (!setupBehaviour("getFlowEntries()")) {
            return Collections.emptyList();
        }

        if (driverBoolProperty(READ_FROM_MIRROR,
                               DEFAULT_READ_FROM_MIRROR)) {
            return getFlowEntriesFromMirror();
        }

        final ImmutableList.Builder<FlowEntry> result = ImmutableList.builder();
        final List<PiTableEntry> inconsistentEntries = Lists.newArrayList();

        // Read table entries from device.
        final Collection<PiTableEntry> deviceEntries = getAllTableEntriesFromDevice();
        if (deviceEntries == null) {
            // Potential error at the client level.
            return Collections.emptyList();
        }

        // Synchronize mirror with the device state.
        tableMirror.sync(deviceId, deviceEntries);

        if (deviceEntries.isEmpty()) {
            // Nothing to do.
            return Collections.emptyList();
        }

        final Map<PiTableEntryHandle, PiCounterCellData> counterCellMap =
                readEntryCounters(deviceEntries);
        // Forge flow entries with counter values.
        for (PiTableEntry entry : deviceEntries) {
            final PiTableEntryHandle handle = entry.handle(deviceId);
            final FlowEntry flowEntry = forgeFlowEntry(
                    entry, handle, counterCellMap.get(handle));
            if (flowEntry == null) {
                // Entry is on device but unknown to translation service or
                // device mirror. Inconsistent. Mark for removal if this is not
                // an original default entry (i.e, the same defined in the P4
                // program via default_action, which cannot be removed.)
                if (!isOriginalDefaultEntry(entry)) {
                    inconsistentEntries.add(entry);
                }
            } else {
                result.add(flowEntry);
            }
        }

        // Default entries need to be treated in a different way according to the spec:
        // the client can modify (reset or update) them but cannot remove the entries
        List<PiTableEntry> inconsistentDefaultEntries = Lists.newArrayList();
        List<PiTableEntry> tempDefaultEntries = inconsistentEntries.stream()
                .filter(PiTableEntry::isDefaultAction)
                .collect(Collectors.toList());
        inconsistentEntries.removeAll(tempDefaultEntries);
        // Once we have removed the default entry from inconsistentEntries we need to
        // craft for each default entry a copy without the action field. According to
        // the spec leaving the action field unset will reset the original default entry.
        tempDefaultEntries.forEach(piTableEntry -> {
            PiTableEntry resetEntry = PiTableEntry.builder()
                    .forTable(piTableEntry.table()).build();
            inconsistentDefaultEntries.add(resetEntry);
        });

        // Clean up of inconsistent entries.
        if (!inconsistentEntries.isEmpty() || !inconsistentDefaultEntries.isEmpty()) {
            WriteRequest writeRequest = client.write(p4DeviceId, pipeconf);
            // Trigger remove of inconsistent entries.
            if (!inconsistentEntries.isEmpty()) {
                log.warn("Found {} inconsistent table entries on {}, removing them...",
                        inconsistentEntries.size(), deviceId);
                writeRequest = writeRequest.entities(inconsistentEntries, DELETE);
            }

            // Trigger reset of inconsistent default entries.
            if (!inconsistentDefaultEntries.isEmpty()) {
                log.warn("Found {} inconsistent default table entries on {}, resetting them...",
                        inconsistentDefaultEntries.size(), deviceId);
                writeRequest = writeRequest.entities(inconsistentDefaultEntries, MODIFY);
            }

            // Submit delete request for non-default entries and modify request
            // for default entries. Updates mirror when done.
            writeRequest.submit().whenComplete((response, ex) -> {
                if (ex != null) {
                    log.error("Exception removing inconsistent table entries", ex);
                } else {
                    log.debug("Successfully removed {} out of {} inconsistent entries",
                            response.success().size(), response.all().size());
                }
                // We can use the entity as the handle does not contain the action field
                // so the key will be removed even if the table entry is different
                response.success().forEach(entity -> tableMirror.remove((PiTableEntryHandle) entity.handle()));
            });
        }

        return result.build();
    }

    private Collection<PiTableEntry> getAllTableEntriesFromDevice() {
        final P4RuntimeReadClient.ReadRequest request = client.read(
                p4DeviceId, pipeconf);
        final boolean supportDefaultTableEntry = driverBoolProperty(
                SUPPORT_DEFAULT_TABLE_ENTRY, DEFAULT_SUPPORT_DEFAULT_TABLE_ENTRY);
        final boolean tableWildcardReads = driverBoolProperty(
                TABLE_WILCARD_READS, DEFAULT_TABLE_WILCARD_READS);
        if (!tableWildcardReads) {
            // Read entries from all non-constant tables, including default ones.
            pipelineModel.tables().stream()
                    .filter(t -> !t.isConstantTable())
                    .forEach(t -> {
                        request.tableEntries(t.id());
                        if (supportDefaultTableEntry && t.constDefaultAction().isEmpty()) {
                            request.defaultTableEntry(t.id());
                        }
                    });
        } else {
            request.allTableEntries();
            if (supportDefaultTableEntry) {
                request.allDefaultTableEntries();
            }
        }
        final P4RuntimeReadClient.ReadResponse response = request.submitSync();
        if (!response.isSuccess()) {
            return null;
        }
        Stream<PiTableEntry> piTableEntries = response.all(PiTableEntry.class).stream()
                // Device implementation might return duplicate entries. For
                // example if reading only default ones is not supported and
                // non-default entries are returned, by using distinct() we
                // are robust against that possibility.
                .distinct();
        if (tableWildcardReads) {
            // When doing a wildcard read on all tables, the device might
            // return table entries of tables not present in the pipeline
            // model or constant (default) entries that are filtered out.
            piTableEntries = piTableEntries.filter(te -> {
                var piTableModel = pipelineModel.table(te.table());
                    if (piTableModel.isEmpty() ||
                            piTableModel.get().isConstantTable() ||
                            (supportDefaultTableEntry && piTableModel.get().constDefaultAction().isPresent())) {
                        return false;
                    }
                return true;
            });
        }
        return piTableEntries.collect(Collectors.toList());
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
                                     PiTableEntryHandle handle,
                                     PiCounterCellData cellData) {
        final Optional<PiTranslatedEntity<FlowRule, PiTableEntry>>
                translatedEntity = translator.lookup(handle);
        final TimedEntry<PiTableEntry> timedEntry = tableMirror.get(handle);

        // A default entry might not be present in the translation store if it
        // was not inserted by an app. No need to log.
        if (translatedEntity.isEmpty()) {
            if (!isOriginalDefaultEntry(entry)) {
                log.warn("Table entry handle not found in translation store: {}", handle);
            }
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
                        timedEntry.entry(), timedEntry.entry().handle(deviceId), null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Collection<FlowRule> processFlowRules(Collection<FlowRule> rules,
                                                  Operation driverOperation) {
        if (!setupBehaviour("processFlowRules()") || rules.isEmpty()) {
            return Collections.emptyList();
        }
        // Created batched write request.
        final WriteRequest request = client.write(p4DeviceId, pipeconf);
        // For each rule, translate to PI and append to write request.
        final Map<PiHandle, FlowRule> handleToRuleMap = Maps.newHashMap();
        final List<FlowRule> skippedRules = Lists.newArrayList();
        final CompletableFuture<WriteResponse> futureResponse;
        WRITE_LOCKS.get(deviceId).lock();
        try {
            for (FlowRule rule : rules) {
                // Translate.
                final PiTableEntry entry;
                try {
                    entry = translator.translate(rule, pipeconf);
                } catch (PiTranslationException e) {
                    log.warn("Unable to translate flow rule for pipeconf '{}': {} [{}]",
                             pipeconf.id(), e.getMessage(), rule);
                    // Next rule.
                    continue;
                }
                final PiTableEntryHandle handle = entry.handle(deviceId);
                handleToRuleMap.put(handle, rule);
                // Update translation store.
                if (driverOperation.equals(APPLY)) {
                    translator.learn(handle, new PiTranslatedEntity<>(
                            rule, entry, handle));
                } else {
                    translator.forget(handle);
                }
                // Append entry to batched write request (returns false), or skip (true)
                if (appendEntryToWriteRequestOrSkip(
                        request, handle, entry, driverOperation)) {
                    skippedRules.add(rule);
                }
            }
            if (request.pendingUpdates().isEmpty()) {
                // All good. No need to write on device.
                return rules;
            }
            // Update mirror.
            tableMirror.applyWriteRequest(request);
            // Async submit request to server.
            futureResponse = request.submit();
        } finally {
            WRITE_LOCKS.get(deviceId).unlock();
        }
        // Wait for response.
        final WriteResponse response = Futures.getUnchecked(futureResponse);
        // Derive successfully applied flow rule from response.
        final List<FlowRule> appliedRules = getAppliedFlowRules(
                response, handleToRuleMap, driverOperation);
        // Return skipped and applied rules.
        return ImmutableList.<FlowRule>builder()
                .addAll(skippedRules).addAll(appliedRules).build();
    }

    private List<FlowRule> getAppliedFlowRules(
            WriteResponse response,
            Map<PiHandle, FlowRule> handleToFlowRuleMap,
            Operation driverOperation) {
        // Returns a list of flow rules that were successfully written on the
        // server according to the given write response and operation.
        return response.success().stream()
                .filter(r -> r.entityType().equals(PiEntityType.TABLE_ENTRY))
                .filter(r -> {
                    // Filter intermediate responses (e.g. P4Runtime DELETE
                    // during FlowRule APPLY because we are performing
                    // delete-before-update)
                    return isUpdateTypeRelevant(r.updateType(), driverOperation);
                })
                .map(r -> {
                    final FlowRule rule = handleToFlowRuleMap.get(r.handle());
                    if (rule == null) {
                        log.warn("Server returned unrecognized table entry " +
                                         "handle in write response: {}", r.handle());
                    }
                    return rule;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isUpdateTypeRelevant(UpdateType p4UpdateType, Operation driverOperation) {
        switch (p4UpdateType) {
            case INSERT:
            case MODIFY:
                if (!driverOperation.equals(APPLY)) {
                    return false;
                }
                break;
            case DELETE:
                if (!driverOperation.equals(REMOVE)) {
                    return false;
                }
                break;
            default:
                log.error("Unknown update type {}", p4UpdateType);
                return false;
        }
        return true;
    }

    private boolean appendEntryToWriteRequestOrSkip(
            final WriteRequest writeRequest,
            final PiTableEntryHandle handle,
            PiTableEntry piEntryToApply,
            final Operation driverOperation) {
        // Depending on the driver operation, and if a matching rule exists on
        // the device/mirror, decide which P4Runtime update operation to perform
        // for this entry. In some cases, the entry is skipped from the write
        // request but we want to return the corresponding flow rule as
        // successfully written. In this case, we return true.
        final TimedEntry<PiTableEntry> piEntryOnDevice = tableMirror.get(handle);
        final UpdateType updateType;

        final boolean supportDefaultEntry = driverBoolProperty(
                SUPPORT_DEFAULT_TABLE_ENTRY,
                DEFAULT_SUPPORT_DEFAULT_TABLE_ENTRY);
        final boolean deleteBeforeUpdate = driverBoolProperty(
                DELETE_BEFORE_UPDATE,
                DEFAULT_DELETE_BEFORE_UPDATE);

        if (driverOperation == APPLY) {
            if (piEntryOnDevice == null) {
                // Entry is first-timer, INSERT or MODIFY if default action.
                updateType = !piEntryToApply.isDefaultAction() || !supportDefaultEntry
                        ? INSERT : MODIFY;
            } else {
                if (piEntryToApply.action().equals(piEntryOnDevice.entry().action())) {
                    // FIXME: should we check for other attributes of the table
                    //  entry? For example can we modify the priority?
                    log.debug("Ignoring re-apply of existing entry: {}", piEntryToApply);
                    return true;
                } else if (deleteBeforeUpdate && !piEntryToApply.isDefaultAction()) {
                    // Some devices return error when updating existing entries.
                    // If requested, remove entry before re-inserting the
                    // modified one, except the default action entry, that
                    // cannot be removed.
                    writeRequest.delete(handle);
                    updateType = INSERT;
                } else {
                    updateType = MODIFY;
                }
            }
        } else {
            // REMOVE.
            if (piEntryToApply.isDefaultAction()) {
                // Cannot remove default action. Instead we should modify it to
                // use the original one as specified in the P4 program.
                final PiTableEntry originalDefaultEntry = getOriginalDefaultEntry(
                        piEntryToApply.table());
                if (originalDefaultEntry == null) {
                    return false;
                }
                return appendEntryToWriteRequestOrSkip(
                        writeRequest, originalDefaultEntry.handle(deviceId),
                        originalDefaultEntry, APPLY);
            } else {
                if (piEntryOnDevice == null) {
                    log.debug("Ignoring delete of missing entry: {}",
                              piEntryToApply);
                    return true;
                }
                updateType = DELETE;
            }
        }
        writeRequest.entity(piEntryToApply, updateType);
        return false;
    }

    private PiTableEntry getOriginalDefaultEntry(PiTableId tableId) {
        final PiTableEntryHandle handle = PiTableEntry.builder()
                .forTable(tableId)
                .withMatchKey(PiMatchKey.EMPTY)
                .build()
                .handle(deviceId);
        final TimedEntry<PiTableEntry> originalDefaultEntry = defaultEntryMirror.get(handle);
        if (originalDefaultEntry != null) {
            return originalDefaultEntry.entry();
        }
        return null;
    }

    private boolean isOriginalDefaultEntry(PiTableEntry entry) {
        if (!entry.isDefaultAction()) {
            return false;
        }
        final PiTableEntry originalDefaultEntry = getOriginalDefaultEntry(entry.table());
        if (originalDefaultEntry == null) {
            return false;
        }
        // Sometimes the default action may be null
        // e.g. In basic pipeline, the default action in wcmp_table is null
        if (originalDefaultEntry.action() == null) {
            return entry.action() == null;
        }
        return originalDefaultEntry.action().equals(entry.action());
    }

    private Map<PiTableEntryHandle, PiCounterCellData> readEntryCounters(
            Collection<PiTableEntry> tableEntries) {

        if (!driverBoolProperty(SUPPORT_TABLE_COUNTERS,
                                DEFAULT_SUPPORT_TABLE_COUNTERS)
                || tableEntries.isEmpty()) {
            return Collections.emptyMap();
        }

        if (driverBoolProperty(READ_COUNTERS_WITH_TABLE_ENTRIES,
                               DEFAULT_READ_COUNTERS_WITH_TABLE_ENTRIES)) {
            return tableEntries.stream()
                    .filter(t -> t.counter() != null)
                    .collect(Collectors.toMap(
                            t -> t.handle(deviceId), PiTableEntry::counter));
        } else {
            final Set<PiHandle> cellHandles = tableEntries.stream()
                    .filter(e -> !e.isDefaultAction())
                    .filter(e -> tableHasCounter(e.table()))
                    .map(PiCounterCellId::ofDirect)
                    .map(id -> PiCounterCellHandle.of(deviceId, id))
                    .collect(Collectors.toSet());
            // FIXME: We might be sending a very large read request...
            return client.read(p4DeviceId, pipeconf)
                    .handles(cellHandles)
                    .submitSync()
                    .all(PiCounterCell.class).stream()
                    .filter(c -> c.cellId().counterType().equals(PiCounterType.DIRECT))
                    .collect(Collectors.toMap(
                            c -> c.cellId().tableEntry().handle(deviceId),
                            PiCounterCell::data));
        }
    }

    private boolean tableHasCounter(PiTableId tableId) {
        return pipelineModel.table(tableId).isPresent() &&
                !pipelineModel.table(tableId).get().counters().isEmpty();
    }

    enum Operation {
        APPLY, REMOVE
    }
}
