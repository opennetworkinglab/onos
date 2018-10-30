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

public class LacpTerminatorTlvTest {
    private static final String PACKET_DUMP = "terminatorinfo.bin";

    private byte[] data;

    static final LacpTerminatorTlv TERMINATOR_TLV = new LacpTerminatorTlv();

    @Before
    public void setUp() throws Exception {
         data = Resources.toByteArray(LacpTerminatorTlvTest.class.getResource(PACKET_DUMP));
    }

    @Test
    public void deserializer() throws Exception {
        LacpTerminatorTlv lacpTerminatorTlv = LacpTerminatorTlv.deserializer().deserialize(data, 0, data.length);
    }

    @Test
    public void serialize() {
        assertArrayEquals(data, TERMINATOR_TLV.serialize());
    }
}