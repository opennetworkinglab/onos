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
 *
 */

package org.onosproject.drivers.bmv2;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import org.onosproject.core.GroupId;
import org.onosproject.drivers.bmv2.api.Bmv2DeviceAgent;
import org.onosproject.drivers.bmv2.api.Bmv2PreController;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2PreGroup;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2PreGroupHandle;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.drivers.bmv2.impl.Bmv2PreGroupTranslatorImpl;
import org.onosproject.drivers.bmv2.mirror.Bmv2PreGroupMirror;
import org.onosproject.drivers.p4runtime.P4RuntimeGroupProgrammable;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the group programmable behaviour for BMv2.
 */
public class Bmv2GroupProgrammable extends P4RuntimeGroupProgrammable {

    private static final Logger log = getLogger(Bmv2GroupProgrammable.class);

    private static final int PRE_GROUP_LOCK_EXPIRE_TIME_IN_MIN = 10;

    // Needed to synchronize operations over the same group.
    private static final LoadingCache<Bmv2PreGroupHandle, Lock> PRE_GROUP_LOCKS = CacheBuilder.newBuilder()
            .expireAfterAccess(PRE_GROUP_LOCK_EXPIRE_TIME_IN_MIN, TimeUnit.MINUTES)
            .build(new CacheLoader<Bmv2PreGroupHandle, Lock>() {
                @Override
                public Lock load(Bmv2PreGroupHandle bmv2PreGroupHandle) {
                    return new ReentrantLock();
                }
            });

    private Bmv2PreGroupMirror preGroupMirror;
    private Bmv2PreController bmv2PreController;

    @Override
    protected boolean setupBehaviour() {
        if (!super.setupBehaviour()) {
            return false;
        }

        preGroupMirror = handler().get(Bmv2PreGroupMirror.class);
        bmv2PreController = handler().get(Bmv2PreController.class);

        return getBmv2DeviceAgent() != null
                && preGroupMirror != null
                && bmv2PreController != null;
    }

    @Override
    public void performGroupOperation(DeviceId deviceId,
                                      GroupOperations groupOps) {
        if (!setupBehaviour()) {
            return;
        }
        groupOps.operations().forEach(op -> processGroupOp(deviceId, op));
    }


    /**
     * Fetches all groups of P4Runtime and BMv2 PRE in a device. Combines and returns them respectively.
     *
     * @return all the groups which are managed via both P4Runtime and BMv2 PRE
     */
    @Override
    public Collection<Group> getGroups() {
        //get groups managed via P4Runtime
        Collection<Group> groups = getP4Groups();
        //get groups managed via BMv2 Thrift
        groups.addAll(getPreGroups());
        return ImmutableList.copyOf(groups);
    }

    private Collection<Group> getP4Groups() {
        return super.getGroups();
    }

    /**
     * Returns BMv2 agent associated with a BMv2 device.
     *
     * @return BMv2 agent
     */
    private Bmv2DeviceAgent getBmv2DeviceAgent() {
        return bmv2PreController.getPreClient(deviceId);
    }

