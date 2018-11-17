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
package org.onosproject.net.config;

import com.google.common.annotations.Beta;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostDescription;

import java.util.Optional;


/**
 * {@link ConfigOperator} for Host.
 * <p>
 * Note: We currently assume {@link HostConfigOperator}s are commutative.
 */
@Beta
public interface HostConfigOperator extends ConfigOperator {

    /**
     * Binds {@link NetworkConfigService} to use for retrieving configuration.
     *
     * @param networkConfigService the service to use
     */
    void bindService(NetworkConfigService networkConfigService);


    /**
     * Generates a HostDescription containing fields from a HostDescription and
     * configuration.
     *
     * @param hostId {@link HostId} representing the port.
     * @param descr input {@link HostDescription}
     * @param prevConfig previous config {@link Config}
     * @return Combined {@link HostDescription}
     */
    HostDescription combine(HostId hostId, HostDescription descr,
                              Optional<Config> prevConfig);
}
