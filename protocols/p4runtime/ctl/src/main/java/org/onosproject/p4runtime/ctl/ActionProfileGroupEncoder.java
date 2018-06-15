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

import com.google.common.collect.Maps;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupId;
import p4.v1.P4RuntimeOuterClass.ActionProfileGroup;
import p4.v1.P4RuntimeOuterClass.ActionProfileGroup.Member;
import p4.v1.P4RuntimeOuterClass.ActionProfileMember;
import p4.config.v1.P4InfoOuterClass;

import java.util.Collection;
import java.util.Map;

import static java.lang.String.format;

/**
 * Encoder/Decoder for action profile group.
 */
final class ActionProfileGroupEncoder {

    private ActionProfileGroupEncoder() {
        // hide default constructor
    }

    /**
     * Encode a PI action group to a action profile group.
     *
     * @param piActionGroup the action profile group
     * @param pipeconf the pipeconf
     * @return a action profile group encoded from PI action group
     * @throws P4InfoBrowser.NotFoundException if can't find action profile from P4Info browser
     * @throws EncodeException if can't find P4Info from pipeconf
     */
    static ActionProfileGroup encode(PiActionGroup piActionGroup, PiPipeconf pipeconf)
            throws P4InfoBrowser.NotFoundException, EncodeException {
        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        if (browser == null) {
            throw new EncodeException(format("Can't get P4 info browser from pipeconf %s", pipeconf));
        }

        PiActionProfileId piActionProfileId = piActionGroup.actionProfileId();
        P4InfoOuterClass.ActionProfile actionProfile = browser.actionProfiles()
                .getByName(piActionProfileId.id());
        int actionProfileId = actionProfile.getPreamble().getId();
        ActionProfileGroup.Builder actionProfileGroupBuilder = ActionProfileGroup.newBuilder()
                        .setGroupId(piActionGroup.id().id())
                        .setActionProfileId(actionProfileId);

        piActionGroup.members().forEach(m -> {
            // TODO: currently we don't set "watch" field of member
            Member member = Member.newBuilder()
                    .setMemberId(m.id().id())
                    .setWeight(m.weight())
                    .build();
            actionProfileGroupBuilder.addMembers(member);
        });

        actionProfileGroupBuilder.setMaxSize(piActionGroup.members().size());

        return actionProfileGroupBuilder.build();
    }

    /**
     * Decode an action profile group with members information to a PI action group.
     *
     * @param actionProfileGroup the action profile group
     * @param members members of the action profile group
     * @param pipeconf the pipeconf
     * @return decoded PI action group
     * @throws P4InfoBrowser.NotFoundException if can't find action profile from P4Info browser
     * @throws EncodeException if can't find P4Info from pipeconf
     */
    static PiActionGroup decode(ActionProfileGroup actionProfileGroup,
                                Collection<ActionProfileMember> members,
                                PiPipeconf pipeconf)
            throws P4InfoBrowser.NotFoundException, EncodeException {
        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        if (browser == null) {
            throw new EncodeException(format("Can't get P4 info browser from pipeconf %s", pipeconf));
        }
        PiActionGroup.Builder piActionGroupBuilder = PiActionGroup.builder();

        P4InfoOuterClass.ActionProfile actionProfile = browser.actionProfiles()
                .getById(actionProfileGroup.getActionProfileId());
        PiActionProfileId piActionProfileId = PiActionProfileId.of(actionProfile.getPreamble().getName());

        piActionGroupBuilder
                .withActionProfileId(piActionProfileId)
                .withId(PiActionGroupId.of(actionProfileGroup.getGroupId()));

        Map<Integer, Integer> memberWeights = Maps.newHashMap();
        actionProfileGroup.getMembersList().forEach(member -> {
            int weight = member.getWeight();
            if (weight < 1) {
                // FIXME: currently PI has a bug which will always return weight 0
                // ONOS won't accept group member with weight 0
                weight = 1;
            }
            memberWeights.put(member.getMemberId(), weight);
        });

        for (ActionProfileMember member : members) {
            if (!memberWeights.containsKey(member.getMemberId())) {
                // Not a member of this group, ignore.
                continue;
            }
            int weight = memberWeights.get(member.getMemberId());
            piActionGroupBuilder.addMember(ActionProfileMemberEncoder.decode(member, weight, pipeconf));
        }

        return piActionGroupBuilder.build();
    }
}
