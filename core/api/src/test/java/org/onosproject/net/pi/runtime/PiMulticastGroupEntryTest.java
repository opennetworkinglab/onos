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

package org.onosproject.net.pi.runtime;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.PortNumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link PiMulticastGroupEntry}.
 */
public class PiMulticastGroupEntryTest {
    private final int groupId1 = 1;
    private final int groupId2 = 2;

    private final int instanceId1 = 1;

    private final PortNumber port1 = PortNumber.portNumber(1);
    private final PortNumber port2 = PortNumber.portNumber(2);
    private final PortNumber port3 = PortNumber.portNumber(3);

    private final PiPreReplica replica1 = new PiPreReplica(port1, instanceId1);
    private final PiPreReplica replica2 = new PiPreReplica(port2, instanceId1);
    private final PiPreReplica replica3 = new PiPreReplica(port3, instanceId1);

    private final PiMulticastGroupEntry group1 = PiMulticastGroupEntry.builder()
            .withGroupId(groupId1)
            .addReplica(replica1)
            .addReplica(replica2)
            .build();

    private final PiMulticastGroupEntry sameAsGroup1 = PiMulticastGroupEntry.builder()
            .withGroupId(groupId1)
            .addReplica(replica1)
            .addReplica(replica2)
            .build();

    private final PiMulticastGroupEntry group2 = PiMulticastGroupEntry.builder()
            .withGroupId(groupId2)
            .addReplica(replica1)
            .addReplica(replica2)
            .addReplica(replica3)
            .build();

    @Test
    public void testPiMulticastGroupEntry() {
        assertThat("Invalid group ID",
                   group1.groupId(), is(groupId1));
        assertThat("Invalid replicas size",
                   group1.replicas().size(), is(2));
        assertThat("Invalid replicas",
                   group1.replicas(), contains(replica1, replica2));

        assertThat("Invalid group ID",
                   group2.groupId(), is(groupId2));
        assertThat("Invalid replicas size",
                   group2.replicas().size(), is(3));
        assertThat("Invalid replicas",
                   group2.replicas(), contains(replica1, replica2, replica3));
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(group1, sameAsGroup1)
                .addEqualityGroup(group2)
                .testEquals();
    }
}
