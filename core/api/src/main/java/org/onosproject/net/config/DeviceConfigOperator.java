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
package org.onosproject.net.config;

import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceDescription;

import com.google.common.annotations.Beta;

import java.util.Optional;


/**
 * {@link ConfigOperator} for Device.
 * <p>
 * Note: We currently assume {@link DeviceConfigOperator}s are commutative.
 */
@Beta
public interface DeviceConfigOperator extends ConfigOperator {

    /**
     * Binds {@link NetworkConfigService} to use for retrieving configuration.
     *
     * @param networkConfigService the service to use
     */
    void bindService(NetworkConfigService networkConfigService);


    /**
     * Generates a DeviceDescription containing fields from a DeviceDescription and
     * configuration.
     *
     * @param deviceId {@link DeviceId} representing the port.
     * @param descr input {@link DeviceDescription}
     * @param prevConfig previous config {@link Config}
     * @return Combined {@link DeviceDescription}
     */
    DeviceDescription combine(DeviceId deviceId, DeviceDescription descr,
                              Optional<Config> prevConfig);
}
