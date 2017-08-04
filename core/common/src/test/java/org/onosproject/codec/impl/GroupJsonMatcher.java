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

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Hamcrest matcher for groups.
 */

public final class GroupJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final Group group;

    private GroupJsonMatcher(Group group) {
        this.group = group;
    }

    @Override
    public boolean matchesSafely(JsonNode jsonGroup, Description description) {
        // check id
        String jsonGroupId = jsonGroup.get("id").asText();
        String groupId = group.id().id().toString();
        if (!jsonGroupId.equals(groupId)) {
            description.appendText("group id was " + jsonGroupId);
            return false;
        }

        // check state
        String jsonState = jsonGroup.get("state").asText();
        String state = group.state().toString();
        if (!jsonState.equals(state)) {
            description.appendText("state was " + jsonState);
            return false;
        }

        // check life
        long jsonLife = jsonGroup.get("life").asLong();
        long life = group.life();
        if (life != jsonLife) {
            description.appendText("life was " + jsonLife);
            return false;
        }

        // check bytes
        long jsonBytes = jsonGroup.get("bytes").asLong();
        long bytes = group.bytes();
        if (bytes != jsonBytes) {
            description.appendText("bytes was " + jsonBytes);
            return false;
        }

        // check packets
        long jsonPackets = jsonGroup.get("packets").asLong();
        long packets = group.packets();
        if (packets != jsonPackets) {
            description.appendText("packets was " + jsonPackets);
            return false;
        }

        // check size of bucket array
        JsonNode jsonBuckets = jsonGroup.get("buckets");
        if (jsonBuckets.size() != group.buckets().buckets().size()) {
            description.appendText("buckets size was " + jsonBuckets.size());
            return false;
        }

        // Check buckets
        for (GroupBucket bucket : group.buckets().buckets()) {
            boolean bucketFound = false;
            for (int bucketIndex = 0; bucketIndex < jsonBuckets.size(); bucketIndex++) {
                GroupBucketJsonMatcher bucketMatcher =
                        GroupBucketJsonMatcher.matchesGroupBucket(bucket);
                if (bucketMatcher.matches(jsonBuckets.get(bucketIndex))) {
                    bucketFound = true;
                    break;
                }
            }
            if (!bucketFound) {
                description.appendText("bucket not found " + bucket.toString());
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(group.toString());
    }

    /**
     * Factory to allocate a group matcher.
     *
     * @param group group object we are looking for
     * @return matcher
     */
    public static GroupJsonMatcher matchesGroup(Group group) {
        return new GroupJsonMatcher(group);
    }
}
