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

package org.onosproject.incubator.net.virtual;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TestDeviceParams;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Test of the default virtual port model entity.
 */
public class DefaultVirtualPortTest extends TestDeviceParams {

    /**
     * Checks that the DefaultVirtualPort class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultVirtualPort.class);
    }

    @Test
    public void testEquality() {
        DefaultVirtualDevice device1 =
                new DefaultVirtualDevice(NetworkId.networkId(0), DID1);
        DefaultVirtualDevice device2 =
                new DefaultVirtualDevice(NetworkId.networkId(0), DID2);

        Port portA = new DefaultPort(device1, PortNumber.portNumber(1), true);
        Port portB = new DefaultPort(device1, PortNumber.portNumber(2), true);
        Port portC = new DefaultPort(device2, PortNumber.portNumber(2), true);

        DefaultVirtualPort port1 =
                new DefaultVirtualPort(NetworkId.networkId(0), device1, PortNumber.portNumber(1), portA);
        DefaultVirtualPort port2 =
                new DefaultVirtualPort(NetworkId.networkId(0), device1, PortNumber.portNumber(1), portA);
        DefaultVirtualPort port3 =
                new DefaultVirtualPort(NetworkId.networkId(0), device1, PortNumber.portNumber(2), portB);
        DefaultVirtualPort port4 =
                new DefaultVirtualPort(NetworkId.networkId(1), device2, PortNumber.portNumber(2), portC);


        new EqualsTester().addEqualityGroup(port1, port2).addEqualityGroup(port3)
                .addEqualityGroup(port4).testEquals();
    }
}
