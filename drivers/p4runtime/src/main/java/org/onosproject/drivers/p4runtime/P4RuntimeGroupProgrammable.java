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
import com.google.common.collect.Lists;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProgrammable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of GroupProgrammable for P4Runtime devices that uses two
 * different implementation of the same behavior to handle both action profile
 * groups and PRE entries.
 */
public class P4RuntimeGroupProgrammable
        extends AbstractHandlerBehaviour implements GroupProgrammable {

    private final Logger log = getLogger(this.getClass());

    private void doPerformGroupOperation(DeviceId deviceId, GroupOperations groupOps) {
        // TODO: fix GroupProgrammable API, passing the device ID is ambiguous
        checkArgument(deviceId.equals(data().deviceId()),
                      "passed deviceId must be the same assigned to this behavior");
        final List<GroupOperation> actionGroups = Lists.newArrayList();
        final List<GroupOperation> preGroups = Lists.newArrayList();
        groupOps.operations().forEach(op -> {
            switch (op.groupType()) {
                case INDIRECT:
                case SELECT:
                    actionGroups.add(op);
                    break;
                case ALL:
                case CLONE:
                    preGroups.add(op);
                    break;
                case FAILOVER:
                default:
                    log.warn("{} group type not supported [{}]", op.groupType(), op);
            }
        });
        if (!actionGroups.isEmpty()) {
            actionProgrammable().performGroupOperation(
                    deviceId, new GroupOperations(actionGroups));
        }
        if (!preGroups.isEmpty()) {
            replicationProgrammable().performGroupOperation(
                    deviceId, new GroupOperations(preGroups));
        }
    }

    private Collection<Group> doGetGroups() {
        return new ImmutableList.Builder<Group>()
                .addAll(actionProgrammable().getGroups())
                .addAll(replicationProgrammable().getGroups())
                .build();
    }

    private P4RuntimeActionGroupProgrammable actionProgrammable() {
        P4RuntimeActionGroupProgrammable prog = new P4RuntimeActionGroupProgrammable();
        prog.setData(data());
        prog.setHandler(handler());
        return prog;
    }

    private P4RuntimeReplicationGroupProgrammable replicationProgrammable() {
        P4RuntimeReplicationGroupProgrammable prog = new P4RuntimeReplicationGroupProgrammable();
        prog.setData(data());
        prog.setHandler(handler());
        return prog;
    }

    @Override
    public void performGroupOperation(DeviceId deviceId, GroupOperations groupOps) {
        try {
            doPerformGroupOperation(deviceId, groupOps);
        } catch (Throwable ex) {
            log.error("Unhandled exception on performGroupOperation", ex);
        }
    }

    @Override
    public Collection<Group> getGroups() {
        try {
            return doGetGroups();
        } catch (Throwable ex) {
            log.error("Unhandled exception on getGroups", ex);
            return Collections.emptyList();
        }
    }
}
