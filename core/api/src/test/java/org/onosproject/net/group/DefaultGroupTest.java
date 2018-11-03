/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.group;

import org.junit.Test;
import org.onosproject.core.GroupId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.onosproject.net.NetTestTools.did;

/**
 * Unit tests for DefaultGroup class.
 */
public class DefaultGroupTest {
    private final GroupId id1 = new GroupId(6);
    private final GroupId id2 = new GroupId(7);
    private final GroupId id3 = new GroupId(1234);

    private final GroupBucket failoverBucket = DefaultGroupBucket.createFailoverGroupBucket(
            DefaultTrafficTreatment.emptyTreatment(), PortNumber.IN_PORT, id1);
    private final GroupBuckets failoverGroupBuckets = new GroupBuckets(ImmutableList.of(failoverBucket));

    private final GroupBucket indirectBucket =
            DefaultGroupBucket.createIndirectGroupBucket(DefaultTrafficTreatment.emptyTreatment());
    private final GroupBuckets indirectGroupBuckets = new GroupBuckets(ImmutableList.of(indirectBucket));

    private final GroupDescription groupDesc1 =
            new DefaultGroupDescription(did("1"), GroupDescription.Type.FAILOVER, failoverGroupBuckets);
    private final GroupDescription groupDesc2 =
            new DefaultGroupDescription(did("2"), GroupDescription.Type.FAILOVER, failoverGroupBuckets);
    private final GroupDescription groupDesc3 =
            new DefaultGroupDescription(did("3"), GroupDescription.Type.INDIRECT, indirectGroupBuckets);

    DefaultGroup group1 = new DefaultGroup(id1, groupDesc1);
    DefaultGroup sameAsGroup1 = new DefaultGroup(id1, groupDesc1);
    DefaultGroup group2 = new DefaultGroup(id1, groupDesc2);
    DefaultGroup group3 = new DefaultGroup(id2, groupDesc2);
    DefaultGroup group4 = new DefaultGroup(id3, groupDesc3);

    /**
     * Tests for proper operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void checkEquals() {
        new EqualsTester()
                .addEqualityGroup(group1, sameAsGroup1)
                .addEqualityGroup(group2)
                .addEqualityGroup(group3)
                .addEqualityGroup(group4)
                .testEquals();
    }

    /**
     * Tests that objects are created properly.
     */
    @Test
    public void checkConstruction() {
        assertThat(group1.id(), is(id1));
        assertThat(group1.bytes(), is(0L));
        assertThat(group1.life(), is(0L));
        assertThat(group1.packets(), is(0L));
        assertThat(group1.referenceCount(), is(0L));
        assertThat(group1.buckets(), is(failoverGroupBuckets));
        assertThat(group1.state(), is(Group.GroupState.PENDING_ADD));
        assertThat(group1.failedRetryCount(), is(0));
    }

    /**
     * Tests that objects are created properly using the device based constructor.
     */
    @Test
    public void checkConstructionWithDid() {
        DefaultGroup group = new DefaultGroup(id2, NetTestTools.did("1"),
                GroupDescription.Type.FAILOVER, failoverGroupBuckets);
        assertThat(group.id(), is(id2));
        assertThat(group.bytes(), is(0L));
        assertThat(group.life(), is(0L));
        assertThat(group.packets(), is(0L));
        assertThat(group.referenceCount(), is(0L));
        assertThat(group.deviceId(), is(NetTestTools.did("1")));
        assertThat(group.buckets(), is(failoverGroupBuckets));
        assertThat(group.failedRetryCount(), is(0));
    }

    /**
     * Test failedRetryCount field.
     */
    @Test
    public void checkFailedRetyCount() {
        assertThat(group1.failedRetryCount(), is(0));
        group1.incrFailedRetryCount();
        assertThat(group1.failedRetryCount(), is(1));
        group1.setFailedRetryCount(3);
        assertThat(group1.failedRetryCount(), is(3));
        group1.incrFailedRetryCount();
        assertThat(group1.failedRetryCount(), is(4));
        group1.setFailedRetryCount(0);
        assertThat(group1.failedRetryCount(), is(0));
    }
}
