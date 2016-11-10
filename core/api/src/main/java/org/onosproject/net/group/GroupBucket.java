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
package org.onosproject.net.group;

import org.onosproject.core.GroupId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;

/**
 * Group Bucket definition. A default group Bucket is collection of
 * Instructions that can be performed on a traffic flow. A failover
 * group bucket is associated with a specific port or group that
 * controls its liveness. A select group bucket contains optional
 * weight field to define the weights among the buckets in the group.
 */
public interface GroupBucket {
    /**
     * Returns group type of the bucket.
     *
     * @return GroupType group type
     */
    GroupDescription.Type type();

    /**
     * Returns list of Traffic instructions that are part of the bucket.
     *
     * @return TrafficTreatment traffic instruction list
     */
    TrafficTreatment treatment();

    /**
     * Returns weight of select group bucket.
     *
     * @return short weight associated with a bucket
     */
    short weight();

    /**
     * Returns port number used for liveness detection for a
     * failover bucket.
     *
     * @return PortNumber port number used for liveness detection
     */
    PortNumber watchPort();

    /**
     * Returns group identifier used for liveness detection for a
     * failover bucket.
     *
     * @return GroupId group identifier to be used for liveness detection
     */
    GroupId watchGroup();

    /**
     * Returns the number of packets processed by this group bucket.
     *
     * @return number of packets
     */
    long packets();

    /**
     * Returns the number of bytes processed by this group bucket.
     *
     * @return number of bytes
     */
    long bytes();

    /**
     * Returns whether the given group bucket has the same parameters (weight,
     * watchPort and watchGroup) as this.
     *
     * @param other group bucket to compare
     * @return true if this bucket has the same parameters as other, false otherwise
     */
    boolean hasSameParameters(GroupBucket other);
}
