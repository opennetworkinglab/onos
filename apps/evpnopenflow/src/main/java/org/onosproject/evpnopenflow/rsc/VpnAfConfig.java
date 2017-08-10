/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.evpnopenflow.rsc;

import org.onosproject.evpnrouteservice.VpnRouteTarget;

/**
 * Representation of a VPN af configuration.
 */
public interface VpnAfConfig {

    /**
     * Returns the export route policy information.
     *
     * @return export route policy
     */
    String exportRoutePolicy();

    /**
     * Returns the import route policy information.
     *
     * @return export route policy
     */
    String importRoutePolicy();

    /**
     * Returns the route target value.
     *
     * @return route target value
     */
    VpnRouteTarget routeTarget();

    /**
     * Returns the route target type.
     *
     * @return route target type
     */
    String routeTargetType();
}
