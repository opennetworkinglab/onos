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
import org.onosproject.net.pi.runtime.PiActionProfileMember;
import org.onosproject.net.pi.runtime.PiActionProfileMemberHandle;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.v1.P4RuntimeOuterClass;
import p4.v1.P4RuntimeOuterClass.ActionProfileMember;

import static org.onosproject.p4runtime.ctl.codec.Codecs.CODECS;

/**
 * Codec for ActionProfileMember.
 */
public final class ActionProfileMemberCodec
        extends AbstractEntityCodec<PiActionProfileMember, PiActionProfileMemberHandle, ActionProfileMember, Object> {

    @Override
    public ActionProfileMember encode(
            PiActionProfileMember piEntity, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException {
        return keyMsgBuilder(
                piEntity.actionProfile(), piEntity.id(), browser)
                .setAction(CODECS.action().encode(
                        piEntity.action(), null, pipeconf))
                .build();
    }

    @Override
    protected ActionProfileMember encodeKey(
            PiActionProfileMemberHandle handle, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        return keyMsgBuilder(handle.actionProfileId(), handle.memberId(), browser)
                .build();
    }

    @Override
    protected ActionProfileMember encodeKey(
            PiActionProfileMember piEntity, Object metadata,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        return keyMsgBuilder(
                piEntity.actionProfile(), piEntity.id(), browser)
                .build();
    }

    private P4RuntimeOuterClass.ActionProfileMember.Builder keyMsgBuilder(
            PiActionProfileId actProfId, PiActionProfileMemberId memberId,
            P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        return P4RuntimeOuterClass.ActionProfileMember.newBuilder()
                .setActionProfileId(browser.actionProfiles()
                                            .getByName(actProfId.id())
                                            .getPreamble().getId())
                .setMemberId(memberId.id());
    }

    @Override
    public PiActionProfileMember decode(
            ActionProfileMember message, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, CodecException {
        final PiActionProfileId actionProfileId = PiActionProfileId.of(
                browser.actionProfiles()
                        .getById(message.getActionProfileId())
                        .getPreamble()
                        .getName());
        return PiActionProfileMember.builder()
                .forActionProfile(actionProfileId)
                .withId(PiActionProfileMemberId.of(message.getMemberId()))
                .withAction(CODECS.action().decode(
                        message.getAction(), null, pipeconf))
                .build();
    }
}
