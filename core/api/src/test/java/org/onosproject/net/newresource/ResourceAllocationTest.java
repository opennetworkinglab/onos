/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.newresource;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.LinkKey;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.IntentId;

public class ResourceAllocationTest {

    private static final DeviceId D1 = DeviceId.deviceId("of:001");
    private static final DeviceId D2 = DeviceId.deviceId("of:002");
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final ConnectPoint CP1_1 = new ConnectPoint(D1, P1);
    private static final ConnectPoint CP2_1 = new ConnectPoint(D2, P1);
    private static final VlanId VLAN1 = VlanId.vlanId((short) 100);
    private static final IntentId IID1 = IntentId.valueOf(30);
    private static final LinkKey LK1 = LinkKey.linkKey(CP1_1, CP2_1);
    private static final LinkKey LK2 = LinkKey.linkKey(CP2_1, CP1_1);

    @Test
    public void testEquals() {
        ResourceAllocation alloc1 = new ResourceAllocation(new ResourcePath(LK1, VLAN1), IID1);
        ResourceAllocation sameAsAlloc1 = new ResourceAllocation(new ResourcePath(LK1, VLAN1), IID1);
        ResourceAllocation alloc2 = new ResourceAllocation(new ResourcePath(LK2, VLAN1), IID1);

        new EqualsTester()
                .addEqualityGroup(alloc1, sameAsAlloc1)
                .addEqualityGroup(alloc2)
                .testEquals();
    }
}
