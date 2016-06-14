/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.ospf.controller.util;

import org.onlab.packet.Ip4Address;

/**
 * Represents a router who is eligible for DR election.
 */
public class OspfEligibleRouter {

    private Ip4Address ipAddress;
    private Ip4Address routerId;
    private int routerPriority;
    private boolean isDr;
    private boolean isBdr;

    /**
     * Creates an instance.
     * Initialize IP address of eligible router.
     */
    public OspfEligibleRouter() {
        ipAddress = Ip4Address.valueOf("0.0.0.0");
    }

    /**
     * Gets the value of IP address.
     *
     * @return IP address
     */
    public Ip4Address getIpAddress() {
        return ipAddress;
    }

    /**
     * Sets the value of IP address.
     *
     * @param ipAddress IP address
     */
    public void setIpAddress(Ip4Address ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Gets the value of router id.
     *
     * @return router id.
     */
    public Ip4Address getRouterId() {
        return routerId;
    }

    /**
     * Sets the value of router id.
     *
     * @param routerId router id
     */
    public void setRouterId(Ip4Address routerId) {
        this.routerId = routerId;
    }

    /**
     * Gets the value of router priority.
     *
     * @return router priority.
     */
    public int getRouterPriority() {
        return routerPriority;
    }

    /**
     * Sets the value of router priority.
     *
     * @param routerPriority router priority
     */
    public void setRouterPriority(int routerPriority) {
        this.routerPriority = routerPriority;
    }

    /**
     * Gets whether the router is DR.
     *
     * @return boolean true if router is DR else return false.
     */
    public boolean isDr() {
        return isDr;
    }

    /**
     * Sets the router is DR or not.
     *
     * @param isDr router is DR or not
     */
    public void setIsDr(boolean isDr) {
        this.isDr = isDr;
    }

    /**
     * Gets whether the router is BDR or not.
     *
     * @return boolean true if router is Bdr else return false.
     */
    public boolean isBdr() {
        return isBdr;
    }

    /**
     * Sets the router is BDR or not.
     *
     * @param isBdr the router is BDR or not
     */
    public void setIsBdr(boolean isBdr) {
        this.isBdr = isBdr;
    }
}