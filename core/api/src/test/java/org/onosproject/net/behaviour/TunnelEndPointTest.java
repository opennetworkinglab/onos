/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.net.behaviour;

import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.NetTestTools;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

public class TunnelEndPointTest {

    private ConnectPoint cp1 = NetTestTools.connectPoint("cp1", 1);
    private TunnelEndPoint<ConnectPoint> endPoint1 =
            new TunnelEndPoint<>(cp1);
    private TunnelEndPoint<ConnectPoint> sameAsEndPoint1 =
            new TunnelEndPoint<>(cp1);

    private ConnectPoint cp2 = NetTestTools.connectPoint("cp2", 2);
    private TunnelEndPoint<ConnectPoint> endPoint2 =
            new TunnelEndPoint<>(cp2);

    private TunnelEndPoint<MacAddress> endPoint3 =
            new TunnelEndPoint<>(MacAddress.BROADCAST);

    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(TunnelEndPoint.class);
    }

    @Test
    public void testConstruction() {
        assertThat(endPoint1.value(), is(cp1));
        assertThat(endPoint1.strValue(), is(cp1.toString()));
    }

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(endPoint1, sameAsEndPoint1)
                .addEqualityGroup(endPoint2)
                .addEqualityGroup(endPoint3)
                .testEquals();
    }
}
