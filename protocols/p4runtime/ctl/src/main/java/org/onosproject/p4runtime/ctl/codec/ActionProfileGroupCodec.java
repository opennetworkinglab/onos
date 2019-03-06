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

package org.onosproject.p4runtime.ctl.codec;

import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiActionProfileGroup;
import org.onosproject.net.pi.runtime.PiActionProfileGroupHandle;
import org.onosproject.net.pi.runtime.PiActionProfileGroupId;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.v1.P4RuntimeOuterClass.ActionProfileGroup;

/**
 * Codec for P4Runtime ActionProfileGroup.
 */
public final class ActionProfileGroupCodec
        extends AbstractEntityCodec<PiActionProfileGroup, PiActionProfileGroupHandle, ActionProfileGroup, Object> {

    @Override
    public ActionProfileGroup encode(
            PiActionProfileGroup piGroup, Object ignored, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        final ActionProfileGroup.Builder msgBuilder = keyMsgBuilder(
                piGroup.actionProfile(), piGroup.id(), browser)
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
    protected ActionProfileGroup encodeKey(
            PiActionProfileGroupHandle handle, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        return keyMsgBuilder(handle.actionProfile(), handle.groupId(), browser)
                .build();
    }

    @Override
    protected ActionProfileGroup encodeKey(
            PiActionProfileGroup piEntity, Object metadata,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        return keyMsgBuilder(piEntity.actionProfile(), piEntity.id(), browser)
                .build();
    }

    private ActionProfileGroup.Builder keyMsgBuilder(
            PiActionProfileId actProfId, PiActionProfileGroupId groupId,
            P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        return ActionProfileGroup.newBuilder()
                .setGroupId(groupId.id())
                .setActionProfileId(browser.actionProfiles()
                                            .getByName(actProfId.id())
                                            .getPreamble().getId());
    }

    @Override
    public PiActionProfileGroup decode(
            ActionProfileGroup msg, Object ignored, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        final PiActionProfileGroup.Builder piGroupBuilder = PiActionProfileGroup.builder()
                .withActionProfileId(PiActionProfileId.of(
                        browser.actionProfiles()
                                .getById(msg.getActionProfileId())
                                .getPreamble().getName()))
                .withId(PiActionProfileGroupId.of(msg.getGroupId()))
                .withMaxSize(msg.getMaxSize());
        msg.getMembersList().forEach(m -> {
            final int weight;
            if (m.getWeight() < 1) {
                log.warn("Decoding group with invalid weight '{}', will set to 1",
                         m.getWeight());
                weight = 1;
            } else {
                weight = m.getWeight();
            }
            piGroupBuilder.addMember(
                PiActionProfileMemberId.of(m.getMemberId()), weight);
        });
        return piGroupBuilder.build();
    }
}
