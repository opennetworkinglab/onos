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
import org.onosproject.net.ConnectPoint;
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

        ConnectPoint cpA = new ConnectPoint(device1.id(), PortNumber.portNumber(1));
        ConnectPoint cpB = new ConnectPoint(device1.id(), PortNumber.portNumber(2));
        ConnectPoint cpC = new ConnectPoint(device2.id(), PortNumber.portNumber(2));

        DefaultVirtualPort port1 =
                new DefaultVirtualPort(NetworkId.networkId(0), device1,
                                       PortNumber.portNumber(1), cpA);
        DefaultVirtualPort port2 =
                new DefaultVirtualPort(NetworkId.networkId(0), device1,
                                       PortNumber.portNumber(1), cpA);
        DefaultVirtualPort port3 =
                new DefaultVirtualPort(NetworkId.networkId(0), device1,
                                       PortNumber.portNumber(2), cpB);
        DefaultVirtualPort port4 =
                new DefaultVirtualPort(NetworkId.networkId(1), device2,
                                       PortNumber.portNumber(2), cpC);


        new EqualsTester().addEqualityGroup(port1, port2).addEqualityGroup(port3)
                .addEqualityGroup(port4).testEquals();
    }
}
