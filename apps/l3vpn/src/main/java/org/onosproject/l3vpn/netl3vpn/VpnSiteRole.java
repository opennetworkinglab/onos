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
 * Representation of VPN instance name and its respective site role for each
 * site.
 */
public class VpnSiteRole {

    /**
     * VPN instance name of the site.
     */
    private String name;

    /**
     * Site role of the site.
     */
    private VpnType role;

    /**
     * Creates VPN instance site role.
     *
     * @param n VPN name
     * @param r site role
     */
    public VpnSiteRole(String n, VpnType r) {
        name = n;
        role = r;
    }

    /**
     * Returns the VPN instance name of the site.
     *
     * @return VPN name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the site role.
     *
     * @return site role
     */
    public VpnType role() {
        return role;
    }
}
