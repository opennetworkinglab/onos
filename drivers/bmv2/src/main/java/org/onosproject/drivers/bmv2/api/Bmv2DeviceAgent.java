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
 *
 */

package org.onosproject.drivers.bmv2.api;

import org.onosproject.drivers.bmv2.api.runtime.Bmv2PreGroup;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.net.DeviceId;

import java.util.List;

/**
 * An agent to control a BMv2 device.
 */
public interface Bmv2DeviceAgent {

    /**
     * Returns the device ID of this agent.
     *
     * @return a device id
     */
    DeviceId deviceId();

    /**
     * Creates a multicast group and related entries atomically on a BMv2 device.
     * If successful returns modified version of the group object that is filled with BMv2 switch-specific identifiers
     * of the created group and nodes; throws Bmv2RuntimeException otherwise.
     *
     * @param preGroup Bmv2PreGroup
     * @return modified version of preGroup param.
     * @throws Bmv2RuntimeException if any error occurs
     */
    Bmv2PreGroup writePreGroup(Bmv2PreGroup preGroup) throws Bmv2RuntimeException;

    /**
     * Deletes a multicast group and all associated nodes from a BMV2 device.
     *
     * @param preGroup Bmv2PreGroup
     * @throws Bmv2RuntimeException if any error occurs
     */
    void deletePreGroup(Bmv2PreGroup preGroup) throws Bmv2RuntimeException;

    /**
     * Returns all BMv2 PRE groups.
     *
     * @return list of PRE groups.
     * @throws Bmv2RuntimeException if any error occurs
     */
    List<Bmv2PreGroup> getPreGroups() throws Bmv2RuntimeException;
}
