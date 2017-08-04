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
package org.onosproject.net;

import com.google.common.testing.EqualsTester;

import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import static org.onosproject.net.HostId.hostId;

/**
 * Test for the host identifier.
 */
public class HostIdTest {

    private static final MacAddress MAC1 = MacAddress.valueOf("00:11:00:00:00:01");
    private static final MacAddress MAC2 = MacAddress.valueOf("00:22:00:00:00:02");
    private static final VlanId VLAN1 = VlanId.vlanId((short) 11);
    private static final VlanId VLAN2 = VlanId.vlanId((short) 22);

    @Test
    public void basics() {
        new EqualsTester()
                .addEqualityGroup(hostId(MAC1, VLAN1), hostId(MAC1, VLAN1))
                .addEqualityGroup(hostId(MAC2, VLAN2), hostId(MAC2, VLAN2))
                .testEquals();
    }

}
