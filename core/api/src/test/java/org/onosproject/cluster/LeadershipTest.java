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
package org.onosproject.cluster;

import java.util.Arrays;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the Leadership class.
 */
public class LeadershipTest {
    private final NodeId node1 = new NodeId("1");
    private final NodeId node2 = new NodeId("2");
    private final Leadership lead1 = new Leadership("topic1", new Leader(node1, 1L, 2L), Arrays.asList(node1));
    private final Leadership sameAsLead1 = new Leadership("topic1", new Leader(node1, 1L, 2L), Arrays.asList(node1));
    private final Leadership lead2 = new Leadership("topic2", new Leader(node1, 1L, 2L), Arrays.asList(node1));
    private final Leadership lead3 = new Leadership("topic1", new Leader(node1, 2L, 2L), Arrays.asList(node1));
    private final Leadership lead4 = new Leadership("topic1", new Leader(node1, 3L, 2L), Arrays.asList(node1));
    private final Leadership lead5 = new Leadership("topic1", new Leader(node1, 3L, 3L), Arrays.asList(node1));
    private final Leadership lead6 = new Leadership("topic1", new Leader(node2, 1L, 2L), Arrays.asList(node2, node1));
    private final Leadership lead7 = new Leadership("topic1", null, ImmutableList.of());

    /**
     * Tests for proper operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void checkEquals() {
        new EqualsTester()
                .addEqualityGroup(lead1, sameAsLead1)
                .addEqualityGroup(lead2)
                .addEqualityGroup(lead3)
                .addEqualityGroup(lead4)
                .addEqualityGroup(lead5)
                .addEqualityGroup(lead6)
                .addEqualityGroup(lead7)
                .testEquals();
    }

    /**
     * Tests that objects are created properly and accessor methods return
     * the correct values.
     */
    @Test
    public void checkConstruction() {
        assertThat(lead6.leader(), is(new Leader(node2, 1L, 2L)));
        assertThat(lead6.topic(), is("topic1"));
        assertThat(lead6.candidates(), hasSize(2));
        assertThat(lead6.candidates().get(1), is(node1));
        assertThat(lead6.candidates().get(0), is(node2));
    }
}
