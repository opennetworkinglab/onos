/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.api;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupDescription.Type;

import java.util.List;

/**
 * Service for setting group rules.
 */
public interface K8sGroupRuleService {

    /**
     * Configures the group table rule.
     *
     * @param appId         application ID
     * @param deviceId      device ID
     * @param groupId       group ID
     * @param type          group type
     * @param buckets       a list of group buckets
     * @param install       true for rule addition, false for rule removal
     */
    void setRule(ApplicationId appId, DeviceId deviceId, int groupId,
                 Type type, List<GroupBucket> buckets, boolean install);

    /**
     * Checks whether has the group in store with given device ID and group ID.
     *
     * @param deviceId      device ID
     * @param groupId       group ID
     * @return true if the group exists, false otherwise
     */
    boolean hasGroup(DeviceId deviceId, int groupId);

    /**
     * Configures buckets to the existing group.
     * With install flag true, this method will add buckets to existing buckets,
     * while with install flag false, this method will remove buckets from
     * existing buckets.
     *
     * @param appId         application ID
     * @param deviceId      device ID
     * @param groupId       group ID
     * @param buckets       a list of group buckets
     * @param install       true for buckets addition, false for buckets removal
     */
    void setBuckets(ApplicationId appId, DeviceId deviceId, int groupId,
                    List<GroupBucket> buckets, boolean install);

    /**
     * Configures buckets.
     *
     * @param appId         application ID
     * @param deviceId      device ID
     * @param groupId       group ID
     * @param buckets       a lit of group buckets
     */
    void setBuckets(ApplicationId appId, DeviceId deviceId, int groupId,
                    List<GroupBucket> buckets);
}
