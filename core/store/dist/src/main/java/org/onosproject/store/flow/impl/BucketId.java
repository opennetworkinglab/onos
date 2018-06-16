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

import org.onosproject.net.DeviceId;

/**
 * Represents a distinct device flow bucket.
 */
public class BucketId {
    private final DeviceId deviceId;
    private final int bucket;

    BucketId(DeviceId deviceId, int bucket) {
        this.deviceId = deviceId;
        this.bucket = bucket;
    }

    /**
     * Returns the bucket device identifier.
     *
     * @return the device identifier
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the bucket number.
     *
     * @return the bucket number
     */
    public int bucket() {
        return bucket;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, bucket);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof BucketId) {
            BucketId that = (BucketId) other;
            return this.deviceId.equals(that.deviceId)
                && this.bucket == that.bucket;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s/%d", deviceId, bucket);
    }
}