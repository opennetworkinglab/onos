package org.onosproject.net.tunnel;

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
        TunnelId tunnelId = TunnelId.valueOf(41654654);
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
