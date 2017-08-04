/*
 * Copyright 2014-present Open Networking Foundation
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
import org.onosproject.net.DeviceId;

import static org.junit.Assert.*;
import static org.onosproject.net.DeviceId.deviceId;

/**
 * Tests of the topology graph vertex.
 */
public class DefaultTopologyVertexTest {

    private static final DeviceId D1 = deviceId("1");
    private static final DeviceId D2 = deviceId("2");

    @Test
    public void basics() {
        DefaultTopologyVertex v = new DefaultTopologyVertex(D1);
        assertEquals("incorrect device id", D1, v.deviceId());

        new EqualsTester()
                .addEqualityGroup(new DefaultTopologyVertex(D1),
                                  new DefaultTopologyVertex(D1))
                .addEqualityGroup(new DefaultTopologyVertex(D2),
                                  new DefaultTopologyVertex(D2)).testEquals();
    }
}
