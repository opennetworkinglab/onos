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
 * Abstraction of VPN config which contains RD value for the VPN instance.
 */
public class VpnConfig {

    /**
     * RD value for VPN instance.
     */
    private String rd;

    /**
     * Created VPN config.
     */
    public VpnConfig() {
    }

    /**
     * Returns RD value.
     *
     * @return RD value
     */
    public String rd() {
        return rd;
    }

    /**
     * Sets the RD value.
     *
     * @param rd RD value
     */
    public void rd(String rd) {
        this.rd = rd;
    }
}
