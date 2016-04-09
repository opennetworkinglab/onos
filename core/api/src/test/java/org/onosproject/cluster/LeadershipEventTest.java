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

import com.google.common.testing.EqualsTester;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the leadership event test.
 */
public class LeadershipEventTest {
    private final NodeId node1 = new NodeId("1");
    private final NodeId node2 = new NodeId("2");
    private final Leadership lead1 = new Leadership("topic1", new Leader(node1, 1L, 2L), Arrays.asList(node1));
    private final Leadership lead2 = new Leadership("topic1", new Leader(node1, 1L, 2L), Arrays.asList(node1, node2));
    private final Leadership lead3 = new Leadership("topic1", new Leader(node2, 1L, 2L), Arrays.asList(node2));
    private final LeadershipEvent event1 =
            new LeadershipEvent(LeadershipEvent.Type.LEADER_CHANGED, lead1);
    private final long time = System.currentTimeMillis();
    private final LeadershipEvent event2 =
            new LeadershipEvent(LeadershipEvent.Type.CANDIDATES_CHANGED,
                    lead2, time);
    private final LeadershipEvent sameAsEvent2 =
            new LeadershipEvent(LeadershipEvent.Type.CANDIDATES_CHANGED,
                    lead2, time);
    private final LeadershipEvent event3 =
            new LeadershipEvent(LeadershipEvent.Type.LEADER_CHANGED, lead2);
    private final LeadershipEvent event4 =
            new LeadershipEvent(LeadershipEvent.Type.LEADER_AND_CANDIDATES_CHANGED, lead3);

    /**
     * Tests for proper operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void checkEquals() {
        new EqualsTester()
                .addEqualityGroup(event1)
                .addEqualityGroup(event2, sameAsEvent2)
                .addEqualityGroup(event3)
                .addEqualityGroup(event4)
                .testEquals();
    }

    /**
     * Tests that objects are created properly.
     */
    @Test
    public void checkConstruction() {
        assertThat(event1.type(), is(LeadershipEvent.Type.LEADER_CHANGED));
        assertThat(event1.subject(), is(lead1));

        assertThat(event2.time(), is(time));
        assertThat(event2.type(), is(LeadershipEvent.Type.CANDIDATES_CHANGED));
        assertThat(event2.subject(), is(lead2));
    }

}
