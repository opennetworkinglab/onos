/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.mcast.web;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.mcast.api.McastRoute;
import org.onosproject.mcast.api.MulticastRouteService;
import org.slf4j.Logger;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Codec to encode and decode a multicast route to and from JSON.
 */
public class McastHostRouteCodec extends JsonCodec<McastRoute> {

    private Logger log = getLogger(getClass());

    private static final String SOURCE = "source";
    private static final String GROUP = "group";
    private static final String TYPE = "type";
    private static final String SOURCES = "sources";
    private static final String SINKS = "sinks";

    @Override
    public ObjectNode encode(McastRoute route, CodecContext context) {
        checkNotNull(route);

        ObjectNode root = context.mapper().createObjectNode()
                .put(TYPE, route.type().toString())
                .put(GROUP, route.group().toString());
        Optional<IpAddress> sourceIp = route.source();
        if (sourceIp.isPresent()) {
            root.put(SOURCE, sourceIp.get().toString());
        } else {
            root.put(SOURCE, "*");
        }

        ObjectNode sources = context.mapper().createObjectNode();
        context.getService(MulticastRouteService.class).routeData(route).sources().forEach((k, v) -> {
            ArrayNode node = context.mapper().createArrayNode();
            v.forEach(source -> {
                node.add(source.toString());
            });
            sources.putPOJO(k.toString(), node);
        });

        root.putPOJO(SOURCES, sources);

        ObjectNode sinks = context.mapper().createObjectNode();
        context.getService(MulticastRouteService.class).routeData(route).sinks().forEach((k, v) -> {
            ArrayNode node = context.mapper().createArrayNode();
            v.forEach(sink -> {
                node.add(sink.toString());
            });
            sinks.putPOJO(k.toString(), node);
        });
        root.putPOJO(SINKS, sinks);

        return root;
    }

    @Override
    public McastRoute decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String source = json.path(SOURCE).asText();

        IpAddress sourceIp = null;

        if (!source.equals("*")) {
            sourceIp = IpAddress.valueOf(source);
        }

        IpAddress group = IpAddress.valueOf(json.path(GROUP).asText());

        McastRoute route = new McastRoute(sourceIp, group, McastRoute.Type.STATIC);

        return route;
    }
}
