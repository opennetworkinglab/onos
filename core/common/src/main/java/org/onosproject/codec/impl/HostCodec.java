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

import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.Host;
import org.onosproject.net.HostLocation;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Host JSON codec.
 */
public final class HostCodec extends AnnotatedCodec<Host> {

    @Override
    public ObjectNode encode(Host host, CodecContext context) {
        checkNotNull(host, "Host cannot be null");
        final JsonCodec<HostLocation> locationCodec =
                context.codec(HostLocation.class);
        final ObjectNode result = context.mapper().createObjectNode()
                .put("id", host.id().toString())
                .put("mac", host.mac().toString())
                .put("vlan", host.vlan().toString())
                .put("configured", host.configured());

        final ArrayNode jsonIpAddresses = result.putArray("ipAddresses");
        for (final IpAddress ipAddress : host.ipAddresses()) {
            jsonIpAddresses.add(ipAddress.toString());
        }
        result.set("ipAddresses", jsonIpAddresses);
        result.set("location", locationCodec.encode(host.location(), context));

        return annotate(result, host, context);
    }

}

