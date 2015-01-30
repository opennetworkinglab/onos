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

import java.util.Objects;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

/**
 * Default implementation of group description interface.
 */
public class DefaultGroupDescription implements GroupDescription {
    private final GroupDescription.Type type;
    private final GroupBuckets buckets;
    private final GroupKey appCookie;
    private final ApplicationId appId;
    private final DeviceId deviceId;

    /**
     * Constructor to be used by north bound applications.
     * NOTE: The caller of this subsystem MUST ensure the appCookie
     * provided in this API is immutable
     *
     * @param deviceId device identifier
     * @param type type of the group
     * @param buckets immutable list of group bucket
     * @param appCookie immutable application cookie to be associated with the group
     * @param appId application id
     */
    public DefaultGroupDescription(DeviceId deviceId,
                                   GroupDescription.Type type,
                                   GroupBuckets buckets,
                                   GroupKey appCookie,
                                   ApplicationId appId) {
        this.type = checkNotNull(type);
        this.deviceId = checkNotNull(deviceId);
        this.buckets = checkNotNull(buckets);
        this.appCookie = appCookie;
        this.appId = appId;
    }

    /**
     * Constructor to be used by group subsystem internal components.
     * Creates group description object from another object of same type.
     *
     * @param groupDesc group description object
     *
     */
    public DefaultGroupDescription(GroupDescription groupDesc) {
        this.type = groupDesc.type();
        this.deviceId = groupDesc.deviceId();
        this.buckets = groupDesc.buckets();
        this.appCookie = groupDesc.appCookie();
        this.appId = groupDesc.appId();
    }

    /**
     * Constructor to be used by group subsystem internal components.
     * Creates group description object from the information retrieved
     * from data plane.
     *
     * @param deviceId device identifier
     * @param type type of the group
     * @param buckets immutable list of group bucket
     *
     */
    public DefaultGroupDescription(DeviceId deviceId,
                                   GroupDescription.Type type,
                                   GroupBuckets buckets) {
        this(deviceId, type, buckets, null, null);
    }

    /**
     * Returns type of a group object.
     *
     * @return GroupType group type
     */
    @Override
    public GroupDescription.Type type() {
        return this.type;
    }

    /**
     * Returns device identifier on which this group object is created.
     *
     * @return DeviceId device identifier
     */
    @Override
    public DeviceId deviceId() {
        return this.deviceId;
    }

    /**
     * Returns application identifier that has created this group object.
     *
     * @return ApplicationId application identifier
     */
    @Override
    public ApplicationId appId() {
        return this.appId;
    }

    /**
     * Returns application cookie associated with a group object.
     *
     * @return GroupKey application cookie
     */
    @Override
    public GroupKey appCookie() {
        return this.appCookie;
    }

    /**
     * Returns group buckets of a group.
     *
     * @return GroupBuckets immutable list of group bucket
     */
    @Override
    public GroupBuckets buckets() {
        return this.buckets;
    }

    @Override
    /*
     * The deviceId, type and buckets are used for hash.
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public int hashCode() {
        return Objects.hash(deviceId, type, buckets);
    }

    @Override
    /*
     * The deviceId, type and buckets should be same.
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
       if (obj instanceof DefaultGroupDescription) {
            DefaultGroupDescription that = (DefaultGroupDescription) obj;
            return Objects.equals(deviceId, that.deviceId) &&
                    Objects.equals(type, that.type) &&
                    Objects.equals(buckets, that.buckets);

        }
        return false;
    }

}