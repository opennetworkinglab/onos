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

package org.onosproject.incubator.net.dpi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of encoder for ProtocolStatInfo codec.
 */
public final class ProtocolStatInfoCodec extends JsonCodec<ProtocolStatInfo> {

    private final Logger log = getLogger(getClass());

    @Override
    public ObjectNode encode(ProtocolStatInfo psi, CodecContext context) {
        checkNotNull(psi, "ProtocolStatInfo cannot be null");

        return context.mapper().createObjectNode()
                .put("name", psi.name())
                .put("breed", psi.breed())
                .put("packets", psi.packets())
                .put("bytes", psi.bytes())
                .put("flows", psi.flows());
    }

    @Override
    public ProtocolStatInfo decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        log.debug("name={}, full json={} ", json.get("name"), json);
        final String name = json.get("name").asText();
        final String breed = json.get("breed").asText();
        final long packets = json.get("packets").asLong();
        final long bytes = json.get("bytes").asLong();
        final int flows = json.get("flows").asInt();

        return new ProtocolStatInfo(name, breed, packets, bytes, flows);
    }
}
