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

package org.onosproject.evpnopenflow.manager;

import org.onosproject.evpnopenflow.rsc.VpnPort;
import org.onosproject.evpnrouteservice.EvpnRoute;
import org.onosproject.net.Host;

/**
 * Service for interacting with the route and host events.
 */
public interface EvpnService {
    /**
     * Transfer remote route to private route and set mpls flows out when
     * BgpRoute update.
     *
     * @param route evpn route
     */
    void onBgpEvpnRouteUpdate(EvpnRoute route);

    /**
     * Transfer remote route to private route and delete mpls flows out when
     * BgpRoute delete.
     *
     * @param route evpn route
     */
    void onBgpEvpnRouteDelete(EvpnRoute route);

    /**
     * Get VPN info from EVPN app store and create route, set flows when host
     * detected.
     *
     * @param host host information
     */
    void onHostDetected(Host host);

    /**
     * Get VPN info from EVPN app store and delete route, set flows when
     * host
     * vanished.
     *
     * @param host host information
     */
    void onHostVanished(Host host);

    /**
     * Get VPN info from EVPN app store and create route, set flows when
     * host
     * detected.
     *
     * @param vpnPort vpnPort information
     */
    void onVpnPortSet(VpnPort vpnPort);

    /**
     * Get VPN info from EVPN app store and delete route, set flows when host
     * vanished.
     *
     * @param vpnPort vpnPort information
     */
    void onVpnPortDelete(VpnPort vpnPort);
}
