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
 * Represents the route protocol of BGP info.
 */
public enum RouteProtocol {

    /**
     * Requested route protocol type is of BGP.
     */
    BGP("bgp"),

    /**
     * Requested route protocol type is of direct.
     */
    DIRECT("direct"),

    /**
     * Requested route protocol type is of OSPF.
     */
    OSPF("ospf"),

    /**
     * Requested route protocol type is of RIP.
     */
    RIP("rip"),

    /**
     * Requested route protocol type is of RIPNG.
     */
    RIP_NG("ripng"),

    /**
     * Requested route protocol type is of VRRP.
     */
    VRRP("vrrp"),

    /**
     * Requested route protocol type is of static.
     */
    STATIC("yangautoprefixstatic");

    /**
     * Defined protocol type from the enum value.
     */
    private final String proType;

    /**
     * Constructs protocol type value from enum.
     *
     * @param proType value of enum
     */
    RouteProtocol(String proType) {
        this.proType = proType;
    }

    /**
     * Returns route protocol for corresponding protocol name.
     *
     * @param name protocol name
     * @return route protocol
     */
    public static RouteProtocol getProType(String name) {
        for (RouteProtocol protocol : values()) {
            if (protocol.proType.equals(name.toLowerCase())) {
                return protocol;
            }
        }
        throw new NetL3VpnException("There is no protocol type as " + name);
    }

}
