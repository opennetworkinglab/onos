/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual;

import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TenantId;
import org.onosproject.net.provider.ProviderId;

import static org.junit.Assert.*;
import static org.onosproject.incubator.net.virtual.Comparators.VIRTUAL_DEVICE_COMPARATOR;
import static org.onosproject.incubator.net.virtual.Comparators.VIRTUAL_NETWORK_COMPARATOR;
import static org.onosproject.incubator.net.virtual.Comparators.VIRTUAL_PORT_COMPARATOR;
import static org.onosproject.net.DeviceId.deviceId;

public class ComparatorsTest {
    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DeviceId DID = deviceId("of:foo");
    private static final String MFR = "whitebox";
    private static final String HW = "1.1.x";
    private static final String SW = "3.9.1";
    private static final String SN = "43311-12345";
    private static final ChassisId CID = new ChassisId();

    @Test
    public void testVirtualNetworkComparator() {
        assertNotEquals(0, VIRTUAL_NETWORK_COMPARATOR.compare(network(10, "tenantID"), network(10, "tenantID1")));
        assertNotEquals(0, VIRTUAL_NETWORK_COMPARATOR.compare(network(10, "tenantID"), network(15, "tenantID1")));
        assertNotEquals(0, VIRTUAL_NETWORK_COMPARATOR.compare(network(15, "tenantID1"), network(10, "tenantID1")));
        assertNotEquals(0, VIRTUAL_NETWORK_COMPARATOR.compare(network(15, "tenantID"), network(10, "tenantID1")));
    }

    private VirtualNetwork network(int networkID, String tenantID) {
        return new DefaultVirtualNetwork(NetworkId.networkId(networkID), TenantId.tenantId(tenantID));
    }

    @Test
    public void testVirtualDeviceComparator() {
        assertEquals(0, VIRTUAL_DEVICE_COMPARATOR.compare(vd(0, "of:foo"), vd(0, "of:foo")));
        assertEquals(0, VIRTUAL_DEVICE_COMPARATOR.compare(vd(3, "of:foo"), vd(0, "of:foo")));
        assertNotEquals(0, VIRTUAL_DEVICE_COMPARATOR.compare(vd(0, "of:bar"), vd(0, "of:foo")));
        assertNotEquals(0, VIRTUAL_DEVICE_COMPARATOR.compare(vd(3, "of:bar"), vd(0, "of:foo")));
    }

    private VirtualDevice vd(int netID, String devID) {
        return new DefaultVirtualDevice(NetworkId.networkId(netID), DeviceId.deviceId(devID));
    }

    @Test
    public void testVirtualPortComparator() {
        assertEquals(0, VIRTUAL_PORT_COMPARATOR.compare(vPort(2), vPort(2)));
        assertEquals(4, VIRTUAL_PORT_COMPARATOR.compare(vPort(900), vPort(5)));
        assertEquals(-8, VIRTUAL_PORT_COMPARATOR.compare(vPort(0), vPort(8)));
    }

    private VirtualPort vPort(int portNumber) {
        return new DefaultVirtualPort(NetworkId.networkId(20),
                                      new DefaultDevice(PID, DID, null, MFR, HW, SW, SN, CID),
                                      PortNumber.portNumber(portNumber),
                                      new ConnectPoint(DID, PortNumber.portNumber(900)));
    }
}











