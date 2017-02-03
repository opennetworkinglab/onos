/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.newoptical;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.util.Bandwidth;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;

import static org.junit.Assert.*;

/**
 * Test class for PacketLinkRealizedByOptical class.
 */
public class PacketLinkRealizedByOpticalTest {

    private final ApplicationId appId = new DefaultApplicationId(0, "PacketLinkRealizedByOpticalTest");
    private IdGenerator idGenerator;

    @Before
    public void setUp() {
        idGenerator = new IdGenerator() {
            int counter = 1;

            @Override
            public long getNewId() {
                return counter++;
            }
        };

        Intent.unbindIdGenerator(idGenerator);
        Intent.bindIdGenerator(idGenerator);
    }

    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }

    /**
     * Checks the construction of PacketLinkRealizedByOptical object with all parameters specified.
     */
    @Test
    public void testCreate() {
        ConnectPoint cp1 = new ConnectPoint(DeviceId.deviceId("of:0000000000000001"), PortNumber.portNumber(1L));
        ConnectPoint cp2 = new ConnectPoint(DeviceId.deviceId("of:0000000000000002"), PortNumber.portNumber(2L));
        Key key = Key.of(10L, appId);
        Bandwidth bandwidth = Bandwidth.bps(100L);

        PacketLinkRealizedByOptical plink = new PacketLinkRealizedByOptical(cp1, cp2, key, bandwidth);

        assertNotNull(plink);
        assertEquals(plink.src(), cp1);
        assertEquals(plink.dst(), cp2);
        assertEquals((long) plink.bandwidth().bps(), 100L);
    }

    /**
     * Checks the construction of OpticalConnectivityId object with OpticalCircuitIntent.
     */
    @Test
    public void testCreateWithCircuitIntent() {
        ConnectPoint cp1 = new ConnectPoint(DeviceId.deviceId("of:0000000000000001"), PortNumber.portNumber(1L));
        ConnectPoint cp2 = new ConnectPoint(DeviceId.deviceId("of:0000000000000002"), PortNumber.portNumber(2L));
        OpticalCircuitIntent circuitIntent = OpticalCircuitIntent.builder()
                .appId(appId)
                .src(cp1)
                .dst(cp2)
                .bidirectional(true)
                .key(Key.of(0, appId))
                .signalType(CltSignalType.CLT_1GBE)
                .build();

        PacketLinkRealizedByOptical plink = PacketLinkRealizedByOptical.create(cp1, cp2, circuitIntent);

        assertNotNull(plink);
        assertEquals(plink.src(), cp1);
        assertEquals(plink.dst(), cp2);
        assertEquals((long) plink.bandwidth().bps(), CltSignalType.CLT_1GBE.bitRate());
    }

    /**
     * Checks the construction of OpticalConnectivityId object with OpticalConnectivityIntent.
     */
    @Test
    public void testCreateWithConnectivityIntent() {
        ConnectPoint cp1 = new ConnectPoint(DeviceId.deviceId("of:0000000000000001"), PortNumber.portNumber(1L));
        ConnectPoint cp2 = new ConnectPoint(DeviceId.deviceId("of:0000000000000002"), PortNumber.portNumber(2L));
        OpticalConnectivityIntent connIntent = OpticalConnectivityIntent.builder()
                .appId(appId)
                .src(cp1)
                .dst(cp2)
                .bidirectional(true)
                .key(Key.of(0, appId))
                .signalType(OduSignalType.ODU4)
                .build();

        PacketLinkRealizedByOptical plink = PacketLinkRealizedByOptical.create(cp1, cp2, connIntent);

        assertNotNull(plink);
        assertEquals(plink.src(), cp1);
        assertEquals(plink.dst(), cp2);
        assertEquals((long) plink.bandwidth().bps(), OduSignalType.ODU4.bitRate());
    }

    /**
     * Checks that isBetween() method works.
     */
    @Test
    public void testIsBetween() {
        ConnectPoint cp1 = new ConnectPoint(DeviceId.deviceId("of:0000000000000001"), PortNumber.portNumber(1L));
        ConnectPoint cp2 = new ConnectPoint(DeviceId.deviceId("of:0000000000000002"), PortNumber.portNumber(2L));
        ConnectPoint cp3 = new ConnectPoint(DeviceId.deviceId("of:0000000000000003"), PortNumber.portNumber(3L));
        OpticalCircuitIntent ochIntent = OpticalCircuitIntent.builder()
                .appId(appId)
                .src(cp1)
                .dst(cp2)
                .bidirectional(true)
                .key(Key.of(0, appId))
                .signalType(CltSignalType.CLT_1GBE)
                .build();

        PacketLinkRealizedByOptical plink = PacketLinkRealizedByOptical.create(cp1, cp2, ochIntent);

        assertTrue(plink.isBetween(cp1, cp2));
        assertFalse(plink.isBetween(cp1, cp3));
    }
}