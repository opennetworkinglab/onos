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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Striped;
import org.onlab.util.SharedExecutors;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeActionProfileGroupMirror;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeActionProfileMemberMirror;
import org.onosproject.drivers.p4runtime.mirror.TimedEntry;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProgrammable;
import org.onosproject.net.group.GroupStore;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiActionProfileModel;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionProfileGroup;
import org.onosproject.net.pi.runtime.PiActionProfileGroupHandle;
import org.onosproject.net.pi.runtime.PiActionProfileMember;
import org.onosproject.net.pi.runtime.PiActionProfileMemberHandle;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import org.onosproject.net.pi.service.PiGroupTranslator;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.net.pi.service.PiTranslationException;
import org.onosproject.p4runtime.api.P4RuntimeClient;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.DELETE;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.INSERT;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.MODIFY;

/**
 * Implementation of GroupProgrammable to handle action profile groups in
 * P4Runtime.
 */
public class P4RuntimeActionGroupProgrammable
        extends AbstractP4RuntimeHandlerBehaviour
        implements GroupProgrammable {

    // If true, we avoid querying the device and return what's already known by
    // the ONOS store.
    private static final String READ_ACTION_GROUPS_FROM_MIRROR = "actionGroupReadFromMirror";
    private static final boolean DEFAULT_READ_ACTION_GROUPS_FROM_MIRROR = false;
    private static final String MAX_MEM_SIZE = "maxMemSize";

    protected GroupStore groupStore;
    private P4RuntimeActionProfileGroupMirror groupMirror;
    private P4RuntimeActionProfileMemberMirror memberMirror;
    private PiGroupTranslator groupTranslator;

    // Needed to synchronize operations over the same group.
    private static final Striped<Lock> STRIPED_LOCKS = Striped.lock(30);
    private static final int GROUP_MEMBERS_BUFFER_SIZE = 3;

    @Override
    protected boolean setupBehaviour() {
        if (!super.setupBehaviour()) {
            return false;
        }
        groupMirror = this.handler().get(P4RuntimeActionProfileGroupMirror.class);
        memberMirror = this.handler().get(P4RuntimeActionProfileMemberMirror.class);
        groupStore = handler().get(GroupStore.class);
        groupTranslator = translationService.groupTranslator();
        return true;
    }

    @Override
    public void performGroupOperation(DeviceId deviceId,
                                      GroupOperations groupOps) {
        if (!setupBehaviour()) {
            return;
        }

        groupOps.operations().stream()
                .filter(op -> !op.groupType().equals(GroupDescription.Type.ALL))
                .forEach(op -> {
                    // ONOS-7785 We need app cookie (action profile id) from the group
                    Group groupOnStore = groupStore.getGroup(deviceId, op.groupId());
                    GroupDescription groupDesc = new DefaultGroupDescription(
                            deviceId, op.groupType(), op.buckets(), groupOnStore.appCookie(),
                            op.groupId().id(), groupOnStore.appId());
                    DefaultGroup groupToApply = new DefaultGroup(op.groupId(), groupDesc);
                    processGroupOperation(groupToApply, op.opType());
                });
    }

    @Override
    public Collection<Group> getGroups() {
        if (!setupBehaviour()) {
            return Collections.emptyList();
        }
        return getActionGroups();
    }

    private Collection<Group> getActionGroups() {

        if (driverBoolProperty(READ_ACTION_GROUPS_FROM_MIRROR,
                               DEFAULT_READ_ACTION_GROUPS_FROM_MIRROR)) {
            return getActionGroupsFromMirror();
        }

        final Collection<PiActionProfileId> actionProfileIds = pipeconf.pipelineModel()
                .actionProfiles()
                .stream()
                .map(PiActionProfileModel::id)
                .collect(Collectors.toList());
        final List<PiActionProfileGroup> groupsOnDevice = actionProfileIds.stream()
                .flatMap(this::streamGroupsFromDevice)
                .collect(Collectors.toList());
        final Set<PiActionProfileMemberHandle> membersOnDevice = actionProfileIds
                .stream()
                .flatMap(actProfId -> getMembersFromDevice(actProfId)
                        .stream()
                        .map(memberId -> PiActionProfileMemberHandle.of(
                                deviceId, actProfId, memberId)))
                .collect(Collectors.toSet());

        if (groupsOnDevice.isEmpty()) {
            return Collections.emptyList();
        }

        // Sync mirrors.
        syncGroupMirror(groupsOnDevice);
        syncMemberMirror(membersOnDevice);

        final List<Group> result = Lists.newArrayList();
        final List<PiActionProfileGroup> inconsistentGroups = Lists.newArrayList();
        final List<PiActionProfileGroup> validGroups = Lists.newArrayList();

        for (PiActionProfileGroup piGroup : groupsOnDevice) {
            final Group pdGroup = forgeGroupEntry(piGroup);
            if (pdGroup == null) {
                // Entry is on device but unknown to translation service or
                // device mirror. Inconsistent. Mark for removal.
                inconsistentGroups.add(piGroup);
            } else {
                validGroups.add(piGroup);
                result.add(pdGroup);
            }
        }

        // Trigger clean up of inconsistent groups and members. This will also
        // remove all members that are not used by any group, and update the
        // mirror accordingly.
        final Set<PiActionProfileMemberHandle> membersToKeep = validGroups.stream()
                .flatMap(g -> g.members().stream())
                .map(m -> PiActionProfileMemberHandle.of(deviceId, m))
                .collect(Collectors.toSet());
        final Set<PiActionProfileMemberHandle> inconsistentMembers = Sets.difference(
                membersOnDevice, membersToKeep);
        SharedExecutors.getSingleThreadExecutor().execute(
                () -> cleanUpInconsistentGroupsAndMembers(
                        inconsistentGroups, inconsistentMembers));

        return result;
    }

    private void syncGroupMirror(Collection<PiActionProfileGroup> groups) {
        Map<PiActionProfileGroupHandle, PiActionProfileGroup> handleMap = Maps.newHashMap();
        groups.forEach(g -> handleMap.put(PiActionProfileGroupHandle.of(deviceId, g), g));
        groupMirror.sync(deviceId, handleMap);
    }

    private void syncMemberMirror(Collection<PiActionProfileMemberHandle> memberHandles) {
        Map<PiActionProfileMemberHandle, PiActionProfileMember> handleMap = Maps.newHashMap();
        memberHandles.forEach(handle -> handleMap.put(
                handle, dummyMember(handle.actionProfileId(), handle.memberId())));
        memberMirror.sync(deviceId, handleMap);
    }

    private Collection<Group> getActionGroupsFromMirror() {
        return groupMirror.getAll(deviceId).stream()
                .map(TimedEntry::entry)
                .map(this::forgeGroupEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void cleanUpInconsistentGroupsAndMembers(Collection<PiActionProfileGroup> groupsToRemove,
                                                     Collection<PiActionProfileMemberHandle> membersToRemove) {
        if (!groupsToRemove.isEmpty()) {
            log.warn("Found {} inconsistent action profile groups on {}, removing them...",
                     groupsToRemove.size(), deviceId);
            groupsToRemove.forEach(piGroup -> {
                log.debug(piGroup.toString());
                processGroup(piGroup, null, Operation.REMOVE);
            });
        }
        if (!membersToRemove.isEmpty()) {
            log.warn("Found {} inconsistent action profile members on {}, removing them...",
                     membersToRemove.size(), deviceId);
            // FIXME: implement client call to remove members from multiple
            // action profiles in one shot.
            final ListMultimap<PiActionProfileId, PiActionProfileMemberId>
                    membersByActProfId = ArrayListMultimap.create();
            membersToRemove.forEach(m -> membersByActProfId.put(
                    m.actionProfileId(), m.memberId()));
            membersByActProfId.keySet().forEach(actProfId -> {
                List<PiActionProfileMemberId> removedMembers = getFutureWithDeadline(
                        client.removeActionProfileMembers(
                                actProfId, membersByActProfId.get(actProfId), pipeconf),
                        "cleaning up action profile members", Collections.emptyList());
                // Update member mirror.
                removedMembers.stream()
                        .map(id -> PiActionProfileMemberHandle.of(deviceId, actProfId, id))
                        .forEach(memberMirror::remove);
            });
        }
    }

    private Stream<PiActionProfileGroup> streamGroupsFromDevice(PiActionProfileId actProfId) {
        // TODO: implement P4Runtime client call to read all groups with one call
        // Good if pipeline has multiple action profiles.
        final Collection<PiActionProfileGroup> groups = getFutureWithDeadline(
                client.dumpActionProfileGroups(actProfId, pipeconf),
                "dumping groups", Collections.emptyList());
        return groups.stream();
    }

    private List<PiActionProfileMemberId> getMembersFromDevice(PiActionProfileId actProfId) {
        // TODO: implement P4Runtime client call to read all members with one call
        // Good if pipeline has multiple action profiles.
        return getFutureWithDeadline(
                client.dumpActionProfileMemberIds(actProfId, pipeconf),
                "dumping action profile ids", Collections.emptyList());
    }

    private Group forgeGroupEntry(PiActionProfileGroup piGroup) {
        final PiActionProfileGroupHandle handle = PiActionProfileGroupHandle.of(deviceId, piGroup);
        final Optional<PiTranslatedEntity<Group, PiActionProfileGroup>>
                translatedEntity = groupTranslator.lookup(handle);
        final TimedEntry<PiActionProfileGroup> timedEntry = groupMirror.get(handle);
        // Is entry consistent with our state?
        if (!translatedEntity.isPresent()) {
            log.warn("Group handle not found in translation store: {}", handle);
            return null;
        }
        if (!translatedEntity.get().translated().equals(piGroup)) {
            log.warn("Group obtained from device {} is different from the one in " +
                             "translation store: device={}, store={}",
                     deviceId, piGroup, translatedEntity.get().translated());
            return null;
        }
        if (timedEntry == null) {
            log.warn("Group handle not found in device mirror: {}", handle);
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

    private void processGroupOperation(Group pdGroup, GroupOperation.Type opType) {
        final PiActionProfileGroup piGroup;
        try {
            piGroup = groupTranslator.translate(pdGroup, pipeconf);
        } catch (PiTranslationException e) {
            log.warn("Unable to translate group, aborting {} operation: {} [{}]",
                     opType, e.getMessage(), pdGroup);
            return;
        }
        final Operation operation = opType.equals(GroupOperation.Type.DELETE)
                ? Operation.REMOVE : Operation.APPLY;
        processGroup(piGroup, pdGroup, operation);
    }

    private void processGroup(PiActionProfileGroup groupToApply,
                              Group pdGroup,
                              Operation operation) {
        final PiActionProfileGroupHandle handle = PiActionProfileGroupHandle.of(deviceId, groupToApply);
        STRIPED_LOCKS.get(handle).lock();
        try {
            switch (operation) {
                case APPLY:
                    if (applyGroupWithMembersOrNothing(groupToApply, handle)) {
                        groupTranslator.learn(handle, new PiTranslatedEntity<>(
                                pdGroup, groupToApply, handle));
                    }
                    return;
                case REMOVE:
                    if (deleteGroup(groupToApply, handle)) {
                        groupTranslator.forget(handle);
                    }
                    return;
                default:
                    log.error("Unknwon group operation type {}, cannot process group", operation);
                    break;
            }
        } finally {
            STRIPED_LOCKS.get(handle).unlock();
        }
    }

    private boolean applyGroupWithMembersOrNothing(PiActionProfileGroup group, PiActionProfileGroupHandle handle) {
        // First apply members, then group, if fails, delete members.
        if (!applyAllMembersOrNothing(group.members())) {
            return false;
        }
        if (!applyGroup(group, handle)) {
            deleteMembers(group.members());
            return false;
        }
        return true;
    }

    private boolean applyGroup(PiActionProfileGroup group, PiActionProfileGroupHandle handle) {
        final int currentMemberSize = group.members().size();
        if (groupMirror.get(handle) != null) {
            String maxMemSize = "";
            if (groupMirror.annotations(handle) != null &&
                    groupMirror.annotations(handle).value(MAX_MEM_SIZE) != null) {
                maxMemSize = groupMirror.annotations(handle).value(MAX_MEM_SIZE);
            }
            if (maxMemSize.equals("") || currentMemberSize > Integer.parseInt(maxMemSize)) {
                deleteGroup(group, handle);
            }
        }

        P4RuntimeClient.WriteOperationType opType =
                groupMirror.get(handle) == null ? INSERT : MODIFY;
        int currentMaxMemberSize = opType == INSERT ? (currentMemberSize + GROUP_MEMBERS_BUFFER_SIZE) : 0;

        final boolean success = getFutureWithDeadline(
                client.writeActionProfileGroup(group, opType, pipeconf, currentMaxMemberSize),
                "performing action profile group " + opType, false);
        if (success) {
            groupMirror.put(handle, group);
            if (opType == INSERT) {
                groupMirror.putAnnotations(handle, DefaultAnnotations
                        .builder()
                        .set(MAX_MEM_SIZE, Integer.toString(currentMaxMemberSize))
                        .build());
            }
        }
        return success;
    }

    private boolean deleteGroup(PiActionProfileGroup group, PiActionProfileGroupHandle handle) {
        final boolean success = getFutureWithDeadline(
                client.writeActionProfileGroup(group, DELETE, pipeconf, 0),
                "performing action profile group " + DELETE, false);
        if (success) {
            groupMirror.remove(handle);
        }
        return success;
    }

    private boolean applyAllMembersOrNothing(Collection<PiActionProfileMember> members) {
        Collection<PiActionProfileMember> appliedMembers = applyMembers(members);
        if (appliedMembers.size() == members.size()) {
            return true;
        } else {
            deleteMembers(appliedMembers);
            return false;
        }
    }

    private Collection<PiActionProfileMember> applyMembers(
            Collection<PiActionProfileMember> members) {
        return members.stream()
                .filter(this::applyMember)
                .collect(Collectors.toList());
    }

    private boolean applyMember(PiActionProfileMember member) {
        // If exists, modify, otherwise insert
        final PiActionProfileMemberHandle handle = PiActionProfileMemberHandle.of(
                deviceId, member);
        final P4RuntimeClient.WriteOperationType opType =
                memberMirror.get(handle) == null ? INSERT : MODIFY;
        final boolean success = getFutureWithDeadline(
                client.writeActionProfileMembers(Collections.singletonList(member),
                                                 opType, pipeconf),
                "performing action profile member " + opType, false);
        if (success) {
            memberMirror.put(handle, dummyMember(member.actionProfile(), member.id()));
        }
        return success;
    }

    private void deleteMembers(Collection<PiActionProfileMember> members) {
        members.forEach(this::deleteMember);
    }

    private void deleteMember(PiActionProfileMember member) {
        final PiActionProfileMemberHandle handle = PiActionProfileMemberHandle.of(
                deviceId, member);
        final boolean success = getFutureWithDeadline(
                client.writeActionProfileMembers(Collections.singletonList(member),
                                                 DELETE, pipeconf),
                "performing action profile member " + DELETE, false);
        if (success) {
            memberMirror.remove(handle);
        }
    }

    // FIXME: this is nasty, we have to rely on a dummy member of the mirror
    // because the PiActionProfileMember abstraction is broken, since it includes
    // attributes that are not part of a P4Runtime member, e.g. weight.
    // We should remove weight from the class, and have client methods that
    // return the full PiActionProfileMember, not just the IDs.
    private PiActionProfileMember dummyMember(
            PiActionProfileId actionProfileId, PiActionProfileMemberId memberId) {
        return PiActionProfileMember.builder()
                .forActionProfile(actionProfileId)
                .withId(memberId)
                .withAction(PiAction.builder()
                                    .withId(PiActionId.of("dummy"))
                                    .build())
                .build();
    }

    enum Operation {
        APPLY, REMOVE
    }
}
