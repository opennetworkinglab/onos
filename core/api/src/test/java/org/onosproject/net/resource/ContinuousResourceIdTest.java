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
import org.onlab.util.Bandwidth;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test for ContinuousResourceId.
 */
public class ContinuousResourceIdTest {

    private static final DeviceId D1 = DeviceId.deviceId("of:001");
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final Bandwidth BW1 = Bandwidth.gbps(2);
    private static final Bandwidth BW2 = Bandwidth.gbps(1);

    @Test
    public void testEquality() {
        ContinuousResourceId id1 = Resources.continuous(D1, P1, Bandwidth.class)
                .resource(BW1.bps()).id();
        // intentionally set a different value
        ContinuousResourceId sameAsId1 = Resources.continuous(D1, P1, Bandwidth.class)
                .resource(BW2.bps()).id();

        new EqualsTester()
                .addEqualityGroup(id1, sameAsId1)
                .testEquals();
    }

    @Test
    public void testSimpleTypeName() {
        ContinuousResourceId id1 = Resources.continuous(D1, P1, Bandwidth.class).resource(BW1.bps()).id();

        assertThat(id1.simpleTypeName(), is("Bandwidth"));
    }
}
