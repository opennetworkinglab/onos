/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual.provider;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupOperation;

import java.util.Collection;

/**
 * Service through which Group providers can inject information into
 * the virtual network core.
 */
public interface VirtualGroupProviderService
        extends VirtualProviderService<VirtualGroupProvider> {
    /**
     * Notifies core if any failure from data plane during group operations.
     *
     * @param networkId the identity of the virtual network where this rule applies
     * @param deviceId the device ID
     * @param operation offended group operation
     */
    void groupOperationFailed(NetworkId networkId, DeviceId deviceId,
                              GroupOperation operation);

    /**
     * Pushes the collection of group detected in the data plane along
     * with statistics.
     *
     * @param networkId the identity of the virtual network where this rule applies
     * @param deviceId device identifier
     * @param groupEntries collection of group entries as seen in data plane
     */
    void pushGroupMetrics(NetworkId networkId,
                          DeviceId deviceId, Collection<Group> groupEntries);

    /**
     * Notifies store of group failovers.
     *
     * @param networkId the identity of the virtual network where this rule applies
     * @param failoverGroups failover groups in which a failover has occurred
     */
    void notifyOfFailovers(NetworkId networkId, Collection<Group> failoverGroups);
}
