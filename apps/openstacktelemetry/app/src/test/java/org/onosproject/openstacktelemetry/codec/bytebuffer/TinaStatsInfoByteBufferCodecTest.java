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
package org.onosproject.openstacktelemetry.codec.bytebuffer;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.openstacktelemetry.api.DefaultStatsInfo;
import org.onosproject.openstacktelemetry.api.StatsInfo;

import java.nio.ByteBuffer;

/**
 * Unit tests for TinaStatsInfoByteBufferCodef.
 */
public class TinaStatsInfoByteBufferCodecTest {

    private static final int STARTUP_TIME = 1000;
    private static final int CURRENT_ACCUMULATED_PACKETS = 8000;
    private static final long CURRENT_ACCUMULATED_BYTES = 9000;
    private static final int PREVIOUS_ACCUMULATED_PACKETS = 8000;
    private static final long PREVIOUS_ACCUMULATED_BYTES = 9000;
    private static final long FIRST_PACKET_ARRIVAL_TIME = 10000;
    private static final int LAST_PACKET_OFFSET = 20000;
    private static final short ERROR_PACKETS = 30000;
    private static final short DROP_PACKETS = 30000;

    private StatsInfo info;
    private final TinaStatsInfoByteBufferCodec codec = new TinaStatsInfoByteBufferCodec();

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setup() {
        StatsInfo.Builder builder = new DefaultStatsInfo.DefaultBuilder();

        info = builder
                .withStartupTime(STARTUP_TIME)
                .withCurrAccPkts(CURRENT_ACCUMULATED_PACKETS)
                .withCurrAccBytes(CURRENT_ACCUMULATED_BYTES)
                .withPrevAccPkts(PREVIOUS_ACCUMULATED_PACKETS)
                .withPrevAccBytes(PREVIOUS_ACCUMULATED_BYTES)
                .withFstPktArrTime(FIRST_PACKET_ARRIVAL_TIME)
                .withLstPktOffset(LAST_PACKET_OFFSET)
                .withErrorPkts(ERROR_PACKETS)
                .withDropPkts(DROP_PACKETS)
                .build();
    }

    /**
     * Tests codec encode and decode.
     */
    @Test
    public void testEncodeDecode() {
        ByteBuffer buffer = codec.encode(info);
        StatsInfo decoded = codec.decode(ByteBuffer.wrap(buffer.array()));
        new EqualsTester().addEqualityGroup(info, decoded).testEquals();
    }
}
