/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.dhcprelay.store;

import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.DHCP;
import org.onlab.packet.DHCP6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;

import java.util.Optional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Unit test for DHCP record.
 */
public class DhcpRecordTest {
    private static final MacAddress MAC = MacAddress.valueOf("1a:1a:1a:1a:1a:1a");
    private static final VlanId VLAN = VlanId.vlanId("100");
    private static final HostId HOST_ID = HostId.hostId(MAC, VLAN);
    private static final HostLocation HL1 =
            new HostLocation(ConnectPoint.deviceConnectPoint("of:0000000000000001/1"), 0);
    private static final HostLocation HL2 =
            new HostLocation(ConnectPoint.deviceConnectPoint("of:0000000000000001/2"), 0);
    private static final Ip4Address IP4ADDR = Ip4Address.valueOf("10.0.2.1");
    private static final MacAddress GW_MAC = MacAddress.valueOf("00:00:00:00:04:01");
    private static final Ip6Address IP6ADDR = Ip6Address.valueOf("2001::1");

    /**
     * Test creating a DHCP relay record.
     */
    @Test
    public void testCreateRecord() {
        DhcpRecord record = new DhcpRecord(HOST_ID)
                .addLocation(HL1)
                .addLocation(HL2)
                .ip4Address(IP4ADDR)
                .nextHop(GW_MAC)
                .ip4Status(DHCP.MsgType.DHCPACK)
                .ip6Address(IP6ADDR)
                .ip6Status(DHCP6.MsgType.REPLY)
                .setDirectlyConnected(true);

        assertThat(record.locations().size(), is(2));
        assertThat(record.locations(), containsInAnyOrder(HL1, HL2));
        assertThat(record.ip4Address(), is(Optional.of(IP4ADDR)));
        assertThat(record.nextHop(), is(Optional.of(GW_MAC)));
        assertThat(record.ip4Status(), is(Optional.of(DHCP.MsgType.DHCPACK)));
        assertThat(record.ip6Address(), is(Optional.of(IP6ADDR)));
        assertThat(record.ip6Status(), is(Optional.of(DHCP6.MsgType.REPLY)));
        assertThat(record.directlyConnected(), is(true));

        DhcpRecord record2 = new DhcpRecord(HOST_ID)
                .nextHop(GW_MAC)
                .addLocation(HL2)
                .ip6Address(IP6ADDR)
                .addLocation(HL1)
                .ip6Status(DHCP6.MsgType.REPLY)
                .ip4Address(IP4ADDR)
                .ip4Status(DHCP.MsgType.DHCPACK)
                .setDirectlyConnected(true);

        TestUtils.setField(record, "lastSeen", 0);
        TestUtils.setField(record2, "lastSeen", 0);
        TestUtils.setField(record, "addrPrefTime", 0);
        TestUtils.setField(record2, "addrPrefTime", 0);
        TestUtils.setField(record, "pdPrefTime", 0);
        TestUtils.setField(record2, "pdPrefTime", 0);
        TestUtils.setField(record, "v6Counter", null);
        TestUtils.setField(record2, "v6Counter", null);

        assertThat(record, equalTo(record2));
        assertThat(record.hashCode(), equalTo(record2.hashCode()));
    }

    /**
     * Test clone a DHCP record.
     */
    @Test
    public void testCloneRecord() {
        DhcpRecord record = new DhcpRecord(HOST_ID)
                .addLocation(HL1)
                .addLocation(HL2)
                .ip4Address(IP4ADDR)
                .nextHop(GW_MAC)
                .ip4Status(DHCP.MsgType.DHCPACK)
                .ip6Address(IP6ADDR)
                .ip6Status(DHCP6.MsgType.REPLY)
                .setDirectlyConnected(true);
        DhcpRecord clonedRecord = record.clone();
        assertEquals(record, clonedRecord);
    }
}
