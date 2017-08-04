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

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.topology.DefaultTopologyEdgeTest.*;

public class DefaultGraphDescriptionTest {

    static final DefaultTopologyEdge E1 = new DefaultTopologyEdge(V1, V2, L1);
    static final DefaultTopologyEdge E2 = new DefaultTopologyEdge(V1, V2, L1);

    private static final DeviceId D3 = deviceId("3");

    static final Device DEV1 = new DefaultDevice(PID, D1, SWITCH, "", "", "", "", null);
    static final Device DEV2 = new DefaultDevice(PID, D2, SWITCH, "", "", "", "", null);
    static final Device DEV3 = new DefaultDevice(PID, D3, SWITCH, "", "", "", "", null);

    @Test
    public void basics() {
        DefaultGraphDescription desc =
                new DefaultGraphDescription(4321L, System.currentTimeMillis(), ImmutableSet.of(DEV1, DEV2, DEV3),
                                            ImmutableSet.of(L1, L2));
        assertEquals("incorrect time", 4321L, desc.timestamp());
        assertEquals("incorrect vertex count", 3, desc.vertexes().size());
        assertEquals("incorrect edge count", 2, desc.edges().size());
    }

    @Test
    public void missingVertex() {
        GraphDescription desc = new DefaultGraphDescription(4321L, System.currentTimeMillis(),
                                                            ImmutableSet.of(DEV1, DEV3),
                                                            ImmutableSet.of(L1, L2));
        assertEquals("incorrect time", 4321L, desc.timestamp());
        assertEquals("incorrect vertex count", 2, desc.vertexes().size());
        assertEquals("incorrect edge count", 0, desc.edges().size());
    }
}
