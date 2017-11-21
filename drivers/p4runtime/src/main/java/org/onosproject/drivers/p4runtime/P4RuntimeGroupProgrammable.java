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
import org.onosproject.core.GroupId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProgrammable;
import org.onosproject.net.group.GroupStore;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupId;
import org.onosproject.net.pi.service.PiTranslationService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeGroupReference;
import org.onosproject.p4runtime.api.P4RuntimeGroupWrapper;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the group programmable behaviour for P4Runtime.
 */
public class P4RuntimeGroupProgrammable extends AbstractP4RuntimeHandlerBehaviour implements GroupProgrammable {
    private static final String ACT_GRP_MEMS = "action group members";
    private static final String DELETE = "delete";
    private static final String ACT_GRP = "action group";
    private static final String INSERT = "insert";
    private static final Logger log = getLogger(P4RuntimeGroupProgrammable.class);

    /*
     * About action groups in P4runtime:
     * The type field is a place holder in p4runtime.proto right now, and we haven't defined it yet. You can assume all
     * the groups are "select" as per the OF spec. As a remainder, in the P4 terminology a member corresponds to an OF
     * bucket. Each member can also be used directly in the match table (kind of like an OF indirect group).
     */

    // TODO: make this attribute configurable by child drivers (e.g. BMv2 or Tofino)
    /*
    When updating an existing rule, if true, we issue a DELETE operation before inserting the new one, otherwise we
    issue a MODIFY operation. This is useful fore devices that do not support MODIFY operations for table entries.
     */
    private boolean deleteBeforeUpdate = true;

    // TODO: can remove this check as soon as the multi-apply-per-same-flow rule bug is fixed.
    /*
    If true, we ignore re-installing rules that are already known in the ENTRY_STORE, i.e. same match key and action.
     */
    private boolean checkStoreBeforeUpdate = true;

    // Needed to synchronize operations over the same group.
    private static final Map<P4RuntimeGroupReference, Lock> GROUP_LOCKS = Maps.newConcurrentMap();

    // TODO: replace with distribute store
    private static final Map<P4RuntimeGroupReference, P4RuntimeGroupWrapper> GROUP_STORE = Maps.newConcurrentMap();

    @Override
    public void performGroupOperation(DeviceId deviceId, GroupOperations groupOps) {
        if (!setupBehaviour()) {
            return;
        }

        Device device = handler().get(DeviceService.class).getDevice(deviceId);

        for (GroupOperation groupOp : groupOps.operations()) {
            processGroupOp(device, groupOp);
        }
    }

