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

package org.onosproject.incubator.net.virtual;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.TestDeviceParams;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Test of the default virtual host model entity.
 */
public class DefaultVirtualHostTest extends TestDeviceParams {

    /**
     * Checks that the DefaultVirtualHost class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultVirtualHost.class);
    }

    /**
     * Tests the DefaultVirtualHost equality method.
     */
    @Test
    public void testEquality() {
        DefaultVirtualHost host1 =
                new DefaultVirtualHost(NetworkId.networkId(0), HID1, MAC1, VLAN1, LOC1, IPSET1);
        DefaultVirtualHost host2 =
                new DefaultVirtualHost(NetworkId.networkId(0), HID1, MAC1, VLAN1, LOC1, IPSET1);
        DefaultVirtualHost host3 =
                new DefaultVirtualHost(NetworkId.networkId(0), HID2, MAC1, VLAN1, LOC1, IPSET1);
        DefaultVirtualHost host4 =
                new DefaultVirtualHost(NetworkId.networkId(1), HID2, MAC1, VLAN1, LOC1, IPSET1);

        new EqualsTester().addEqualityGroup(host1, host2).addEqualityGroup(host3)
                .addEqualityGroup(host4).testEquals();
    }
}
