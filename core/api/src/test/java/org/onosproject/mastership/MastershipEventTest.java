/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.mastership;

import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the mastership event.
 */
public class MastershipEventTest {
    private final long time = System.currentTimeMillis();
    private final DeviceId deviceId1 = DeviceId.deviceId("foo:bar");
    private final DeviceId deviceId2 = DeviceId.deviceId("bar:baz");
    private final NodeId node1 = new NodeId("1");
    private final NodeId node2 = new NodeId("2");
    private final MastershipInfo mastershipInfo1 = new MastershipInfo(
        1,
        Optional.of(node1),
        ImmutableMap.<NodeId, MastershipRole>builder()
            .put(node1, MastershipRole.MASTER)
            .put(node2, MastershipRole.STANDBY)
            .build());
    private final MastershipInfo mastershipInfo2 = new MastershipInfo(
        2,
        Optional.of(node1),
        ImmutableMap.<NodeId, MastershipRole>builder()
            .put(node2, MastershipRole.MASTER)
            .put(node1, MastershipRole.STANDBY)
            .build());

    private final MastershipEvent event1 =
            new MastershipEvent(MastershipEvent.Type.BACKUPS_CHANGED, deviceId1, mastershipInfo1);
    private final MastershipEvent event2 =
            new MastershipEvent(MastershipEvent.Type.MASTER_CHANGED, deviceId1, mastershipInfo1);
    private final MastershipEvent event3 =
            new MastershipEvent(MastershipEvent.Type.SUSPENDED, deviceId1, mastershipInfo1);
    private final MastershipEvent event4 =
            new MastershipEvent(MastershipEvent.Type.MASTER_CHANGED, deviceId1, mastershipInfo2, time);
    private final MastershipEvent sameAsEvent4 =
            new MastershipEvent(MastershipEvent.Type.MASTER_CHANGED, deviceId1, mastershipInfo2, time);
    private final MastershipEvent event5 =
            new MastershipEvent(MastershipEvent.Type.BACKUPS_CHANGED, deviceId2, mastershipInfo1);

    /**
     * Tests for proper operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void checkEquals() {
        new EqualsTester()
                .addEqualityGroup(event1)
                .addEqualityGroup(event2)
                .addEqualityGroup(event3)
                .addEqualityGroup(event4, sameAsEvent4)
                .addEqualityGroup(event5)
                .testEquals();
    }

    /**
     * Tests that objects are created properly.
     */
    @Test
    public void checkConstruction() {
        assertThat(event1.type(), is(MastershipEvent.Type.BACKUPS_CHANGED));
        assertThat(event1.subject(), is(deviceId1));
        assertThat(event1.mastershipInfo(), is(mastershipInfo1));

        assertThat(event4.time(), is(time));
        assertThat(event4.type(), is(MastershipEvent.Type.MASTER_CHANGED));
        assertThat(event4.subject(), is(deviceId1));
        assertThat(event4.mastershipInfo(), is(mastershipInfo2));
    }

}
