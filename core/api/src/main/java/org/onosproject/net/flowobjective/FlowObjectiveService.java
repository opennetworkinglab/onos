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
package org.onosproject.net.flowobjective;

import com.google.common.annotations.Beta;

import java.util.List;

import org.onosproject.net.DeviceId;

/**
 * Service for programming data plane flow rules in manner independent of
 * specific device table pipeline configuration.
 */
@Beta
public interface FlowObjectiveService {

    /**
     * Installs the filtering rules onto the specified device.
     *
     * @param deviceId           device identifier
     * @param filteringObjective the filtering objective
     */
    void filter(DeviceId deviceId, FilteringObjective filteringObjective);

    /**
     * Installs the forwarding rules onto the specified device.
     *
     * @param deviceId            device identifier
     * @param forwardingObjective the forwarding objective
     */
    void forward(DeviceId deviceId, ForwardingObjective forwardingObjective);

    /**
     * Installs the next hop elements into the specified device.
     *
     * @param deviceId      device identifier
     * @param nextObjective a next objective
     */
    void next(DeviceId deviceId, NextObjective nextObjective);

    /**
     * Obtains a globally unique next objective.
     *
     * @return an integer
     */
    int allocateNextId();

    /**
     * Provides a composition policy expression.
     * <p>
     * WARNING: This method is a no-op in the default implementation.
     *
     * @param policy policy expression
     */
    void initPolicy(String policy);

    /**
     * Installs the objective onto the specified device.
     *
     * @param deviceId  device identifier
     * @param objective the objective
     */
    default void apply(DeviceId deviceId, Objective objective) {
        if (ForwardingObjective.class.isAssignableFrom(objective.getClass())) {
            forward(deviceId, (ForwardingObjective) objective);
        } else if (FilteringObjective.class.isAssignableFrom(objective.getClass())) {
            filter(deviceId, (FilteringObjective) objective);
        } else if (NextObjective.class.isAssignableFrom(objective.getClass())) {
            next(deviceId, (NextObjective) objective);
        } else {
            throw new UnsupportedOperationException("Unsupported objective of type " + objective.getClass());
        }
    }

    /**
     * Retrieve all nextObjective to group mappings known to this onos instance,
     * in a format meant for display on the CLI, to help with debugging. Applications
     * are only aware of next-Ids, while the group sub-system is only aware of group-ids.
     * This method fills in the gap by providing information on the mapping
     * between next-ids and group-ids done by device-drivers.
     *
     * @return a list of strings preformatted by the device-drivers to provide
     *         information on next-id to group-id mapping. Consumed by the
     *         "obj-next-ids" command on the CLI.
     */
    List<String> getNextMappings();

    /**
     * Retrieve all nextObjectives that are waiting to hear back from device
     * drivers, and the forwarding-objectives that are waiting on the
     * successful completion of the next-objectives. Consumed by the
     * "obj-pending-nexts" command on the CLI.
     *
     * @return a list of strings preformatted to provide information on the
     *          next-ids awaiting confirmation from the device-drivers.
     */
    List<String> getPendingNexts();
}
