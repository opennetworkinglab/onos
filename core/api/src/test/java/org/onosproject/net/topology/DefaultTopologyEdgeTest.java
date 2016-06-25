/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.topology;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Tests of the topology graph edge.
 */
public class DefaultTopologyEdgeTest {

    static final DeviceId D1 = deviceId("1");
    static final DeviceId D2 = deviceId("2");
    static final PortNumber P1 = portNumber(1);
    static final PortNumber P2 = portNumber(2);

    static final ConnectPoint CP1 = new ConnectPoint(D1, P1);
    static final ConnectPoint CP2 = new ConnectPoint(D2, P1);
    static final ConnectPoint CP3 = new ConnectPoint(D2, P1);
    static final ConnectPoint CP4 = new ConnectPoint(D1, P2);

    static final DefaultTopologyVertex V1 = new DefaultTopologyVertex(D1);
    static final DefaultTopologyVertex V2 = new DefaultTopologyVertex(D2);

    static final ProviderId PID = new ProviderId("foo", "bar");

    /** D1:P1 {@literal ->} D2:P1. */
    static final Link L1 = DefaultLink.builder()
            .providerId(PID)
            .src(CP1)
            .dst(CP2)
            .type(Link.Type.INDIRECT)
            .build();
    /** D2:P1 {@literal ->} D1:P2. */
    static final Link L2 = DefaultLink.builder()
            .providerId(PID)
            .src(CP3)
            .dst(CP4)
            .type(Link.Type.INDIRECT)
            .build();

    @Test
    public void basics() {
        DefaultTopologyEdge e = new DefaultTopologyEdge(V1, V2, L1);
        assertEquals("incorrect src", V1, e.src());
        assertEquals("incorrect dst", V2, e.dst());
        assertEquals("incorrect link", L1, e.link());

        new EqualsTester()
                .addEqualityGroup(new DefaultTopologyEdge(V1, V2, L1),
                                  new DefaultTopologyEdge(V1, V2, L1))
                .addEqualityGroup(new DefaultTopologyEdge(V2, V1, L2),
                                  new DefaultTopologyEdge(V2, V1, L2))
                .testEquals();
    }
}
