/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.l3vpn.netl3vpn;

/**
 * Representation of the full mesh VPN configuration containing RT.
 */
public class FullMeshVpnConfig extends VpnConfig {

    /**
     * Route target value.
     */
    private String rt;

    /** Constructs full mesh VPN config.
     *
     * @param r RT value
     */
    public FullMeshVpnConfig(String r) {
        rt = r;
    }

    /**
     * Returns the RT value.
     *
     * @return RT value
     */
    public String rt() {
        return rt;
    }
}
