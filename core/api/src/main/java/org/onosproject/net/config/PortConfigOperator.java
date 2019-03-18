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

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.PortDescription;

import com.google.common.annotations.Beta;

import java.util.Optional;

/**
 * {@link ConfigOperator} for Port.
 * <p>
 * Note: We currently assumes {@link PortConfigOperator}s are commutative.
 */
@Beta
public interface PortConfigOperator extends ConfigOperator {

    /**
     * Binds {@link NetworkConfigService} to use for retrieving configuration.
     *
     * @param networkConfigService the service to use
     */
    void bindService(NetworkConfigService networkConfigService);

    /**
     * Generates a PortDescription containing fields from a PortDescription and
     * configuration.
     *
     * @param cp {@link ConnectPoint} representing the port.
     * @param descr input {@link PortDescription}
     * @return Combined {@link PortDescription}
     * @deprecated onos-2.0
     */
    @Deprecated
    PortDescription combine(ConnectPoint cp, PortDescription descr);

    /**
     * Generates a PortDescription containing fields from a PortDescription and
     * configuration.
     *
     * @param cp {@link ConnectPoint} representing the port.
     * @param descr input {@link PortDescription}
     * @param prevConf previous Config {@link Config}
     * @return Combined {@link PortDescription}
     */
    PortDescription combine(ConnectPoint cp, PortDescription descr, Optional<Config> prevConf);

    /**
     * Generates a PortDescription containing fields from a PortDescription and
     * configuration.
     *
     * @param did DeviceId which the port described by {@code descr} resides.
     * @param descr input {@link PortDescription}
     * @return Combined {@link PortDescription}
     */
    default PortDescription combine(DeviceId did, PortDescription descr) {
        return combine(new ConnectPoint(did, descr.portNumber()), descr);
    }

}
