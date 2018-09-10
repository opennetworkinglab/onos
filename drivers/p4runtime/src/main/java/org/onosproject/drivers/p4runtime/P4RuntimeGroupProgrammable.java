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
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Striped;
import org.onlab.util.SharedExecutors;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeGroupMirror;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeMulticastGroupMirror;
import org.onosproject.drivers.p4runtime.mirror.TimedEntry;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProgrammable;
import org.onosproject.net.group.GroupStore;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiActionProfileModel;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupHandle;
import org.onosproject.net.pi.runtime.PiActionGroupMember;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntry;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntryHandle;
import org.onosproject.net.pi.service.PiGroupTranslator;
import org.onosproject.net.pi.service.PiMulticastGroupTranslator;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.net.pi.service.PiTranslationException;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.DELETE;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.INSERT;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.MODIFY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the group programmable behaviour for P4Runtime.
 * <p>
 * This implementation distinguishes between ALL groups, and other types. ALL
 * groups are handled via PRE multicast group programming, while other types are
 * handled via action profile group programming.
 */
public class P4RuntimeGroupProgrammable
        extends AbstractP4RuntimeHandlerBehaviour
        implements GroupProgrammable {

    private static final String ACT_GRP_MEMS_STR = "action group members";
    private static final String DELETE_STR = "delete";
    private static final String ACT_GRP_STR = "action group";
    private static final String INSERT_STR = "insert";
    private static final String MODIFY_STR = "modify";

    private static final Logger log = getLogger(P4RuntimeGroupProgrammable.class);

    // If true, we ignore re-installing groups that are already known in the
    // device mirror.
    private static final String CHECK_MIRROR_BEFORE_UPDATE = "checkMirrorBeforeUpdate";
    private static final boolean DEFAULT_CHECK_MIRROR_BEFORE_UPDATE = true;

    // If true, we avoid querying the device and return what's already known by
    // the ONOS store.
    private static final String READ_ACTION_GROUPS_FROM_MIRROR = "actionGroupReadFromMirror";
    private static final boolean DEFAULT_READ_ACTION_GROUPS_FROM_MIRROR = false;

    protected GroupStore groupStore;
    private P4RuntimeGroupMirror groupMirror;
    private PiGroupTranslator groupTranslator;
    private P4RuntimeMulticastGroupMirror mcGroupMirror;
    private PiMulticastGroupTranslator mcGroupTranslator;

    // Needed to synchronize operations over the same group.
    private static final Striped<Lock> STRIPED_LOCKS = Striped.lock(30);

    @Override
    protected boolean setupBehaviour() {
        if (!super.setupBehaviour()) {
            return false;
        }
        groupMirror = this.handler().get(P4RuntimeGroupMirror.class);
        mcGroupMirror = this.handler().get(P4RuntimeMulticastGroupMirror.class);
        groupStore = handler().get(GroupStore.class);
        groupTranslator = piTranslationService.groupTranslator();
        mcGroupTranslator = piTranslationService.multicastGroupTranslator();
        return true;
    }

    @Override
    public void performGroupOperation(DeviceId deviceId,
                                      GroupOperations groupOps) {
        if (!setupBehaviour()) {
            return;
        }

        // TODO: fix GroupProgrammable API, passing the device ID is ambiguous
        checkArgument(deviceId.equals(this.deviceId),
                      "passed deviceId must be the same assigned to this behavior");

        groupOps.operations().forEach(op -> {
            // ONOS-7785 We need app cookie (action profile id) from the group
            Group groupOnStore = groupStore.getGroup(deviceId, op.groupId());
            GroupDescription groupDesc = new DefaultGroupDescription(deviceId,
                                                                     op.groupType(),
                                                                     op.buckets(),
                                                                     groupOnStore.appCookie(),
                                                                     op.groupId().id(),
                                                                     groupOnStore.appId());
            DefaultGroup groupToApply = new DefaultGroup(op.groupId(), groupDesc);
            if (op.groupType().equals(GroupDescription.Type.ALL)) {
                processMcGroupOp(groupToApply, op.opType());
            } else {

                processGroupOp(groupToApply, op.opType());
            }
        });
    }

    @Override
    public Collection<Group> getGroups() {
        if (!setupBehaviour()) {
            return Collections.emptyList();
        }
        return new ImmutableList.Builder<Group>()
                .addAll(getActionGroups())
                .addAll(getMcGroups()).build();
    }

    private Collection<Group> getActionGroups() {

        if (driverBoolProperty(READ_ACTION_GROUPS_FROM_MIRROR,
                               DEFAULT_READ_ACTION_GROUPS_FROM_MIRROR)) {
            return getActionGroupsFromMirror();
        }

        final Collection<PiActionGroup> piGroups = pipeconf.pipelineModel()
                .actionProfiles()
                .stream()
                .map(PiActionProfileModel::id)
                .flatMap(this::streamPiGroupsFromDevice)
                .collect(Collectors.toList());

        if (piGroups.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Group> result = Lists.newArrayList();
        final List<PiActionGroup> inconsistentGroups = Lists.newArrayList();

        for (PiActionGroup piGroupOnDevice : piGroups) {
            final Group group = forgeGroupEntry(piGroupOnDevice);
            if (group == null) {
                // Entry is on device but unknown to translation service or
                // device mirror. Inconsistent. Mark for removal.
                inconsistentGroups.add(piGroupOnDevice);
            } else {
                result.add(group);
            }
        }
        // Trigger clean up of inconsistent entries (is any).
        // TODO: make this behaviour configurable, in some cases it's fine for
        // the device to have groups that were not installed by us.
        if (!inconsistentGroups.isEmpty()) {
            SharedExecutors.getSingleThreadExecutor().execute(
                    () -> cleanUpInconsistentGroups(inconsistentGroups));
        }
        return result;
    }

    private Collection<Group> getActionGroupsFromMirror() {
        return groupMirror.getAll(deviceId).stream()
                .map(TimedEntry::entry)
                .map(this::forgeGroupEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void cleanUpInconsistentGroups(Collection<PiActionGroup> piGroups) {
        log.warn("Found {} inconsistent groups on {}, removing them...",
                 piGroups.size(), deviceId);
        piGroups.forEach(piGroup -> {
            log.debug(piGroup.toString());
            // Per-piGroup lock.
            final PiActionGroupHandle handle = PiActionGroupHandle.of(deviceId, piGroup);
            STRIPED_LOCKS.get(handle).lock();
            try {
                processPiGroup(handle, piGroup, piGroup, null,
                               GroupOperation.Type.DELETE);
            } finally {
                STRIPED_LOCKS.get(handle).unlock();
            }
        });
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

    private void processGroupOp(Group pdGroup, GroupOperation.Type opType) {
        final PiActionGroup piGroup;
        try {
            piGroup = groupTranslator.translate(pdGroup, pipeconf);
        } catch (PiTranslationException e) {
            log.warn("Unable to translate group, aborting {} operation: {} [{}]",
                     opType, e.getMessage(), pdGroup);
            return;
        }
        final PiActionGroupHandle handle = PiActionGroupHandle.of(deviceId, piGroup);
        final PiActionGroup groupOnDevice = groupMirror.get(handle) == null
                ? null : groupMirror.get(handle).entry();
        // Per-piGroup lock.
        final Lock lock = STRIPED_LOCKS.get(handle);
        lock.lock();
        try {
            processPiGroup(handle, piGroup,
                           groupOnDevice, pdGroup, opType);
        } finally {
            lock.unlock();
        }
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

    private void processPiGroup(PiActionGroupHandle handle,
                                PiActionGroup groupToApply,
                                PiActionGroup groupOnDevice,
                                Group pdGroup, GroupOperation.Type operationType) {
        if (operationType == GroupOperation.Type.ADD) {
            if (groupOnDevice != null) {
                log.warn("Unable to add group {} since group already on device {}",
                         groupToApply.id(), deviceId);
                log.debug("To apply: {}", groupToApply);
                log.debug("On device: {}", groupOnDevice);
                return;
            }

            if (writeGroupToDevice(groupToApply)) {
                groupMirror.put(handle, groupToApply);
                groupTranslator.learn(handle, new PiTranslatedEntity<>(
                        pdGroup, groupToApply, handle));
            }
        } else if (operationType == GroupOperation.Type.MODIFY) {
            if (groupOnDevice == null) {
                log.warn("Group {} does not exists on device {}, can not modify it",
                         groupToApply.id(), deviceId);
                return;
            }
            if (driverBoolProperty(CHECK_MIRROR_BEFORE_UPDATE, DEFAULT_CHECK_MIRROR_BEFORE_UPDATE)
                    && groupOnDevice.equals(groupToApply)) {
                // Group on device has the same members, ignore operation.
                return;
            }
            if (modifyGroupFromDevice(groupToApply, groupOnDevice)) {
                groupMirror.put(handle, groupToApply);
                groupTranslator.learn(handle,
                                      new PiTranslatedEntity<>(pdGroup, groupToApply, handle));
            }
        } else {
            if (groupOnDevice == null) {
                log.warn("Unable to remove group {} from device {} since it does" +
                                 "not exists on device.", groupToApply.id(), deviceId);
                return;
            }
            if (deleteGroupFromDevice(groupOnDevice)) {
                groupMirror.remove(handle);
                groupTranslator.forget(handle);
            }
        }
    }

    private void processMcGroup(PiMulticastGroupEntryHandle handle,
                                PiMulticastGroupEntry groupToApply,
                                PiMulticastGroupEntry groupOnDevice,
                                Group pdGroup, GroupOperation.Type opType) {
        if (opType == GroupOperation.Type.DELETE) {
            if (writeMcGroupOnDevice(groupToApply, DELETE)) {
                mcGroupMirror.remove(handle);
                mcGroupTranslator.forget(handle);
            }
            return;
        }

        final P4RuntimeClient.WriteOperationType p4OpType =
                opType == GroupOperation.Type.ADD ? INSERT : MODIFY;

        if (driverBoolProperty(CHECK_MIRROR_BEFORE_UPDATE,
                               DEFAULT_CHECK_MIRROR_BEFORE_UPDATE)
                && p4OpType == MODIFY
                && groupOnDevice != null
                && groupOnDevice.equals(groupToApply)) {
            // Ignore.
            return;
        }

        if (writeMcGroupOnDevice(groupToApply, p4OpType)) {
            mcGroupMirror.put(handle, groupToApply);
            mcGroupTranslator.learn(handle, new PiTranslatedEntity<>(
                    pdGroup, groupToApply, handle));
        }
    }

    private boolean writeMcGroupOnDevice(PiMulticastGroupEntry group, P4RuntimeClient.WriteOperationType opType) {
        return getFutureWithDeadline(
                client.writePreMulticastGroupEntries(
                        Collections.singleton(group), opType),
                "performing multicast group " + opType, false);
    }

    private boolean modifyGroupFromDevice(PiActionGroup groupToApply, PiActionGroup groupOnDevice) {
        PiActionProfileId groupProfileId = groupToApply.actionProfileId();
        Collection<PiActionGroupMember> membersToRemove = Sets.newHashSet(groupOnDevice.members());
        membersToRemove.removeAll(groupToApply.members());
        Collection<PiActionGroupMember> membersToAdd = Sets.newHashSet(groupToApply.members());
        membersToAdd.removeAll(groupOnDevice.members());

        if (!membersToAdd.isEmpty() &&
                !completeFuture(client.writeActionGroupMembers(groupProfileId, membersToAdd, INSERT, pipeconf),
                                ACT_GRP_MEMS_STR, INSERT_STR)) {
            // remove what we added
            completeFuture(client.writeActionGroupMembers(groupProfileId, membersToAdd, DELETE, pipeconf),
                           ACT_GRP_MEMS_STR, INSERT_STR);
            return false;
        }

        if (!completeFuture(client.writeActionGroup(groupToApply, MODIFY, pipeconf),
                            ACT_GRP_STR, MODIFY_STR)) {
            // recover group information
            completeFuture(client.writeActionGroup(groupOnDevice, MODIFY, pipeconf),
                           ACT_GRP_STR, MODIFY_STR);
            // remove what we added
            completeFuture(client.writeActionGroupMembers(groupProfileId, membersToAdd, DELETE, pipeconf),
                           ACT_GRP_MEMS_STR, INSERT_STR);
            return false;
        }

        if (!membersToRemove.isEmpty() &&
                !completeFuture(client.writeActionGroupMembers(groupProfileId, membersToRemove, DELETE, pipeconf),
                                ACT_GRP_MEMS_STR, DELETE_STR)) {
            // add what we removed
            completeFuture(client.writeActionGroupMembers(groupProfileId, membersToRemove, INSERT, pipeconf),
                           ACT_GRP_MEMS_STR, DELETE_STR);
            // recover group information
            completeFuture(client.writeActionGroup(groupOnDevice, MODIFY, pipeconf),
                           ACT_GRP_STR, MODIFY_STR);
            // remove what we added
            completeFuture(client.writeActionGroupMembers(groupProfileId, membersToAdd, DELETE, pipeconf),
                           ACT_GRP_MEMS_STR, INSERT_STR);
            return false;
        }

        return true;
    }

    private boolean writeGroupToDevice(PiActionGroup groupToApply) {
        // First insert members, then group.
        // The operation is deemed successful if both operations are successful.
        // FIXME: add transactional semantics, i.e. remove members if group fails.
        final boolean membersSuccess = completeFuture(
                client.writeActionGroupMembers(groupToApply.actionProfileId(),
                                               groupToApply.members(),
                                               INSERT, pipeconf),
                ACT_GRP_MEMS_STR, INSERT_STR);
        return membersSuccess && completeFuture(
                client.writeActionGroup(groupToApply, INSERT, pipeconf),
                ACT_GRP_STR, INSERT_STR);
    }

    private boolean deleteGroupFromDevice(PiActionGroup piActionGroup) {
        // First delete group, then members.
        // The operation is deemed successful if both operations are successful.
        final boolean groupSuccess = completeFuture(
                client.writeActionGroup(piActionGroup, DELETE, pipeconf),
                ACT_GRP_STR, DELETE_STR);
        return groupSuccess && completeFuture(
                client.writeActionGroupMembers(piActionGroup.actionProfileId(),
                                               piActionGroup.members(),
                                               DELETE, pipeconf),
                ACT_GRP_MEMS_STR, DELETE_STR);
    }

    private boolean completeFuture(CompletableFuture<Boolean> completableFuture,
                                   String topic, String action) {
        return getFutureWithDeadline(
                completableFuture, format("performing %s %s", action, topic), false);
    }

    private Stream<PiActionGroup> streamPiGroupsFromDevice(PiActionProfileId actProfId) {
        // Read PI groups and return original PD one.
        // TODO: implement P4Runtime client call to read all groups with one call
        // Good is pipeline has multiple action profiles.
        final Collection<PiActionGroup> groups = getFutureWithDeadline(
                client.dumpGroups(actProfId, pipeconf),
                "dumping groups", Collections.emptyList());
        return groups.stream();
    }

    private Group forgeGroupEntry(PiActionGroup piGroup) {
        final PiActionGroupHandle handle = PiActionGroupHandle.of(deviceId, piGroup);
        final Optional<PiTranslatedEntity<Group, PiActionGroup>>
                translatedEntity = groupTranslator.lookup(handle);
        final TimedEntry<PiActionGroup> timedEntry = groupMirror.get(handle);
        // Is entry consistent with our state?
        if (!translatedEntity.isPresent()) {
            log.warn("Group handle not found in translation store: {}", handle);
            return null;
        }
        if (timedEntry == null) {
            // Don't bother logging more than debug, most probably it's the EC
            // map backing the store that has not received all the updates yet.
            log.debug("Group handle not found in device mirror: {}", handle);
            return null;
        }
        return addedGroup(translatedEntity.get().original(), timedEntry.lifeSec());
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
