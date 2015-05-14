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
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.GroupBucket;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

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
}
