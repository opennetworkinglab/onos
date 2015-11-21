/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.bgpio.types;

import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Test for IsIsPseudonode Tlv.
 */
public class IsIsPseudonodeTest {
    private final byte[] value1 = new byte[] {0x01, 0x02, 0x01, 0x02, 0x01, 0x02};
    byte value;
    List<Byte> isoNodeID1 = new ArrayList<Byte>();
    ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
    private final byte[] value2 = new byte[] {0x01, 0x02, 0x01, 0x02, 0x01, 0x03};
    List<Byte> isoNodeID2 = new ArrayList<Byte>();
    ChannelBuffer buffer1 = ChannelBuffers.dynamicBuffer();
    private final IsIsPseudonode tlv1 = IsIsPseudonode.of(isoNodeID1, (byte) 1);
    private final IsIsPseudonode sameAsTlv1 = IsIsPseudonode.of(isoNodeID1, (byte) 1);
    private final IsIsPseudonode tlv2 = IsIsPseudonode.of(isoNodeID2, (byte) 1);

    @Test
    public void testEquality() {
        buffer.writeBytes(value1);
        for (int i = 0; i < 6; i++) {
            value = buffer.readByte();
            isoNodeID1.add(value);
        }
        buffer1.writeBytes(value2);
        for (int i = 0; i < 6; i++) {
            value = buffer1.readByte();
            isoNodeID1.add(value);
        }
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
