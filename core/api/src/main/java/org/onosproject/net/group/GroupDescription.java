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

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

/**
 * ONOS representation of group description that is used to create
 * a group. It contains immutable properties of a ONOS group construct
 * such as "type", "DeviceId", "appCookie", "appId" and "buckets"
 */
public interface GroupDescription {
    /**
     * Types of the group supported by ONOS.
     */
    enum Type {
        /**
         * Load-balancing among different buckets in a group.
         */
        SELECT,
        /**
         * Single Bucket Group.
         */
        INDIRECT,
        /**
         * Multicast to all buckets in a group.
         */
        ALL,
        /**
         * Similar to {@link Type#ALL} but used for cloning of packets
         * independently of the egress decision (singleton treatment or other
         * group).
         */
        CLONE,
        /**
         * Uses the first live bucket in a group.
         */
        FAILOVER
    }

    /**
     * Returns type of a group object.
     *
     * @return GroupType group type
     */
    Type type();

    /**
     * Returns device identifier on which this group object is created.
     *
     * @return DeviceId device identifier
     */
    DeviceId deviceId();

    /**
     * Returns application identifier that has created this group object.
     *
     * @return ApplicationId application identifier
     */
    ApplicationId appId();

    /**
     * Returns application cookie associated with a group object.
     *
     * @return GroupKey application cookie
     */
    GroupKey appCookie();

    /**
     * Returns groupId passed in by caller.
     *
     * @return Integer group id passed in by caller. May be null if caller
     *                 passed in null to let groupService determine the group id.
     */
    Integer givenGroupId();

    /**
     * Returns group buckets of a group.
     *
     * @return GroupBuckets immutable list of group bucket
     */
    GroupBuckets buckets();
}
