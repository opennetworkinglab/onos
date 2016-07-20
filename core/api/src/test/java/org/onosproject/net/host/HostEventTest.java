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
package org.onosproject.net.host;

import java.util.Set;

import org.junit.Test;
import org.onosproject.event.AbstractEventTest;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.collect.Sets;

public class HostEventTest extends AbstractEventTest {

    private Host createHost() {
        MacAddress mac = MacAddress.valueOf("00:00:11:00:00:01");
        VlanId vlan = VlanId.vlanId((short) 10);
        HostLocation loc = new HostLocation(
                    DeviceId.deviceId("of:foo"),
                    PortNumber.portNumber(100),
                    123L
                );
        Set<IpAddress> ipset = Sets.newHashSet(
                    IpAddress.valueOf("10.0.0.1"),
                    IpAddress.valueOf("10.0.0.2")
                );
        HostId hid = HostId.hostId(mac, vlan);

        return new DefaultHost(
                new ProviderId("of", "foo"), hid, mac, vlan, loc, ipset);
    }

    @Override
    @Test
    public void withTime() {
        Host host = createHost();
        HostEvent event = new HostEvent(HostEvent.Type.HOST_ADDED, host, 123L);
        validateEvent(event, HostEvent.Type.HOST_ADDED, host, 123L);
    }

    @Override
    @Test
    public void withoutTime() {
        Host host = createHost();
        long before = System.currentTimeMillis();
        HostEvent event = new HostEvent(HostEvent.Type.HOST_ADDED, host, before);
        long after = System.currentTimeMillis();
        validateEvent(event, HostEvent.Type.HOST_ADDED, host, before, after);
    }
}
