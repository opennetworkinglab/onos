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
package org.onosproject.ne;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Objects;

/**
 * VrfEntity is virtual routing forwarding entity.
 */
public class VrfEntity {
    private final String vrfName;
    private final String netVpnId;
    private final String routeDistinguisher;
    private final List<String> importTargets;
    private final List<String> exportTargets;
    private final List<String> acIdList;
    private final Bgp bgp;

    /**
     * VrfEntity constructor.
     *
     * @param vrfName vrf name
     * @param netVpnId netVpninstance identifier
     * @param routeDistinguisher route distinguisher
     * @param importTargets list of importTarget
     * @param exportTargets list of exportTarget
     * @param acIdList list of access identifier
     * @param bgp bgp
     */
    public VrfEntity(String vrfName, String netVpnId, String routeDistinguisher,
                     List<String> importTargets, List<String> exportTargets,
                     List<String> acIdList, Bgp bgp) {
        checkNotNull(vrfName, "vrfName cannot be null");
        checkNotNull(netVpnId, "netVpnId cannot be null");
        checkNotNull(routeDistinguisher, "routeDistinguisher cannot be null");
        checkNotNull(importTargets, "importTargets cannot be null");
        checkNotNull(exportTargets, "exportTargets cannot be null");
        checkNotNull(acIdList, "acIdList cannot be null");
        checkNotNull(bgp, "bgp cannot be null");
        this.vrfName = vrfName;
        this.netVpnId = netVpnId;
        this.routeDistinguisher = routeDistinguisher;
        this.importTargets = importTargets;
        this.exportTargets = exportTargets;
        this.acIdList = acIdList;
        this.bgp = bgp;
    }

    /**
     * Returns vrfName.
     *
     * @return vrfName
     */
    public String vrfName() {
        return vrfName;
    }

    /**
     * Returns netVpnId.
     *
     * @return netVpnId
     */
    public String netVpnId() {
        return netVpnId;
    }

    /**
     * Returns routeDistinguisher.
     *
     * @return routeDistinguisher
     */
    public String routeDistinguisher() {
        return routeDistinguisher;
    }

    /**
     * Returns importTargets.
     *
     * @return importTargets
     */
    public List<String> importTargets() {
        return importTargets;
    }

    /**
     * Returns exportTargets.
     *
     * @return exportTargets
     */
    public List<String> exportTargets() {
        return exportTargets;
    }

    /**
     * Returns acIdList.
     *
     * @return acIdList
     */
    public List<String> acIdList() {
        return acIdList;
    }

    /**
     * Returns bgp.
     *
     * @return bgp
     */
    public Bgp bgp() {
        return bgp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vrfName, netVpnId, routeDistinguisher,
                            importTargets, exportTargets, acIdList, bgp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof VrfEntity) {
            final VrfEntity other = (VrfEntity) obj;
            return Objects.equals(this.vrfName, other.vrfName)
                    && Objects.equals(this.netVpnId, other.netVpnId)
                    && Objects.equals(this.routeDistinguisher,
                                      other.routeDistinguisher)
                    && Objects.equals(this.importTargets, other.importTargets)
                    && Objects.equals(this.exportTargets, other.exportTargets)
                    && Objects.equals(this.acIdList, other.acIdList)
                    && Objects.equals(this.bgp, other.bgp);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("vrfName", vrfName)
                .add("netVpnId", netVpnId)
                .add("routeDistinguisher", routeDistinguisher)
                .add("importTargets", importTargets)
                .add("exportTargets", exportTargets).add("bgp", bgp)
                .add("acIdList", acIdList).toString();
    }
}
