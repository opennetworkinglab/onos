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
package org.onosproject.driver.extensions.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.Ip4Address;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.driver.extensions.NiciraSetTunnelDst;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for NiciraSetTunnelDst class.
 */
public final class NiciraSetTunnelDstCodec extends JsonCodec<NiciraSetTunnelDst> {

    private static final String TUNNEL_DST = "tunnelDst";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in NiciraSetTunnelDst";

    @Override
    public ObjectNode encode(NiciraSetTunnelDst niciraSetTunnelDst, CodecContext context) {
        checkNotNull(niciraSetTunnelDst, "Nicira Set Tunnel DST cannot be null");
        ObjectNode root = context.mapper().createObjectNode()
                .put(TUNNEL_DST, niciraSetTunnelDst.tunnelDst().toString());
        return root;
    }

    @Override
    public NiciraSetTunnelDst decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse tunnel destination IP address
        String dstIp = nullIsIllegal(json.get(TUNNEL_DST), TUNNEL_DST + MISSING_MEMBER_MESSAGE).asText();

        Ip4Address tunnelDst = Ip4Address.valueOf(dstIp);

        return new NiciraSetTunnelDst(tunnelDst);
    }
}
