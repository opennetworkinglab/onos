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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionProfileGroup;
import org.onosproject.net.pi.runtime.PiActionProfileMember;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import org.onosproject.net.pi.runtime.PiGroupKey;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.net.pi.service.PiTranslationException;
import org.onosproject.p4runtime.model.P4InfoParser;
import org.onosproject.p4runtime.model.P4InfoParserException;
import org.onosproject.pipelines.basic.PipeconfLoader;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.group.GroupDescription.Type.SELECT;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.P4_INFO_TEXT;
import static org.onosproject.net.pi.runtime.PiActionProfileGroup.WeightedMember.DEFAULT_WEIGHT;
import static org.onosproject.pipelines.basic.BasicConstants.INGRESS_WCMP_CONTROL_SET_EGRESS_PORT;
import static org.onosproject.pipelines.basic.BasicConstants.INGRESS_WCMP_CONTROL_WCMP_SELECTOR;
import static org.onosproject.pipelines.basic.BasicConstants.INGRESS_WCMP_CONTROL_WCMP_TABLE;
import static org.onosproject.pipelines.basic.BasicConstants.PORT;

/**
 * Test for {@link PiGroupTranslatorImpl}.
 */
public class PiGroupTranslatorImplTest {

    private static final DeviceId DEVICE_ID = DeviceId.deviceId("device:dummy:1");
    private static final ApplicationId APP_ID = TestApplicationId.create("dummy");
    private static final GroupId GROUP_ID = GroupId.valueOf(1);
    private static final PiGroupKey GROUP_KEY = new PiGroupKey(
            INGRESS_WCMP_CONTROL_WCMP_TABLE, INGRESS_WCMP_CONTROL_WCMP_SELECTOR, GROUP_ID.id());
    private static final List<GroupBucket> BUCKET_LIST = ImmutableList.of(
            selectOutputBucket(1),
            selectOutputBucket(2),
            selectOutputBucket(3));
    private static final GroupBuckets BUCKETS = new GroupBuckets(BUCKET_LIST);
    private static final GroupDescription SELECT_GROUP_DESC = new DefaultGroupDescription(
            DEVICE_ID, SELECT, BUCKETS, GROUP_KEY, GROUP_ID.id(), APP_ID);
    private static final Group SELECT_GROUP = new DefaultGroup(GROUP_ID, SELECT_GROUP_DESC);
    private static final int DEFAULT_MEMBER_WEIGHT = 1;
    private static final int BASE_MEM_ID = 991;
    private static final int PORT_BITWIDTH = 9;
    private Collection<PiActionProfileMember> expectedMemberInstances;
    private Collection<PiActionProfileGroup.WeightedMember> expectedWeightedMembers;

    private PiPipeconf pipeconf;

    // Derived from basic.p4info, with wcmp_table annotated with @oneshot
    private static final String PATH_ONESHOT_P4INFO = "oneshot.p4info";
    private static final PiPipeconfId ONE_SHOT_PIPECONF_ID = new PiPipeconfId("org.onosproject.pipelines.wcmp_oneshot");
    private PiPipeconf pipeconfOneShot;

    @Before
    public void setUp() throws Exception {
        pipeconf = PipeconfLoader.BASIC_PIPECONF;
        pipeconfOneShot = loadP4InfoPipeconf(ONE_SHOT_PIPECONF_ID, PATH_ONESHOT_P4INFO);
        expectedMemberInstances = ImmutableSet.of(outputMember(1),
                                                  outputMember(2),
                                                  outputMember(3));
        expectedWeightedMembers = expectedMemberInstances.stream()
                .map(m -> new PiActionProfileGroup.WeightedMember(m, DEFAULT_WEIGHT))
                .collect(Collectors.toSet());

    }

    private static GroupBucket selectOutputBucket(int portNum) {
        ImmutableByteSequence paramVal = copyFrom(portNum);
        PiActionParam param = new PiActionParam(PORT, paramVal);
        PiTableAction action = PiAction.builder()
                .withId(INGRESS_WCMP_CONTROL_SET_EGRESS_PORT)
                .withParameter(param).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .add(Instructions.piTableAction(action))
                .build();
        return DefaultGroupBucket.createSelectGroupBucket(treatment);
    }

    private static PiActionProfileMember outputMember(int portNum)
            throws ImmutableByteSequence.ByteSequenceTrimException {
        PiActionParam param = new PiActionParam(PORT, copyFrom(portNum).fit(PORT_BITWIDTH));
        PiAction piAction = PiAction.builder()
                .withId(INGRESS_WCMP_CONTROL_SET_EGRESS_PORT)
                .withParameter(param).build();
        return PiActionProfileMember.builder()
                .forActionProfile(INGRESS_WCMP_CONTROL_WCMP_SELECTOR)
                .withAction(piAction)
                .withId(PiActionProfileMemberId.of(BASE_MEM_ID + portNum))
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
                   piGroup1.actionProfile(), is(equalTo(INGRESS_WCMP_CONTROL_WCMP_SELECTOR)));

        // members installed
        Collection<PiActionProfileGroup.WeightedMember> weightedMembers = piGroup1.members();
        Collection<PiActionProfileMember> memberInstances = weightedMembers.stream()
                .map(PiActionProfileGroup.WeightedMember::instance)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        assertThat("The number of group members must be equal",
                   piGroup1.members().size(), is(expectedWeightedMembers.size()));
        assertThat("Group weighted members must be equal",
                   weightedMembers.containsAll(expectedWeightedMembers)
                           && expectedWeightedMembers.containsAll(weightedMembers));
        assertThat("Group member instances must be equal",
                   memberInstances.containsAll(expectedMemberInstances)
                           && expectedMemberInstances.containsAll(memberInstances));

    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Test add group with buckets.
     */
    @Test
    public void testTranslateGroupsOneShotError() throws Exception {
        thrown.expect(PiTranslationException.class);
        thrown.expectMessage(format("Table associated to action profile '%s' " +
                                            "supports only one-shot action profile programming",
                                    INGRESS_WCMP_CONTROL_WCMP_SELECTOR.id()));
        PiGroupTranslatorImpl.translate(SELECT_GROUP, pipeconfOneShot, null);
    }

    private static PiPipeconf loadP4InfoPipeconf(PiPipeconfId pipeconfId, String p4infoPath) {
        final URL p4InfoUrl = PiGroupTranslatorImpl.class.getResource(p4infoPath);
        final PiPipelineModel pipelineModel;
        try {
            pipelineModel = P4InfoParser.parse(p4InfoUrl);
        } catch (P4InfoParserException e) {
            throw new IllegalStateException(e);
        }
        return DefaultPiPipeconf.builder()
                .withId(pipeconfId)
                .withPipelineModel(pipelineModel)
                .addExtension(P4_INFO_TEXT, p4InfoUrl)
                .build();
    }
}
