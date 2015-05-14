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
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Group JSON codec.
 */
public final class GroupCodec extends JsonCodec<Group> {
    // JSON field names
    private static final String ID = "id";
    private static final String STATE = "state";
    private static final String LIFE = "life";
    private static final String PACKETS = "packets";
    private static final String BYTES = "bytes";
    private static final String REFERENCE_COUNT = "referenceCount";
    private static final String TYPE = "type";
    private static final String DEVICE_ID = "deviceId";
    private static final String APP_ID = "appId";
    private static final String APP_COOKIE  = "appCookie";
    private static final String GIVEN_GROUP_ID  = "givenGroupId";
    private static final String BUCKETS = "buckets";

    @Override
    public ObjectNode encode(Group group, CodecContext context) {
        checkNotNull(group, "Group cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(ID, group.id().toString())
                .put(STATE, group.state().toString())
                .put(LIFE, group.life())
                .put(PACKETS, group.packets())
                .put(BYTES, group.bytes())
                .put(REFERENCE_COUNT, group.referenceCount())
                .put(TYPE, group.type().toString())
                .put(DEVICE_ID, group.deviceId().toString());

        if (group.appId() != null) {
            result.put(APP_ID, group.appId().toString());
        }

        if (group.appCookie() != null) {
            result.put(APP_COOKIE, group.appCookie().toString());
        }

        if (group.givenGroupId() != null) {
            result.put(GIVEN_GROUP_ID, group.givenGroupId());
        }

        ArrayNode buckets = context.mapper().createArrayNode();
        group.buckets().buckets().forEach(bucket -> {
                    ObjectNode bucketJson = context.codec(GroupBucket.class).encode(bucket, context);
                    buckets.add(bucketJson);
                });
        result.set(BUCKETS, buckets);
        return result;
    }
}
