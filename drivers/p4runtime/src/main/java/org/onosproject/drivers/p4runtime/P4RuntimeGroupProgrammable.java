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

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProgrammable;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupId;
import org.onosproject.net.pi.runtime.PiActionGroupMember;
import org.onosproject.net.pi.runtime.PiActionGroupMemberId;
import org.onosproject.net.pi.runtime.PiTableId;

import java.nio.ByteBuffer;

public class P4RuntimeGroupProgrammable extends AbstractHandlerBehaviour implements GroupProgrammable {

    /*
    Work in progress.
     */

    private Device device;

    /*
    About action groups in P4runtime:
    The type field is a place holder in p4runtime.proto right now, and we haven't defined it yet. You can assume all
    the groups are "select" as per the OF spec. As a remainder, in the P4 terminology a member corresponds to an OF
    bucket. Each member can also be used directly in the match table (kind of like an OF indirect group).
     */

    @Override
    public void performGroupOperation(DeviceId deviceId, GroupOperations groupOps) {

        for (GroupOperation groupOp : groupOps.operations()) {
            switch (groupOp.opType()) {
                case ADD:
                    addGroup(deviceId, groupOp);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private void addGroup(DeviceId deviceId, GroupOperation groupOp) {

        // Most of this logic can go in a core service, e.g. PiGroupTranslationService

        // From a P4Runtime perspective, we need first to insert members, then the group.

        PiActionGroupId piActionGroupId = PiActionGroupId.of(groupOp.groupId().id());

        PiActionGroup.Builder piActionGroupBuilder = PiActionGroup.builder()
                .withId(piActionGroupId)
                .withType(PiActionGroup.Type.SELECT);

        if (groupOp.groupType() != GroupDescription.Type.SELECT) {
            // log error
        }

        int bucketIdx = 0;
        for (GroupBucket bucket : groupOp.buckets().buckets()) {
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
            int memberId = ByteBuffer.allocate(4)
                    .putShort((short) (piActionGroupId.id() % 2 ^ 16))
                    .putShort((short) (bucketIdx % 2 ^ 16))
                    .getInt();

            // Need an interpreter to map the bucket treatment to a PI action

            if (!device.is(PiPipelineInterpreter.class)) {
                // log error
            }

            PiPipelineInterpreter interpreter = device.as(PiPipelineInterpreter.class);

            /*
            Problem:
            In P4Runtime, action profiles (i.e. group tables) are specific to one or more tables.
            Mapping of treatments depends on the target table. How do we derive the target table from here?

            Solution:
            - Change GroupDescription to allow applications to specify a table where this group will be called from.

            Hack:
            Assume we support pipelines with only one action profile associated to only one table, i.e. derive the
            table ID by looking at the P4Info.
             */

            PiTableId piTableId = PiTableId.of("derive from P4Info");


            PiAction action = null;
            try {
                action = interpreter.mapTreatment(bucket.treatment(), piTableId);
            } catch (PiPipelineInterpreter.PiInterpreterException e) {
                // log error
            }

            PiActionGroupMember member = PiActionGroupMember.builder()
                    .withId(PiActionGroupMemberId.of(memberId))
                    .withAction(action)
                    .withWeight(bucket.weight())
                    .build();

            piActionGroupBuilder.addMember(member);

            // Use P4RuntimeClient to install member;
            // TODO: implement P4RuntimeClient method.
        }

        PiActionGroup piActionGroup = piActionGroupBuilder.build();

        // Use P4RuntimeClient to insert group.
        // TODO: implement P4RuntimeClient method.
    }
}
