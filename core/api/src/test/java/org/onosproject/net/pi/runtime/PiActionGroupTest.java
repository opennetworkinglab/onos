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

import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.pi.runtime.PiConstantsTest.ACTION_PROF_ID;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DST_ADDR;
import static org.onosproject.net.pi.runtime.PiConstantsTest.MOD_NW_DST;

/**
 * Unit tests for PiActionGroup class.
 */
public class PiActionGroupTest {

    private final PiActionGroupMemberId piActionGroupMemberId = PiActionGroupMemberId.of(10);
    private final PiAction piAction = PiAction.builder().withId(PiActionId.of(MOD_NW_DST))
            .withParameter(new PiActionParam(PiActionParamId.of(DST_ADDR), copyFrom(0x0a010101)))
            .build();

    private final PiActionGroupMember piActionGroupMember = PiActionGroupMember.builder()
            .withId(piActionGroupMemberId)
            .withAction(piAction)
            .withWeight(10)
            .build();
    private PiActionGroupId piActionGroupId = PiActionGroupId.of(10);
    private PiActionGroup piActionGroup1 = PiActionGroup.builder()
            .addMember(piActionGroupMember)
            .withId(piActionGroupId)
            .withActionProfileId(ACTION_PROF_ID)
            .build();

    private PiActionGroup sameAsPiActionGroup1 = PiActionGroup.builder()
            .addMember(piActionGroupMember)
            .withId(piActionGroupId)
            .withActionProfileId(ACTION_PROF_ID)
            .build();

    private PiActionGroupId piActionGroupId2 = PiActionGroupId.of(20);
    private PiActionGroup piActionGroup2 = PiActionGroup.builder()
            .addMember(piActionGroupMember)
            .withId(piActionGroupId2)
            .withActionProfileId(ACTION_PROF_ID)
            .build();

    /**
     * Checks that the PiActionGroup class is immutable.
     */
    @Test
    public void testImmutability() {

        assertThatClassIsImmutable(PiActionGroup.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {

        new EqualsTester()
                .addEqualityGroup(piActionGroup1, sameAsPiActionGroup1)
                .addEqualityGroup(piActionGroup2)
                .testEquals();
    }

    /**
     * Checks the methods of PiActionGroup.
     */
    @Test
    public void testMethods() {

        Collection<PiActionGroupMember> piActionGroupMembers = Lists.newArrayList();

        piActionGroupMembers.add(piActionGroupMember);
        assertThat(piActionGroup1, is(notNullValue()));
        assertThat(piActionGroup1.id(), is(piActionGroupId));
        assertThat("Incorrect members value",
                   CollectionUtils.isEqualCollection(piActionGroup1.members(), piActionGroupMembers));
    }
}
