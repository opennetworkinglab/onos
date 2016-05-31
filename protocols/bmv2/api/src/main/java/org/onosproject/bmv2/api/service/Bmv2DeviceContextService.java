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

package org.onosproject.bmv2.api.service;

import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.net.DeviceId;

/**
 * A service for managing BMv2 device contexts.
 */
public interface Bmv2DeviceContextService {

    // TODO: handle the potential configuration states (e.g. RUNNING, SWAP_REQUESTED, etc.)

    /**
     * Returns the context of a given device. The context returned is the last one for which a configuration swap was
     * triggered, hence there's no guarantees that the device is enforcing the returned context's  configuration at the
     * time of the call.
     *
     * @param deviceId a device ID
     * @return a BMv2 device context
     */
    Bmv2DeviceContext getContext(DeviceId deviceId);

    /**
     * Triggers a configuration swap on a given device.
     *
     * @param deviceId a device ID
     * @param context  a BMv2 device context
     */
    void triggerConfigurationSwap(DeviceId deviceId, Bmv2DeviceContext context);

    /**
     * Binds the given interpreter with the given class loader so that other ONOS instances in the cluster can properly
     * load the interpreter.
     *
     * @param interpreterClass an interpreter class
     * @param loader           a class loader
     */
    void registerInterpreterClassLoader(Class<? extends Bmv2Interpreter> interpreterClass, ClassLoader loader);

    /**
     * Notifies this service that a given device has been updated, meaning a potential context change.
     * It returns true if the device configuration is the same as the last for which a swap was triggered, false
     * otherwise. In the last case, the service will asynchronously trigger a swap to the last
     * configuration stored by this service. If no swap has already been triggered then a default configuration will be
     * applied.
     *
     * @param deviceId a device ID
     * @return a boolean value
     */
    boolean notifyDeviceChange(DeviceId deviceId);
}
