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
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiTableModel;
import org.onosproject.net.pi.runtime.PiFlowRuleTranslationService;
import org.onosproject.net.pi.runtime.PiPipeconfService;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableId;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.p4runtime.api.P4RuntimeFlowRuleWrapper;
import org.onosproject.p4runtime.api.P4RuntimeTableEntryReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.collect.Lists.newArrayList;
import static org.onosproject.drivers.p4runtime.P4RuntimeFlowRuleProgrammable.Operation.APPLY;
import static org.onosproject.drivers.p4runtime.P4RuntimeFlowRuleProgrammable.Operation.REMOVE;
import static org.onosproject.net.flow.FlowEntry.FlowEntryState.ADDED;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.*;

/**
 * Implementation of the flow rule programmable behaviour for BMv2.
 */
public class P4RuntimeFlowRuleProgrammable extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    // TODO: make this attribute configurable by child drivers (e.g. BMv2 or Tofino)
    /*
    When updating an existing rule, if true, we issue a DELETE operation before inserting the new one, otherwise we
    issue a MODIFY operation. This is useful fore devices that do not support MODIFY operations for table entries.
     */
    private boolean deleteEntryBeforeUpdate = true;

    // TODO: can remove this check as soon as the multi-apply-per-same-flow rule bug is fixed.
    /*
    If true, we ignore re-installing rules that are already known in the ENTRY_STORE, i.e. same match key and action.
     */
    private boolean checkEntryStoreBeforeUpdate = true;

    private final Logger log = LoggerFactory.getLogger(getClass());

    // Needed to synchronize operations over the same table entry.
    private static final ConcurrentMap<P4RuntimeTableEntryReference, Lock> ENTRY_LOCKS = Maps.newConcurrentMap();

    // TODO: replace with distributed store.
    // Can reuse old BMv2TableEntryService from ONOS 1.6
    private static final ConcurrentMap<P4RuntimeTableEntryReference, P4RuntimeFlowRuleWrapper> ENTRY_STORE =
            Maps.newConcurrentMap();

    private DeviceId deviceId;
    private P4RuntimeClient client;
    private PiPipeconf pipeconf;
    private PiPipelineModel pipelineModel;
    private PiPipelineInterpreter interpreter;
    private PiFlowRuleTranslationService piFlowRuleTranslationService;

    private boolean init() {

        deviceId = handler().data().deviceId();

        P4RuntimeController controller = handler().get(P4RuntimeController.class);
        if (!controller.hasClient(deviceId)) {
            log.warn("Unable to find client for {}, aborting flow rule operation", deviceId);
            return false;
        }

        PiPipeconfService piPipeconfService = handler().get(PiPipeconfService.class);
        if (!piPipeconfService.ofDevice(deviceId).isPresent() ||
                !piPipeconfService.getPipeconf(piPipeconfService.ofDevice(deviceId).get()).isPresent()) {
            log.warn("Unable to get the pipeconf of {}", deviceId);
            return false;
        }

        DeviceService deviceService = handler().get(DeviceService.class);
        Device device = deviceService.getDevice(deviceId);
        if (!device.is(PiPipelineInterpreter.class)) {
            log.warn("Unable to get interpreter of {}", deviceId);
            return false;
        }

        client = controller.getClient(deviceId);
        pipeconf = piPipeconfService.getPipeconf(piPipeconfService.ofDevice(deviceId).get()).get();
        pipelineModel = pipeconf.pipelineModel();
        interpreter = device.as(PiPipelineInterpreter.class);
        piFlowRuleTranslationService = handler().get(PiFlowRuleTranslationService.class);

        return true;
    }

    @Override
    public Collection<FlowEntry> getFlowEntries() {

        if (!init()) {
            return Collections.emptyList();
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
                installedEntries = client.dumpTable(piTableId, pipeconf).get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Exception while dumping table {} of {}", piTableId, deviceId, e);
                return Collections.emptyList();
            }

            for (PiTableEntry installedEntry : installedEntries) {

                P4RuntimeTableEntryReference entryRef = new P4RuntimeTableEntryReference(deviceId, piTableId,
                                                                                         installedEntry.matchKey());

                P4RuntimeFlowRuleWrapper frWrapper = ENTRY_STORE.get(entryRef);


                if (frWrapper == null) {
                    // Inconsistent entry
                    inconsistentEntries.add(installedEntry);
                    continue; // next one.
                }

                // TODO: implement table entry counter retrieval.
                long bytes = 0L;
                long packets = 0L;

                FlowEntry entry = new DefaultFlowEntry(frWrapper.rule(), ADDED, frWrapper.lifeInSeconds(),
                                                       packets, bytes);
                resultBuilder.add(entry);
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

        if (!init()) {
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