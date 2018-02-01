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
import org.onosproject.net.pi.service.PiGroupTranslator;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.net.pi.service.PiTranslationException;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.DELETE;
import static org.onosproject.p4runtime.api.P4RuntimeClient.WriteOperationType.INSERT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the group programmable behaviour for P4Runtime.
 */
public class P4RuntimeGroupProgrammable
        extends AbstractP4RuntimeHandlerBehaviour
        implements GroupProgrammable {

    private enum Operation {
        APPLY, REMOVE
    }

    private static final String ACT_GRP_MEMS_STR = "action group members";
    private static final String DELETE_STR = "delete";
    private static final String ACT_GRP_STR = "action group";
    private static final String INSERT_STR = "insert";

    private static final Logger log = getLogger(P4RuntimeGroupProgrammable.class);

    // If true, we ignore re-installing groups that are already known in the
    // device mirror.
    private boolean checkMirrorBeforeUpdate = true;

    // If true, we avoid querying the device and return what's already known by
    // the ONOS store.
    private boolean ignoreDeviceWhenGet = true;

    private GroupStore groupStore;
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
        if (!ignoreDeviceWhenGet) {
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
            final Operation operation;
            switch (groupOp.opType()) {
                case ADD:
                case MODIFY:
                    operation = Operation.APPLY;
                    break;
                case DELETE:
                    operation = Operation.REMOVE;
                    break;
                default:
                    log.warn("Group operation {} not supported", groupOp.opType());
                    return;
            }
            processPiGroup(handle, piGroup,
                           groupOnDevice, pdGroup, operation);
        } finally {
            lock.unlock();
        }
    }

    private void processPiGroup(PiActionGroupHandle handle,
                                PiActionGroup groupToApply,
                                PiActionGroup groupOnDevice,
                                Group pdGroup, Operation operation) {
        if (operation == Operation.APPLY) {
            if (groupOnDevice != null) {
                if (checkMirrorBeforeUpdate
                        && groupOnDevice.equals(groupToApply)) {
                    // Group on device has the same members, ignore operation.
                    return;
                }
                // Remove before adding it.
                processPiGroup(handle, groupToApply, groupOnDevice,
                               pdGroup, Operation.REMOVE);
            }
            if (writeGroupToDevice(groupToApply)) {
                groupMirror.put(handle, groupToApply);
                translator.learn(handle, new PiTranslatedEntity<>(
                        pdGroup, groupToApply, handle));
            }
        } else {
            if (deleteGroupFromDevice(groupToApply)) {
                groupMirror.remove(handle);
                translator.forget(handle);
            }
        }
    }

    private boolean writeGroupToDevice(PiActionGroup groupToApply) {
        // First insert members, then group.
        // The operation is deemed successful if both operations are successful.
        // FIXME: add transactional semantics, i.e. remove members if group fails.
        final boolean membersSuccess = completeFuture(
                client.writeActionGroupMembers(groupToApply, INSERT, pipeconf),
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
                client.writeActionGroupMembers(piActionGroup, DELETE, pipeconf),
                ACT_GRP_MEMS_STR, DELETE_STR);
    }

    private boolean completeFuture(CompletableFuture<Boolean> completableFuture,
                                   String topic, String action) {
        try {
            if (completableFuture.get()) {
                return true;
            } else {
                log.warn("Unable to {} {}", action, topic);
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Exception while performing {} {}: {}", action, topic, e.getMessage());
            log.debug("Exception", e);
            return false;
        }
    }

    private Stream<Group> streamGroupsFromDevice(PiActionProfileId actProfId) {
        try {
            // Read PI groups and return original PD one.
            return client.dumpGroups(actProfId, pipeconf).get().stream()
                    .map(this::forgeGroupEntry)
                    .filter(Objects::nonNull);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Exception while dumping groups from action profile '{}' on {}: {}",
                      actProfId.id(), deviceId, e);
            return Stream.empty();
        }
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
