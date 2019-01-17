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

package org.onosproject.net.pi.runtime;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiActionProfileId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.pi.runtime.PiActionProfileGroup.WeightedMember.DEFAULT_WEIGHT;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DST_ADDR;
import static org.onosproject.net.pi.runtime.PiConstantsTest.MOD_NW_DST;
import static org.onosproject.net.pi.runtime.PiConstantsTest.MOD_VLAN_VID;
import static org.onosproject.net.pi.runtime.PiConstantsTest.VID;

/**
 * Unit tests for PiActionProfileGroup class.
 */
public class PiActionProfileGroupTest {

    private final PiActionProfileId actionProfileId1 = PiActionProfileId.of("foo");
    private final PiActionProfileId actionProfileId2 = PiActionProfileId.of("bar");

    private final PiActionProfileGroupId groupId1 = PiActionProfileGroupId.of(100);
    private final PiActionProfileGroupId groupId2 = PiActionProfileGroupId.of(200);

    private final PiActionProfileMemberId actProfMemberId1 = PiActionProfileMemberId.of(10);
    private final PiActionProfileMemberId actProfMemberId2 = PiActionProfileMemberId.of(20);

    private final PiAction piAction1 = PiAction.builder().withId(PiActionId.of(MOD_NW_DST))
            .withParameter(new PiActionParam(PiActionParamId.of(DST_ADDR), copyFrom(0x0a010101)))
            .build();
    private final PiAction piAction2 = PiAction.builder().withId(PiActionId.of(MOD_VLAN_VID))
            .withParameter(new PiActionParam(PiActionParamId.of(VID), copyFrom(0x0b)))
            .build();

    private final PiActionProfileMember actProfMember11 = PiActionProfileMember.builder()
            .forActionProfile(actionProfileId1)
            .withId(actProfMemberId1)
            .withAction(piAction1)
            .build();
    private final PiActionProfileMember actProfMember12 = PiActionProfileMember.builder()
            .forActionProfile(actionProfileId1)
            .withId(actProfMemberId2)
            .withAction(piAction2)
            .build();
    private final PiActionProfileMember actProfMember21 = PiActionProfileMember.builder()
            .forActionProfile(actionProfileId2)
            .withId(actProfMemberId1)
            .withAction(piAction1)
            .build();
    private final PiActionProfileMember actProfMember22 = PiActionProfileMember.builder()
            .forActionProfile(actionProfileId2)
            .withId(actProfMemberId2)
            .withAction(piAction2)
            .build();

    private final PiActionProfileGroup.WeightedMember weightedMember1 = new PiActionProfileGroup.WeightedMember(
            actProfMemberId1, DEFAULT_WEIGHT);
    private final PiActionProfileGroup.WeightedMember weightedMember2 = new PiActionProfileGroup.WeightedMember(
            actProfMemberId2, DEFAULT_WEIGHT);

    private PiActionProfileGroup group1 = PiActionProfileGroup.builder()
            .withActionProfileId(actionProfileId1)
            // Group members defined with PiActionProfileMember instance.
            .addMember(actProfMember11)
            .addMember(actProfMember12)
            .withId(groupId1)
            .build();

    private PiActionProfileGroup sameAsGroup1 = PiActionProfileGroup.builder()
            .withActionProfileId(actionProfileId1)
            // Group members defined with PiActionProfileMember instance, in
            // different order.
            .addMember(actProfMember12)
            .addMember(actProfMember11)
            .withId(groupId1)
            .build();

    private PiActionProfileGroup sameAsGroup1NoInstance = PiActionProfileGroup.builder()
            .withActionProfileId(actionProfileId1)
            // Group members defined with WeightedMember instances.
            .addMember(weightedMember1)
            .addMember(weightedMember2)
            .withId(groupId1)
            .build();

    private PiActionProfileGroup group2 = PiActionProfileGroup.builder()
            .withActionProfileId(actionProfileId2)
            // Group members defined with PiActionProfileMember instance.
            .addMember(actProfMember21)
            .addMember(actProfMember22)
            .withId(groupId2)
            .build();

    private PiActionProfileGroup sameAsGroup2NoInstance = PiActionProfileGroup.builder()
            .withActionProfileId(actionProfileId2)
            // Members defined by their ID only.
            .addMember(actProfMemberId1)
            .addMember(actProfMemberId2)
            .withId(groupId2)
            .build();

    private PiActionProfileGroup asGroup2WithDifferentWeights = PiActionProfileGroup.builder()
            .withActionProfileId(actionProfileId2)
            // Members defined by their ID only and different weight.
            .addMember(actProfMemberId1, 100)
            .addMember(actProfMemberId2, 100)
            .withId(groupId2)
            .build();

    /**
     * Checks that the PiActionProfileGroup class is immutable.
     */
    @Test
    public void testImmutability() {

        assertThatClassIsImmutable(PiActionProfileGroup.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {

        new EqualsTester()
                .addEqualityGroup(group1, sameAsGroup1, sameAsGroup1NoInstance)
                .addEqualityGroup(group2, sameAsGroup2NoInstance)
                .addEqualityGroup(asGroup2WithDifferentWeights)
                .testEquals();
    }

    /**
     * Checks the methods of PiActionProfileGroup.
     */
    @Test
    public void testMethods() {
        assertThat(group1, is(notNullValue()));
        assertThat(group1.id(), is(groupId1));
        assertThat(group1.actionProfile(), is(actionProfileId1));
        assertThat(group1.members().size(), is(2));
        // Check members (with instance)
        assertThat(group1.members().contains(weightedMember1), is(true));
        assertThat(group1.members().contains(weightedMember2), is(true));
        assertThat(group1.member(actProfMemberId1).isPresent(), is(notNullValue()));
        assertThat(group1.member(actProfMemberId2).isPresent(), is(notNullValue()));
        assertThat(group1.member(actProfMemberId1).get().instance(), is(actProfMember11));
        assertThat(group1.member(actProfMemberId2).get().instance(), is(actProfMember12));
        // Check members (no instance)
        assertThat(sameAsGroup2NoInstance.member(actProfMemberId1).isPresent(), is(true));
        assertThat(sameAsGroup2NoInstance.member(actProfMemberId2).isPresent(), is(true));
        assertThat(sameAsGroup2NoInstance.member(actProfMemberId1).get().instance(), is(nullValue()));
        assertThat(sameAsGroup2NoInstance.member(actProfMemberId2).get().instance(), is(nullValue()));
    }
}
