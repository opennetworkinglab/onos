/*
 * Copyright 2018-present Open Networking Foundation
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
import com.google.common.util.concurrent.Striped;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimePreEntryMirror;
import org.onosproject.drivers.p4runtime.mirror.TimedEntry;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProgrammable;
import org.onosproject.net.group.GroupStore;
import org.onosproject.net.pi.runtime.PiPreEntry;
import org.onosproject.net.pi.runtime.PiPreEntryHandle;
import org.onosproject.net.pi.service.PiReplicationGroupTranslator;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.net.pi.service.PiTranslationException;
import org.onosproject.p4runtime.api.P4RuntimeClient;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static org.onosproject.p4runtime.api.P4RuntimeWriteClient.UpdateType.DELETE;
import static org.onosproject.p4runtime.api.P4RuntimeWriteClient.UpdateType.INSERT;
import static org.onosproject.p4runtime.api.P4RuntimeWriteClient.UpdateType.MODIFY;

/**
 * Implementation of GroupProgrammable to handle PRE entries in P4Runtime.
 */
public class P4RuntimeReplicationGroupProgrammable
        extends AbstractP4RuntimeHandlerBehaviour implements GroupProgrammable {

    // TODO: implement reading groups from device and mirror sync.

    // Needed to synchronize operations over the same group.
    private static final Striped<Lock> STRIPED_LOCKS = Striped.lock(30);

    private GroupStore groupStore;
    private P4RuntimePreEntryMirror mirror;
    private PiReplicationGroupTranslator translator;

    @Override
    protected boolean setupBehaviour(String opName) {
        if (!super.setupBehaviour(opName)) {
            return false;
        }
        mirror = this.handler().get(P4RuntimePreEntryMirror.class);
        groupStore = handler().get(GroupStore.class);
        translator = translationService.replicationGroupTranslator();
        return true;
    }

    @Override
    public void performGroupOperation(DeviceId deviceId, GroupOperations groupOps) {
        if (!setupBehaviour("performGroupOperation()")) {
            return;
        }
        groupOps.operations().forEach(op -> {
            final Group group = groupStore.getGroup(deviceId, op.groupId());
            if (group == null) {
                log.warn("Unable to find group {} in store, aborting {} operation [{}]",
                         op.groupId(), op.opType(), op);
                return;
            }
            processGroupOp(group, op.opType());
        });
    }

    @Override
    public Collection<Group> getGroups() {
        if (!setupBehaviour("getGroups()")) {
            return Collections.emptyList();
        }
        // TODO: missing support for reading multicast groups in PI/Stratum.
        return ImmutableList.copyOf(getGroupsFromMirror());
    }

    private Collection<Group> getGroupsFromMirror() {
        return mirror.getAll(deviceId).stream()
                .map(TimedEntry::entry)
                .map(this::forgeGroupEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void processGroupOp(Group pdGroup, GroupOperation.Type opType) {
        final PiPreEntry preEntry;
        try {
            preEntry = translator.translate(pdGroup, pipeconf);
        } catch (PiTranslationException e) {
            log.warn("Unable to translate replication group, aborting {} operation: {} [{}]",
                     opType, e.getMessage(), pdGroup);
            return;
        }
        final PiPreEntryHandle handle = (PiPreEntryHandle) preEntry.handle(deviceId);
        final PiPreEntry entryOnDevice = mirror.get(handle) == null
                ? null : mirror.get(handle).entry();
        final Lock lock = STRIPED_LOCKS.get(handle);
        lock.lock();
        try {
            processPreEntry(handle, preEntry,
                            entryOnDevice, pdGroup, opType);
        } finally {
            lock.unlock();
        }
    }

    private void processPreEntry(PiPreEntryHandle handle,
                                 PiPreEntry entryToApply,
                                 PiPreEntry entryOnDevice,
                                 Group pdGroup, GroupOperation.Type opType) {
        switch (opType) {
            case ADD:
                robustInsert(handle, entryToApply, pdGroup);
                return;
            case MODIFY:
                // Since reading multicast groups is not supported yet on
                // PI/Stratum, we cannot trust groupOnDevice as we don't have a
                // mechanism to enforce consistency of the mirror with the
                // device state.
                // if (driverBoolProperty(CHECK_MIRROR_BEFORE_UPDATE,
                //                        DEFAULT_CHECK_MIRROR_BEFORE_UPDATE)
                //         && p4OpType == MODIFY
                //         && groupOnDevice != null
                //         && groupOnDevice.equals(groupToApply)) {
                //     // Ignore.
                //     return;
                // }
                robustModify(handle, entryToApply, pdGroup);
                return;
            case DELETE:
                preEntryWrite(handle, entryToApply, pdGroup, DELETE);
                return;
            default:
                log.error("Unknown group operation type {}, " +
                                  "cannot process multicast group", opType);
        }
    }

    private boolean writeEntryOnDevice(
            PiPreEntry entry, P4RuntimeClient.UpdateType opType) {
        return client.write(p4DeviceId, pipeconf)
                .entity(entry, opType).submitSync().isSuccess();
    }

    private boolean preEntryWrite(PiPreEntryHandle handle,
                                  PiPreEntry preEntry,
                                  Group pdGroup,
                                  P4RuntimeClient.UpdateType opType) {
        switch (opType) {
            case DELETE:
                if (writeEntryOnDevice(preEntry, DELETE)) {
                    mirror.remove(handle);
                    translator.forget(handle);
                    return true;
                } else {
                    return false;
                }
            case INSERT:
            case MODIFY:
                if (writeEntryOnDevice(preEntry, opType)) {
                    mirror.put(handle, preEntry);
                    translator.learn(handle, new PiTranslatedEntity<>(
                            pdGroup, preEntry, handle));
                    return true;
                } else {
                    return false;
                }
            default:
                log.warn("Unknown operation type {}, cannot apply group", opType);
                return false;
        }
    }

    private void robustInsert(PiPreEntryHandle handle,
                              PiPreEntry preEntry,
                              Group pdGroup) {
        if (preEntryWrite(handle, preEntry, pdGroup, INSERT)) {
            return;
        }
        // Try to delete (perhaps it already exists) and re-add...
        preEntryWrite(handle, preEntry, pdGroup, DELETE);
        preEntryWrite(handle, preEntry, pdGroup, INSERT);
    }

    private void robustModify(PiPreEntryHandle handle,
                              PiPreEntry preEntry,
                              Group pdGroup) {
        if (preEntryWrite(handle, preEntry, pdGroup, MODIFY)) {
            return;
        }
        // Not sure for which reason it cannot be modified, so try to delete and insert instead...
        preEntryWrite(handle, preEntry, pdGroup, DELETE);
        preEntryWrite(handle, preEntry, pdGroup, INSERT);
    }

    private Group forgeGroupEntry(PiPreEntry preEntry) {
        final PiPreEntryHandle handle = (PiPreEntryHandle) preEntry.handle(deviceId);
        final Optional<PiTranslatedEntity<Group, PiPreEntry>>
                translatedEntity = translator.lookup(handle);
        final TimedEntry<PiPreEntry> timedEntry = mirror.get(handle);
        // Is entry consistent with our state?
        if (!translatedEntity.isPresent()) {
            log.warn("PRE entry handle not found in translation store: {}", handle);
            return null;
        }
        if (timedEntry == null) {
            log.warn("PRE entry handle not found in device mirror: {}", handle);
            return null;
        }
        return addedGroup(translatedEntity.get().original(), timedEntry.lifeSec());
    }

    private Group addedGroup(Group original, long life) {
        final DefaultGroup forgedGroup = new DefaultGroup(original.id(), original);
        forgedGroup.setState(Group.GroupState.ADDED);
        forgedGroup.setLife(life);
        return forgedGroup;
    }

}