    private void processGroupOp(Device device, GroupOperation groupOp) {
        GroupId groupId = groupOp.groupId();
        GroupStore groupStore = handler().get(GroupStore.class);
        Group group = groupStore.getGroup(device.id(), groupId);

        PiActionGroup piActionGroup;
        try {
            piActionGroup = piTranslationService.translate(group, pipeconf);
        } catch (PiTranslationService.PiTranslationException e) {
            log.warn("Unable translate group, aborting group operation {}: {}", groupOp.opType(), e.getMessage());
            return;
        }

        P4RuntimeGroupReference groupRef = new P4RuntimeGroupReference(deviceId, piActionGroup.actionProfileId(),
                                                                       piActionGroup.id());

        Lock lock = GROUP_LOCKS.computeIfAbsent(groupRef, k -> new ReentrantLock());
        lock.lock();

        try {
            P4RuntimeGroupWrapper oldGroupWrapper = GROUP_STORE.get(groupRef);
            P4RuntimeGroupWrapper newGroupWrapper = new P4RuntimeGroupWrapper(piActionGroup, group,
                                                                              System.currentTimeMillis());
            switch (groupOp.opType()) {
                case ADD:
                case MODIFY:
                    if (writeGroupToDevice(oldGroupWrapper, piActionGroup)) {
                        GROUP_STORE.put(groupRef, newGroupWrapper);
                    }
                    break;
                case DELETE:
                    if (deleteGroupFromDevice(piActionGroup)) {
                        GROUP_STORE.remove(groupRef);
                    }
                    break;
                default:
                    log.warn("Group operation {} not supported", groupOp.opType());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Installs action group and members to device via client interface.
     *
     * @param oldGroupWrapper old group wrapper for the group; null if not exists
     * @param piActionGroup   the action group to be installed
     * @return true if install success; false otherwise
     */
    private boolean writeGroupToDevice(P4RuntimeGroupWrapper oldGroupWrapper, PiActionGroup piActionGroup) {
        boolean success = true;
        CompletableFuture<Boolean> writeSuccess;
        if (checkStoreBeforeUpdate && oldGroupWrapper != null &&
                oldGroupWrapper.piActionGroup().equals(piActionGroup)) {
            // Action group already exists, ignore it
            return true;
        }
        if (deleteBeforeUpdate && oldGroupWrapper != null) {
            success = deleteGroupFromDevice(oldGroupWrapper.piActionGroup());
        }
        writeSuccess = client.writeActionGroupMembers(piActionGroup,
                                                      P4RuntimeClient.WriteOperationType.INSERT,
                                                      pipeconf);
        success = success && completeSuccess(writeSuccess, ACT_GRP_MEMS, INSERT);

        writeSuccess = client.writeActionGroup(piActionGroup,
                                               P4RuntimeClient.WriteOperationType.INSERT,
                                               pipeconf);
        success = success && completeSuccess(writeSuccess, ACT_GRP, INSERT);
        return success;
    }

    private boolean deleteGroupFromDevice(PiActionGroup piActionGroup) {
        boolean success;
        CompletableFuture<Boolean> writeSuccess;
        writeSuccess = client.writeActionGroup(piActionGroup,
                                               P4RuntimeClient.WriteOperationType.DELETE,
                                               pipeconf);
        success = completeSuccess(writeSuccess, ACT_GRP, DELETE);
        writeSuccess = client.writeActionGroupMembers(piActionGroup,
                                                      P4RuntimeClient.WriteOperationType.DELETE,
                                                      pipeconf);
        success = success && completeSuccess(writeSuccess, ACT_GRP_MEMS, DELETE);
        return success;
    }

    private boolean completeSuccess(CompletableFuture<Boolean> completableFuture,
                                    String topic, String action) {
        try {
            return completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Can't {} {} due to {}", action, topic, e.getMessage());
            return false;
        }
    }

    @Override
    public Collection<Group> getGroups() {
        if (!setupBehaviour()) {
            return Collections.emptyList();
        }

        Collection<Group> result = Sets.newHashSet();
        Collection<PiActionProfileId> piActionProfileIds = Sets.newHashSet();

        // TODO: find better way to get all action profile ids. e.g. by providing them in the interpreter
        GROUP_STORE.forEach((groupRef, wrapper) -> piActionProfileIds.add(groupRef.actionProfileId()));

        AtomicBoolean success = new AtomicBoolean(true);
        piActionProfileIds.forEach(actionProfileId -> {
            Collection<PiActionGroup> piActionGroups = Sets.newHashSet();
            try {
                Collection<PiActionGroup> groupsFromDevice =
                        client.dumpGroups(actionProfileId, pipeconf).get();
                if (groupsFromDevice == null) {
                    // Got error
                    success.set(false);
                } else {
                    piActionGroups.addAll(groupsFromDevice);
                }
            } catch (ExecutionException | InterruptedException e) {
                log.error("Exception while dumping groups for action profile {}: {}",
                          actionProfileId.id(), deviceId, e);
                success.set(false);
            }

            piActionGroups.forEach(piActionGroup -> {
                PiActionGroupId actionGroupId = piActionGroup.id();
                P4RuntimeGroupReference groupRef =
                        new P4RuntimeGroupReference(deviceId, actionProfileId, actionGroupId);
                P4RuntimeGroupWrapper wrapper = GROUP_STORE.get(groupRef);

                if (wrapper == null) {
                    // group exists in client, but can't find in ONOS
                    log.warn("Can't find action profile group {} from local store.",
                             groupRef);
                    return;
                }
                if (!wrapper.piActionGroup().equals(piActionGroup)) {
                    log.warn("Group from device is different to group from local store.");
                    return;
                }
                result.add(wrapper.group());

            });
        });

        if (!success.get()) {
            // Got error while dump groups from device.
            return Collections.emptySet();
        } else {
            return result;
        }
    }

}
