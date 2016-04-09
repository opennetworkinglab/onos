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

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.net.group.GroupBucket;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Hamcrest matcher for instructions.
 */
public final class GroupBucketJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final GroupBucket bucket;

    private GroupBucketJsonMatcher(GroupBucket bucket) {
        this.bucket = bucket;
    }

    /**
     * Matches the contents of a group bucket.
     *
     * @param bucketJson JSON representation of bucket to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    @Override
    public boolean matchesSafely(JsonNode bucketJson, Description description) {

        // check type
        final String jsonType = bucketJson.get("type").textValue();
        if (!bucket.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        final long jsonWeight = bucketJson.get("weight").longValue();
        if (bucket.weight() != jsonWeight) {
            description.appendText("weight was " + jsonWeight);
            return false;
        }

        final long packetsJson = bucketJson.get("packets").asLong();
        if (bucket.packets() != packetsJson) {
            description.appendText("packets was " + packetsJson);
            return false;
        }

        final long bytesJson = bucketJson.get("bytes").asLong();
        if (bucket.bytes() != bytesJson) {
            description.appendText("bytes was " + packetsJson);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(bucket.toString());
    }

    /**
     * Factory to allocate an bucket matcher.
     *
     * @param bucket bucket object we are looking for
     * @return matcher
     */
    public static GroupBucketJsonMatcher matchesGroupBucket(GroupBucket bucket) {
        return new GroupBucketJsonMatcher(bucket);
    }
}
