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
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Striped;
import org.onlab.util.SharedExecutors;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeActionProfileGroupMirror;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeActionProfileMemberMirror;
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

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
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

    protected GroupStore groupStore;
    private P4RuntimeActionProfileGroupMirror groupMirror;
    private P4RuntimeActionProfileMemberMirror memberMirror;
    private PiGroupTranslator groupTranslator;

    // Needed to synchronize operations over the same group.
    private static final Striped<Lock> STRIPED_LOCKS = Striped.lock(30);

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
                    // ONOS-7785 We need the group app cookie (which includes
                    // the action profile ID) but this is not part of the
                    // GroupDescription.
                    Group groupOnStore = groupStore.getGroup(deviceId, op.groupId());
                    if (groupOnStore == null) {
                        log.warn("Unable to find group {} in store, aborting {} operation",
                                 op.groupId(), op.opType());
                        return;
                    }
                    GroupDescription groupDesc = new DefaultGroupDescription(
                            deviceId, op.groupType(), op.buckets(), groupOnStore.appCookie(),
                            op.groupId().id(), groupOnStore.appId());
                    DefaultGroup groupToApply = new DefaultGroup(op.groupId(), groupDesc);
                    processPdGroup(groupToApply, op.opType());
                });
    }

    @Override
    public Collection<Group> getGroups() {
        if (!setupBehaviour()) {
            return Collections.emptyList();
        }

        if (driverBoolProperty(READ_ACTION_GROUPS_FROM_MIRROR,
                               DEFAULT_READ_ACTION_GROUPS_FROM_MIRROR)) {
            return getGroupsFromMirror();
        }

        // Dump groups and members from device for all action profiles.
        final Set<PiActionProfileId> actionProfileIds = pipeconf.pipelineModel()
                .actionProfiles()
                .stream()
                .map(PiActionProfileModel::id)
                .collect(Collectors.toSet());
        final Map<PiActionProfileGroupHandle, PiActionProfileGroup>
                groupsOnDevice = dumpAllGroupsFromDevice(actionProfileIds);
        final Map<PiActionProfileMemberHandle, PiActionProfileMember> membersOnDevice =
                dumpAllMembersFromDevice(actionProfileIds);

        // Sync mirrors.
        groupMirror.sync(deviceId, groupsOnDevice);
        memberMirror.sync(deviceId, membersOnDevice);

        // Retrieve the original PD group before translation.
        final List<Group> result = Lists.newArrayList();
        final List<PiActionProfileGroup> groupsToRemove = Lists.newArrayList();
        final Set<PiActionProfileMemberHandle> memberHandlesToKeep = Sets.newHashSet();
        for (PiActionProfileGroup piGroup : groupsOnDevice.values()) {
            final Group pdGroup = checkAndForgeGroupEntry(piGroup, membersOnDevice);
            if (pdGroup == null) {
                // Entry is on device but unknown to translation service or
                // device mirror. Inconsistent. Mark for removal.
                groupsToRemove.add(piGroup);
            } else {
                result.add(pdGroup);
                // Keep track of member handles used in groups.
                piGroup.members().stream()
                        .map(m -> PiActionProfileMemberHandle.of(
                                deviceId, piGroup.actionProfile(), m.id()))
                        .forEach(memberHandlesToKeep::add);
            }
        }

        // Trigger clean up of inconsistent groups and members. This will update
        // the mirror accordingly.
        final Set<PiActionProfileMemberHandle> memberHandlesToRemove = Sets.difference(
                membersOnDevice.keySet(), memberHandlesToKeep);
        SharedExecutors.getSingleThreadExecutor().execute(
                () -> cleanUpInconsistentGroupsAndMembers(
                        groupsToRemove, memberHandlesToRemove));

        // Done.
        return result;
    }

    private Collection<Group> getGroupsFromMirror() {
        final Map<PiActionProfileMemberHandle, PiActionProfileMember> members =
                memberMirror.deviceHandleMap(deviceId);
        return groupMirror.getAll(deviceId).stream()
                .map(TimedEntry::entry)
                .map(g -> checkAndForgeGroupEntry(
                        g, members))
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
                processPiGroup(piGroup, null, Operation.REMOVE);
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

    private Map<PiActionProfileGroupHandle, PiActionProfileGroup> dumpAllGroupsFromDevice(
            Set<PiActionProfileId> actProfIds) {
        // TODO: implement P4Runtime client call to read all groups with one call
        // Good if pipeline has multiple action profiles.
        return actProfIds.stream()
                .flatMap(actProfId -> getFutureWithDeadline(
                        client.dumpActionProfileGroups(actProfId, pipeconf),
                        "dumping groups", Collections.emptyList()).stream())
                .collect(toMap(g -> PiActionProfileGroupHandle.of(deviceId, g), g -> g));
    }

    private Map<PiActionProfileMemberHandle, PiActionProfileMember> dumpAllMembersFromDevice(
            Set<PiActionProfileId> actProfIds) {
        // TODO: implement P4Runtime client call to read all members with one call
        // Good if pipeline has multiple action profiles.
        return actProfIds.stream()
                .flatMap(actProfId -> getFutureWithDeadline(
                        client.dumpActionProfileMembers(actProfId, pipeconf),
                        "dumping members", Collections.emptyList()).stream())
                .collect(toMap(m -> PiActionProfileMemberHandle.of(deviceId, m), m -> m));
    }

    private Group checkAndForgeGroupEntry(
            PiActionProfileGroup piGroupOnDevice,
            Map<PiActionProfileMemberHandle, PiActionProfileMember> membersOnDevice) {
        final PiActionProfileGroupHandle handle = PiActionProfileGroupHandle.of(
                deviceId, piGroupOnDevice);
        final Optional<PiTranslatedEntity<Group, PiActionProfileGroup>>
                translatedEntity = groupTranslator.lookup(handle);
        final TimedEntry<PiActionProfileGroup> mirrorEntry = groupMirror.get(handle);
        // Check that entry obtained from device is consistent with what is known
        // by the translation store.
        if (!translatedEntity.isPresent()) {
            log.warn("Group not found in translation store: {}", handle);
            return null;
        }
        final PiActionProfileGroup piGroupFromStore = translatedEntity.get().translated();
        if (!piGroupFromStore.equals(piGroupOnDevice)) {
            log.warn("Group on device {} is different from the one in " +
                             "translation store: {} [device={}, store={}]",
                     deviceId, handle, piGroupOnDevice, piGroupFromStore);
            return null;
        }
        // Groups in P4Runtime contains only a reference to members. Check that
        // the actual member instances in the translation store are the same
        // found on the device.
        if (!validateMembers(piGroupFromStore, membersOnDevice)) {
            log.warn("Group on device {} refers to members that are different " +
                             "than those found in translation store: {}", handle);
            return null;
        }
        if (mirrorEntry == null) {
            log.warn("Group handle not found in device mirror: {}", handle);
            return null;
        }
        // Check that members from device are the same as in the translated group.
        return addedGroup(translatedEntity.get().original(), mirrorEntry.lifeSec());
    }

    private boolean validateMembers(
            PiActionProfileGroup piGroupFromStore,
            Map<PiActionProfileMemberHandle, PiActionProfileMember> membersOnDevice) {
        final Collection<PiActionProfileMember> groupMembers =
                extractAllMemberInstancesOrNull(piGroupFromStore);
        if (groupMembers == null) {
            return false;
        }
        return groupMembers.stream().allMatch(
                memberFromStore -> memberFromStore.equals(
                        membersOnDevice.get(
                                PiActionProfileMemberHandle.of(deviceId, memberFromStore))));
    }

    private Group addedGroup(Group original, long life) {
        final DefaultGroup forgedGroup = new DefaultGroup(original.id(), original);
        forgedGroup.setState(Group.GroupState.ADDED);
        forgedGroup.setLife(life);
        return forgedGroup;
    }

    private void processPdGroup(Group pdGroup, GroupOperation.Type opType) {
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
        processPiGroup(piGroup, pdGroup, operation);
    }

    private void processPiGroup(PiActionProfileGroup groupToApply,
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
        Collection<PiActionProfileMember> members = extractAllMemberInstancesOrNull(group);
        if (members == null) {
            return false;
        }
        if (!applyAllMembersOrNothing(members)) {
            return false;
        }
        if (!applyGroup(group, handle)) {
            deleteMembers(handles(members));
            return false;
        }
        return true;
    }

    private boolean applyGroup(PiActionProfileGroup groupToApply, PiActionProfileGroupHandle handle) {
        final TimedEntry<PiActionProfileGroup> groupOnDevice = groupMirror.get(handle);
        final P4RuntimeClient.WriteOperationType opType =
                groupOnDevice == null ? INSERT : MODIFY;
        if (opType.equals(MODIFY) && groupToApply.equals(groupOnDevice.entry())) {
            // Skip writing, group is unchanged.
            return true;
        }
        final boolean success = getFutureWithDeadline(
                client.writeActionProfileGroup(groupToApply, opType, pipeconf),
                "performing action profile group " + opType, false);
        if (success) {
            groupMirror.put(handle, groupToApply);
        }
        return success;
    }

    private boolean deleteGroup(PiActionProfileGroup group, PiActionProfileGroupHandle handle) {
        final boolean success = getFutureWithDeadline(
                client.writeActionProfileGroup(group, DELETE, pipeconf),
                "performing action profile group " + DELETE, false);
        if (success) {
            groupMirror.remove(handle);
        }
        // Orphan members will be removed at the next reconciliation cycle.
        return success;
    }

    private boolean applyAllMembersOrNothing(Collection<PiActionProfileMember> members) {
        Collection<PiActionProfileMember> appliedMembers = applyMembers(members);
        if (appliedMembers.size() == members.size()) {
            return true;
        } else {
            deleteMembers(handles(appliedMembers));
            return false;
        }
    }

    private Collection<PiActionProfileMember> applyMembers(
            Collection<PiActionProfileMember> members) {
        return members.stream()
                .filter(this::applyMember)
                .collect(Collectors.toList());
    }

    private boolean applyMember(PiActionProfileMember memberToApply) {
        // If exists, modify, otherwise insert.
        final PiActionProfileMemberHandle handle = PiActionProfileMemberHandle.of(
                deviceId, memberToApply);
        final TimedEntry<PiActionProfileMember> memberOnDevice = memberMirror.get(handle);
        final P4RuntimeClient.WriteOperationType opType =
                memberOnDevice == null ? INSERT : MODIFY;
        if (opType.equals(MODIFY) && memberToApply.equals(memberOnDevice.entry())) {
            // Skip writing if member is unchanged.
            return true;
        }
        final boolean success = getFutureWithDeadline(
                client.writeActionProfileMembers(
                        singletonList(memberToApply), opType, pipeconf),
                "performing action profile member " + opType, false);
        if (success) {
            memberMirror.put(handle, memberToApply);
        }
        return success;
    }

    private void deleteMembers(Collection<PiActionProfileMemberHandle> handles) {
        // TODO: improve by batching deletes.
        handles.forEach(this::deleteMember);
    }

    private void deleteMember(PiActionProfileMemberHandle handle) {
        final boolean success = getFutureWithDeadline(
                client.removeActionProfileMembers(
                        handle.actionProfileId(),
                        singletonList(handle.memberId()), pipeconf),
                "performing action profile member " + DELETE,
                Collections.emptyList())
                // Successful if the only member passed has been removed.
                .size() == 1;
        if (success) {
            memberMirror.remove(handle);
        }
    }

    private Collection<PiActionProfileMemberHandle> handles(
            Collection<PiActionProfileMember> members) {
        return members.stream()
                .map(m -> PiActionProfileMemberHandle.of(
                        deviceId, m.actionProfile(), m.id()))
                .collect(Collectors.toList());
    }

    private Collection<PiActionProfileMember> extractAllMemberInstancesOrNull(
            PiActionProfileGroup group) {
        final Collection<PiActionProfileMember> instances = group.members().stream()
                .map(PiActionProfileGroup.WeightedMember::instance)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (instances.size() != group.members().size()) {
            log.error("PiActionProfileGroup has {} member references, " +
                              "but only {} instances were found",
                      group.members().size(), instances.size());
            return null;
        }
        return instances;
    }

    enum Operation {
        APPLY, REMOVE
    }
}
