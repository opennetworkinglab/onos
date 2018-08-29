/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.routeservice.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.routeservice.Route;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Codec to encode and decode a unicast route to and from JSON.
 */
public class RouteCodec extends JsonCodec<Route> {

    private static final String SOURCE = "source";
    private static final String PREFIX = "prefix";
    private static final String NEXT_HOP = "nextHop";

    @Override
    public ObjectNode encode(Route route, CodecContext context) {
        checkNotNull(route);
        ObjectNode root = context.mapper().createObjectNode()
                .put(SOURCE, route.source().toString())
                .put(PREFIX, route.prefix().toString())
                .put(NEXT_HOP, route.nextHop().toString());

        return root;
    }

    @Override
    public Route decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        IpPrefix prefix = IpPrefix.valueOf(json.path(PREFIX).asText());
        IpAddress nextHop = IpAddress.valueOf(json.path(NEXT_HOP).asText());
        String source = json.path(SOURCE).asText();

        // Routes through the REST API without mentioning source in the json are created as STATIC,
        // otherwise routes are created with corresponding source.

        Route route = source.isEmpty() ?
                new Route(Route.Source.STATIC, prefix, nextHop) :
                new Route(Route.Source.valueOf(source), prefix, nextHop);
        return route;
    }
}
