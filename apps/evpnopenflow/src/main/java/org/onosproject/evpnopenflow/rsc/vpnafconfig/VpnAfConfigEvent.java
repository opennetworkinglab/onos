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

package org.onosproject.evpnopenflow.rsc.vpnafconfig;

import org.onosproject.event.AbstractEvent;
import org.onosproject.evpnopenflow.rsc.VpnAfConfig;

/**
 * Describes network VPN af config event.
 */
public class VpnAfConfigEvent extends AbstractEvent<VpnAfConfigEvent.Type, VpnAfConfig> {

    /**
     * Type of VPN port events.
     */
    public enum Type {
        /**
         * Signifies that VPN af config has been set.
         */
        VPN_AF_CONFIG_SET,
        /**
         * Signifies that VPN af config has been deleted.
         */
        VPN_AF_CONFIG_DELETE,
        /**
         * Signifies that VPN af config has been updated.
         */
        VPN_AF_CONFIG_UPDATE
    }

    /**
     * Creates an event of a given type and for the specified VPN af config.
     *
     * @param type        VPN af config type
     * @param vpnAfConfig VPN af config subject
     */
    public VpnAfConfigEvent(Type type, VpnAfConfig vpnAfConfig) {
        super(type, vpnAfConfig);
    }

    /**
     * Creates an event of a given type and for the specified VPN af config.
     *
     * @param type        VPN af config type
     * @param vpnAfConfig VPN af config subject
     * @param time        occurrence time
     */
    public VpnAfConfigEvent(Type type, VpnAfConfig vpnAfConfig, long time) {
        super(type, vpnAfConfig, time);
    }
}
