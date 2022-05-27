/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnetworking.api.KubevirtHostRoute;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubevirt host route codec used for serializing and de-serializing JSON string.
 */
public final class KubevirtHostRouteCodec extends JsonCodec<KubevirtHostRoute> {

    private final Logger log = getLogger(getClass());

    private static final String DESTINATION = "destination";
    private static final String NEXTHOP = "nexthop";

    private static final String MISSING_MESSAGE = " is required in KubevirtHostRoute";

    @Override
    public ObjectNode encode(KubevirtHostRoute hostRoute, CodecContext context) {
        checkNotNull(hostRoute, "Kubernetes network cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(DESTINATION, hostRoute.destination().toString());

        if (hostRoute.nexthop() != null) {
            result.put(NEXTHOP, hostRoute.nexthop().toString());
        }

        return result;
    }

    @Override
    public KubevirtHostRoute decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String destination = nullIsIllegal(json.get(DESTINATION).asText(),
                DESTINATION + MISSING_MESSAGE);
        JsonNode nexthopJson = json.get(NEXTHOP);
        IpAddress nexthop = null;

        if (nexthopJson != null) {
            nexthop = IpAddress.valueOf(nexthopJson.asText());
        }

        return new KubevirtHostRoute(IpPrefix.valueOf(destination), nexthop);
    }
}
