/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.collect.ImmutableList;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Collection;

/**
 * Group programmable device behaviour.
 */
public interface GroupProgrammable extends HandlerBehaviour {

    /**
     * Performs the Group operations for the specified device.
     *
     * @param deviceId ID of the device
     * @param groupOps operations to be performed
     */
    void performGroupOperation(DeviceId deviceId, GroupOperations groupOps);

    /**
     * Queries the groups from the device.
     *
     * @return collection of groups
     */
    default Collection<Group> getGroups() {
        return ImmutableList.of();
    }
}
