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

import org.onosproject.core.ApplicationId;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;

/**
 * Service for create/update/delete "group" in the devices.
 * Flow entries can point to a "group" defined in the devices that enables
 * to represent additional methods of forwarding like load-balancing or
 * failover among different group of ports or multicast to all ports
 * specified in a group.
 * "group" can also be used for grouping common actions of different flows,
 * so that in some scenarios only one group entry required to be modified
 * for all the referencing flow entries instead of modifying all of them.
 *
 * This implements semantics of a distributed authoritative group store
 * where the master copy of the groups lies with the controller and
 * the devices hold only the 'cached' copy.
 */
public interface GroupService
    extends ListenerService<GroupEvent, GroupListener> {

    /**
     * Creates a group in the specified device with the provided buckets.
     * This API provides an option for application to associate a cookie
     * while creating a group, so that applications can look-up the
     * groups based on the cookies. These Groups will be retained by
     * the core system and re-applied if any groups found missing in the
     * device when it reconnects. This API would immediately return after
     * submitting the request locally or to a remote Master controller
     * instance. As a response to this API invocation, GROUP_ADDED or
     * GROUP_ADD_FAILED notifications would be provided along with cookie
     * depending on the result of the operation on the device in the
     * data plane. The caller may also use "getGroup" API to get the
     * Group object created as part of this request.
     *
     * @param groupDesc group creation parameters
     *
     */
    void addGroup(GroupDescription groupDesc);

    /**
     * Returns a group object associated to an application cookie.
     *
     * NOTE1: The presence of group object in the system does not
     * guarantee that the "group" is actually created in device.
     * GROUP_ADDED notification would confirm the creation of
     * this group in data plane.
     *
     * @param deviceId device identifier
     * @param appCookie application cookie to be used for lookup
     * @return group associated with the application cookie or
     *               NULL if Group is not found for the provided cookie
     */
    Group getGroup(DeviceId deviceId, GroupKey appCookie);

    /**
     * Appends buckets to existing group. The caller can optionally
     * associate a new cookie during this updation. GROUP_UPDATED or
     * GROUP_UPDATE_FAILED notifications would be provided along with
     * cookie depending on the result of the operation on the device.
     *
     * @param deviceId device identifier
     * @param oldCookie cookie to be used to retrieve the existing group
     * @param buckets immutable list of group bucket to be added
     * @param newCookie immutable cookie to be used post update operation
     * @param appId Application Id
     */
    void addBucketsToGroup(DeviceId deviceId,
                           GroupKey oldCookie,
                           GroupBuckets buckets,
                           GroupKey newCookie,
                           ApplicationId appId);

    /**
     * Removes buckets from existing group. The caller can optionally
     * associate a new cookie during this updation. GROUP_UPDATED or
     * GROUP_UPDATE_FAILED notifications would be provided along with
     * cookie depending on the result of the operation on the device.
     *
     * @param deviceId device identifier
     * @param oldCookie cookie to be used to retrieve the existing group
     * @param buckets immutable list of group bucket to be removed
     * @param newCookie immutable cookie to be used post update operation
     * @param appId Application Id
     */
    void removeBucketsFromGroup(DeviceId deviceId,
                                GroupKey oldCookie,
                                GroupBuckets buckets,
                                GroupKey newCookie,
                                ApplicationId appId);

    /**
     * Purges all the group entries on the specified device.
     * @param deviceId device identifier
     */
    void purgeGroupEntries(DeviceId deviceId);

    /**
     * Deletes a group associated to an application cookie.
     * GROUP_DELETED or GROUP_DELETE_FAILED notifications would be
     * provided along with cookie depending on the result of the
     * operation on the device.
     *
     * @param deviceId device identifier
     * @param appCookie application cookie to be used for lookup
     * @param appId Application Id
     */
    void removeGroup(DeviceId deviceId, GroupKey appCookie, ApplicationId appId);

    /**
     * Retrieves all groups created by an application in the specified device
     * as seen by current controller instance.
     *
     * @param deviceId device identifier
     * @param appId application id
     * @return collection of immutable group objects created by the application
     */
    Iterable<Group> getGroups(DeviceId deviceId, ApplicationId appId);

    /**
     * Returns all groups associated with the given device.
     *
     * @param deviceId device ID to get groups for
     * @return iterable of device's groups
     */
    Iterable<Group> getGroups(DeviceId deviceId);

}
