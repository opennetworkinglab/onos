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

import org.onosproject.openstacktelemetry.api.ByteBufferCodec;
import org.onosproject.openstacktelemetry.api.StatsInfo;
import org.onosproject.openstacktelemetry.api.DefaultStatsInfo;

import java.nio.ByteBuffer;

/**
 * StatsInfo ByteBuffer Codec.
 */
public class TinaStatsInfoByteBufferCodec extends ByteBufferCodec<StatsInfo> {

    private static final int MESSAGE_SIZE = 48;

    @Override
    public ByteBuffer encode(StatsInfo statsInfo) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);

        byteBuffer.putLong(statsInfo.startupTime())
                .putLong(statsInfo.fstPktArrTime())
                .putInt(statsInfo.lstPktOffset())
                .putLong(statsInfo.prevAccBytes())
                .putInt(statsInfo.prevAccPkts())
                .putLong(statsInfo.currAccBytes())
                .putInt(statsInfo.currAccPkts())
                .putShort(statsInfo.errorPkts())
                .putShort(statsInfo.dropPkts());

        return byteBuffer;
    }

    @Override
    public StatsInfo decode(ByteBuffer byteBuffer) {

        long startupTime = byteBuffer.getLong();
        long fstPktArrTime = byteBuffer.getLong();
        int lstPktOffset = byteBuffer.getInt();
        long prevAccBytes = byteBuffer.getLong();
        int prevAccPkts = byteBuffer.getInt();
        long currAccBytes = byteBuffer.getLong();
        int currAccPkts = byteBuffer.getInt();
        short errorPkts = byteBuffer.getShort();
        short dropPkts = byteBuffer.getShort();

        return new DefaultStatsInfo.DefaultBuilder()
                .withStartupTime(startupTime)
                .withFstPktArrTime(fstPktArrTime)
                .withLstPktOffset(lstPktOffset)
                .withPrevAccBytes(prevAccBytes)
                .withPrevAccPkts(prevAccPkts)
                .withCurrAccBytes(currAccBytes)
                .withCurrAccPkts(currAccPkts)
                .withErrorPkts(errorPkts)
                .withDropPkts(dropPkts)
                .build();
    }
}
