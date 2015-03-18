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
package org.onosproject.codec.impl;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.PointToPointIntent;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Point to point intent codec.
 */
public final class PointToPointIntentCodec extends JsonCodec<PointToPointIntent> {

    @Override
    public ObjectNode encode(PointToPointIntent intent, CodecContext context) {
        checkNotNull(intent, "Point to point intent cannot be null");

        final JsonCodec<ConnectivityIntent> connectivityIntentCodec =
                context.codec(ConnectivityIntent.class);
        final ObjectNode result = connectivityIntentCodec.encode(intent, context);

        final JsonCodec<ConnectPoint> connectPointCodec =
                context.codec(ConnectPoint.class);
        final ObjectNode ingress =
                connectPointCodec.encode(intent.ingressPoint(), context);
        final ObjectNode egress =
                connectPointCodec.encode(intent.egressPoint(), context);

        result.set("ingressPoint", ingress);
        result.set("egressPoint", egress);

        return result;
    }
}
