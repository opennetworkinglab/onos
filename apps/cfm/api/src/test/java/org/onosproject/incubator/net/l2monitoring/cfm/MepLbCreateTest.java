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
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.util.HexString;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;

public class MepLbCreateTest {

    MepLbCreate lb1;
    MepLbCreate lb2;

    @Before
    public void setUp() throws Exception {
        lb1 = DefaultMepLbCreate
                .builder(MacAddress.valueOf("aa:bb:cc:dd:ee:ff"))
                .numberMessages(5)
                .dataTlv(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06})
                .vlanPriority(Priority.PRIO3)
                .vlanDropEligible(true)
                .build();
        lb2 = DefaultMepLbCreate
                .builder(MepId.valueOf((short) 12))
                .build();

    }

    @Test
    public void testRemoteMepAddress() {
        assertEquals("aa:bb:cc:dd:ee:ff".toUpperCase(),
                lb1.remoteMepAddress().toString());
        assertNull(lb1.remoteMepId());
    }

    @Test
    public void testRemoteMepId() {
        assertEquals(12, lb2.remoteMepId().id().intValue());
        assertNull(lb2.remoteMepAddress());
    }

    @Test
    public void testNumberMessages() {
        assertEquals(5, lb1.numberMessages().intValue());
    }

    @Test
    public void testDataTlv() {
        assertEquals(0x06, HexString.fromHexString(lb1.dataTlvHex())[5]);
    }

    @Test
    public void testVlanPriority() {
        assertEquals(3, lb1.vlanPriority().ordinal());
    }

    @Test
    public void testVlanDropElegible() {
        assertEquals(true, lb1.vlanDropEligible());
    }
}
