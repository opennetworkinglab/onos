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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Multi Point to Single Point intent codec.
 */
public class MultiPointToSinglePointIntentCodec extends JsonCodec<MultiPointToSinglePointIntent> {
    private static final String INGRESS_POINT = "ingressPoint";
    private static final String EGRESS_POINT = "egressPoint";
    private static final String CP_POINTS = "connectPoints";

    @Override
    public ObjectNode encode(MultiPointToSinglePointIntent intent, CodecContext context) {
        checkNotNull(intent, "Multi Point to Single Point intent cannot be null");

        final JsonCodec<ConnectivityIntent> connectivityIntentCodec =
                context.codec(ConnectivityIntent.class);
        final ObjectNode result = connectivityIntentCodec.encode(intent, context);

        final JsonCodec<ConnectPoint> connectPointCodec =
                context.codec(ConnectPoint.class);
        final ObjectNode egress =
                connectPointCodec.encode(intent.egressPoint(), context);

        // Check ingress are not null and not contain egress
        ObjectNode objectCP = context.mapper().createObjectNode();
        ArrayNode jsonconnectPoints = objectCP.putArray(CP_POINTS);

        if (intent.ingressPoints() != null) {
            for (final ConnectPoint cp : intent.ingressPoints()) {
                jsonconnectPoints.add(connectPointCodec.encode(cp, context));
            }
            result.set(INGRESS_POINT, jsonconnectPoints);
        }
        result.set(EGRESS_POINT, egress);
        return result;
    }

    @Override
    public MultiPointToSinglePointIntent decode(ObjectNode json, CodecContext context) {
        MultiPointToSinglePointIntent.Builder builder = MultiPointToSinglePointIntent.builder();
        IntentCodec.intentAttributes(json, context, builder);
        ConnectivityIntentCodec.intentAttributes(json, context, builder);

        ArrayNode ingressJson = nullIsIllegal((ArrayNode) json.get(INGRESS_POINT),
                                              INGRESS_POINT + IntentCodec.MISSING_MEMBER_MESSAGE);

        if (ingressJson != null) {
            final JsonCodec<ConnectPoint> connectPointCodec =
                    context.codec(ConnectPoint.class);
            JsonNode connectPointsJson = json.get(INGRESS_POINT);

            Set<FilteredConnectPoint> ingressCp = new HashSet<>();
            if (connectPointsJson != null) {
                for (int i = 0; i < connectPointsJson.size(); i++) {
                    ingressCp.add(new FilteredConnectPoint(connectPointCodec.decode(get(connectPointsJson, i),
                                                           context)));
                }
                builder.filteredIngressPoints(ingressCp);
            }
        }

        ObjectNode egressJson = nullIsIllegal(get(json, EGRESS_POINT),
                                              EGRESS_POINT + IntentCodec.MISSING_MEMBER_MESSAGE);
        ConnectPoint egress = context.codec(ConnectPoint.class)
                .decode(egressJson, context);
        builder.filteredEgressPoint(new FilteredConnectPoint(egress));

        return builder.build();
    }
}
