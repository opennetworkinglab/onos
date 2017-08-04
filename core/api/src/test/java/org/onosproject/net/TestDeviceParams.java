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

import static org.onosproject.net.DeviceId.deviceId;

import java.util.Set;

import org.onosproject.net.provider.ProviderId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.collect.Sets;

/**
 * Provides a set of test DefaultDevice parameters for use with Host-
 * related tests.
 */
public abstract class TestDeviceParams {

    protected static final ProviderId PID = new ProviderId("of", "foo");
    protected static final DeviceId DID1 = deviceId("of:foo");
    protected static final DeviceId DID2 = deviceId("of:bar");
    protected static final DeviceId DID3 = deviceId("of:who");
    protected static final DeviceId DID4 = deviceId("of:what");
    protected static final DeviceId DID5 = deviceId("of:when");
    protected static final MacAddress MAC1 = MacAddress.valueOf("00:11:00:00:00:01");
    protected static final MacAddress MAC2 = MacAddress.valueOf("00:22:00:00:00:02");
    protected static final VlanId VLAN1 = VlanId.vlanId((short) 11);
    protected static final VlanId VLAN2 = VlanId.vlanId((short) 22);
    protected static final IpAddress IP1 = IpAddress.valueOf("10.0.0.1");
    protected static final IpAddress IP2 = IpAddress.valueOf("10.0.0.2");
    protected static final IpAddress IP3 = IpAddress.valueOf("10.0.0.3");

    protected static final PortNumber P1 = PortNumber.portNumber(100);
    protected static final PortNumber P2 = PortNumber.portNumber(200);
    protected static final HostId HID1 = HostId.hostId(MAC1, VLAN1);
    protected static final HostId HID2 = HostId.hostId(MAC2, VLAN2);
    protected static final HostLocation LOC1 = new HostLocation(DID1, P1, 123L);
    protected static final HostLocation LOC2 = new HostLocation(DID2, P2, 456L);
    protected static final HostLocation LOC3 = new HostLocation(DID3, P1, 789L);
    protected static final Set<IpAddress> IPSET1 = Sets.newHashSet(IP1, IP2);
    protected static final Set<IpAddress> IPSET2 = Sets.newHashSet(IP1, IP3);

}
