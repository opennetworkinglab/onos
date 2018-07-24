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
 *
 */

package org.onlab.packet.lacp;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;

import static org.junit.Assert.*;

public class LacpBaseTlvTest {
    private static final String PACKET_DUMP = "baseinfo.bin";

    private byte[] data;

    private static final short SYS_PRIORITY = (short) 32768;
    private static final MacAddress SYS_MAC = MacAddress.valueOf("a4:23:05:00:11:22");
    private static final short KEY = (short) 13;
    private static final short PORT_PRIORITY = (short) 32768;
    private static final short PORT = 22;
    private static final byte STATE = (byte) 0x85;

    static final LacpBaseTlv BASE_TLV = new LacpBaseTlv()
            .setSystemPriority(SYS_PRIORITY)
            .setSystemMac(SYS_MAC)
            .setKey(KEY)
            .setPortPriority(PORT_PRIORITY)
            .setPort(PORT)
            .setState(STATE);

    @Before
    public void setUp() throws Exception {
         data = Resources.toByteArray(LacpBaseTlvTest.class.getResource(PACKET_DUMP));
    }

    @Test
    public void deserializer() throws Exception {
        LacpBaseTlv actorInfo = LacpBaseTlv.deserializer().deserialize(data, 0, data.length);
        assertEquals(SYS_PRIORITY, actorInfo.getSystemPriority());
        assertEquals(SYS_MAC, actorInfo.getSystemMac());
        assertEquals(KEY, actorInfo.getKey());
        assertEquals(PORT_PRIORITY, actorInfo.getPortPriority());
        assertEquals(PORT, actorInfo.getPort());
        assertEquals(STATE, actorInfo.getState().toByte());
    }

    @Test
    public void serialize() {
        assertArrayEquals(data, BASE_TLV.serialize());
    }
}