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
import org.onosproject.openstacktelemetry.api.LinkInfo;
import org.onosproject.openstacktelemetry.api.LinkStatsInfo;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * JSON codec for 3DV's LinkInfo.
 */
public final class ThreeDVLinkInfoJsonCodec extends JsonCodec<LinkInfo> {

    private static final String LINK_ID = "linkId";
    private static final String SRC_IP = "srcIp";
    private static final String DST_IP = "dstIp";
    private static final String SRC_PORT = "srcPort";
    private static final String DST_PORT = "dstPort";
    private static final String PROTOCOL = "protocol";
    private static final String STATS_INFO = "statsInfo";

    @Override
    public ObjectNode encode(LinkInfo info, CodecContext context) {
        checkNotNull(info, "LinkInfo cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(LINK_ID, info.linkId())
                .put(SRC_IP, info.srcIp())
                .put(DST_IP, info.dstIp())
                .put(SRC_PORT, info.srcPort())
                .put(DST_PORT, info.dstPort())
                .put(PROTOCOL, info.protocol());

        ObjectNode statsInfoJson =
                context.codec(LinkStatsInfo.class).encode(info.linkStats(), context);

        result.set(STATS_INFO, statsInfoJson);
        return result;
    }
}
