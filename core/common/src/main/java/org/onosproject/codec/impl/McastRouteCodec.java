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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.mcast.McastRoute;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Codec to encode and decode a multicast route to and from JSON.
 */
public class McastRouteCodec extends JsonCodec<McastRoute> {

    private static final String SOURCE = "source";
    private static final String GROUP = "group";
    private static final String TYPE = "type";

    @Override
    public ObjectNode encode(McastRoute route, CodecContext context) {
        checkNotNull(route);
        ObjectNode root = context.mapper().createObjectNode()
                .put(TYPE, route.type().toString())
                .put(SOURCE, route.source().toString())
                .put(GROUP, route.group().toString());

        return root;
    }

    @Override
    public McastRoute decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        IpAddress source = IpAddress.valueOf(json.path(SOURCE).asText());
        IpAddress group = IpAddress.valueOf(json.path(GROUP).asText());

        McastRoute route = new McastRoute(source, group, McastRoute.Type.STATIC);

        return route;
    }
}
