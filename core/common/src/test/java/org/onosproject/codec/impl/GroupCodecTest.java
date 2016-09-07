/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;

import java.io.IOException;
import java.io.InputStream;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.onosproject.codec.impl.GroupJsonMatcher.matchesGroup;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.group.GroupDescription.Type.*;

/**
 * Group codec unit tests.
 */

public class GroupCodecTest {

    MockCodecContext context;
    JsonCodec<Group> groupCodec;
    final CoreService mockCoreService = createMock(CoreService.class);

    /**
     * Sets up for each test.  Creates a context and fetches the flow rule
     * codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        groupCodec = context.codec(Group.class);
        assertThat(groupCodec, notNullValue());

        expect(mockCoreService.registerApplication(GroupCodec.REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    @Test
    public void codecEncodeTest() {
        GroupBucket bucket1 = DefaultGroupBucket
                .createSelectGroupBucket(DefaultTrafficTreatment.emptyTreatment());
        GroupBucket bucket2 = DefaultGroupBucket
                .createIndirectGroupBucket(DefaultTrafficTreatment.emptyTreatment());
        GroupBuckets buckets = new GroupBuckets(ImmutableList.of(bucket1, bucket2));
        GroupBuckets bucketsIndirect = new GroupBuckets(ImmutableList.of(bucket2));

        DefaultGroup group = new DefaultGroup(
                new DefaultGroupId(1),
                NetTestTools.did("d1"),
                ALL,
                buckets);
        DefaultGroup group1 = new DefaultGroup(
                new DefaultGroupId(2),
                NetTestTools.did("d2"),
                INDIRECT,
                bucketsIndirect);

        MockCodecContext context = new MockCodecContext();
        GroupCodec codec = new GroupCodec();
        ObjectNode groupJson = codec.encode(group, context);

        ObjectNode groupJsonIndirect = codec.encode(group1, context);

        assertThat(groupJson, matchesGroup(group));
        assertThat(groupJsonIndirect, matchesGroup(group1));
    }

    @Test
    public void codecDecodeTest() throws IOException {
        Group group = getGroup("simple-group.json");
        checkCommonData(group);

        assertThat(group.buckets().buckets().size(), is(1));
        GroupBucket groupBucket = group.buckets().buckets().get(0);
        assertThat(groupBucket.type().toString(), is("ALL"));
        assertThat(groupBucket.treatment().allInstructions().size(), is(1));
        Instruction instruction1 = groupBucket.treatment().allInstructions().get(0);
        assertThat(instruction1.type(), is(Instruction.Type.OUTPUT));
        assertThat(((Instructions.OutputInstruction) instruction1).port(), is(PortNumber.portNumber(2)));

    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidGroupTest() throws IOException {
        Group group = getGroup("invalid-group.json");
    }

    /**
     * Checks that the data shared by all the resource is correct for a given group.
     *
     * @param group group to check
     */
    private void checkCommonData(Group group) {
        assertThat(group.appId(), is(APP_ID));
        assertThat(group.deviceId().toString(), is("of:0000000000000001"));
        assertThat(group.type().toString(), is("ALL"));
        assertThat(group.appCookie().key(),
                equalTo(new byte[]{(byte) 0x12, (byte) 0x34, (byte) 0xAB, (byte) 0xCD}));
        assertThat(group.id().id(), is(1));
    }


    /**
     * Reads in a group from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded group
     * @throws IOException if processing the resource fails
     */
    private Group getGroup(String resourceName) throws IOException {
        InputStream jsonStream = GroupCodecTest.class
                .getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        Group group = groupCodec.decode((ObjectNode) json, context);
        assertThat(group, notNullValue());
        return group;
    }
}
