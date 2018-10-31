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
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeMulticastGroupMirror;
import org.onosproject.drivers.p4runtime.mirror.TimedEntry;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProgrammable;
import org.onosproject.net.group.GroupStore;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntry;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntryHandle;
import org.onosproject.net.pi.service.PiMulticastGroupTranslator;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.net.pi.service.PiTranslationException;
import org.onosproject.p4runtime.api.P4RuntimeClient;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.DELETE;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.INSERT;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.MODIFY;

/**
 * Implementation of GroupProgrammable to handle multicast groups in P4Runtime.
 */
public class P4RuntimeMulticastGroupProgrammable
        extends AbstractP4RuntimeHandlerBehaviour implements GroupProgrammable {

    // Needed to synchronize operations over the same group.
    private static final Striped<Lock> STRIPED_LOCKS = Striped.lock(30);

    private GroupStore groupStore;
    private P4RuntimeMulticastGroupMirror mcGroupMirror;
    private PiMulticastGroupTranslator mcGroupTranslator;

    @Override
    protected boolean setupBehaviour() {
        if (!super.setupBehaviour()) {
            return false;
        }
        mcGroupMirror = this.handler().get(P4RuntimeMulticastGroupMirror.class);
        groupStore = handler().get(GroupStore.class);
        mcGroupTranslator = translationService.multicastGroupTranslator();
        return true;
    }

    @Override
    public void performGroupOperation(DeviceId deviceId, GroupOperations groupOps) {
        if (!setupBehaviour()) {
            return;
        }
        groupOps.operations().stream()
                .filter(op -> op.groupType().equals(GroupDescription.Type.ALL))
                .forEach(op -> {
                    final Group group = groupStore.getGroup(deviceId, op.groupId());
                    processMcGroupOp(group, op.opType());
                });
    }

    @Override
    public Collection<Group> getGroups() {
        if (!setupBehaviour()) {
            return Collections.emptyList();
        }
        return ImmutableList.copyOf(getMcGroups());
    }

    private Collection<Group> getMcGroups() {
        // TODO: missing support for reading multicast groups is ready in PI/Stratum.
        return getMcGroupsFromMirror();
    }

    private Collection<Group> getMcGroupsFromMirror() {
        return mcGroupMirror.getAll(deviceId).stream()
                .map(TimedEntry::entry)
                .map(this::forgeMcGroupEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void processMcGroupOp(Group pdGroup, GroupOperation.Type opType) {
        final PiMulticastGroupEntry mcGroup;
        try {
            mcGroup = mcGroupTranslator.translate(pdGroup, pipeconf);
        } catch (PiTranslationException e) {
            log.warn("Unable to translate multicast group, aborting {} operation: {} [{}]",
                     opType, e.getMessage(), pdGroup);
            return;
        }
        final PiMulticastGroupEntryHandle handle = PiMulticastGroupEntryHandle.of(
                deviceId, mcGroup);
        final PiMulticastGroupEntry groupOnDevice = mcGroupMirror.get(handle) == null
                ? null
                : mcGroupMirror.get(handle).entry();
        final Lock lock = STRIPED_LOCKS.get(handle);
        lock.lock();
        try {
            processMcGroup(handle, mcGroup,
                           groupOnDevice, pdGroup, opType);
        } finally {
            lock.unlock();
        }
    }

    private void processMcGroup(PiMulticastGroupEntryHandle handle,
                                PiMulticastGroupEntry groupToApply,
                                PiMulticastGroupEntry groupOnDevice,
                                Group pdGroup, GroupOperation.Type opType) {
        switch (opType) {
            case ADD:
                robustMcGroupAdd(handle, groupToApply, pdGroup);
                return;
            case MODIFY:
                // Since reading multicast groups is not supported yet on
                // PI/Stratum, we cannot trust groupOnDevic) as we don't have a
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
                robustMcGroupModify(handle, groupToApply, pdGroup);
                return;
            case DELETE:
                mcGroupApply(handle, groupToApply, pdGroup, DELETE);
                return;
            default:
                log.error("Unknown group operation type {}, " +
                                  "cannot process multicast group", opType);
        }
    }

    private boolean writeMcGroupOnDevice(PiMulticastGroupEntry group, P4RuntimeClient.WriteOperationType opType) {
        return getFutureWithDeadline(
                client.writePreMulticastGroupEntries(
                        Collections.singletonList(group), opType),
                "performing multicast group " + opType, false);
    }

    private boolean mcGroupApply(PiMulticastGroupEntryHandle handle,
                                 PiMulticastGroupEntry piGroup,
                                 Group pdGroup,
                                 P4RuntimeClient.WriteOperationType opType) {
        switch (opType) {
            case DELETE:
                if (writeMcGroupOnDevice(piGroup, DELETE)) {
                    mcGroupMirror.remove(handle);
                    mcGroupTranslator.forget(handle);
                    return true;
                } else {
                    return false;
                }
            case INSERT:
            case MODIFY:
                if (writeMcGroupOnDevice(piGroup, opType)) {
                    mcGroupMirror.put(handle, piGroup);
                    mcGroupTranslator.learn(handle, new PiTranslatedEntity<>(
                            pdGroup, piGroup, handle));
                    return true;
                } else {
                    return false;
                }
            default:
                log.warn("Unknown operation type {}, cannot apply group", opType);
                return false;
        }
    }

    private void robustMcGroupAdd(PiMulticastGroupEntryHandle handle,
                                  PiMulticastGroupEntry piGroup,
                                  Group pdGroup) {
        if (mcGroupApply(handle, piGroup, pdGroup, INSERT)) {
            return;
        }
        // Try to delete (perhaps it already exists) and re-add...
        mcGroupApply(handle, piGroup, pdGroup, DELETE);
        mcGroupApply(handle, piGroup, pdGroup, INSERT);
    }

    private void robustMcGroupModify(PiMulticastGroupEntryHandle handle,
                                     PiMulticastGroupEntry piGroup,
                                     Group pdGroup) {
        if (mcGroupApply(handle, piGroup, pdGroup, MODIFY)) {
            return;
        }
        // Not sure for which reason it cannot be modified, so try to delete and insert instead...
        mcGroupApply(handle, piGroup, pdGroup, DELETE);
        mcGroupApply(handle, piGroup, pdGroup, INSERT);
    }

    private Group forgeMcGroupEntry(PiMulticastGroupEntry mcGroup) {
        final PiMulticastGroupEntryHandle handle = PiMulticastGroupEntryHandle.of(
                deviceId, mcGroup);
        final Optional<PiTranslatedEntity<Group, PiMulticastGroupEntry>>
                translatedEntity = mcGroupTranslator.lookup(handle);
        final TimedEntry<PiMulticastGroupEntry> timedEntry = mcGroupMirror.get(handle);
        // Is entry consistent with our state?
        if (!translatedEntity.isPresent()) {
            log.warn("Multicast group handle not found in translation store: {}", handle);
            return null;
        }
        if (timedEntry == null) {
            log.warn("Multicast group handle not found in device mirror: {}", handle);
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
