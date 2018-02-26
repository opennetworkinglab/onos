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

import static org.junit.Assert.assertEquals;

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry.InterfaceStatusTlvType;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry.PortStatusTlvType;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry.RemoteMepState;
import org.onosproject.incubator.net.l2monitoring.cfm.SenderIdTlv.SenderIdTlvType;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;

public class RemoteMepEntryTest {

    RemoteMepEntry rmep1;

    @Before
    public void setUp() throws Exception, CfmConfigException {
        rmep1 = DefaultRemoteMepEntry
                .builder(MepId.valueOf((short) 1), RemoteMepState.RMEP_IDLE)
                .failedOrOkTime(Duration.ofSeconds(5))
                .macAddress(MacAddress.valueOf("AA:BB:CC:DD:EE:FF"))
                .rdi(true)
                .portStatusTlvType(PortStatusTlvType.PS_UP)
                .interfaceStatusTlvType(InterfaceStatusTlvType.IS_UP)
                .senderIdTlvType(SenderIdTlvType.SI_MAC_ADDRESS)
                .build();
    }

    @Test
    public void testRemoteMepId() {
        assertEquals(1, rmep1.remoteMepId().value());
    }

    @Test
    public void testState() {
        assertEquals(RemoteMepState.RMEP_IDLE, rmep1.state());
    }

    @Test
    public void testFailedOrOkTime() {
        assertEquals(5, rmep1.failedOrOkTime().getSeconds());
    }

    @Test
    public void testMacAddress() {
        assertEquals("AA:BB:CC:DD:EE:FF", rmep1.macAddress().toString());
    }

    @Test
    public void testRdi() {
        assertEquals(true, rmep1.rdi());
    }

    @Test
    public void testPortStatusTlvType() {
        assertEquals(PortStatusTlvType.PS_UP, rmep1.portStatusTlvType());
    }

    @Test
    public void testInterfaceStatusTlvType() {
        assertEquals(InterfaceStatusTlvType.IS_UP, rmep1.interfaceStatusTlvType());
    }

    @Test
    public void testSenderIdTlvType() {
        assertEquals(SenderIdTlvType.SI_MAC_ADDRESS, rmep1.senderIdTlvType());
    }

}
