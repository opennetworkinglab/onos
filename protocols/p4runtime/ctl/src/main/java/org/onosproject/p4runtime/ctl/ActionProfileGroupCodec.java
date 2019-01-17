/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.p4runtime.ctl;

import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiActionProfileGroup;
import org.onosproject.net.pi.runtime.PiActionProfileGroupId;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import p4.v1.P4RuntimeOuterClass.ActionProfileGroup;

/**
 * Codec for P4Runtime ActionProfileGroup.
 */
final class ActionProfileGroupCodec
        extends AbstractP4RuntimeCodec<PiActionProfileGroup, ActionProfileGroup> {

    @Override
    public ActionProfileGroup encode(
            PiActionProfileGroup piGroup, PiPipeconf pipeconf, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {

        final int p4ActionProfileId = browser.actionProfiles()
                .getByName(piGroup.actionProfile().id())
                .getPreamble().getId();
        final ActionProfileGroup.Builder msgBuilder = ActionProfileGroup.newBuilder()
                .setGroupId(piGroup.id().id())
                .setActionProfileId(p4ActionProfileId)
                .setMaxSize(piGroup.maxSize());
        piGroup.members().forEach(m -> {
            // TODO: currently we don't set "watch" field
            ActionProfileGroup.Member member = ActionProfileGroup.Member.newBuilder()
                    .setMemberId(m.id().id())
                    .setWeight(m.weight())
                    .build();
            msgBuilder.addMembers(member);
        });

        return msgBuilder.build();
    }

    @Override
    public PiActionProfileGroup decode(
            ActionProfileGroup msg, PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException {

        final PiActionProfileGroup.Builder piGroupBuilder = PiActionProfileGroup.builder()
                .withActionProfileId(PiActionProfileId.of(
                        browser.actionProfiles()
                                .getById(msg.getActionProfileId())
                                .getPreamble().getName()))
                .withId(PiActionProfileGroupId.of(msg.getGroupId()))
                .withMaxSize(msg.getMaxSize());

        msg.getMembersList().forEach(m -> {
            int weight = m.getWeight();
            if (weight < 1) {
                // FIXME: currently PI has a bug which will always return weight 0
                // ONOS won't accept group buckets with weight 0
                log.warn("Decoding ActionProfileGroup with 'weight' " +
                                 "field {}, will set to 1", weight);
                weight = 1;
            }
            piGroupBuilder.addMember(PiActionProfileMemberId.of(
                    m.getMemberId()), weight);
        });
        return piGroupBuilder.build();
    }
}
