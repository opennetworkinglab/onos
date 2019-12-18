/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.provider.netcfghost;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.provider.AbstractProviderService;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Set of tests of the host location provider for CORD.
 */
public class NetworkConfigHostProviderTest {
    private NetworkConfigHostProvider provider = new NetworkConfigHostProvider();
    private MockHostProviderService providerService = new MockHostProviderService(provider);

    private MacAddress mac = MacAddress.valueOf("c0:ff:ee:c0:ff:ee");
    private VlanId vlan = VlanId.vlanId(VlanId.UNTAGGED);
    private DeviceId deviceId = DeviceId.deviceId("of:0000000000000001");
    private PortNumber port = PortNumber.portNumber(5);
    private Set<HostLocation> locations = Sets.newHashSet(new HostLocation(deviceId, port, 100));
    private DeviceId auxDeviceId = DeviceId.deviceId("of:0000000000000002");
    private PortNumber auxPort = PortNumber.portNumber(7);
    private Set<HostLocation> auxLocations = Sets.newHashSet(new HostLocation(auxDeviceId, auxPort, 100));
    private Set<IpAddress> ips = new HashSet<>();
    private HostId hostId = HostId.hostId(mac, vlan);
    private HostDescription hostDescription;
    private VlanId innerVlan = VlanId.vlanId((short) 20);
    private EthType outerTpid = EthType.EtherType.lookup((short) 0x88a8).ethType();

    @Before
    public void setUp() {
        provider.providerService = providerService;

        // Initialize test variables
        ips.add(IpAddress.valueOf("10.0.0.1"));
        ips.add(IpAddress.valueOf("192.168.0.1"));
        hostDescription = new DefaultHostDescription(mac, vlan, locations, auxLocations, ips,
                                                     innerVlan, outerTpid, true);
    }

    @Test
    public void testAddHost() throws Exception {
        provider.addHost(mac, vlan, locations, auxLocations, ips, innerVlan, outerTpid);
        assertThat(providerService.hostId, is(hostId));
        assertThat(providerService.hostDescription, is(hostDescription));
        assertThat(providerService.event, is("hostDetected"));
        providerService.clear();
    }

    @Test
    public void testUpdateHost() throws Exception {
        provider.updateHost(mac, vlan, locations, auxLocations, ips, innerVlan, outerTpid);
        assertThat(providerService.hostId, is(hostId));
        assertThat(providerService.hostDescription, is(hostDescription));
        assertThat(providerService.event, is("hostDetected"));
        providerService.clear();
    }

    @Test
    public void testRemoveHost() throws Exception {
        provider.removeHost(mac, vlan);
        assertThat(providerService.hostId, is(hostId));
        assertNull(providerService.hostDescription);
        assertThat(providerService.event, is("hostVanished"));
        providerService.clear();
    }

    /**
     * Mock HostProviderService.
     */
    private class MockHostProviderService
            extends AbstractProviderService<HostProvider>
            implements HostProviderService {
        private HostId hostId = null;
        private HostDescription hostDescription = null;
        private String event = null;

        public MockHostProviderService(HostProvider provider) {
            super(provider);
        }

        @Override
        public void hostDetected(HostId hostId, HostDescription hostDescription, boolean replaceIps) {
            this.hostId = hostId;
            this.hostDescription = hostDescription;
            this.event = "hostDetected";
        }

        @Override
        public void hostVanished(HostId hostId) {
            this.hostId = hostId;
            this.event = "hostVanished";
        }

        @Override
        public void removeIpFromHost(HostId hostId, IpAddress ipAddress) {

        }

        @Override
        public void removeLocationFromHost(HostId hostId, HostLocation location) {

        }

        public void clear() {
            this.hostId = null;
            this.hostDescription = null;
            this.event = null;
        }
    }
}
