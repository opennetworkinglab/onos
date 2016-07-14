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

package org.onosproject.virtualbng;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map.Entry;

import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;

/**
 * Codec for encoding a IP address mapping entry to JSON.
 */
public final class IpAddressMapEntryCodec extends JsonCodec<Entry<IpAddress, IpAddress>> {

    @Override
    public ObjectNode encode(Entry<IpAddress, IpAddress> entry, CodecContext context) {
        checkNotNull(entry, "IP address map entry cannot be null");
        final ObjectNode result = context.mapper().createObjectNode()
                .put(entry.getKey().toString(), entry.getValue().toString());

        return result;
    }
}
