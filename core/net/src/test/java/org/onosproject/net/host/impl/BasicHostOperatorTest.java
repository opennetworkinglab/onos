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
package org.onosproject.net.host.impl;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class BasicHostOperatorTest {
    private static final MacAddress MAC = MacAddress.valueOf("00:00:11:00:00:01");
    private static final VlanId VLAN = VlanId.vlanId((short) 10);
    private static final IpAddress IP = IpAddress.valueOf("10.0.0.1");

    private static final HostId ID = HostId.hostId(MAC);
    private static final HostLocation LOC = new HostLocation(
            DeviceId.deviceId("of:foo"),
            PortNumber.portNumber(100),
            123L
    );
    private static final HostDescription HOST = new DefaultHostDescription(MAC, VLAN, LOC, IP);

    private final ConfigApplyDelegate delegate = config -> { };
    private final ObjectMapper mapper = new ObjectMapper();

    private static final BasicHostConfig BHC = new BasicHostConfig();
    private static final String NAME = "testhost";
    private static final double LAT = 40.96;
    private static final double LON = 0.0;

    @Before
    public void setUp() {
        BHC.init(ID, "test", JsonNodeFactory.instance.objectNode(), mapper, delegate);
        BHC.setLocations(Sets.newHashSet(LOC)).name(NAME).latitude(40.96);
        // if you set lat or long, the other becomes valid as 0.0 (not null)
    }

    @Test
    public void testDescOps() {
        HostDescription desc = BasicHostOperator.combine(BHC, HOST);
        assertEquals(NAME, desc.annotations().value(AnnotationKeys.NAME));
        assertEquals(String.valueOf(LON), desc.annotations().value(AnnotationKeys.LONGITUDE));
        assertEquals(String.valueOf(LAT), desc.annotations().value(AnnotationKeys.LATITUDE));
    }
}
