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

import java.util.Collection;

import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderService;

/**
 * Service through which Group providers can inject information into
 * the core.
 */
public interface GroupProviderService extends ProviderService<GroupProvider> {

    /**
     * Notifies core if any failure from data plane during group operations.
     *
     * @param deviceId the device ID
     * @param operation offended group operation
     */
    void groupOperationFailed(DeviceId deviceId, GroupOperation operation);

    /**
     * Pushes the collection of group detected in the data plane along
     * with statistics.
     *
     * @param deviceId device identifier
     * @param groupEntries collection of group entries as seen in data plane
     */
    void pushGroupMetrics(DeviceId deviceId, Collection<Group> groupEntries);

    /**
     * Notifies store of group failovers.
     *
     * @param failoverGroups failover groups in which a failover has occurred
     */
    void notifyOfFailovers(Collection<Group> failoverGroups);
}
