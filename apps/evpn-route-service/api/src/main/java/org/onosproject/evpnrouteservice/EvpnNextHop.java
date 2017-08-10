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

import java.util.List;
import java.util.Objects;

import org.onlab.packet.IpAddress;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents a evpn next hop.
 */
public final class EvpnNextHop {

    private final IpAddress nextHop;
    private final List<VpnRouteTarget> importRtList;
    private final List<VpnRouteTarget> exportRtList;
    private final Label label;

    /**
     * Constructor to initialize the parameters.
     *
     * @param nextHop      evpn next hop
     * @param importRtList import route targets
     * @param importRtList export route targets
     * @param label        label
     */
    private EvpnNextHop(IpAddress nextHop, List<VpnRouteTarget> importRtList, List<VpnRouteTarget> exportRtList,
                        Label label) {
        this.nextHop = nextHop;
        this.importRtList = importRtList;
        this.exportRtList = exportRtList;
        this.label = label;
    }

    /**
     * Creates the Evpn Next hop with given parameters.
     *
     * @param nextHop      Next  hop of the route
     * @param importRtList route target import list
     * @param exportRtList route target export list
     * @param label        label of evpn route
     * @return EvpnNextHop
     */
    public static EvpnNextHop evpnNextHop(IpAddress nextHop, List<VpnRouteTarget> importRtList,
                                          List<VpnRouteTarget> exportRtList,
                                          Label label) {
        return new EvpnNextHop(nextHop, importRtList, exportRtList, label);
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
     * Returns the Route targets.
     *
     * @return RouteTarget List
     */

    public List<VpnRouteTarget> importRouteTarget() {
        return importRtList;
    }

    /**
     * Returns the Route targets.
     *
     * @return RouteTarget List
     */
    public List<VpnRouteTarget> exportRouteTarget() {
        return exportRtList;
    }

    /**
     * Returns the label of evpn route.
     *
     * @return Label
     */
    public Label label() {
        return label;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nextHop, importRtList, exportRtList, label);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnNextHop)) {
            return false;
        }

        EvpnNextHop that = (EvpnNextHop) other;

        return Objects.equals(this.nextHop(), that.nextHop())
                && Objects.equals(this.importRtList, that.importRtList)
                && Objects.equals(this.exportRtList, that.exportRtList)
                && Objects.equals(this.label, that.label);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("nextHop", this.nextHop())
                .add("import rt list", this.importRtList).add("export rt list", this.exportRtList)
                .add("label", this.label).toString();
    }
}
