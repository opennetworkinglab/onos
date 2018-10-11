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
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link PiPreReplica}.
 */
public class PiPreReplicaTest {

    private final int instanceId1 = 1;
    private final int instanceId2 = 2;
    private final PortNumber port1 = PortNumber.portNumber(1);
    private final PortNumber port2 = PortNumber.portNumber(2);

    private final PiPreReplica replica1of1 = new PiPreReplica(port1, instanceId1);
    private final PiPreReplica sameAsReplica1of1 = new PiPreReplica(port1, instanceId1);

    private final PiPreReplica replica1of2 = new PiPreReplica(port2, instanceId1);
    private final PiPreReplica sameAsReplica1of2 = new PiPreReplica(port2, instanceId1);

    private final PiPreReplica replica2of2 = new PiPreReplica(port2, instanceId2);
    private final PiPreReplica sameAsReplica2of2 = new PiPreReplica(port2, instanceId2);

    @Test
    public void testPiPreReplica() {
        assertThat("Invalid port", replica1of1.egressPort(), is(port1));
        assertThat("Invalid instance ID", replica1of1.instanceId(), is(instanceId1));
        assertThat("Invalid port", replica1of2.egressPort(), is(port2));
        assertThat("Invalid instance ID", replica1of2.instanceId(), is(instanceId1));
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(replica1of1, sameAsReplica1of1)
                .addEqualityGroup(replica1of2, sameAsReplica1of2)
                .addEqualityGroup(replica2of2, sameAsReplica2of2)
                .testEquals();
    }
}
