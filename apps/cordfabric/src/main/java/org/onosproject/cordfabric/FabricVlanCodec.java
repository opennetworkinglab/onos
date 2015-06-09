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

package org.onosproject.cordfabric;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Codec for encoding/decoding a FabricVlan object to/from JSON.
 */
public final class FabricVlanCodec extends JsonCodec<FabricVlan> {

    // JSON field names
    private static final String VLAN = "vlan";
    private static final String PORTS = "ports";
    private static final String IPTV = "iptv";

    @Override
    public ObjectNode encode(FabricVlan vlan, CodecContext context) {
        checkNotNull(vlan, "Vlan cannot be null");
        final ObjectNode result = context.mapper().createObjectNode()
                .put(VLAN, vlan.vlan().toShort());

        final ArrayNode jsonPorts = result.putArray(PORTS);

        vlan.ports().forEach(cp -> jsonPorts.add(context.codec(ConnectPoint.class).encode(cp, context)));

        return result;
    }

    @Override
    public FabricVlan decode(ObjectNode json, CodecContext context) {
        short vlan =  json.path(VLAN).shortValue();
        boolean iptv = json.path(IPTV).booleanValue();
        List<ConnectPoint> ports = new ArrayList<>();

        ArrayNode portArray = (ArrayNode) json.path(PORTS);
        for (JsonNode o : portArray) {
            ports.add(context.codec(ConnectPoint.class).decode((ObjectNode) o, context));
        }

        return new FabricVlan(VlanId.vlanId(vlan), ports, iptv);
    }
}
