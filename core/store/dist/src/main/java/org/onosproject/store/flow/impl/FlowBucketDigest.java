/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.flow.impl;

import java.util.Objects;

import org.onosproject.store.LogicalTimestamp;

/**
 * Flow bucket digest.
 */
public class FlowBucketDigest {
    private final int bucket;
    private final long term;
    private final LogicalTimestamp timestamp;

    FlowBucketDigest(int bucket, long term, LogicalTimestamp timestamp) {
        this.bucket = bucket;
        this.term = term;
        this.timestamp = timestamp;
    }

    /**
     * Returns the bucket identifier.
     *
     * @return the bucket identifier
     */
    public int bucket() {
        return bucket;
    }

    /**
     * Returns the bucket term.
     *
     * @return the bucket term
     */
    public long term() {
        return term;
    }

    /**
     * Returns the bucket timestamp.
     *
     * @return the bucket timestamp
     */
    public LogicalTimestamp timestamp() {
        return timestamp;
    }

    /**
     * Returns a boolean indicating whether this digest is newer than the given digest.
     *
     * @param digest the digest to check
     * @return indicates whether this digest is newer than the given digest
     */
    public boolean isNewerThan(FlowBucketDigest digest) {
        return digest == null || term() > digest.term() || timestamp().isNewerThan(digest.timestamp());
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucket);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof FlowBucketDigest
            && ((FlowBucketDigest) object).bucket == bucket;
    }
}