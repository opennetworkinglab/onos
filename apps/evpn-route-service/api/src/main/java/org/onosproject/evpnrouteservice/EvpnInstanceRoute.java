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
import org.onlab.packet.IpPrefix;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a evpn instance route.
 */
public class EvpnInstanceRoute {

    private final EvpnInstanceName evpnName;
    private final RouteDistinguisher rd;
    private List<VpnRouteTarget> importRtList;
    private List<VpnRouteTarget> exportRtList;
    private final EvpnInstancePrefix evpnInstancePrefix;
    private final EvpnInstanceNextHop evpnInstanceNextHop;
    private final IpPrefix prefix;
    private final IpAddress nextHop;
    private final Label label;

    /**
     * Constructor to initialize the parameters.
     *
     * @param evpnName            vpn instance name
     * @param rd                  route distinguisher
     * @param importRtList        import route targets
     * @param exportRtList        export route targets
     * @param evpnInstancePrefix  evpn intance prefix
     * @param evpnInstanceNextHop evpn instance nexthop
     * @param prefix              evpn prefix
     * @param nextHop             evpn nexthop
     * @param label               label
     */
    public EvpnInstanceRoute(EvpnInstanceName evpnName,
                             RouteDistinguisher rd,
                             List<VpnRouteTarget> importRtList,
                             List<VpnRouteTarget> exportRtList,
                             EvpnInstancePrefix evpnInstancePrefix,
                             EvpnInstanceNextHop evpnInstanceNextHop,
                             IpPrefix prefix,
                             IpAddress nextHop,
                             Label label) {
        checkNotNull(evpnName);
        checkNotNull(prefix);
        //checkNotNull(nextHop); //can be NULL in MP un reach
        checkNotNull(rd);

        this.evpnName = evpnName;
        this.rd = rd;
        this.importRtList = importRtList;
        this.exportRtList = exportRtList;
        this.prefix = prefix;
        this.nextHop = nextHop;
        this.evpnInstancePrefix = evpnInstancePrefix;
        this.evpnInstanceNextHop = evpnInstanceNextHop;
        this.label = label;
    }

    /**
     * Returns the evpnName.
     *
     * @return EvpnInstanceName
     */
    public EvpnInstanceName evpnInstanceName() {
        return evpnName;
    }

    /**
     * Returns the route distinguisher.
     *
     * @return RouteDistinguisher
     */
    public RouteDistinguisher routeDistinguisher() {
        return rd;
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
     * Set import list.
     *
     * @param importRtList import list
     */
    public void setImportRtList(List<VpnRouteTarget> importRtList) {
        this.importRtList = importRtList;
    }

    /**
     * Set export list.
     *
     * @param exportRtList export list
     */
    public void setExportRtList(List<VpnRouteTarget> exportRtList) {
        this.exportRtList = exportRtList;
    }

    /**
     * Returns EvpnInstancePrefix of the evpn private route.
     *
     * @return EvpnInstancePrefix
     */

    public EvpnInstancePrefix getevpnInstancePrefix() {
        return evpnInstancePrefix;
    }

    /**
     * Returns EvpnInstanceNextHop of the evpn private route.
     *
     * @return EvpnInstancePrefix
     */

    public EvpnInstanceNextHop getEvpnInstanceNextHop() {
        return evpnInstanceNextHop;
    }

    /**
     * Returns prefix of the evpn private route.
     *
     * @return EvpnInstancePrefix
     */
    public IpPrefix prefix() {
        return prefix;
    }

    /**
     * Returns the label.
     *
     * @return EvpnInstanceName
     */
    public Label getLabel() {
        return label;
    }

    /**
     * Returns the label.
     *
     * @return EvpnInstanceName
     */
    public IpAddress getNextHopl() {
        return nextHop;
    }

    @Override
    public int hashCode() {
        return Objects.hash(evpnName, prefix, nextHop,
                            rd, importRtList, exportRtList);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnInstanceRoute)) {
            return false;
        }

        EvpnInstanceRoute that = (EvpnInstanceRoute) other;

        return Objects.equals(prefix, that.prefix)
                && Objects.equals(nextHop, that.nextHop)
                && Objects.equals(evpnName, that.evpnName)
                && Objects.equals(rd, that.rd)
                && Objects.equals(importRtList, that.importRtList)
                && Objects.equals(exportRtList, that.exportRtList);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("prefix", prefix)
                .add("nextHop", nextHop)
                .add("rd", rd)
                .add("import rt", importRtList)
                .add("export rt", exportRtList)
                .add("evpnName", evpnName)
                .toString();
    }
}
