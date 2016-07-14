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

import com.google.common.annotations.Beta;
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.net.DeviceId;

/**
 * A service for managing BMv2 device contexts.
 */
@Beta
public interface Bmv2DeviceContextService {

    /**
     * Returns the context of the given device, null if no context has been previously set.
     *
     * @param deviceId a device ID
     * @return a BMv2 device context
     */
    Bmv2DeviceContext getContext(DeviceId deviceId);

    /**
     * Sets the context for the given device.
     *
     * @param deviceId a device ID
     * @param context  a BMv2 device context
     */
    void setContext(DeviceId deviceId, Bmv2DeviceContext context);

    /**
     * Binds the given interpreter with the given class loader so that other ONOS instances in the cluster can properly
     * load the interpreter.
     *
     * @param interpreterClass an interpreter class
     * @param loader           a class loader
     */
    void registerInterpreterClassLoader(Class<? extends Bmv2Interpreter> interpreterClass, ClassLoader loader);

    /**
     * Returns the default context.
     *
     * @return a BMv2 device context
     */
    Bmv2DeviceContext defaultContext();

    /**
     * Sets the default context for the given device.
     */
    void setDefaultContext(DeviceId deviceId);
}
