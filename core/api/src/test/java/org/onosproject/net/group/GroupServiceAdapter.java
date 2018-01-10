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

package org.onosproject.net.group;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

/**
 * Test adapter for group service.
 */
public class GroupServiceAdapter implements GroupService {
    @Override
    public void addGroup(GroupDescription groupDesc) {

    }

    @Override
    public Group getGroup(DeviceId deviceId, GroupKey appCookie) {
        return null;
    }

    @Override
    public void addBucketsToGroup(DeviceId deviceId, GroupKey oldCookie, GroupBuckets buckets,
                                  GroupKey newCookie, ApplicationId appId) {

    }

    @Override
    public void removeBucketsFromGroup(DeviceId deviceId, GroupKey oldCookie, GroupBuckets buckets,
                                       GroupKey newCookie, ApplicationId appId) {

    }

    @Override
    public void purgeGroupEntries(DeviceId deviceId) {

    }

    @Override
    public void removeGroup(DeviceId deviceId, GroupKey appCookie, ApplicationId appId) {

    }

    @Override
    public Iterable<Group> getGroups(DeviceId deviceId, ApplicationId appId) {
        return null;
    }

    @Override
    public Iterable<Group> getGroups(DeviceId deviceId) {
        return null;
    }

    @Override
    public void addListener(GroupListener listener) {

    }

    @Override
    public void removeListener(GroupListener listener) {

    }
}
