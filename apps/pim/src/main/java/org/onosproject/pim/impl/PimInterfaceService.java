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
package org.onosproject.pim.impl;

import org.onosproject.net.ConnectPoint;

import java.util.Set;

/**
 * Define the PIMInterfaceService.  PIM will use ONOS Interfaces to
 * define PIM Interfaces.  The PIM Application signed up as a Netconfig
 * listener.
 *
 * TODO: Do we need a PIMInterfaceListenerService?  Who should listen to Interfaces changes?
 */
public interface PimInterfaceService {

    /**
     * Returns the PIM interface associated with the given connect point.
     *
     * @param cp the connect point we want to get the PIM interface for
     * @return the PIM interface if it exists, otherwise null
     */
    PimInterface getPimInterface(ConnectPoint cp);

    /**
     * Retrieves the set of all interfaces running PIM.
     *
     * @return set of PIM interfaces
     */
    Set<PimInterface> getPimInterfaces();
}
