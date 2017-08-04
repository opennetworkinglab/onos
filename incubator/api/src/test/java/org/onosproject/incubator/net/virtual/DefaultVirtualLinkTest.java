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
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TestDeviceParams;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Test of the default virtual link model entity.
 */
public class DefaultVirtualLinkTest extends TestDeviceParams {

    /**
     * Checks that the DefaultVirtualLink class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultVirtualLink.class);
    }

    /**
     * Tests the DefaultVirtualLink Builder to ensure that the src cannot be null.
     */
    @Test(expected = NullPointerException.class)
    public void testBuilderNullSrc() {
        DefaultVirtualDevice device1 =
                new DefaultVirtualDevice(NetworkId.networkId(0), DID1);
        DefaultVirtualDevice device2 =
                new DefaultVirtualDevice(NetworkId.networkId(0), DID2);
        ConnectPoint src = new ConnectPoint(device1.id(), PortNumber.portNumber(1));
        ConnectPoint dst = new ConnectPoint(device2.id(), PortNumber.portNumber(2));

        DefaultVirtualLink.builder()
                .src(null)
                .build();
    }

    /**
     * Tests the DefaultVirtualLink Builder to ensure that the dst cannot be null.
     */
    @Test(expected = NullPointerException.class)
    public void testBuilderNullDst() {
        DefaultVirtualDevice device1 =
                new DefaultVirtualDevice(NetworkId.networkId(0), DID1);
        DefaultVirtualDevice device2 =
                new DefaultVirtualDevice(NetworkId.networkId(0), DID2);
        ConnectPoint src = new ConnectPoint(device1.id(), PortNumber.portNumber(1));
        ConnectPoint dst = new ConnectPoint(device2.id(), PortNumber.portNumber(2));

        DefaultVirtualLink.builder()
                .dst(null)
                .build();
    }

    /**
     * Tests the DefaultVirtualLink Builder to ensure that the networkId cannot be null.
     */
    @Test(expected = NullPointerException.class)
    public void testBuilderNullNetworkId() {
        DefaultVirtualDevice device1 =
                new DefaultVirtualDevice(NetworkId.networkId(0), DID1);
        DefaultVirtualDevice device2 =
                new DefaultVirtualDevice(NetworkId.networkId(0), DID2);
        ConnectPoint src = new ConnectPoint(device1.id(), PortNumber.portNumber(1));
        ConnectPoint dst = new ConnectPoint(device2.id(), PortNumber.portNumber(2));

        DefaultVirtualLink.builder()
                .networkId(null)
                .build();
    }

    /**
     * Tests the DefaultVirtualLink equality method.
     */
    @Test
    public void testEquality() {
        DefaultVirtualDevice device1 =
                new DefaultVirtualDevice(NetworkId.networkId(0), DID1);
        DefaultVirtualDevice device2 =
                new DefaultVirtualDevice(NetworkId.networkId(0), DID2);
        ConnectPoint src = new ConnectPoint(device1.id(), PortNumber.portNumber(1));
        ConnectPoint dst = new ConnectPoint(device2.id(), PortNumber.portNumber(2));

        VirtualLink link1 = DefaultVirtualLink.builder()
                .networkId(NetworkId.networkId(0))
                .src(src)
                .dst(dst)
                .tunnelId(TunnelId.valueOf("1"))
                .build();
        VirtualLink link2 = DefaultVirtualLink.builder()
                .networkId(NetworkId.networkId(0))
                .src(src)
                .dst(dst)
                .tunnelId(TunnelId.valueOf("1"))
                .build();
        VirtualLink link3 = DefaultVirtualLink.builder()
                .networkId(NetworkId.networkId(0))
                .src(src)
                .dst(dst)
                .tunnelId(TunnelId.valueOf("2"))
                .build();
        VirtualLink link4 = DefaultVirtualLink.builder()
                .networkId(NetworkId.networkId(1))
                .src(src)
                .dst(dst)
                .tunnelId(TunnelId.valueOf("3"))
                .build();

        new EqualsTester().addEqualityGroup(link1, link2).addEqualityGroup(link3)
                .addEqualityGroup(link4).testEquals();
    }
}
