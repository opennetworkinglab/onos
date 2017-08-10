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

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a evpn instance.
 */
public final class EvpnInstance {

    private final RouteDistinguisher rd;
    private final List<VpnRouteTarget> importRtList;
    private final List<VpnRouteTarget> exportRtList;
    private final EvpnInstanceName evpnName;

    /**
     * Constructor to initialize the parameters.
     *
     * @param rd           route distinguisher
     * @param importRtList import rotue targets
     * @param exportRtList export rotue targets
     * @param evpnName     vpn instance name
     */
    private EvpnInstance(RouteDistinguisher rd,
                         List<VpnRouteTarget> importRtList,
                         List<VpnRouteTarget> exportRtList,
                         EvpnInstanceName evpnName) {
        checkNotNull(rd);
        //checkNotNull(rt);
        checkNotNull(evpnName);
        this.rd = rd;
        this.importRtList = importRtList;
        this.exportRtList = exportRtList;
        this.evpnName = evpnName;
    }

    /**
     * Creats the instance of EvpnInstance.
     *
     * @param rd           route distinguisher
     * @param importRtList import rotue targets
     * @param exportRtList export rotue targets
     * @param evpnName     vpn instance name
     * @return EvpnInstance
     */
    public static EvpnInstance evpnInstance(RouteDistinguisher rd,
                                            List<VpnRouteTarget> importRtList,
                                            List<VpnRouteTarget> exportRtList,
                                            EvpnInstanceName evpnName) {
        return new EvpnInstance(rd, importRtList, exportRtList, evpnName);
    }

    /**
     * Getter of RouteDistinguisher.
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
     * Getter of vpn instance name.
     *
     * @return evpnName
     */
    public EvpnInstanceName evpnName() {
        return evpnName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rd, importRtList, exportRtList, evpnName);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnInstance)) {
            return false;
        }

        EvpnInstance that = (EvpnInstance) other;

        return Objects.equals(this.evpnName, that.evpnName)
                && Objects.equals(this.rd, that.rd)
                && Objects.equals(this.importRtList, that.importRtList)
                && Objects.equals(this.exportRtList, that.exportRtList);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("evpnName", this.evpnName)
                .add("rd", this.rd).add("import rt", this.importRtList)
                .add("export rt", this.exportRtList).toString();
    }
}
