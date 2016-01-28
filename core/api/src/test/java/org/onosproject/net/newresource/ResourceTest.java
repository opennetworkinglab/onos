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

public class ResourceTest {

    private static final DeviceId D1 = DeviceId.deviceId("of:001");
    private static final DeviceId D2 = DeviceId.deviceId("of:002");
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final VlanId VLAN1 = VlanId.vlanId((short) 100);
    private static final Bandwidth BW1 = Bandwidth.gbps(2);
    private static final Bandwidth BW2 = Bandwidth.gbps(1);

    @Test
    public void testEquals() {
        Resource resource1 = Resources.discrete(D1, P1, VLAN1).resource();
        Resource sameAsResource1 = Resources.discrete(D1, P1, VLAN1).resource();
        Resource resource2 = Resources.discrete(D2, P1, VLAN1).resource();
        Resource resource3 = Resources.continuous(D1, P1, Bandwidth.class).resource(BW1.bps());
        Resource sameAsResource3 = Resources.continuous(D1, P1, Bandwidth.class).resource(BW1.bps());

        new EqualsTester()
                .addEqualityGroup(resource1, sameAsResource1)
                .addEqualityGroup(resource2)
                .addEqualityGroup(resource3, sameAsResource3)
                .testEquals();
    }

    @Test
    public void testComponents() {
        Resource port = Resources.discrete(D1, P1).resource();

        assertThat(port.components(), contains(D1, P1));
    }

    @Test
    public void testIdEquality() {
        ResourceId id1 = Resources.discrete(D1, P1, VLAN1).id();
        ResourceId sameAsId1 = Resources.discrete(D1, P1, VLAN1).id();
        ResourceId id2 = Resources.discrete(D2, P1, VLAN1).id();
        ResourceId id3 = Resources.continuous(D1, P1, Bandwidth.class).resource(BW1.bps()).id();
        // intentionally set a different value
        ResourceId sameAsId3 = Resources.continuous(D1, P1, Bandwidth.class).resource(BW2.bps()).id();

        new EqualsTester()
                .addEqualityGroup(id1, sameAsId1)
                .addEqualityGroup(id2)
                .addEqualityGroup(id3, sameAsId3);
    }

    @Test
    public void testChild() {
        Resource r1 = Resources.discrete(D1).resource().child(P1);
        Resource sameAsR2 = Resources.discrete(D1, P1).resource();

        assertThat(r1, is(sameAsR2));
    }

    @Test
    public void testThereIsParent() {
        Resource resource = Resources.discrete(D1, P1, VLAN1).resource();
        Resource parent = Resources.discrete(D1, P1).resource();

        assertThat(resource.parent(), is(Optional.of(parent)));
    }

    @Test
    public void testNoParent() {
        Resource resource = Resources.discrete(D1).resource();

        assertThat(resource.parent(), is(Optional.of(Resource.ROOT)));
    }

    @Test
    public void testBase() {
        Resource resource = Resources.discrete(D1).resource();

        DeviceId child = (DeviceId) resource.last();
        assertThat(child, is(D1));
    }

    @Test
    public void testVolumeOfDiscrete() {
        Resource resource = Resources.discrete(D1).resource();

        DeviceId volume = resource.volume();
        assertThat(volume, is(D1));
    }

    @Test
    public void testVolumeOfContinuous() {
        Resource resource = Resources.continuous(D1, P1, Bandwidth.class).resource(BW1.bps());

        double volume = resource.volume();
        assertThat(volume, is(BW1.bps()));
    }
}
