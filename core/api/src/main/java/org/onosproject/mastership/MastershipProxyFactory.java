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
package org.onosproject.mastership;

import org.onosproject.net.DeviceId;

/**
 * Mastership-based proxy factory.
 * <p>
 * The mastership-based proxy factory constructs proxy instances for the master node of a given {@link DeviceId}.
 * When a proxy method is invoked, the method will be called on the master node for the device.
 */
public interface MastershipProxyFactory<T> {

    /**
     * Returns the proxy for the given device.
     *
     * @param deviceId the device identifier
     * @return the proxy for the given device
     */
    T getProxyFor(DeviceId deviceId);

}
