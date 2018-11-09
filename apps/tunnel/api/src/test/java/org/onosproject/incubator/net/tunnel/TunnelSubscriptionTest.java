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
package org.onosproject.incubator.net.tunnel;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;

import com.google.common.testing.EqualsTester;

/**
 * Test of order model entity.
 */
public class TunnelSubscriptionTest {
    /**
     * Checks that the Order class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(TunnelSubscription.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquality() {
        TunnelEndPoint src = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(23423));
        TunnelEndPoint dst = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(32421));
        ApplicationId appId = new DefaultApplicationId(243, "test");
        ApplicationId appId2 = new DefaultApplicationId(2431, "test1");
        TunnelId tunnelId = TunnelId.valueOf("41654654");
        TunnelSubscription p1 = new TunnelSubscription(appId, src, dst, tunnelId, Tunnel.Type.VXLAN,
                             null);
        TunnelSubscription p2 = new TunnelSubscription(appId, src, dst, tunnelId, Tunnel.Type.VXLAN,
                             null);
        TunnelSubscription p3 = new TunnelSubscription(appId2, src, dst, tunnelId, Tunnel.Type.VXLAN,
                             null);
        new EqualsTester().addEqualityGroup(p1, p2).addEqualityGroup(p3)
                .testEquals();
    }
}
