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

package org.onosproject.evpnopenflow.rsc.vpnport;

import org.onosproject.event.AbstractEvent;
import org.onosproject.evpnopenflow.rsc.VpnPort;

/**
 * Describes network VPN port event.
 */
public class VpnPortEvent extends AbstractEvent<VpnPortEvent.Type, VpnPort> {

    /**
     * Type of VPN port events.
     */
    public enum Type {
        /**
         * Signifies that VPN port has been set.
         */
        VPN_PORT_SET,
        /**
         * Signifies that VPN port has been deleted.
         */
        VPN_PORT_DELETE,
        /**
         * Signifies that VPN port has been updated.
         */
        VPN_PORT_UPDATE
    }

    /**
     * Creates an event of a given type and for the specified VPN port.
     *
     * @param type    VPN port type
     * @param vpnPort VPN port subject
     */
    public VpnPortEvent(Type type, VpnPort vpnPort) {
        super(type, vpnPort);
    }

    /**
     * Creates an event of a given type and for the specified VPN port.
     *
     * @param type    VPN port type
     * @param vpnPort VPN port subject
     * @param time    occurrence time
     */
    public VpnPortEvent(Type type, VpnPort vpnPort, long time) {
        super(type, vpnPort, time);
    }
}
