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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeGroupMirror;
import org.onosproject.drivers.p4runtime.mirror.TimedEntry;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProgrammable;
import org.onosproject.net.group.GroupStore;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiActionProfileModel;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupHandle;
import org.onosproject.net.pi.runtime.PiActionGroupMember;
import org.onosproject.net.pi.service.PiGroupTranslator;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.net.pi.service.PiTranslationException;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.DELETE;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.INSERT;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.MODIFY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the group programmable behaviour for P4Runtime.
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
    private static final String IGNORE_DEVICE_WHEN_GET = "ignoreDeviceWhenGet";
    private static final boolean DEFAULT_IGNORE_DEVICE_WHEN_GET = false;

    protected GroupStore groupStore;
    private P4RuntimeGroupMirror groupMirror;
    private PiGroupTranslator translator;

    // Needed to synchronize operations over the same group.
    private static final Map<PiActionGroupHandle, Lock> GROUP_LOCKS =
            Maps.newConcurrentMap();

    @Override
    protected boolean setupBehaviour() {
        if (!super.setupBehaviour()) {
            return false;
        }
        groupMirror = this.handler().get(P4RuntimeGroupMirror.class);
        groupStore = handler().get(GroupStore.class);
        translator = piTranslationService.groupTranslator();
        return true;
    }

    @Override
    public void performGroupOperation(DeviceId deviceId,
                                      GroupOperations groupOps) {
        if (!setupBehaviour()) {
            return;
        }
        groupOps.operations().forEach(op -> processGroupOp(deviceId, op));
    }

    @Override
    public Collection<Group> getGroups() {
        if (!setupBehaviour()) {
            return Collections.emptyList();
        }
        if (!driverBoolProperty(IGNORE_DEVICE_WHEN_GET, DEFAULT_IGNORE_DEVICE_WHEN_GET)) {
            return pipeconf.pipelineModel().actionProfiles().stream()
                    .map(PiActionProfileModel::id)
                    .flatMap(this::streamGroupsFromDevice)
                    .collect(Collectors.toList());
        } else {
            return groupMirror.getAll(deviceId).stream()
                    .map(TimedEntry::entry)
                    .map(this::forgeGroupEntry)
                    .collect(Collectors.toList());
        }
    }

    private void processGroupOp(DeviceId deviceId, GroupOperation groupOp) {
        final Group pdGroup = groupStore.getGroup(deviceId, groupOp.groupId());

        final PiActionGroup piGroup;
        try {
            piGroup = translator.translate(pdGroup, pipeconf);
        } catch (PiTranslationException e) {
            log.warn("Unable translate group, aborting {} operation: {}",
                     groupOp.opType(), e.getMessage());
            return;
        }

        final PiActionGroupHandle handle = PiActionGroupHandle.of(deviceId, piGroup);

        final PiActionGroup groupOnDevice = groupMirror.get(handle) == null
                ? null
                : groupMirror.get(handle).entry();

        final Lock lock = GROUP_LOCKS.computeIfAbsent(handle, k -> new ReentrantLock());
        lock.lock();
        try {
            processPiGroup(handle, piGroup,
                           groupOnDevice, pdGroup, groupOp.opType());
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
                translator.learn(handle, new PiTranslatedEntity<>(
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
                translator.learn(handle,
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
                translator.forget(handle);
            }
        }
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

    private Stream<Group> streamGroupsFromDevice(PiActionProfileId actProfId) {
        // Read PI groups and return original PD one.
        Collection<PiActionGroup> groups = getFutureWithDeadline(
                client.dumpGroups(actProfId, pipeconf),
                "dumping groups", Collections.emptyList());
        return groups.stream()
                .map(this::forgeGroupEntry)
                .filter(Objects::nonNull);
    }

    private Group forgeGroupEntry(PiActionGroup piGroup) {
        final PiActionGroupHandle handle = PiActionGroupHandle.of(deviceId, piGroup);
        if (!translator.lookup(handle).isPresent()) {
            log.warn("Missing PI group from translation store: {} - {}:{}",
                     pipeconf.id(), piGroup.actionProfileId(),
                     piGroup.id());
            return null;
        }
        final long life = groupMirror.get(handle) != null
                ? groupMirror.get(handle).lifeSec() : 0;
        final Group original = translator.lookup(handle).get().original();
        final DefaultGroup forgedGroup = new DefaultGroup(original.id(), original);
        forgedGroup.setState(Group.GroupState.ADDED);
        forgedGroup.setLife(life);
        return forgedGroup;
    }
}
