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

import java.util.LinkedList;
import java.util.List;

/**
 * Representation of protocol info of the BGP info. It contains the route
 * protocol and the interfaces which are associated with the information.
 */
public class ProtocolInfo {

    /**
     * Route protocol.
     */
    private RouteProtocol routeProtocol;

    /**
     * Interface details which uses this protocol with respect to IPV4 address.
     */
    private List<AccessInfo> v4Accesses;

    /**
     * Interface details which uses this protocol with respect to IPV6 address.
     */
    private List<AccessInfo> v6Accesses;

    /**
     * Status of IPV4 address family available.
     */
    private boolean ipv4Af;

    /**
     * Status of IPV6 address family available.
     */
    private boolean ipv6Af;

    /**
     * Process id of the protocol info.
     */
    private String processId;

    /**
     * Constructs protocol info.
     */
    public ProtocolInfo() {
    }

    /**
     * Returns the route protocol.
     *
     * @return route protocol
     */
    public RouteProtocol routeProtocol() {
        return routeProtocol;
    }

    /**
     * Sets the route protocol.
     *
     * @param routeProtocol route protocol
     */
    public void routeProtocol(RouteProtocol routeProtocol) {
        this.routeProtocol = routeProtocol;
    }

    /**
     * Returns the process id.
     *
     * @return process id
     */
    public String processId() {
        return processId;
    }

    /**
     * Sets the process id.
     *
     * @param processId process id.
     */
    public void processId(String processId) {
        this.processId = processId;
    }

    /**
     * Returns true if the IPV4 address family uses the protocol info; false
     * otherwise.
     *
     * @return true if IPV4 address family uses; false otherwise
     */
    public boolean isIpv4Af() {
        return ipv4Af;
    }

    /**
     * Sets true if the IPV4 address family uses the protocol info; false
     * otherwise.
     *
     * @param ipv4Af true if IPV4 interface uses; false otherwise
     */
    public void ipv4Af(boolean ipv4Af) {
        this.ipv4Af = ipv4Af;
    }

    /**
     * Returns true if the IPV6 address family uses the protocol info; false
     * otherwise.
     *
     * @return true if IPV6 address family uses; false otherwise
     */
    public boolean isIpv6Af() {
        return ipv6Af;
    }

    /**
     * Sets true if the IPV6 address family uses the protocol info; false
     * otherwise.
     *
     * @param ipv6Af true if IPV6 interface uses; false otherwise
     */
    public void ipv6Af(boolean ipv6Af) {
        this.ipv6Af = ipv6Af;
    }

    /**
     * Returns the list of IPV4 network access information.
     *
     * @return IPV4 network accesses
     */
    public List<AccessInfo> v4Accesses() {
        return v4Accesses;
    }

    /**
     * Sets the list of IPV4 network access information.
     *
     * @param v4Accesses IPV4 network accesses
     */
    public void v4Accesses(List<AccessInfo> v4Accesses) {
        this.v4Accesses = v4Accesses;
    }

    /**
     * Adds a access info to the IPV4 network accesses.
     *
     * @param info IPV4 network access
     */
    public void addV4Access(AccessInfo info) {
        if (v4Accesses == null) {
            v4Accesses = new LinkedList<>();
        }
        v4Accesses.add(info);
    }

    /**
     * Returns the list of IPV6 network access information.
     *
     * @return IPV6 network accesses
     */
    public List<AccessInfo> v6Accesses() {
        return v6Accesses;
    }

    /**
     * Sets the list of IPV6 network access information.
     *
     * @param v6Accesses IPV6 network accesses
     */
    public void v6Accesses(List<AccessInfo> v6Accesses) {
        this.v4Accesses = v6Accesses;
    }

    /**
     * Adds a access info to the IPV6 network accesses.
     * @param info IPV4 network access
     */
    public void addV6Access(AccessInfo info) {
        if (v6Accesses == null) {
            v6Accesses = new LinkedList<>();
        }
        v6Accesses.add(info);
    }
}
