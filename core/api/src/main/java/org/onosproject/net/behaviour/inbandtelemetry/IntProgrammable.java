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
package org.onosproject.net.behaviour.inbandtelemetry;

import com.google.common.annotations.Beta;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.HandlerBehaviour;

/**
 * Abstraction of a device implementing In-band Network Telemetry (INT)
 * capabilities.
 */
@Beta
public interface IntProgrammable extends HandlerBehaviour {

    /**
     * INT functionalities that a device can implement.
     */
    enum IntFunctionality {
        /**
         * Source functionality.
         */
        SOURCE,
        /**
         * Sink functionality.
         */
        SINK,
        /**
         * Transit functionality.
         */
        TRANSIT,
        /**
         * Postcard functionality.
         */
        POSTCARD
    }

    /**
     * Initializes the pipeline, by installing required flow rules not relevant
     * to specific watchlist, report and event. Returns true if the operation
     * was successful, false otherwise.
     *
     * @return true if successful, false otherwise
     */
    boolean init();

    /**
     * Configures the given port as an INT source port. Packets received via
     * this port can be modified to add the INT header, if a corresponding INT
     * objective is matched. Returns true if the operation was successful, false
     * otherwise.
     *
     * @param port port
     * @return true if successful, false otherwise
     */
    boolean setSourcePort(PortNumber port);

    /**
     * Configures the given port as an INT sink port. Packets forwarded via this
     * port will be stripped of the INT header and a corresponding INT report
     * will be generated. Returns true if the operation was successful, false
     * otherwise.
     *
     * @param port port
     * @return true if successful, false otherwise
     */
    boolean setSinkPort(PortNumber port);

    /**
     * Adds a given IntObjective to the device.
     *
     * @param obj an IntObjective
     * @return true if the objective was successfully added; false otherwise.
     */
    boolean addIntObjective(IntObjective obj);

    /**
     * Removes a given IntObjective entry from the device.
     *
     * @param obj an IntObjective
     * @return true if the objective was successfully removed; false otherwise.
     */
    boolean removeIntObjective(IntObjective obj);

    /**
     * Set up report-related configuration.
     *
     * @param config a configuration regarding to the collector
     * @return true if the objective is successfully added; false otherwise.
     */
    boolean setupIntConfig(IntDeviceConfig config);

    /**
     * Clean up any INT-related configuration from the device.
     */
    void cleanup();

    /**
     * Returns true if this device supports the given INT functionality.
     *
     * @param functionality INt functionality
     * @return true if functionality is supported, false otherwise
     */
    boolean supportsFunctionality(IntFunctionality functionality);

    //TODO: [ONOS-7616] Design IntEvent and related APIs
}
