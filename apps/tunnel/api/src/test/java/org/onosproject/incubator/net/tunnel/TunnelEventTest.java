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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.core.GroupId;
import org.onosproject.net.provider.ProviderId;

/**
 * Test of a tunnel event.
 */
public class TunnelEventTest {
    /**
     * Checks that the Order class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(TunnelEvent.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testConstructor() {
        TunnelEndPoint src = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                .valueOf(23423));
        TunnelEndPoint dst = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                .valueOf(32421));
        GroupId groupId = new GroupId(92034);
        TunnelName tunnelName = TunnelName.tunnelName("TunnelName");
        TunnelId tunnelId = TunnelId.valueOf("41654654");
        ProviderId producerName1 = new ProviderId("producer1", "13");
        Tunnel p1 = new DefaultTunnel(producerName1, src, dst, Tunnel.Type.VXLAN,
                                      Tunnel.State.ACTIVE, groupId, tunnelId,
                                      tunnelName, null);
        TunnelEvent e1 = new TunnelEvent(TunnelEvent.Type.TUNNEL_ADDED, p1);
        assertThat(e1, is(notNullValue()));
        assertThat(e1.type(), is(TunnelEvent.Type.TUNNEL_ADDED));
        assertThat(e1.subject(), is(p1));
    }
}
