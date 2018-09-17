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
package org.onosproject.net.resource;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test for DiscreteResourceId.
 */
public class DiscreteResourceIdTest {

    private static final DeviceId D1 = DeviceId.deviceId("of:001");
    private static final DeviceId D2 = DeviceId.deviceId("of:002");
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final VlanId VLAN1 = VlanId.vlanId((short) 100);

    @Test
    public void testEquality() {
        DiscreteResourceId id1 = Resources.discrete(D1, P1, VLAN1).id();
        DiscreteResourceId sameAsId1 = Resources.discrete(D1, P1, VLAN1).id();
        DiscreteResourceId id2 = Resources.discrete(D2, P1, VLAN1).id();

        new EqualsTester()
                .addEqualityGroup(id1, sameAsId1)
                .addEqualityGroup(id2)
                .testEquals();
    }


    @Test
    public void testSimpleTypeName() {
        DiscreteResourceId id = Resources.discrete(D1, P1, VLAN1).id();
        assertThat(id.simpleTypeName(), is("VlanId"));
    }

    @Test
    public void testSimpleTypeNameOfRoot() {
        assertThat(ResourceId.ROOT.simpleTypeName(), is("Root"));
    }
}
