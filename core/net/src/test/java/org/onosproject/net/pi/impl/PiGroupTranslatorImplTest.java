/*
 * Copyright 2018-present Open Networking Foundation
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionProfileGroup;
import org.onosproject.net.pi.runtime.PiActionProfileMember;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import org.onosproject.net.pi.runtime.PiGroupKey;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.pipelines.basic.PipeconfLoader;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.group.GroupDescription.Type.SELECT;
import static org.onosproject.pipelines.basic.BasicConstants.ACT_PRF_WCMP_SELECTOR_ID;
import static org.onosproject.pipelines.basic.BasicConstants.ACT_PRM_PORT_ID;
import static org.onosproject.pipelines.basic.BasicConstants.ACT_SET_EGRESS_PORT_WCMP_ID;
import static org.onosproject.pipelines.basic.BasicConstants.PORT_BITWIDTH;
import static org.onosproject.pipelines.basic.BasicConstants.TBL_WCMP_TABLE_ID;

/**
 * Test for {@link PiGroupTranslatorImpl}.
 */
public class PiGroupTranslatorImplTest {

    private static final DeviceId DEVICE_ID = DeviceId.deviceId("device:dummy:1");
    private static final ApplicationId APP_ID = TestApplicationId.create("dummy");
    private static final GroupId GROUP_ID = GroupId.valueOf(1);
    private static final PiGroupKey GROUP_KEY = new PiGroupKey(
            TBL_WCMP_TABLE_ID, ACT_PRF_WCMP_SELECTOR_ID, GROUP_ID.id());
    private static final List<GroupBucket> BUCKET_LIST = ImmutableList.of(
            selectOutputBucket(1),
            selectOutputBucket(2),
            selectOutputBucket(3));
    private static final GroupBuckets BUCKETS = new GroupBuckets(BUCKET_LIST);
    private static final GroupDescription SELECT_GROUP_DESC = new DefaultGroupDescription(
            DEVICE_ID, SELECT, BUCKETS, GROUP_KEY, GROUP_ID.id(), APP_ID);
    private static final Group SELECT_GROUP = new DefaultGroup(GROUP_ID, SELECT_GROUP_DESC);
    private static final int DEFAULT_MEMBER_WEIGHT = 1;
    private static final int BASE_MEM_ID = 65535;
    private Collection<PiActionProfileMember> expectedMembers;

    private PiPipeconf pipeconf;

    @Before
    public void setUp() throws Exception {
        pipeconf = PipeconfLoader.BASIC_PIPECONF;
        expectedMembers = ImmutableSet.of(outputMember(1),
                                          outputMember(2),
                                          outputMember(3));
    }

    private static GroupBucket selectOutputBucket(int portNum) {
        ImmutableByteSequence paramVal = copyFrom(portNum);
        PiActionParam param = new PiActionParam(ACT_PRM_PORT_ID, paramVal);
        PiTableAction action = PiAction.builder().withId(ACT_SET_EGRESS_PORT_WCMP_ID).withParameter(param).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .add(Instructions.piTableAction(action))
                .build();
        return DefaultGroupBucket.createSelectGroupBucket(treatment);
    }

    private static PiActionProfileMember outputMember(int portNum)
            throws ImmutableByteSequence.ByteSequenceTrimException {
        PiActionParam param = new PiActionParam(ACT_PRM_PORT_ID, copyFrom(portNum).fit(PORT_BITWIDTH));
        PiAction piAction = PiAction.builder()
                .withId(ACT_SET_EGRESS_PORT_WCMP_ID)
                .withParameter(param).build();
        return PiActionProfileMember.builder()
                .forActionProfile(ACT_PRF_WCMP_SELECTOR_ID)
                .withAction(piAction)
                .withId(PiActionProfileMemberId.of(BASE_MEM_ID + portNum))
                .withWeight(DEFAULT_MEMBER_WEIGHT)
                .build();
    }

    /**
     * Test add group with buckets.
     */
    @Test
    public void testTranslateGroups() throws Exception {

        PiActionProfileGroup piGroup1 = PiGroupTranslatorImpl.translate(SELECT_GROUP, pipeconf, null);
        PiActionProfileGroup piGroup2 = PiGroupTranslatorImpl.translate(SELECT_GROUP, pipeconf, null);

        new EqualsTester()
                .addEqualityGroup(piGroup1, piGroup2)
                .testEquals();

        assertThat("Group ID must be equal",
                   piGroup1.id().id(), is(equalTo(GROUP_ID.id())));
        assertThat("Action profile ID must be equal",
                   piGroup1.actionProfileId(), is(equalTo(ACT_PRF_WCMP_SELECTOR_ID)));

        // members installed
        Collection<PiActionProfileMember> members = piGroup1.members();
        assertThat("The number of group members must be equal",
                   piGroup1.members().size(), is(expectedMembers.size()));
        assertThat("Group members must be equal",
                   members.containsAll(expectedMembers) && expectedMembers.containsAll(members));
    }
}
