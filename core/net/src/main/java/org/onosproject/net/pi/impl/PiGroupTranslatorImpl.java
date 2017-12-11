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

package org.onosproject.net.pi.impl;

import org.onosproject.net.Device;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupId;
import org.onosproject.net.pi.runtime.PiActionGroupMember;
import org.onosproject.net.pi.runtime.PiActionGroupMemberId;
import org.onosproject.net.pi.runtime.PiGroupKey;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.net.pi.service.PiTranslationException;

import java.nio.ByteBuffer;

import static java.lang.String.format;
import static org.onosproject.net.pi.impl.PiFlowRuleTranslatorImpl.translateTreatment;
import static org.onosproject.net.pi.impl.PiUtils.getInterpreterOrNull;
import static org.onosproject.net.pi.runtime.PiTableAction.Type.ACTION;

/**
 * Implementation of group translation logic.
 */
final class PiGroupTranslatorImpl {

    private PiGroupTranslatorImpl() {
        // Hides constructor.
    }

    /**
     * Returns a PI action group equivalent to the given group, for the given pipeconf and device.
     *
     * @param group    group
     * @param pipeconf pipeconf
     * @param device   device
     * @return PI action group
     * @throws PiTranslationException if the group cannot be translated
     */
    static PiActionGroup translate(Group group, PiPipeconf pipeconf, Device device) throws PiTranslationException {

        final PiPipelineInterpreter interpreter = getInterpreterOrNull(device, pipeconf);

        final PiActionGroup.Builder piActionGroupBuilder = PiActionGroup.builder()
                .withId(PiActionGroupId.of(group.id().id()));

        if (!(group.appCookie() instanceof PiGroupKey)) {
            throw new PiTranslationException("Group app cookie is not PI (class should be PiGroupKey)");
        }
        final PiGroupKey groupKey = (PiGroupKey) group.appCookie();

        piActionGroupBuilder.withActionProfileId(groupKey.actionProfileId());

        // Translate group buckets to PI group members
        short bucketIdx = 0;
        for (GroupBucket bucket : group.buckets().buckets()) {
            /*
            FIXME: the way member IDs are computed can cause collisions!
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
                    .putShort((short) (group.id().id() & 0xffff))
                    .putShort(bucketIdx);
            bb.rewind();
            int memberId = bb.getInt();
            bucketIdx++;

            final PiTableAction tableAction = translateTreatment(bucket.treatment(), interpreter, groupKey.tableId(),
                                                                 pipeconf.pipelineModel());

            if (tableAction.type() != ACTION) {
                throw new PiTranslationException(format(
                        "PI table action of type %s is not supported in groups", tableAction.type()));
            }

            piActionGroupBuilder.addMember(PiActionGroupMember.builder()
                                                   .withId(PiActionGroupMemberId.of(memberId))
                                                   .withAction((PiAction) tableAction)
                                                   .withWeight(bucket.weight())
                                                   .build());
        }

        return piActionGroupBuilder.build();
    }
}
