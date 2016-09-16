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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Single Point to Multi Point intent codec.
 */
public class SinglePointToMultiPointIntentCodec extends JsonCodec<SinglePointToMultiPointIntent> {
    private static final String INGRESS_POINT = "ingressPoint";
    private static final String EGRESS_POINT = "egressPoint";
    private static final String CP_POINTS = "connectPoints";

    @Override
    public ObjectNode encode(SinglePointToMultiPointIntent intent, CodecContext context) {
        checkNotNull(intent, "Single Point to Multi Point intent cannot be null");

        final JsonCodec<ConnectivityIntent> connectivityIntentCodec =
                context.codec(ConnectivityIntent.class);
        final ObjectNode result = connectivityIntentCodec.encode(intent, context);

        final JsonCodec<ConnectPoint> connectPointCodec =
                context.codec(ConnectPoint.class);
        final ObjectNode ingress =
                connectPointCodec.encode(intent.ingressPoint(), context);

        final ObjectNode result2 = context.mapper().createObjectNode();
        final ArrayNode jsonconnectPoints = result2.putArray(CP_POINTS);

        if (intent.egressPoints() != null) {
            for (final ConnectPoint cp : intent.egressPoints()) {
                jsonconnectPoints.add(connectPointCodec.encode(cp, context));
            }
            result.set(EGRESS_POINT, jsonconnectPoints);
        }
        result.set(INGRESS_POINT, ingress);

        return result;
    }

    @Override
    public SinglePointToMultiPointIntent decode(ObjectNode json, CodecContext context) {
        SinglePointToMultiPointIntent.Builder builder = SinglePointToMultiPointIntent.builder();

        IntentCodec.intentAttributes(json, context, builder);
        ConnectivityIntentCodec.intentAttributes(json, context, builder);

        ObjectNode ingressJson = nullIsIllegal(get(json, INGRESS_POINT),
                INGRESS_POINT + IntentCodec.MISSING_MEMBER_MESSAGE);
        ConnectPoint ingress = context.codec(ConnectPoint.class)
                .decode(ingressJson, context);
        builder.ingressPoint(ingress);

        ObjectNode egressJson = nullIsIllegal(get(json, EGRESS_POINT),
                EGRESS_POINT + IntentCodec.MISSING_MEMBER_MESSAGE);
        if (egressJson != null) {
            final JsonCodec<ConnectPoint> connectPointCodec =
                    context.codec(ConnectPoint.class);
            JsonNode connectPointsJson = get(json, EGRESS_POINT).get(CP_POINTS);

            Set<ConnectPoint> egressCp = new HashSet<ConnectPoint>();
            if (connectPointsJson != null) {
                for (int i = 0; i < connectPointsJson.size(); i++) {
                    egressCp.add(connectPointCodec.decode(get(connectPointsJson, i),
                            context));
                }
                builder.egressPoints(egressCp);
            }
        }

        return builder.build();
    }
}