    /**
     * Retrieves groups of BMv2 PRE.
     *
     * @return collection of PRE groups
     */
    private Collection<Group> getPreGroups() {
        if (!setupBehaviour()) {
            return Collections.emptyList();
        }
        Bmv2DeviceAgent bmv2DeviceAgent = getBmv2DeviceAgent();

        try {
            return bmv2DeviceAgent.getPreGroups().stream()
                    .map(preGroup -> groupStore.getGroup(deviceId, GroupId.valueOf(preGroup.groupId())))
                    .collect(Collectors.toList());
        } catch (Bmv2RuntimeException e) {
            log.error("Exception while getting Bmv2 PRE groups of {}", deviceId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Checks whether specified group is a PRE group or not.
     *
     * @param group group
     * @return Returns true iff this group is a PRE group; false otherwise.
     */
    private boolean isPreGroup(Group group) {
        return group.type().equals(GroupDescription.Type.ALL);
    }

    /**
     * Makes a decision between two methodologies over group type.
     * A group of ALL type is evaluated by GroupProgrammable of BMv2;
     * it is passed on to GroupProgrammable of P4Runtime otherwise.
     *
     * @param deviceId ID of the device on which the group is being accommodated.
     * @param groupOp  group operation
     */
    private void processGroupOp(DeviceId deviceId, GroupOperation groupOp) {
        final Group group = groupStore.getGroup(deviceId, groupOp.groupId());

        if (isPreGroup(group)) {
            processPreGroupOp(deviceId, groupOp);
        } else {
            //means the group is managed via P4Runtime.
            super.performGroupOperation(deviceId,
                                        new GroupOperations(Arrays.asList(new GroupOperation[]{groupOp})));
        }
    }

    private void processPreGroupOp(DeviceId deviceId, GroupOperation groupOp) {
        if (!setupBehaviour()) {
            return;
        }

        final Group group = groupStore.getGroup(deviceId, groupOp.groupId());

        Bmv2PreGroup preGroup = Bmv2PreGroupTranslatorImpl.translate(group);

        final Bmv2PreGroupHandle handle = Bmv2PreGroupHandle.of(deviceId, preGroup);

        final Bmv2PreGroup groupOnDevice = preGroupMirror.get(handle);

        PRE_GROUP_LOCKS.getUnchecked(handle).lock();
        try {
            switch (groupOp.opType()) {
                case ADD:
                    onAdd(preGroup, handle);
                    break;
                case MODIFY:
                    onModify(preGroup, groupOnDevice, handle);
                    break;
                case DELETE:
                    onDelete(groupOnDevice, handle);
                    break;
                default:
                    log.warn("PRE Group operation {} not supported", groupOp.opType());
            }
        } finally {
            PRE_GROUP_LOCKS.getUnchecked(handle).unlock();
        }
    }

    private void onAdd(Bmv2PreGroup preGroup, Bmv2PreGroupHandle handle) {
        try {
            writeGroup(preGroup, handle);
        } catch (Bmv2RuntimeException e) {
            log.error("Unable to create the PRE group with groupId={}. deviceId={}", preGroup.groupId(), deviceId, e);
        }
    }

    private void onDelete(Bmv2PreGroup preGroupOnDevice, Bmv2PreGroupHandle handle) {
        if (preGroupOnDevice == null) {
            log.warn("Unable to delete the group. Nonexistent in the group mirror! deviceId={}", deviceId);
            return;
        }
        try {
            deleteGroup(preGroupOnDevice, handle);
        } catch (Bmv2RuntimeException e) {
            log.error("Unable to delete the group. deviceId={}", deviceId, e);
        }
    }

    private void onModify(Bmv2PreGroup preGroup, Bmv2PreGroup preGroupOnDevice, Bmv2PreGroupHandle handle) {
        if (preGroupOnDevice == null) {
            log.warn("Unable to modify the group. Nonexistent in the group mirror! deviceId={}", deviceId);
            return;
        }
        if (preGroup.equals(preGroupOnDevice)) {
            return;
        }
        try {
            deleteGroup(preGroupOnDevice, handle);
            writeGroup(preGroup, handle);
        } catch (Bmv2RuntimeException e) {
            log.error("Unable to modify the group. deviceId={}, groupId={}", deviceId, preGroup.groupId(), e);
        }
    }

    private void writeGroup(Bmv2PreGroup preGroup, Bmv2PreGroupHandle handle) throws Bmv2RuntimeException {
        Bmv2DeviceAgent bmv2DeviceAgent = getBmv2DeviceAgent();
        Bmv2PreGroup bmv2PreGroupCreated = bmv2DeviceAgent.writePreGroup(preGroup);
        //put the created group into the mirror store
        preGroupMirror.put(handle, bmv2PreGroupCreated);
    }

    private void deleteGroup(Bmv2PreGroup preGroupOnDevice, Bmv2PreGroupHandle handle) throws Bmv2RuntimeException {
        Bmv2DeviceAgent bmv2DeviceAgent = getBmv2DeviceAgent();
        bmv2DeviceAgent.deletePreGroup(preGroupOnDevice);
        //remove the group from the mirror
        preGroupMirror.remove(handle);
    }

}