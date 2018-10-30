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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.store.service.TestStorageService;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onlab.packet.DHCP.MsgType.DHCPREQUEST;

public class DistributedDhcpRelayStoreTest {
    private static final ConnectPoint CP = ConnectPoint.deviceConnectPoint("of:1/1");
    private static final MacAddress MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final VlanId VLAN_ID = VlanId.vlanId("100");
    private static final HostId HOST_ID = HostId.hostId(MAC, VLAN_ID);
    private static final Ip4Address IP = Ip4Address.valueOf("192.168.1.10");
    private static final MacAddress GW_MAC = MacAddress.valueOf("00:00:00:00:01:01");
    private DistributedDhcpRelayStore store;

    @Before
    public void setup() {
        store = new DistributedDhcpRelayStore();
        store.storageService = new TestStorageService();
        store.activated();
    }

    @After
    public void teerDown() {
        store.deactivated();
    }

    /**
     * Puts and removes a record, should received UPDATED and REMOVED event.
     */
    @Test
    public void testPutAndRemoveRecord() {
        // dhcp request, no IP
        HostId hostId = HostId.hostId(MAC, VLAN_ID);
        DhcpRecord record = new DhcpRecord(hostId);
        record.addLocation(new HostLocation(CP, System.currentTimeMillis()));
        record.setDirectlyConnected(true);
        record.nextHop(GW_MAC);
        record.ip4Status(DHCPREQUEST);

        CompletableFuture<DhcpRelayStoreEvent> recordComplete = new CompletableFuture<>();
        store.setDelegate(recordComplete::complete);
        store.updateDhcpRecord(HOST_ID, record);
        DhcpRelayStoreEvent event = recordComplete.join();
        assertEquals(record, event.subject());
        assertEquals(DhcpRelayStoreEvent.Type.UPDATED, event.type());
        DhcpRecord recordInStore = store.getDhcpRecord(HOST_ID).orElse(null);
        assertNotNull(recordInStore);
        assertEquals(record, recordInStore);
        Collection<DhcpRecord> recordsInStore = store.getDhcpRecords();
        assertEquals(1, recordsInStore.size());
        assertEquals(record, recordsInStore.iterator().next());

        // dhcp request, with IP
        record = new DhcpRecord(hostId);
        record.addLocation(new HostLocation(CP, System.currentTimeMillis()));
        record.setDirectlyConnected(true);
        record.ip4Address(IP);
        record.nextHop(GW_MAC);
        record.ip4Status(DHCPREQUEST);

        recordComplete = new CompletableFuture<>();
        store.setDelegate(recordComplete::complete);
        store.updateDhcpRecord(HOST_ID, record);
        event = recordComplete.join();
        DhcpRecord subject = event.subject();
        assertEquals(record.locations(), subject.locations());
        assertEquals(record.vlanId(), subject.vlanId());
        assertEquals(record.macAddress(), subject.macAddress());
        assertEquals(record.ip4Address(), subject.ip4Address());
        assertEquals(record.nextHop(), subject.nextHop());
        assertEquals(record.ip4Status(), subject.ip4Status());
        assertEquals(record.ip6Address(), subject.ip6Address());
        assertEquals(record.ip6Status(), subject.ip6Status());
        assertEquals(record.directlyConnected(), subject.directlyConnected());

        assertEquals(DhcpRelayStoreEvent.Type.UPDATED, event.type());
        recordInStore = store.getDhcpRecord(HOST_ID).orElse(null);
        assertNotNull(recordInStore);
        assertEquals(record.locations(), recordInStore.locations());
        assertEquals(record.vlanId(), recordInStore.vlanId());
        assertEquals(record.macAddress(), recordInStore.macAddress());
        assertEquals(record.ip4Address(), recordInStore.ip4Address());
        assertEquals(record.nextHop(), recordInStore.nextHop());
        assertEquals(record.ip4Status(), recordInStore.ip4Status());
        assertEquals(record.ip6Address(), recordInStore.ip6Address());
        assertEquals(record.ip6Status(), recordInStore.ip6Status());
        assertEquals(record.directlyConnected(), recordInStore.directlyConnected());
        recordsInStore = store.getDhcpRecords();
        assertEquals(1, recordsInStore.size());

        // removes record
        recordComplete = new CompletableFuture<>();
        store.setDelegate(recordComplete::complete);
        DhcpRecord removedRecord = store.removeDhcpRecord(HOST_ID).orElse(null);
        assertEquals(record.locations(), removedRecord.locations());
        assertEquals(record.vlanId(), removedRecord.vlanId());
        assertEquals(record.macAddress(), removedRecord.macAddress());
        assertEquals(record.ip4Address(), removedRecord.ip4Address());
        assertEquals(record.nextHop(), removedRecord.nextHop());
        assertEquals(record.ip4Status(), removedRecord.ip4Status());
        assertEquals(record.ip6Address(), removedRecord.ip6Address());
        assertEquals(record.ip6Status(), removedRecord.ip6Status());
        assertEquals(record.directlyConnected(), removedRecord.directlyConnected());
        event = recordComplete.join();
        assertEquals(record, event.subject());
        assertEquals(DhcpRelayStoreEvent.Type.REMOVED, event.type());
        recordInStore = store.getDhcpRecord(HOST_ID).orElse(null);
        assertNull(recordInStore);
        recordsInStore = store.getDhcpRecords();
        assertEquals(0, recordsInStore.size());
    }
}
