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

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

public class DefaultGroupDescription implements GroupDescription {
    private final GroupDescription.Type type;
    private final GroupBuckets buckets;
    private final GroupKey appCookie;
    private final ApplicationId appId;
    private final DeviceId deviceId;

    /**
     *
     * @param deviceId device identifier
     * @param type type of the group
     * @param buckets immutable list of group bucket
     * @param appCookie immutable application cookie to be associated with the group
     * @param appId application id
     *
     * NOTE: The caller of this subsystem MUST ensure the appCookie
     * provided in this API is immutable
     */
    public DefaultGroupDescription(DeviceId deviceId,
                                   GroupDescription.Type type,
                                   GroupBuckets buckets,
                                   GroupKey appCookie,
                                   ApplicationId appId) {
        this.type = checkNotNull(type);
        this.deviceId = checkNotNull(deviceId);
        this.buckets = checkNotNull(buckets);
        this.appCookie = checkNotNull(appCookie);
        this.appId = checkNotNull(appId);
    }

    /**
     * Return type of a group object.
     *
     * @return GroupType group type
     */
    @Override
    public GroupDescription.Type type() {
        return this.type;
    }

    /**
     * Return device identifier on which this group object is created.
     *
     * @return DeviceId device identifier
     */
    @Override
    public DeviceId deviceId() {
        return this.deviceId;
    }

    /**
     * Return application identifier that has created this group object.
     *
     * @return ApplicationId application identifier
     */
    @Override
    public ApplicationId appId() {
        return this.appId;
    }

    /**
     * Return application cookie associated with a group object.
     *
     * @return GroupKey application cookie
     */
    @Override
    public GroupKey appCookie() {
        return this.appCookie;
    }

    /**
     * Return group buckets of a group.
     *
     * @return GroupBuckets immutable list of group bucket
     */
    @Override
    public GroupBuckets buckets() {
        return this.buckets;
    }

}