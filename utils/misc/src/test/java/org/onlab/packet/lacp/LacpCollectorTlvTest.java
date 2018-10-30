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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class LacpCollectorTlvTest {
    private static final String PACKET_DUMP = "collectorinfo.bin";

    private byte[] data;

    private static final short COLLECTOR_MAX_DELAY = (short) 32768;

    static final LacpCollectorTlv COLLECTOR_TLV = new LacpCollectorTlv()
            .setCollectorMaxDelay(COLLECTOR_MAX_DELAY);

    @Before
    public void setUp() throws Exception {
         data = Resources.toByteArray(LacpCollectorTlvTest.class.getResource(PACKET_DUMP));
    }

    @Test
    public void deserializer() throws Exception {
        LacpCollectorTlv lacpCollectorTlv = LacpCollectorTlv.deserializer().deserialize(data, 0, data.length);
        assertEquals(COLLECTOR_MAX_DELAY, lacpCollectorTlv.getCollectorMaxDelay());

    }

    @Test
    public void serialize() {
        assertArrayEquals(data, COLLECTOR_TLV.serialize());
    }
}