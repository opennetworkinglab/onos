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

import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.TelemetryCodec;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Tina Message ByteBuffer Codec.
 */
public class TinaMessageByteBufferCodec implements TelemetryCodec {

    private static final int HEADER_SIZE = 8;
    private static final int ENTRY_SIZE = 88;
    private static final int MILLISECONDS = 1000;
    private static final short KAFKA_MESSAGE_TYPE = 1;

    /**
     * Encodes a collection flow infos into byte buffer.
     *
     * @param flowInfos a collection of flow info
     * @return encoded byte buffer
     */
    @Override
    public ByteBuffer encode(Set<FlowInfo> flowInfos) {
        ByteBuffer byteBuffer =
                ByteBuffer.allocate(HEADER_SIZE + flowInfos.size() * ENTRY_SIZE);

        byteBuffer.put(buildMessageHeader(flowInfos));
        byteBuffer.put(buildMessageBody(flowInfos));

        return byteBuffer;
    }

    private byte[] buildMessageHeader(Set<FlowInfo> flowInfos) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_SIZE);

        byteBuffer.putShort((short) flowInfos.size());
        byteBuffer.putShort(KAFKA_MESSAGE_TYPE);
        byteBuffer.putInt((int) (System.currentTimeMillis() / MILLISECONDS));

        return byteBuffer.array();
    }

    private byte[] buildMessageBody(Set<FlowInfo> flowInfos) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(flowInfos.size() * ENTRY_SIZE);

        TinaFlowInfoByteBufferCodec codec = new TinaFlowInfoByteBufferCodec();
        flowInfos.forEach(flowInfo -> byteBuffer.put(codec.encode(flowInfo).array()));

        return byteBuffer.array();
    }
}
