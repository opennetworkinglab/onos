/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.codec.impl;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.device.PortStatistics;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Port statistics entry JSON codec.
 */
public final class PortStatisticsCodec extends JsonCodec<PortStatistics> {

    @Override
    public ObjectNode encode(PortStatistics entry, CodecContext context) {
        checkNotNull(entry, "Port Statistics cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put("port", entry.port())
                .put("packetsReceived", entry.packetsReceived())
                .put("packetsSent", entry.packetsSent())
                .put("bytesReceived", entry.bytesReceived())
                .put("bytesSent", entry.bytesSent())
                .put("packetsRxDropped", entry.packetsRxDropped())
                .put("packetsTxDropped", entry.packetsTxDropped())
                .put("packetsRxErrors", entry.packetsRxErrors())
                .put("packetsTxErrors", entry.packetsTxErrors())
                .put("durationSec", entry.durationSec());

        return result;
    }

}

