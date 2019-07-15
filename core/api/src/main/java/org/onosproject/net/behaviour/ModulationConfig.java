/*
 * Copyright 2019-present Open Networking Foundation
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
 * This Work is contributed by Sterlite Technologies
 */
package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import org.onosproject.net.ModulationScheme;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Optional;

/*
 *
 * Modulation operations act on a network port and a component thereof.
 * Supported components are either the full directed port ({@link org.onosproject.net.Direction})
 * or a wavelength on a port ({@link org.onosproject.net.OchSignal}).
 *
 * Modulation are dependent on channel spacing and bitrate
 */
@Beta
public interface ModulationConfig<T> extends HandlerBehaviour {

    /**
     * Get the target Modulation Scheme on the component.
     *
     * @param port the port
     * @param component the port component
     * @return ModulationScheme as per bitRate value
     */
    Optional<ModulationScheme> getModulationScheme(PortNumber port, T component);

    /**
     * Set the target Modulation Scheme on the component.
     *
     * @param port the port
     * @param component the port component
     * @param bitRate bit rate in bps
     */
    void setModulationScheme(PortNumber port, T component, long bitRate);

}

