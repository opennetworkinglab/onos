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
package org.onosproject.net.group;

import org.junit.Test;
import org.onosproject.core.GroupId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onosproject.net.group.GroupDescription.Type.FAILOVER;
import static org.onosproject.net.group.GroupDescription.Type.INDIRECT;
import static org.onosproject.net.group.GroupDescription.Type.SELECT;

/**
 * Unit tests for the group bucket class.
 */
public class GroupBucketTest {

    private final GroupId groupId = new GroupId(7);
    private final GroupId nullGroup = null;

    private final PortNumber nullPort = null;

    private final TrafficTreatment treatment =
            DefaultTrafficTreatment.emptyTreatment();
    private final GroupBucket selectGroupBucket =
            DefaultGroupBucket.createSelectGroupBucket(treatment);
    private final GroupBucket sameAsSelectGroupBucket =
            DefaultGroupBucket.createSelectGroupBucket(treatment);
    private final GroupBucket failoverGroupBucket =
            DefaultGroupBucket.createFailoverGroupBucket(treatment,
                    PortNumber.IN_PORT, groupId);
    private final GroupBucket indirectGroupBucket =
            DefaultGroupBucket.createIndirectGroupBucket(treatment);
    private final GroupBucket selectGroupBucketWithWeight =
            DefaultGroupBucket.createSelectGroupBucket(treatment, (short) 5);


    /**
     * Tests for proper operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void checkEquals() {
        new EqualsTester()
                .addEqualityGroup(selectGroupBucket,
                        sameAsSelectGroupBucket,
                        selectGroupBucketWithWeight)
                .addEqualityGroup(failoverGroupBucket)
                .addEqualityGroup(indirectGroupBucket)
                .testEquals();
    }

    private void checkValues(GroupBucket bucket, GroupDescription.Type type,
                             long bytes, long packets, short weight,
                             GroupId groupId, PortNumber portNumber) {
        assertThat(bucket.type(), is(type));
        assertThat(bucket.bytes(), is(bytes));
        assertThat(bucket.packets(), is(packets));
        assertThat(bucket.treatment(), is(treatment));
        assertThat(bucket.weight(), is(weight));
        assertThat(bucket.watchGroup(), is(groupId));
        assertThat(bucket.watchPort(), is(portNumber));
    }

    /**
     * Checks that construction of a select group was correct.
     */
    @Test
    public void checkSelectGroup() {
        // Casting needed because only the store accesses the set methods.
        ((DefaultGroupBucket) selectGroupBucket).setBytes(4);
        ((DefaultGroupBucket) selectGroupBucket).setPackets(44);

        checkValues(selectGroupBucket, SELECT, 4L, 44L, (short) 1,
                nullGroup, nullPort);
    }

    /**
     * Checks that construction of a select group with a weight was correct.
     */
    @Test
    public void checkSelectGroupWithPriority() {
        checkValues(selectGroupBucketWithWeight, SELECT, 0L, 0L, (short) 5,
                nullGroup, nullPort);
    }

    /**
     * Checks that construction of an indirect group was correct.
     */
    @Test
    public void checkFailoverGroup() {
        checkValues(failoverGroupBucket, FAILOVER, 0L, 0L, (short) -1,
                groupId, PortNumber.IN_PORT);
    }
    /**
     * Checks that construction of an indirect group was correct.
     */
    @Test
    public void checkIndirectGroup() {
        checkValues(indirectGroupBucket, INDIRECT, 0L, 0L, (short) -1,
                nullGroup, nullPort);
    }

    /**
     * Checks that a weight of 0 results in no group getting created.
     */
    @Test
    public void checkZeroWeight() {
        GroupBucket bucket =
                DefaultGroupBucket.createSelectGroupBucket(treatment, (short) 0);
        assertThat(bucket, nullValue());
    }
}
