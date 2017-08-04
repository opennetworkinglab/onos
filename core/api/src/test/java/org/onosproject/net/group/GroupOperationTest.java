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
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.group.GroupDescription.Type.ALL;
import static org.onosproject.net.group.GroupDescription.Type.INDIRECT;
import static org.onosproject.net.group.GroupOperation.Type.ADD;

/**
 * Tests for the group operation class.
 */
public class GroupOperationTest {

    private final GroupId groupId = new GroupId(6);
    private final TrafficTreatment treatment =
            DefaultTrafficTreatment.emptyTreatment();
    private final GroupBucket bucket =
            DefaultGroupBucket.createSelectGroupBucket(treatment);
    private final GroupBuckets groupBuckets =
            new GroupBuckets(ImmutableList.of(bucket));
    private final GroupOperation op1 =
            GroupOperation.createAddGroupOperation(groupId, ALL, groupBuckets);
    private final GroupOperation sameAsOp1 =
            GroupOperation.createAddGroupOperation(groupId, ALL, groupBuckets);
    private final GroupOperation op2 =
            GroupOperation.createAddGroupOperation(groupId, INDIRECT, groupBuckets);
    private final GroupOperation op3 =
            GroupOperation.createDeleteGroupOperation(groupId, INDIRECT);
    private final GroupOperation op4 =
            GroupOperation.createModifyGroupOperation(groupId, INDIRECT, groupBuckets);

    /**
     * Checks that the GroupOperation class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(GroupOperation.class);
    }

    /**
     * Tests for proper operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void checkEquals() {
        new EqualsTester()
                .addEqualityGroup(op1, sameAsOp1)
                .addEqualityGroup(op2)
                .addEqualityGroup(op3)
                .addEqualityGroup(op4)
                .testEquals();
    }

    /**
     * Checks that the construction of the add operation is correct.
     */
    @Test
    public void testAddGroupOperation() {
        assertThat(op1.buckets(), is(groupBuckets));
        assertThat(op1.groupId(), is(groupId));
        assertThat(op1.groupType(), is(ALL));
        assertThat(op1.opType(), is(ADD));
    }

}
