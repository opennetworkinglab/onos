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

package org.onosproject.bgpio.types;

import org.onlab.packet.Ip4Address;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents the Evpn prefix.
 */
public class BgpEvpnPrefix {
    private Ip4Address nextHop;
    private RouteTarget rt;
    BgpEvpnLabel label;

    /**
     * Constructor for initializing the evpn prefix.
     *
     * @param nextHop next hop
     * @param rt      route target
     * @param label   label
     */
    public BgpEvpnPrefix(Ip4Address nextHop, RouteTarget rt, BgpEvpnLabel label) {
        this.nextHop = nextHop;
        this.rt = rt;
        this.label = label;
    }

    /**
     * Constructor for initializing the evpn prefix.
     *
     * @param nextHop next hop
     * @param label   label
     */
    public BgpEvpnPrefix(Ip4Address nextHop, BgpEvpnLabel label) {
        this.nextHop = nextHop;
        this.label = label;
    }

    /**
     * Get next hop address.
     *
     * @return next hop
     */
    public Ip4Address getNextHop() {
        return nextHop;
    }

    /**
     * Get the route target.
     *
     * @return route target
     */
    public RouteTarget getRouteTarget() {
        return rt;
    }

    /**
     * Get the label.
     *
     * @return label
     */
    public BgpEvpnLabel getLabel() {
        return label;

    }

    /**
     * Set the next hop address.
     *
     * @param nextHop next hop
     */
    public void setNetHop(Ip4Address nextHop) {
        this.nextHop = nextHop;
    }

    /**
     * Set the route target.
     *
     * @param rt route target
     */
    public void setRouteTarget(RouteTarget rt) {
        this.rt = rt;
    }

    /**
     * Set the label.
     *
     * @param label label.
     */
    public void setLabel(BgpEvpnLabel label) {
        this.label = label;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nextHop,
                            rt,
                            label);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof BgpEvpnPrefix)) {
            return false;
        }

        BgpEvpnPrefix that = (BgpEvpnPrefix) other;

        return Objects.equals(nextHop, that.nextHop)
                && Objects.equals(rt, that.rt)
                && Objects.equals(label, that.label);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("next hop", nextHop)
                .add("route target", rt)
                .add("label", label)
                .toString();
    }
}
