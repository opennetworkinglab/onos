/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.PointToPointIntent;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Point to point intent codec.
 */
public final class PointToPointIntentCodec extends JsonCodec<PointToPointIntent> {

    private static final String INGRESS_POINT = "ingressPoint";
    private static final String EGRESS_POINT = "egressPoint";

    @Override
    public ObjectNode encode(PointToPointIntent intent, CodecContext context) {
        checkNotNull(intent, "Point to point intent cannot be null");

        final JsonCodec<ConnectivityIntent> connectivityIntentCodec =
                context.codec(ConnectivityIntent.class);
        final ObjectNode result = connectivityIntentCodec.encode(intent, context);

        final JsonCodec<ConnectPoint> connectPointCodec =
                context.codec(ConnectPoint.class);
        final ObjectNode ingress =
                connectPointCodec.encode(intent.filteredIngressPoint().connectPoint(), context);
        final ObjectNode egress =
                connectPointCodec.encode(intent.filteredEgressPoint().connectPoint(), context);

        result.set(INGRESS_POINT, ingress);
        result.set(EGRESS_POINT, egress);

        return result;
    }


    @Override
    public PointToPointIntent decode(ObjectNode json, CodecContext context) {
        PointToPointIntent.Builder builder = PointToPointIntent.builder();

        IntentCodec.intentAttributes(json, context, builder);
        ConnectivityIntentCodec.intentAttributes(json, context, builder);

        ObjectNode ingressJson = nullIsIllegal(get(json, INGRESS_POINT),
                INGRESS_POINT + IntentCodec.MISSING_MEMBER_MESSAGE);
        ConnectPoint ingress = context.codec(ConnectPoint.class)
                .decode(ingressJson, context);
        builder.filteredIngressPoint(new FilteredConnectPoint(ingress));

        ObjectNode egressJson = nullIsIllegal(get(json, EGRESS_POINT),
                EGRESS_POINT + IntentCodec.MISSING_MEMBER_MESSAGE);
        ConnectPoint egress = context.codec(ConnectPoint.class)
                .decode(egressJson, context);
        builder.filteredEgressPoint(new FilteredConnectPoint(egress));

        return builder.build();
    }
}
