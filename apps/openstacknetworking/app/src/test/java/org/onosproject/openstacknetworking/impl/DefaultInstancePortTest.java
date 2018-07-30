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
package org.onosproject.openstacknetworking.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openstacknetworking.api.InstancePort;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.openstacknetworking.api.Constants.ANNOTATION_CREATE_TIME;
import static org.onosproject.openstacknetworking.api.InstancePort.State.ACTIVE;

/**
 * Unit tests for the default instance port class.
 */
public class DefaultInstancePortTest {

    private static final String ANNOTATION_NETWORK_ID = "networkId";
    private static final String ANNOTATION_PORT_ID = "portId";

    private static final IpAddress IP_ADDRESS_1 = IpAddress.valueOf("1.2.3.4");
    private static final IpAddress IP_ADDRESS_2 = IpAddress.valueOf("5.6.7.8");

    private static final MacAddress MAC_ADDRESS_1 = MacAddress.valueOf("11:22:33:44:55:66");
    private static final MacAddress MAC_ADDRESS_2 = MacAddress.valueOf("77:88:99:AA:BB:CC");

    private static final DeviceId DEV_ID_1 = DeviceId.deviceId("of:0000000000000001");
    private static final DeviceId DEV_ID_2 = DeviceId.deviceId("of:0000000000000002");
    private static final PortNumber PORT_NUM_1 = PortNumber.portNumber(1L);
    private static final PortNumber PORT_NUM_2 = PortNumber.portNumber(2L);

    private static final VlanId VLAN_ID = VlanId.vlanId();
    private static final ProviderId PROVIDER_ID = ProviderId.NONE;
    private static final HostId HOST_ID_1 = HostId.hostId("00:00:11:00:00:01/1");
    private static final HostId HOST_ID_2 = HostId.hostId("00:00:11:00:00:02/1");

    private static final String NETWORK_ID_1 = "net-id-1";
    private static final String NETWORK_ID_2 = "net-id-2";
    private static final String PORT_ID_1 = "port-id-1";
    private static final String PORT_ID_2 = "port-id-2";

    private static final long TIME_1 = 1L;
    private static final long TIME_2 = 2L;

    private InstancePort instancePort1;
    private InstancePort sameAsInstancePort1;
    private InstancePort instancePort2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultInstancePort.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {

        HostLocation location1 = new HostLocation(DEV_ID_1, PORT_NUM_1, TIME_1);
        HostLocation location2 = new HostLocation(DEV_ID_2, PORT_NUM_2, TIME_2);

        DefaultAnnotations.Builder annotations1 = DefaultAnnotations.builder()
                .set(ANNOTATION_NETWORK_ID, NETWORK_ID_1)
                .set(ANNOTATION_PORT_ID, PORT_ID_1)
                .set(ANNOTATION_CREATE_TIME, String.valueOf(TIME_1));

        DefaultAnnotations.Builder annotations2 = DefaultAnnotations.builder()
                .set(ANNOTATION_NETWORK_ID, NETWORK_ID_2)
                .set(ANNOTATION_PORT_ID, PORT_ID_2)
                .set(ANNOTATION_CREATE_TIME, String.valueOf(TIME_2));

        Host host1 = new DefaultHost(PROVIDER_ID, HOST_ID_1, MAC_ADDRESS_1,
                                VLAN_ID, location1, ImmutableSet.of(IP_ADDRESS_1),
                                annotations1.build());
        Host host2 = new DefaultHost(PROVIDER_ID, HOST_ID_2, MAC_ADDRESS_2,
                                VLAN_ID, location2, ImmutableSet.of(IP_ADDRESS_2),
                                annotations2.build());

        instancePort1 = DefaultInstancePort.from(host1, ACTIVE);
        sameAsInstancePort1 = DefaultInstancePort.from(host1, ACTIVE);
        instancePort2 = DefaultInstancePort.from(host2, ACTIVE);
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(instancePort1, sameAsInstancePort1)
                .addEqualityGroup(instancePort2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        InstancePort instancePort = instancePort1;

        assertThat(instancePort.portId(), is(PORT_ID_1));
        assertThat(instancePort.networkId(), is(NETWORK_ID_1));
        assertThat(instancePort.macAddress(), is(MAC_ADDRESS_1));
        assertThat(instancePort.ipAddress(), is(IP_ADDRESS_1));
        assertThat(instancePort.portNumber(), is(PORT_NUM_1));
        assertThat(instancePort.deviceId(), is(DEV_ID_1));
        assertThat(instancePort.state(), is(ACTIVE));
    }
}
