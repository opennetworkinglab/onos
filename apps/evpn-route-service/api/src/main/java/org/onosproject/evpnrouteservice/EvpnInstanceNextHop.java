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

package org.onosproject.evpnrouteservice;

import java.util.Objects;

import org.onlab.packet.IpAddress;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents a evpn instance nexthop.
 */
public final class EvpnInstanceNextHop {

    private final IpAddress nextHop;
    private final Label label;

    /**
     * Constructor to initialize the parameters.
     *
     * @param nextHop nexthop
     * @param label   label
     */
    private EvpnInstanceNextHop(IpAddress nextHop, Label label) {
        this.nextHop = nextHop;
        this.label = label;
    }

    /**
     * creates instance of EvpnInstanceNextHop.
     *
     * @param nextHop nexthop
     * @param label   label
     * @return evpnInstanceNexthop
     */
    public static EvpnInstanceNextHop evpnNextHop(IpAddress nextHop,
                                                  Label label) {
        return new EvpnInstanceNextHop(nextHop, label);
    }

    /**
     * Returns the next hop IP address.
     *
     * @return next hop
     */
    public IpAddress nextHop() {
        return nextHop;
    }

    /**
     * Returns the label.
     *
     * @return Label
     */
    public Label label() {
        return label;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nextHop, label);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnInstanceNextHop)) {
            return false;
        }

        EvpnInstanceNextHop that = (EvpnInstanceNextHop) other;

        return Objects.equals(this.nextHop(), that.nextHop())
                && Objects.equals(this.label, that.label);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("nextHop", this.nextHop())
                .add("label", this.label).toString();
    }
}
