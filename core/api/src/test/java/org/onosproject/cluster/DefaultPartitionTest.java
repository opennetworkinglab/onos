/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cluster;

import java.util.Collection;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;

/**
 * Unit tests for the default partition implementation.
 */
public class DefaultPartitionTest {

    NodeId id1 = new NodeId("1");
    NodeId id2 = new NodeId("2");
    NodeId id3 = new NodeId("3");

    PartitionId pid1 = new PartitionId(1);
    PartitionId pid2 = new PartitionId(2);
    PartitionId pid3 = new PartitionId(3);

    DefaultPartition partition1 = new DefaultPartition(pid1, ImmutableSet.of(id1));
    DefaultPartition sameAsPartition1 = new DefaultPartition(pid1, ImmutableSet.of(id1));

    DefaultPartition partition2 = new DefaultPartition(pid2, ImmutableSet.of(id2));
    DefaultPartition copyOfPartition2 = new DefaultPartition(partition2);

    DefaultPartition partition3 = new DefaultPartition(pid3, ImmutableSet.of(id1, id2, id3));

    /**
     * Checks that the default partition implementation is an immutable
     * base class.
     */
    @Test
    public void checkImmutability() {
        assertThatClassIsImmutableBaseClass(DefaultPartition.class);
    }

    /**
     * Tests operation of the equals(), hashCode(), and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(partition1, sameAsPartition1)
                .addEqualityGroup(partition2, copyOfPartition2)
                .addEqualityGroup(partition3)
                .testEquals();
    }

    /**
     * Tests that default partition objects are properly constructed.
     */
    @Test
    public void testConstruction() {
        Collection<NodeId> members = partition3.getMembers();
        assertThat(members, notNullValue());
        assertThat(members, hasSize(3));
        assertThat(members, contains(id1, id2, id3));
        assertThat(partition3.getId(), is(pid3));
    }

    /**
     * Tests the empty defaut partition constructor.
     */
    @Test
    public void testEmptyConstructor() {
        DefaultPartition empty = new DefaultPartition();
        assertThat(empty.getId(), nullValue());
        assertThat(empty.getMembers(), nullValue());
    }
}
