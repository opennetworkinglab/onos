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
package org.onosproject.net.group;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Immutable collection of group bucket.
 */
public final class GroupBuckets {
    private final List<GroupBucket> buckets;

    /**
     * Creates a immutable list of group bucket.
     *
     * @param buckets list of group bucket
     */
    public GroupBuckets(List<GroupBucket> buckets) {
        this.buckets = ImmutableList.copyOf(checkNotNull(buckets));
    }

    /**
     * Returns immutable list of group buckets.
     *
     * @return list of group bucket
     */
    public List<GroupBucket> buckets() {
        return buckets;
    }

    @Override
    public int hashCode() {
        int result = 17;
        int combinedHash = 0;
        for (GroupBucket bucket:buckets) {
            combinedHash = combinedHash + bucket.hashCode();
        }
        result = 31 * result + combinedHash;

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GroupBuckets) {
            return (this.buckets.containsAll(((GroupBuckets) obj).buckets) &&
                    ((GroupBuckets) obj).buckets.containsAll(this.buckets));
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("buckets", buckets.toString())
                .toString();
    }
}