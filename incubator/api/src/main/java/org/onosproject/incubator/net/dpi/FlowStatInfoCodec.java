/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.dpi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of encoder for FlowStatInfo codec.
 */
public final class FlowStatInfoCodec extends JsonCodec<FlowStatInfo> {

    private final Logger log = getLogger(getClass());

    @Override
    public ObjectNode encode(FlowStatInfo fsi, CodecContext context) {
        checkNotNull(fsi, "FlowStatInfo cannot be null");

        return context.mapper().createObjectNode()
                .put("protocol", fsi.protocol())
                .put("hostAName", fsi.hostAName())
                .put("hostAPort", fsi.hostAPort())
                .put("hostBName", fsi.hostBName())
                .put("hostBPort", fsi.hostBPort())
                .put("detectedProtocol", fsi.detectedProtocol())
                .put("detectedProtocolName", fsi.detectedProtocolName())
                .put("packets", fsi.packets())
                .put("bytes", fsi.bytes())
                .put("hostServerName", fsi.hostServerName());
    }

    @Override
    public FlowStatInfo decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        log.debug("protocol={}, full json={} ", json.get("protocol"), json);
        final String protocol = json.get("protocol").asText();
        final String hostAName = json.get("hostAName").asText();
        final int hostAPort = json.get("hostAPort").asInt();
        final String hostBName = json.get("hostBName").asText();
        final int hostBPort = json.get("hostBPort").asInt();
        final int detectedProtocol = json.get("detectedProtocol").asInt();
        final String detectedProtocolName = json.get("detectedProtocolName").asText();
        final long packets = json.get("packets").asLong();
        final long bytes = json.get("bytes").asLong();
        final String hostServerName = json.get("hostServerName").asText();

        return new FlowStatInfo(protocol,
                                hostAName, hostAPort, hostBName, hostBPort,
                                detectedProtocol, detectedProtocolName,
                                packets, bytes, hostServerName);
    }
}
