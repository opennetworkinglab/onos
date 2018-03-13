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

import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.Provider;

/**
 * Abstraction of group provider.
 */
public interface GroupProvider extends Provider {
    /**
     * Group capable property name.
     * A driver is assumed to be group capable if this property is undefined. If
     * the driver is group capable, then it supports group descriptions and
     * optionally group statistics. If the driver is not group capable, then it
     * supports neither group descriptions nor group statistics.
     */
    String GROUP_CAPABLE = "groupCapable";

    /**
     * Performs a batch of group operation in the specified device with the
     * specified parameters.
     *
     * @param deviceId device identifier on which the batch of group
     * operations to be executed
     * @param groupOps immutable list of group operation
     */
    void performGroupOperation(DeviceId deviceId,
                               GroupOperations groupOps);

}
