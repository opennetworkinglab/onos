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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.GroupId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.PiInstruction;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProgrammable;
import org.onosproject.net.group.GroupStore;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.runtime.PiActionProfileId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupId;
import org.onosproject.net.pi.runtime.PiActionGroupMember;
import org.onosproject.net.pi.runtime.PiActionGroupMemberId;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.net.pi.runtime.PiTableId;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeGroupReference;
import org.onosproject.p4runtime.api.P4RuntimeGroupWrapper;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    private static final int GROUP_ID_MASK = 0xffff;
    public static final KryoNamespace KRYO = new KryoNamespace.Builder()
            .register(KryoNamespaces.API)
            .register(DefaultP4RuntimeGroupCookie.class)
            .build("P4RuntimeGroupProgrammable");

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

    private PiPipelineInterpreter interpreter;

    protected boolean init() {
        if (!setupBehaviour()) {
            return false;
        }
        Device device = deviceService.getDevice(deviceId);
        // Need an interpreter to map the bucket treatment to a PI action
        if (!device.is(PiPipelineInterpreter.class)) {
            log.warn("Can't find interpreter for device {}", device.id());
        } else {
            interpreter = device.as(PiPipelineInterpreter.class);
        }
        return true;
    }

    @Override
    public void performGroupOperation(DeviceId deviceId, GroupOperations groupOps) {
        if (!init()) {
            // Ignore group operation of not initialized.
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

        // Most of this logic can go in a core service, e.g. PiGroupTranslationService
        // From a P4Runtime perspective, we need first to insert members, then the group.
        PiActionGroupId piActionGroupId = PiActionGroupId.of(groupOp.groupId().id());

        PiActionGroup.Builder piActionGroupBuilder = PiActionGroup.builder()
                .withId(piActionGroupId);

        switch (group.type()) {
            case SELECT:
                piActionGroupBuilder.withType(PiActionGroup.Type.SELECT);
                break;
            default:
                log.warn("Group type {} not supported, ignore group {}.", group.type(), groupId);
                return;
        }
        /*
            Problem:
            In P4Runtime, action profiles (i.e. group tables) are specific to one or more tables.
            Mapping of treatments depends on the target table. How do we derive the target table from here?

            Solution:
            - Add table information into app cookie and put into group description
         */
        // TODO: notify group service if we get deserialize error
        DefaultP4RuntimeGroupCookie defaultP4RuntimeGroupCookie = KRYO.deserialize(group.appCookie().key());
        PiTableId piTableId = defaultP4RuntimeGroupCookie.tableId();
        PiActionProfileId piActionProfileId = defaultP4RuntimeGroupCookie.actionProfileId();
        piActionGroupBuilder.withActionProfileId(piActionProfileId);

        List<PiActionGroupMember> members = buildMembers(group, piActionGroupId, piTableId);
        if (members == null) {
            log.warn("Can't build members for group {} on {}", group, device.id());
            return;
        }

        piActionGroupBuilder.addMembers(members);
        PiActionGroup piActionGroup = piActionGroupBuilder.build();

        P4RuntimeGroupReference groupRef =
                new P4RuntimeGroupReference(deviceId, piActionProfileId, piActionGroupId);
        Lock lock = GROUP_LOCKS.computeIfAbsent(groupRef, k -> new ReentrantLock());
        lock.lock();


        try {
            P4RuntimeGroupWrapper oldGroupWrapper = GROUP_STORE.get(groupRef);
            P4RuntimeGroupWrapper newGroupWrapper =
                    new P4RuntimeGroupWrapper(piActionGroup, group, System.currentTimeMillis());
            boolean success;
            switch (groupOp.opType()) {
                case ADD:
                case MODIFY:
                    success = writeGroupToDevice(oldGroupWrapper, piActionGroup, members);
                    if (success) {
                        GROUP_STORE.put(groupRef, newGroupWrapper);
                    }
                    break;
                case DELETE:
                    success = deleteGroupFromDevice(piActionGroup, members);
                    if (success) {
                        GROUP_STORE.remove(groupRef);
                    }
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Installs action group and members to device via client interface.
     *
     * @param oldGroupWrapper old group wrapper for the group; null if not exists
     * @param piActionGroup the action group to be installed
     * @param members members of the action group
     * @return true if install success; false otherwise
     */
    private boolean writeGroupToDevice(P4RuntimeGroupWrapper oldGroupWrapper,
                                       PiActionGroup piActionGroup,
                                       Collection<PiActionGroupMember> members) {
        boolean success = true;
        CompletableFuture<Boolean> writeSuccess;
        if (checkStoreBeforeUpdate && oldGroupWrapper != null &&
                oldGroupWrapper.piActionGroup().equals(piActionGroup)) {
            // Action group already exists, ignore it
            return true;
        }
        if (deleteBeforeUpdate && oldGroupWrapper != null) {
            success = deleteGroupFromDevice(oldGroupWrapper.piActionGroup(),
                                            oldGroupWrapper.piActionGroup().members());
        }
        writeSuccess = client.writeActionGroupMembers(piActionGroup,
                                                      members,
                                                      P4RuntimeClient.WriteOperationType.INSERT,
                                                      pipeconf);
        success = success && completeSuccess(writeSuccess, ACT_GRP_MEMS, INSERT);

        writeSuccess = client.writeActionGroup(piActionGroup,
                                               P4RuntimeClient.WriteOperationType.INSERT,
                                               pipeconf);
        success = success && completeSuccess(writeSuccess, ACT_GRP, INSERT);
        return success;
    }

    private boolean deleteGroupFromDevice(PiActionGroup piActionGroup,
                                          Collection<PiActionGroupMember> members) {
        boolean success;
        CompletableFuture<Boolean> writeSuccess;
        writeSuccess = client.writeActionGroup(piActionGroup,
                                P4RuntimeClient.WriteOperationType.DELETE,
                                pipeconf);
        success = completeSuccess(writeSuccess, ACT_GRP, DELETE);
        writeSuccess = client.writeActionGroupMembers(piActionGroup,
                                       members,
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

    /**
     * Build pi action group members from group.
     *
     * @param group the group
     * @param piActionGroupId the PI action group id of the group
     * @param piTableId the PI table related to the group
     * @return list of PI action group members; null if can't build member list
     */
    private List<PiActionGroupMember> buildMembers(Group group, PiActionGroupId piActionGroupId, PiTableId piTableId) {
        GroupId groupId = group.id();
        ImmutableList.Builder<PiActionGroupMember> membersBuilder = ImmutableList.builder();

        int bucketIdx = 0;
        for (GroupBucket bucket : group.buckets().buckets()) {
            /*
            Problem:
            In P4Runtime action group members, i.e. action buckets, are associated to a numeric ID chosen
            at member insertion time. This ID must be unique for the whole action profile (i.e. the group table in
            OpenFlow). In ONOS, GroupBucket doesn't specify any ID.

            Solutions:
            - Change GroupBucket API to force application wanting to perform group operations to specify a member id.
            - Maintain state to dynamically allocate/deallocate member IDs, e.g. in a dedicated service, or in a
            P4Runtime Group Provider.

            Hack:
            Statically derive member ID by combining groupId and position of the bucket in the list.
             */
            ByteBuffer bb = ByteBuffer.allocate(4)
                    .putShort((short) (piActionGroupId.id() & GROUP_ID_MASK))
                    .putShort((short) bucketIdx);
            bb.rewind();
            int memberId = bb.getInt();

            bucketIdx++;
            PiAction action;
            if (interpreter != null) {
                // if we have interpreter, use interpreter
                try {
                    action = interpreter.mapTreatment(bucket.treatment(), piTableId);
                } catch (PiPipelineInterpreter.PiInterpreterException e) {
                    log.warn("Can't map treatment {} to action due to {}, ignore group {}",
                             bucket.treatment(), e.getMessage(), groupId);
                    return null;
                }
            } else {
                // if we don't have interpreter, accept PiInstruction only
                TrafficTreatment treatment = bucket.treatment();

                if (treatment.allInstructions().size() > 1) {
                    log.warn("Treatment {} has multiple instructions, ignore group {}",
                             treatment, groupId);
                    return null;
                }
                Instruction instruction = treatment.allInstructions().get(0);
                if (instruction.type() != Instruction.Type.PROTOCOL_INDEPENDENT) {
                    log.warn("Instruction {} is not a PROTOCOL_INDEPENDENT type, ignore group {}",
                             instruction, groupId);
                    return null;
                }

                PiInstruction piInstruction = (PiInstruction) instruction;
                if (piInstruction.action().type() != PiTableAction.Type.ACTION) {
                    log.warn("Action {} is not an ACTION type, ignore group {}",
                             piInstruction.action(), groupId);
                    return null;
                }
                action = (PiAction) piInstruction.action();
            }

            PiActionGroupMember member = PiActionGroupMember.builder()
                    .withId(PiActionGroupMemberId.of(memberId))
                    .withAction(action)
                    .withWeight(bucket.weight())
                    .build();

            membersBuilder.add(member);
        }
        return membersBuilder.build();
    }

    @Override
    public Collection<Group> getGroups() {
        if (!init()) {
            return Collections.emptySet();
        }

        Collection<Group> result = Sets.newHashSet();
        Collection<PiActionProfileId> piActionProfileIds = Sets.newHashSet();

        // Collection action profile Ids
        // TODO: find better way to get all action profile ids....
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

    /**
     * P4Runtime app cookie for group.
     */
    public static class DefaultP4RuntimeGroupCookie {
        private PiTableId tableId;
        private PiActionProfileId piActionProfileId;
        private Integer groupId;

        public DefaultP4RuntimeGroupCookie(PiTableId tableId,
                                           PiActionProfileId piActionProfileId,
                                           Integer groupId) {
            this.tableId = tableId;
            this.piActionProfileId = piActionProfileId;
            this.groupId = groupId;
        }

        public PiTableId tableId() {
            return tableId;
        }

        public PiActionProfileId actionProfileId() {
            return piActionProfileId;
        }

        public Integer groupId() {
            return groupId;
        }
    }
}
