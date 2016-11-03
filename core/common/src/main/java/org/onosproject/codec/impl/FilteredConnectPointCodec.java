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
package org.onosproject.codec.impl;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.flow.TrafficSelector;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JSON Codec for {@link FilteredConnectPoint}.
 */
public class FilteredConnectPointCodec extends JsonCodec<FilteredConnectPoint> {

    private static final String CONNECT_POINT = "connectPoint";
    private static final String TRAFFIC_SELECTOR = "trafficSelector";


    @Override
    public ObjectNode encode(FilteredConnectPoint entity,
                             CodecContext context) {
        ObjectNode node = context.mapper().createObjectNode();
        node.set(CONNECT_POINT, context.encode(entity.connectPoint(), ConnectPoint.class));
        node.set(TRAFFIC_SELECTOR, context.encode(entity.trafficSelector(), TrafficSelector.class));
        return node;
    }

    @Override
    public FilteredConnectPoint decode(ObjectNode json, CodecContext context) {
        ConnectPoint cp = context.decode(json.get(CONNECT_POINT), ConnectPoint.class);
        TrafficSelector ts = context.decode(json.get(TRAFFIC_SELECTOR), TrafficSelector.class);
        return new FilteredConnectPoint(cp, ts);
    }
}
