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
import org.onlab.packet.MacAddress;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a evpn route.
 */
public class EvpnRoute {


    /**
     * Source of the route.
     */
    public enum Source {
        /**
         * Route came from app source.
         */
        LOCAL,

        /**
         * Route came from remote bgp peer source.
         */
        REMOTE,
    }

    private final Source source;
    private final MacAddress prefixMac;
    private final IpPrefix prefix;
    private final IpAddress nextHop;
    private final RouteDistinguisher rd;
    private List<VpnRouteTarget> importRtList;
    private List<VpnRouteTarget> exportRtList;
    private final Label label;

    /**
     * Constructor to initialize the parameters.
     *
     * @param source       route source
     * @param prefixMac    mac address
     * @param prefix       ip address
     * @param nextHop      evpn nexthop
     * @param rd           route distinguisher
     * @param importRtList import route targets
     * @param exportRtList export route targets
     * @param label        evpn route label
     */
    public EvpnRoute(Source source,
                     MacAddress prefixMac,
                     IpPrefix prefix,
                     IpAddress nextHop,
                     RouteDistinguisher rd,
                     List<VpnRouteTarget> importRtList,
                     List<VpnRouteTarget> exportRtList,
                     Label label) {

        checkNotNull(prefixMac);
        checkNotNull(prefix);
        //checkNotNull(nextHop);//next hop can be null in case of MP un reach.
        checkNotNull(rd);
        checkNotNull(label);
        this.source = checkNotNull(source);
        this.prefix = prefix;
        this.prefixMac = prefixMac;
        this.nextHop = nextHop;
        this.rd = rd;
        this.importRtList = importRtList;
        this.exportRtList = exportRtList;
        this.label = label;
    }

    /**
     * Constructor to initialize the parameters.
     *
     * @param source       route source
     * @param prefixMac    mac address
     * @param prefix       ip address
     * @param nextHop      evpn nexthop
     * @param rdToString   route distinguisher
     * @param importRtList import route targets
     * @param exportRtList export route targets
     * @param labelToInt   evpn route label
     */
    public EvpnRoute(Source source,
                     MacAddress prefixMac,
                     IpPrefix prefix,
                     IpAddress nextHop,
                     String rdToString,
                     List<VpnRouteTarget> importRtList,
                     List<VpnRouteTarget> exportRtList,
                     int labelToInt) {
        checkNotNull(prefixMac);
        checkNotNull(prefix);
        //checkNotNull(nextHop); //next hop can be null in case of MP un reach.
        this.source = checkNotNull(source);
        this.prefix = prefix;
        this.prefixMac = prefixMac;
        this.nextHop = nextHop;
        this.rd = RouteDistinguisher.routeDistinguisher(rdToString);
        this.importRtList = importRtList;
        this.exportRtList = exportRtList;
        this.label = Label.label(labelToInt);
    }

    /**
     * Returns the route source.
     *
     * @return route source
     */
    public Source source() {
        return source;
    }

    /**
     * Returns the address.
     *
     * @return MacAddress
     */
    public MacAddress prefixMac() {
        return prefixMac;
    }

    /**
     * Returns the IPv4 address.
     *
     * @return Ip4Address
     */
    public IpPrefix prefixIp() {
        return prefix;
    }

    /**
     * Returns the IPv4 address.
     *
     * @return Ip4Address
     */
    public EvpnPrefix evpnPrefix() {
        return new EvpnPrefix(rd, prefixMac,
                              prefix);
    }


    /**
     * Returns the next hop IP address.
     *
     * @return Ip4Address
     */
    public IpAddress ipNextHop() {
        return nextHop;
    }

    public EvpnNextHop nextHop() {
        return EvpnNextHop.evpnNextHop(nextHop,
                                       importRtList,
                                       exportRtList,
                                       label);
    }

    /**
     * Returns the routeDistinguisher.
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
     * Returns the label.
     *
     * @return Label
     */
    public Label label() {
        return label;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefixMac,
                            prefix,
                            nextHop,
                            rd,
                            importRtList,
                            exportRtList,
                            label);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnRoute)) {
            return false;
        }

        EvpnRoute that = (EvpnRoute) other;

        return Objects.equals(prefixMac, that.prefixMac)
                && Objects.equals(prefix, that.prefix)
                && Objects.equals(nextHop, that.nextHop)
                && Objects.equals(this.rd, that.rd)
                && Objects.equals(this.importRtList, that.importRtList)
                && Objects.equals(this.exportRtList, that.exportRtList)
                && Objects.equals(this.label, that.label);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("prefixMac", prefixMac)
                .add("prefix", prefix)
                .add("nextHop", nextHop)
                .add("rd", this.rd)
                .add("import rt", this.importRtList)
                .add("export rt", this.exportRtList)
                .add("label", this.label)
                .toString();
    }
}
