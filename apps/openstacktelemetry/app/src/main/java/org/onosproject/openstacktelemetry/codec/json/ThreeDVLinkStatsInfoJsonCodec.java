/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.codec.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstacktelemetry.api.LinkStatsInfo;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * JSON codec for 3DV's LinkStatsInfo.
 */
public final class ThreeDVLinkStatsInfoJsonCodec extends JsonCodec<LinkStatsInfo> {

    private static final String TX_PACKET = "txPacket";
    private static final String RX_PACKET = "rxPacket";
    private static final String TX_BYTE = "txByte";
    private static final String RX_BYTE = "rxByte";
    private static final String TX_DROP = "txDrop";
    private static final String RX_DROP = "rxDrop";
    private static final String TIMESTAMP = "timestamp";

    @Override
    public ObjectNode encode(LinkStatsInfo info, CodecContext context) {
        checkNotNull(info, "LinkStatsInfo cannot be null");

        return context.mapper().createObjectNode()
                .put(TX_PACKET, info.getTxPacket())
                .put(RX_PACKET, info.getRxPacket())
                .put(TX_BYTE, info.getTxByte())
                .put(RX_BYTE, info.getRxByte())
                .put(TX_DROP, info.getTxDrop())
                .put(RX_DROP, info.getRxDrop())
                .put(TIMESTAMP, info.getTimestamp());
    }
}
