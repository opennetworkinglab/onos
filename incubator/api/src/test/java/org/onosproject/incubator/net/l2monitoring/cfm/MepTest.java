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
package org.onosproject.incubator.net.l2monitoring.cfm;

import java.time.Duration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMep.DefaultMepBuilder;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.FngAddress;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.FngAddressType;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.LowestFaultDefect;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.MepDirection;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import static org.junit.Assert.*;

public class MepTest {
    private Mep mep1;

    @Before
    public void setUp() throws Exception {
        try {
            mep1 = DefaultMep.builder(MepId.valueOf((short) 1),
                    DeviceId.deviceId("of:12345678"),
                    PortNumber.portNumber(0),
                    MepDirection.UP_MEP,
                    MdIdCharStr.asMdId("md-1"),
                    MaIdCharStr.asMaId("ma-1-1"))
            .primaryVid(VlanId.vlanId((short) 1000))
            .administrativeState(true)
            .cciEnabled(true)
            .ccmLtmPriority(Priority.PRIO6)
            .fngAddress(FngAddress.ipV4Address(IpAddress.valueOf("10.2.3.1")))
            .lowestFaultPriorityDefect(LowestFaultDefect.ERROR_FD_PLUS)
            .defectPresentTime(Duration.ofSeconds(1))
            .defectAbsentTime(Duration.ofSeconds(2))
            .build();
        } catch (CfmConfigException e) {
            throw new Exception(e);
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testPortError() {
        try {
            DefaultMep.builder(MepId.valueOf((short) 1),
                    DeviceId.deviceId("of:12345678"),
                    PortNumber.FLOOD,
                    MepDirection.UP_MEP,
                    MdIdCharStr.asMdId("md-1"),
                    MaIdCharStr.asMaId("ma-1-1"))
            .build();
            fail("Port = FLOOD should throw exception");
        } catch (CfmConfigException e) {
            assertTrue(e.getMessage().contains("Port must be physical"));
        }
    }

    @Test
    public void testMdNameErrorNull() {
        try {
            DefaultMep.builder(MepId.valueOf((short) 1),
                    DeviceId.deviceId("of:12345678"),
                    PortNumber.portNumber(0),
                    MepDirection.UP_MEP,
                    null,
                    MaIdCharStr.asMaId("ma-1-1"))
            .build();
            fail("Null md Name should throw exception");
        } catch (CfmConfigException e) {
            assertTrue(e.getMessage().contains("MdId is null"));
        }
    }

    @Test
    public void testMaNameErrorNull() {
        try {
            DefaultMep.builder(MepId.valueOf((short) 1),
                    DeviceId.deviceId("of:12345678"),
                    PortNumber.portNumber(0),
                    MepDirection.UP_MEP,
                    MdIdCharStr.asMdId("md-1"),
                    null)
            .build();
            fail("Null ma Name should throw exception");
        } catch (CfmConfigException e) {
            assertTrue(e.getMessage().contains("MaId is null"));
        }
    }

    @Test
    public void testMepCopyConstructor() throws CfmConfigException {
        Mep mep2 = (new DefaultMepBuilder(mep1)).build();
        assertEquals(1, mep2.mepId().value());
        assertEquals("md-1", mep2.mdId().mdName());
    }

    @Test
    public void testMepId() {
        assertEquals(1, mep1.mepId().value());
    }

    @Test
    public void testDeviceId() {
        assertEquals("of:12345678", mep1.deviceId().toString());
    }

    @Test
    public void testPort() {
        assertEquals(0, mep1.port().toLong());
    }

    @Test
    public void testDirection() {
        assertEquals(MepDirection.UP_MEP, mep1.direction());
    }

    @Test
    public void testPrimaryVid() {
        assertEquals(1000, mep1.primaryVid().id().intValue());
    }

    @Test
    public void testAdministrativeState() {
        assertTrue(mep1.administrativeState());
    }

    @Test
    public void testWithAdministrativeState() {
        Mep mep2 = mep1.withAdministrativeState(false);
        assertFalse(mep2.administrativeState());
        assertEquals(1, mep2.mepId().value());
    }

    @Test
    public void testCciEnabled() {
        assertTrue(mep1.cciEnabled());
    }

    @Test
    public void testWithCciEnabled() {
        Mep mep2 = mep1.withCciEnabled(false);
        assertFalse(mep2.cciEnabled());
    }

    @Test
    public void testCcmLtmPriority() {
        assertEquals(6, mep1.ccmLtmPriority().ordinal());
    }

    @Test
    public void testWithCcmLtmPriority() throws CfmConfigException {
        Mep mep2 = mep1.withCcmLtmPriority(Priority.PRIO5);
        assertEquals(5, mep2.ccmLtmPriority().ordinal());
        assertEquals(1, mep2.mepId().value());
    }

    @Test
    public void testWithFngAddress() {
        Mep mep2 = mep1.withFngAddress(FngAddress.ipV4Address(IpAddress.valueOf("10.2.3.2")));
        assertEquals(FngAddressType.IPV4, mep2.fngAddress().addressType());
        assertEquals(IpAddress.valueOf("10.2.3.2"), mep2.fngAddress().ipAddress());
    }

    @Test
    public void testFngAddress() {
        assertEquals(FngAddressType.IPV4, mep1.fngAddress().addressType());
        assertEquals(IpAddress.valueOf("10.2.3.1"), mep1.fngAddress().ipAddress());
    }

    @Test
    public void testLowestFaultPriorityDefect() {
        assertEquals(LowestFaultDefect.ERROR_FD_PLUS, mep1.lowestFaultPriorityDefect());
    }

    @Test
    public void testWithLowestFaultPriorityDefect() {
        Mep mep2 = mep1.withLowestFaultPriorityDefect(LowestFaultDefect.XCON_FD_ONLY);
        assertEquals(LowestFaultDefect.XCON_FD_ONLY, mep2.lowestFaultPriorityDefect());
    }

    @Test
    public void testDefectPresetTime() {
        assertEquals(1000, mep1.defectPresentTime().toMillis());
    }

    @Test
    public void testWithDefectPresentTime() {
        Mep mep2 = mep1.withDefectPresentTime(Duration.ofMillis(1500L));
        assertEquals(1500, mep2.defectPresentTime().toMillis());
    }

    @Test
    public void testDefectAbsentTime() {
        assertEquals(2000, mep1.defectAbsentTime().toMillis());
    }

    @Test
    public void testWithDefectAbsentTime() {
        Mep mep2 = mep1.withDefectAbsentTime(Duration.ofMillis(2500L));
        assertEquals(2500, mep2.defectAbsentTime().toMillis());
    }

    @Test
    public void testEqualsObject() throws CfmConfigException {
        Mep mep2 = mep1.withPrimaryVid(VlanId.vlanId((short) 5));
        assertNotEquals(mep1, mep2);
    }

    @Test
    public void testMepToString() {
        assertEquals(mep1.toString(), "DefaultMep{" +
                "mepId=1, " +
                "deviceId=of:12345678, " +
                "port=0, " +
                "direction=UP_MEP, " +
                "mdId=md-1, " +
                "maId=ma-1-1, " +
                "primaryVid=1000, " +
                "administrativeState=true, " +
                "cciEnabled=true, " +
                "ccmLtmPriority=PRIO6, " +
                "fngAddress=FngAddress{addressType=IPV4, ipAddress=10.2.3.1}, " +
                "lowestFaultPriorityDefect=ERROR_FD_PLUS, " +
                "defectPresentTime=PT1S, " +
                "defectAbsentTime=PT2S}");
    }

    @Test
    public void testEquality() throws CfmConfigException {
        assertFalse(mep1.equals(null));
        assertTrue(mep1.equals(mep1));
        assertFalse(mep1.equals(new String("test")));

        Mep mep2 = new DefaultMepBuilder(mep1).build();
        assertTrue(mep1.equals(mep2));
        assertEquals(mep1.hashCode(), mep2.hashCode());

        Mep mep3 = DefaultMep.builder(MepId.valueOf((short) 2),
                DeviceId.deviceId("of:12345680"),
                PortNumber.portNumber(0),
                MepDirection.UP_MEP,
                MdIdCharStr.asMdId("md-3"),
                MaIdCharStr.asMaId("ma-3-3"))
                .build();
        assertFalse(mep1.equals(mep3));
    }
}
