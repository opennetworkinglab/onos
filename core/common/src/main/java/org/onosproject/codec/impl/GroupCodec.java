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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.util.HexString;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Group JSON codec.
 */
public final class GroupCodec extends JsonCodec<Group> {
    private final Logger log = getLogger(getClass());

    // JSON field names
    private static final String ID = "id";
    private static final String STATE = "state";
    private static final String LIFE = "life";
    private static final String PACKETS = "packets";
    private static final String BYTES = "bytes";
    private static final String REFERENCE_COUNT = "referenceCount";
    private static final String TYPE = "type";
    private static final String GROUP_ID = "groupId";
    private static final String DEVICE_ID = "deviceId";
    private static final String APP_ID = "appId";
    private static final String APP_COOKIE = "appCookie";
    private static final String GIVEN_GROUP_ID = "givenGroupId";
    private static final String BUCKETS = "buckets";
    private static final String MISSING_MEMBER_MESSAGE =
            " member is required in Group";
    public static final String REST_APP_ID = "org.onosproject.rest";

    @Override
    public ObjectNode encode(Group group, CodecContext context) {
        checkNotNull(group, "Group cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                // a Group id should be an unsigned integer
                .put(ID, Integer.toUnsignedLong(group.id().id()))
                .put(STATE, group.state().toString())
                .put(LIFE, group.life())
                .put(PACKETS, group.packets())
                .put(BYTES, group.bytes())
                .put(REFERENCE_COUNT, group.referenceCount())
                .put(TYPE, group.type().toString())
                .put(DEVICE_ID, group.deviceId().toString());

        if (group.appId() != null) {
            result.put(APP_ID, group.appId().name());
        }

        if (group.appCookie() != null) {
            result.put(APP_COOKIE, group.appCookie().toString());
        }

        if (group.givenGroupId() != null) {
            // a given Group id should be an unsigned integer
            result.put(GIVEN_GROUP_ID, Integer.toUnsignedLong(group.givenGroupId()));
        }

        ArrayNode buckets = context.mapper().createArrayNode();
        group.buckets().buckets().forEach(bucket -> {
            ObjectNode bucketJson = context.codec(GroupBucket.class).encode(bucket, context);
            buckets.add(bucketJson);
        });
        result.set(BUCKETS, buckets);
        return result;
    }

    @Override
    public Group decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        final JsonCodec<GroupBucket> groupBucketCodec = context.codec(GroupBucket.class);
        CoreService coreService = context.getService(CoreService.class);

        // parse uInt Group id from the ID field
        JsonNode idNode = json.get(ID);
        Long id = (null == idNode) ?
                // use GROUP_ID for the corresponding Group id if ID is not supplied
                nullIsIllegal(json.get(GROUP_ID), ID + MISSING_MEMBER_MESSAGE).asLong() :
                idNode.asLong();
        GroupId groupId = GroupId.valueOf(id.intValue());

        // parse uInt Group id given by caller. see the GroupDescription javadoc
        JsonNode givenGroupIdNode = json.get(GIVEN_GROUP_ID);
        // if GIVEN_GROUP_ID is not supplied, set null so the group subsystem
        // to choose the Group id (which is the value of ID indeed).
        // else, must be same with ID to show both originate from the same source
        Integer givenGroupIdInt = (null == givenGroupIdNode) ?
                null : new Long(json.get(GIVEN_GROUP_ID).asLong()).intValue();
        if (givenGroupIdInt != null && !givenGroupIdInt.equals(groupId.id())) {
            throw new IllegalArgumentException(GIVEN_GROUP_ID + " must be same with " + ID);
        }

        // parse group key (appCookie)
        String groupKeyStr = nullIsIllegal(json.get(APP_COOKIE),
                APP_COOKIE + MISSING_MEMBER_MESSAGE).asText();
        if (!groupKeyStr.startsWith("0x")) {
            throw new IllegalArgumentException("APP_COOKIE must be a hex string starts with 0x");
        }
        GroupKey groupKey = new DefaultGroupKey(HexString.fromHexString(
                groupKeyStr.split("0x")[1], ""));

        // parse device id
        DeviceId deviceId = DeviceId.deviceId(nullIsIllegal(json.get(DEVICE_ID),
                DEVICE_ID + MISSING_MEMBER_MESSAGE).asText());

        // application id
        ApplicationId appId = coreService.registerApplication(REST_APP_ID);

        // parse group type
        String type = nullIsIllegal(json.get(TYPE),
                TYPE + MISSING_MEMBER_MESSAGE).asText();
        GroupDescription.Type groupType = null;

        switch (type) {
            case "SELECT":
                groupType = Group.Type.SELECT;
                break;
            case "INDIRECT":
                groupType = Group.Type.INDIRECT;
                break;
            case "ALL":
                groupType = Group.Type.ALL;
                break;
            case "CLONE":
                groupType = Group.Type.CLONE;
                break;
            case "FAILOVER":
                groupType = Group.Type.FAILOVER;
                break;
            default:
                nullIsIllegal(groupType, "The requested group type " + type + " is not valid");
        }

        // parse group buckets
        GroupBuckets buckets = null;
        List<GroupBucket> groupBucketList = new ArrayList<>();
        JsonNode bucketsJson = json.get(BUCKETS);
        checkNotNull(bucketsJson);
        if (bucketsJson != null) {
            IntStream.range(0, bucketsJson.size())
                    .forEach(i -> {
                        ObjectNode bucketJson = get(bucketsJson, i);
                        bucketJson.put("type", type);
                        groupBucketList.add(groupBucketCodec.decode(bucketJson, context));
                    });
            buckets = new GroupBuckets(groupBucketList);
        }

        GroupDescription groupDescription = new DefaultGroupDescription(deviceId,
                groupType, buckets, groupKey, givenGroupIdInt, appId);

        return new DefaultGroup(groupId, groupDescription);
    }
}
