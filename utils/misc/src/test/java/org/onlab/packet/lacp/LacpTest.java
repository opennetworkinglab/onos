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

package org.onlab.packet.lacp;

import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class LacpTest {
    private static final String PACKET_DUMP = "lacp.bin";

    private static final byte LACP_VERSION = 1;
    private static final Map<Byte, LacpTlv> LACP_TLV = Maps.newHashMap();
    private static final Lacp LACP = new Lacp();

    static {
        LACP_TLV.put(Lacp.TYPE_ACTOR, LacpBaseTlvTest.BASE_TLV);
        LACP_TLV.put(Lacp.TYPE_PARTNER, LacpBaseTlvTest.BASE_TLV);
        LACP_TLV.put(Lacp.TYPE_COLLECTOR, LacpCollectorTlvTest.COLLECTOR_TLV);
        LACP_TLV.put(Lacp.TYPE_TERMINATOR, LacpTerminatorTlvTest.TERMINATOR_TLV);

        LACP.setLacpVersion(LACP_VERSION)
                .setTlv(LACP_TLV);
    }

    private byte[] data;

    @Before
    public void setUp() throws Exception {
        data = Resources.toByteArray(LacpBaseTlvTest.class.getResource(PACKET_DUMP));
    }

    @Test
    public void deserializer() throws Exception {
        Lacp lacp = Lacp.deserializer().deserialize(data, 0, data.length);
        assertEquals(LACP_VERSION, lacp.getLacpVersion());
        assertEquals(LACP_TLV, lacp.getTlv());
    }

    @Test
    public void serialize() {
        assertArrayEquals(data, LACP.serialize());
    }
}