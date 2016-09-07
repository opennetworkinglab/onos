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
package org.onosproject.store.group.impl;

import org.onosproject.net.DeviceId;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupStore.UpdateType;

/**
 * Format of the Group store message that is used to
 * communicate with the peer nodes in the cluster.
 */
public final class GroupStoreMessage {
    private final DeviceId deviceId;
    private final GroupKey appCookie;
    private final GroupDescription groupDesc;
    private final UpdateType updateType;
    private final GroupBuckets updateBuckets;
    private final GroupKey newAppCookie;
    private final Type type;

    /**
     * Type of group store request.
     */
    public enum Type {
        ADD,
        UPDATE,
        DELETE,
        FAILOVER
    }

    private GroupStoreMessage(Type type,
                             DeviceId deviceId,
                             GroupKey appCookie,
                             GroupDescription groupDesc,
                             UpdateType updateType,
                             GroupBuckets updateBuckets,
                             GroupKey newAppCookie) {
        this.type = type;
        this.deviceId = deviceId;
        this.appCookie = appCookie;
        this.groupDesc = groupDesc;
        this.updateType = updateType;
        this.updateBuckets = updateBuckets;
        this.newAppCookie = newAppCookie;
    }

    /**
     * Creates a group store message for group ADD request.
     *
     * @param deviceId device identifier in which group to be added
     * @param desc group creation parameters
     * @return constructed group store message
     */
    public static GroupStoreMessage createGroupAddRequestMsg(DeviceId deviceId,
                                                             GroupDescription desc) {
        return new GroupStoreMessage(Type.ADD,
                              deviceId,
                              null,
                              desc,
                              null,
                              null,
                              null);
    }

    /**
     * Creates a group store message for group UPDATE request.
     *
     * @param deviceId the device ID
     * @param appCookie the current group key
     * @param updateType update (add or delete) type
     * @param updateBuckets group buckets for updates
     * @param newAppCookie optional new group key
     * @return constructed group store message
     */
    public static GroupStoreMessage createGroupUpdateRequestMsg(DeviceId deviceId,
                                                        GroupKey appCookie,
                                                        UpdateType updateType,
                                                        GroupBuckets updateBuckets,
                                                        GroupKey newAppCookie) {
        return new GroupStoreMessage(Type.UPDATE,
                              deviceId,
                              appCookie,
                              null,
                              updateType,
                              updateBuckets,
                              newAppCookie);
    }

    /**
     * Creates a group store message for group DELETE request.
     *
     * @param deviceId the device ID
     * @param appCookie the group key
     * @return constructed group store message
     */
    public static GroupStoreMessage createGroupDeleteRequestMsg(DeviceId deviceId,
                                                                GroupKey appCookie) {
        return new GroupStoreMessage(Type.DELETE,
                                     deviceId,
                                     appCookie,
                                     null,
                                     null,
                                     null,
                                     null);
    }

    public static GroupStoreMessage createGroupFailoverMsg(DeviceId deviceId,
                                                           GroupDescription desc) {
        return new GroupStoreMessage(Type.FAILOVER,
                                     deviceId,
                                     desc.appCookie(),
                                     desc,
                                     null,
                                     null,
                                     desc.appCookie());
    }


    /**
     * Returns the device identifier of this group request.
     *
     * @return device identifier
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the application cookie associated with this group request.
     *
     * @return application cookie
     */
    public GroupKey appCookie() {
        return appCookie;
    }

    /**
     * Returns the group create parameters associated with this group request.
     *
     * @return group create parameters
     */
    public GroupDescription groupDesc() {
        return groupDesc;
    }

    /**
     * Returns the group buckets to be updated as part of this group request.
     *
     * @return group buckets to be updated
     */
    public GroupBuckets updateBuckets() {
        return updateBuckets;
    }

    /**
     * Returns the update group operation type.
     *
     * @return update operation type
     */
    public UpdateType updateType() {
        return updateType;
    }

    /**
     * Returns the new application cookie associated with this group operation.
     *
     * @return new application cookie
     */
    public GroupKey newAppCookie() {
        return newAppCookie;
    }

    /**
     * Returns the type of this group operation.
     *
     * @return group message type
     */
    public Type type() {
        return type;
    }
}
