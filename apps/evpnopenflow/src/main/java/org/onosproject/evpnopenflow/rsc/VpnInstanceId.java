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

import org.onlab.util.Identifier;

/**
 * Immutable representation of a VPN instance identity.
 */
public final class VpnInstanceId extends Identifier<String> {
    // Public construction is prohibited
    private VpnInstanceId(String vpnInstanceId) {
        super(vpnInstanceId);
    }

    /**
     * Creates a VPN instance identifier.
     *
     * @param vpnInstanceId VPN instance identify string
     * @return VPN instance identifier
     */
    public static VpnInstanceId vpnInstanceId(String vpnInstanceId) {
        return new VpnInstanceId(vpnInstanceId);
    }

    /**
     * Returns VPN instance identifier.
     *
     * @return the VPN instance identifier
     */
    public String vpnInstanceId() {
        return identifier;
    }
}
