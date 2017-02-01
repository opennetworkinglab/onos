/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.GroupId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.GroupBucket;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Group bucket JSON codec.
 */
public class GroupBucketCodec extends JsonCodec<GroupBucket> {

    private static final String TYPE = "type";
    private static final String TREATMENT = "treatment";
    private static final String WEIGHT = "weight";
    private static final String WATCH_PORT = "watchPort";
    private static final String WATCH_GROUP = "watchGroup";
    private static final String PACKETS = "packets";
    private static final String BYTES = "bytes";
    private static final String MISSING_MEMBER_MESSAGE =
            " member is required in Group";

    @Override
    public ObjectNode encode(GroupBucket bucket, CodecContext context) {
        checkNotNull(bucket, "Driver cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(TYPE, bucket.type().toString())
                .put(WEIGHT, bucket.weight())
                .put(PACKETS, bucket.packets())
                .put(BYTES, bucket.bytes());

        if (bucket.watchPort() != null) {
            result.put(WATCH_PORT, bucket.watchPort().toString());
        }

        if (bucket.watchGroup() != null) {
            result.put(WATCH_GROUP, bucket.watchGroup().toString());
        }

        if (bucket.treatment() != null) {
            result.set(TREATMENT, context.codec(TrafficTreatment.class).encode(bucket.treatment(), context));
        }

        return result;
    }

    @Override
    public GroupBucket decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // build traffic treatment
        ObjectNode treatmentJson = get(json, TREATMENT);
        TrafficTreatment trafficTreatment = null;
        if (treatmentJson != null) {
            JsonCodec<TrafficTreatment> treatmentCodec =
                    context.codec(TrafficTreatment.class);
            trafficTreatment = treatmentCodec.decode(treatmentJson, context);
        }

        // parse group type
        String type = nullIsIllegal(json.get(TYPE), TYPE + MISSING_MEMBER_MESSAGE).asText();
        GroupBucket groupBucket = null;

        switch (type) {
            case "SELECT":
                // parse weight
                int weightInt = nullIsIllegal(json.get(WEIGHT), WEIGHT + MISSING_MEMBER_MESSAGE).asInt();

                groupBucket =
                        DefaultGroupBucket.createSelectGroupBucket(trafficTreatment, (short) weightInt);
                break;
            case "INDIRECT":
                groupBucket =
                        DefaultGroupBucket.createIndirectGroupBucket(trafficTreatment);
                break;
            case "ALL":
                groupBucket =
                        DefaultGroupBucket.createAllGroupBucket(trafficTreatment);
                break;
            case "FAILOVER":
                // parse watchPort
                PortNumber watchPort = PortNumber.portNumber(nullIsIllegal(json.get(WATCH_PORT),
                        WATCH_PORT + MISSING_MEMBER_MESSAGE).asText());

                // parse watchGroup
                int groupIdInt = nullIsIllegal(json.get(WATCH_GROUP),
                        WATCH_GROUP + MISSING_MEMBER_MESSAGE).asInt();
                GroupId watchGroup = new GroupId((short) groupIdInt);

                groupBucket =
                        DefaultGroupBucket.createFailoverGroupBucket(trafficTreatment, watchPort, watchGroup);
                break;
            default:
                DefaultGroupBucket.createAllGroupBucket(trafficTreatment);
        }

        return groupBucket;
    }
}
