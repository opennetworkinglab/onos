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
    public enum Type {
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
         * Uses the first live bucket in a group.
         */
        FAILOVER
    }

    /**
     * Return type of a group object.
     *
     * @return GroupType group type
     */
    public Type type();

    /**
     * Return device identifier on which this group object is created.
     *
     * @return DeviceId device identifier
     */
    public DeviceId deviceId();

    /**
     * Return application identifier that has created this group object.
     *
     * @return ApplicationId application identifier
     */
    public ApplicationId appId();

    /**
     * Return application cookie associated with a group object.
     *
     * @return GroupKey application cookie
     */
    public GroupKey appCookie();

    /**
     * Return group buckets of a group.
     *
     * @return GroupBuckets immutable list of group bucket
     */
    public GroupBuckets buckets();
}
