/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.newresource;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Optional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ResourcePathTest {

    private static final DeviceId D1 = DeviceId.deviceId("of:001");
    private static final DeviceId D2 = DeviceId.deviceId("of:002");
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final VlanId VLAN1 = VlanId.vlanId((short) 100);
    private static final Bandwidth BW1 = Bandwidth.gbps(2);
    private static final Bandwidth BW2 = Bandwidth.gbps(1);

    @Test
    public void testEquals() {
        ResourcePath resource1 = ResourcePath.discrete(D1, P1, VLAN1);
        ResourcePath sameAsResource1 = ResourcePath.discrete(D1, P1, VLAN1);
        ResourcePath resource2 = ResourcePath.discrete(D2, P1, VLAN1);
        ResourcePath resource3 = ResourcePath.continuous(BW1.bps(), D1, P1, BW1);
        ResourcePath sameAsResource3 = ResourcePath.continuous(BW2.bps(), D1, P1, BW1);

        new EqualsTester()
                .addEqualityGroup(resource1, sameAsResource1)
                .addEqualityGroup(resource2)
                .addEqualityGroup(resource3, sameAsResource3)   // this is intentional
                .testEquals();
    }

    @Test
    public void testComponents() {
        ResourcePath port = ResourcePath.discrete(D1, P1);

        assertThat(port.components(), contains(D1, P1));
    }

    @Test
    public void testThereIsParent() {
        ResourcePath path = ResourcePath.discrete(D1, P1, VLAN1);
        ResourcePath parent = ResourcePath.discrete(D1, P1);

        assertThat(path.parent(), is(Optional.of(parent)));
    }

    @Test
    public void testNoParent() {
        ResourcePath path = ResourcePath.discrete(D1);

        assertThat(path.parent(), is(Optional.of(ResourcePath.ROOT)));
    }

    @Test
    public void testBase() {
        ResourcePath path = ResourcePath.discrete(D1);

        DeviceId child = (DeviceId) path.last();
        assertThat(child, is(D1));
    }
}
