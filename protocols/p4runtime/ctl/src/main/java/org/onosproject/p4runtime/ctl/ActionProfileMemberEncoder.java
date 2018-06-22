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

package org.onosproject.p4runtime.ctl;

import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiActionGroupMember;
import org.onosproject.net.pi.runtime.PiActionGroupMemberId;
import p4.config.v1.P4InfoOuterClass;
import p4.v1.P4RuntimeOuterClass;
import p4.v1.P4RuntimeOuterClass.ActionProfileMember;

import static java.lang.String.format;
import static org.onosproject.p4runtime.ctl.TableEntryEncoder.decodeActionMsg;
import static org.onosproject.p4runtime.ctl.TableEntryEncoder.encodePiAction;

/**
 * Encoder/Decoder of action profile member.
 */
final class ActionProfileMemberEncoder {
    private ActionProfileMemberEncoder() {
        // Hide default constructor
    }

    /**
     * Encode a PiActionGroupMember to a ActionProfileMember.
     *
     * @param profileId the PI action group profile ID of members
     * @param member    the member to encode
     * @param pipeconf  the pipeconf, as encode spec
     * @return encoded member
     * @throws P4InfoBrowser.NotFoundException can't find action profile from
     *                                         P4Info browser
     * @throws EncodeException                 can't find P4Info from pipeconf
     */
    static ActionProfileMember encode(PiActionProfileId profileId,
                                      PiActionGroupMember member,
                                      PiPipeconf pipeconf)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        if (browser == null) {
            throw new EncodeException(format("Can't get P4 info browser from pipeconf %s", pipeconf));
        }

        ActionProfileMember.Builder actionProfileMemberBuilder =
                ActionProfileMember.newBuilder();

        // member id
        actionProfileMemberBuilder.setMemberId(member.id().id());

        // action profile id
        P4InfoOuterClass.ActionProfile actionProfile =
                browser.actionProfiles().getByName(profileId.id());

        int actionProfileId = actionProfile.getPreamble().getId();
        actionProfileMemberBuilder.setActionProfileId(actionProfileId);

        // Action
        P4RuntimeOuterClass.Action action = encodePiAction(member.action(), browser);
        actionProfileMemberBuilder.setAction(action);

        return actionProfileMemberBuilder.build();
    }

    /**
     * Decode an action profile member to PI action group member.
     *
     * @param member   the action profile member
     * @param weight   the weight of the member
     * @param pipeconf the pipeconf, as decode spec
     * @return decoded PI action group member
     * @throws P4InfoBrowser.NotFoundException can't find definition of action
     *                                         from P4 info
     * @throws EncodeException                 can't get P4 info browser from
     *                                         pipeconf
     */
    static PiActionGroupMember decode(ActionProfileMember member,
                                      int weight,
                                      PiPipeconf pipeconf)
            throws P4InfoBrowser.NotFoundException, EncodeException {
        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        if (browser == null) {
            throw new EncodeException(format("Can't get P4 info browser from pipeconf %s", pipeconf));
        }
        return PiActionGroupMember.builder().withId(PiActionGroupMemberId.of(member.getMemberId()))
                .withWeight(weight)
                .withAction(decodeActionMsg(member.getAction(), browser))
                .build();
    }
}
