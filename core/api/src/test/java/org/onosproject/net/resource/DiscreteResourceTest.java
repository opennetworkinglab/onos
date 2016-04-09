/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.resource;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Unit test for DiscreteResource.
 */
public class DiscreteResourceTest {

    private static final DeviceId D1 = DeviceId.deviceId("of:001");
    private static final DeviceId D2 = DeviceId.deviceId("of:002");
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final VlanId VLAN1 = VlanId.vlanId((short) 100);
    private static final Bandwidth BW1 = Bandwidth.gbps(2);

    @Test
    public void testEquals() {
        DiscreteResource resource1 = Resources.discrete(D1, P1, VLAN1).resource();
        DiscreteResource sameAsResource1 = Resources.discrete(D1, P1, VLAN1).resource();
        DiscreteResource resource2 = Resources.discrete(D2, P1, VLAN1).resource();

        new EqualsTester()
                .addEqualityGroup(resource1, sameAsResource1)
                .addEqualityGroup(resource2)
                .testEquals();
    }

    @Test
    public void testChild() {
        DiscreteResource r1 = Resources.discrete(D1).resource().child(P1);
        DiscreteResource sameAsR2 = Resources.discrete(D1, P1).resource();

        assertThat(r1, is(sameAsR2));
    }

    @Test
    public void testThereIsParent() {
        DiscreteResource resource = Resources.discrete(D1, P1, VLAN1).resource();
        DiscreteResource parent = Resources.discrete(D1, P1).resource();

        assertThat(resource.parent(), is(Optional.of(parent)));
    }

    @Test
    public void testNoParent() {
        DiscreteResource resource = Resources.discrete(D1).resource();

        assertThat(resource.parent(), is(Optional.of(Resource.ROOT)));
    }

    @Test
    public void testTypeOf() {
        DiscreteResource discrete = Resources.discrete(D1, P1, VLAN1).resource();

        assertThat(discrete.isTypeOf(DeviceId.class), is(false));
        assertThat(discrete.isTypeOf(PortNumber.class), is(false));
        assertThat(discrete.isTypeOf(VlanId.class), is(true));
    }

    @Test
    public void testSubTypeOf() {
        DiscreteResource discrete = Resources.discrete(D1, P1, VLAN1).resource();

        assertThat(discrete.isSubTypeOf(DeviceId.class), is(true));
        assertThat(discrete.isSubTypeOf(PortNumber.class), is(true));
        assertThat(discrete.isSubTypeOf(VlanId.class), is(true));
        assertThat(discrete.isSubTypeOf(Bandwidth.class), is(false));
    }

    @Test
    public void testSubTypeOfObject() {
        DiscreteResource discrete = Resources.discrete(D1, P1, VLAN1).resource();

        assertThat(discrete.isSubTypeOf(Object.class), is(true));
    }

    @Test
    public void testValueAs() {
        DiscreteResource resource = Resources.discrete(D1).resource();

        Optional<DeviceId> volume = resource.valueAs(DeviceId.class);
        assertThat(volume.get(), is(D1));
    }

    @Test
    public void testValueOfRoot() {
        DiscreteResource resource = Resource.ROOT;

        assertThat(resource.valueAs(Object.class), is(Optional.empty()));
    }
}
