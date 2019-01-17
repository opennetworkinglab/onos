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
import org.onosproject.net.pi.runtime.PiActionProfileMember;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import p4.config.v1.P4InfoOuterClass;
import p4.v1.P4RuntimeOuterClass;
import p4.v1.P4RuntimeOuterClass.ActionProfileMember;

import static org.onosproject.p4runtime.ctl.TableEntryEncoder.decodeActionMsg;
import static org.onosproject.p4runtime.ctl.TableEntryEncoder.encodePiAction;
/**
 * Codec for P4Runtime ActionProfileMember.
 */
final class ActionProfileMemberCodec
        extends AbstractP4RuntimeCodec<PiActionProfileMember, ActionProfileMember> {

    @Override
    public ActionProfileMember encode(PiActionProfileMember piEntity,
                                      PiPipeconf pipeconf,
                                      P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException {
        final ActionProfileMember.Builder actionProfileMemberBuilder =
                ActionProfileMember.newBuilder();
        // Member ID
        actionProfileMemberBuilder.setMemberId(piEntity.id().id());
        // Action profile ID
        P4InfoOuterClass.ActionProfile actionProfile =
                browser.actionProfiles().getByName(piEntity.actionProfile().id());
        final int actionProfileId = actionProfile.getPreamble().getId();
        actionProfileMemberBuilder.setActionProfileId(actionProfileId);
        // Action
        final P4RuntimeOuterClass.Action action = encodePiAction(piEntity.action(), browser);
        actionProfileMemberBuilder.setAction(action);
        return actionProfileMemberBuilder.build();
    }

    @Override
    public PiActionProfileMember decode(ActionProfileMember message,
                                        PiPipeconf pipeconf,
                                        P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException {
        final PiActionProfileId actionProfileId = PiActionProfileId.of(
                browser.actionProfiles()
                        .getById(message.getActionProfileId())
                        .getPreamble()
                        .getName());
        return PiActionProfileMember.builder()
                .forActionProfile(actionProfileId)
                .withId(PiActionProfileMemberId.of(message.getMemberId()))
                .withAction(decodeActionMsg(message.getAction(), browser))
                .build();
    }
}
