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

package org.onosproject.evpnopenflow.rsc;

import org.onosproject.evpnrouteservice.EvpnInstanceName;
import org.onosproject.evpnrouteservice.RouteDistinguisher;
import org.onosproject.evpnrouteservice.VpnRouteTarget;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.CONFIG_RT_CANNOT_BE_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.DESCRIPTION;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.DESCRIPTION_CANNOT_BE_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.EXPORT_RT_CANNOT_BE_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ID;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ID_CANNOT_BE_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.IMPORT_RT_CANNOT_BE_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.INSTANCE_NAME_CANNOT_BE_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.RD_CANNOT_BE_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ROUTE_DISTINGUISHER;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPNINSTANCE_NAME;

/**
 * Default implementation of VPN instance.
 */
public class DefaultVpnInstance implements VpnInstance {
    private final VpnInstanceId id;
    private final String description;
    private final EvpnInstanceName name;
    private final RouteDistinguisher routeDistinguisher;
    private final Set<VpnRouteTarget> exportRtSet;
    private final Set<VpnRouteTarget> importRtSet;
    private final Set<VpnRouteTarget> configRtSet;


    /**
     * creates vpn instance object.
     *
     * @param id                 vpn instance identifier
     * @param instanceName       the name of vpn instance
     * @param description        the description of vpn instance
     * @param routeDistinguisher the routeDistinguisher of vpn instance
     * @param exportRtSet        the export route target information
     * @param importRtSet        the import route target information
     * @param configRtSet        the config route target information
     */
    public DefaultVpnInstance(VpnInstanceId id, EvpnInstanceName instanceName,
                              String description,
                              RouteDistinguisher routeDistinguisher,
                              Set<VpnRouteTarget> exportRtSet,
                              Set<VpnRouteTarget> importRtSet,
                              Set<VpnRouteTarget> configRtSet) {
        this.id = checkNotNull(id, ID_CANNOT_BE_NULL);
        this.name = checkNotNull(instanceName, INSTANCE_NAME_CANNOT_BE_NULL);
        this.description = checkNotNull(description,
                                        DESCRIPTION_CANNOT_BE_NULL);
        this.routeDistinguisher = checkNotNull(routeDistinguisher,
                                               RD_CANNOT_BE_NULL);
        this.exportRtSet = checkNotNull(exportRtSet, EXPORT_RT_CANNOT_BE_NULL);
        this.importRtSet = checkNotNull(importRtSet, IMPORT_RT_CANNOT_BE_NULL);
        this.configRtSet = checkNotNull(configRtSet, CONFIG_RT_CANNOT_BE_NULL);
    }

    @Override
    public VpnInstanceId id() {
        return id;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public RouteDistinguisher routeDistinguisher() {
        return routeDistinguisher;
    }

    @Override
    public EvpnInstanceName vpnInstanceName() {
        return name;
    }

    @Override
    public Set<VpnRouteTarget> getExportRouteTargets() {
        return exportRtSet;
    }

    @Override
    public Set<VpnRouteTarget> getImportRouteTargets() {
        return importRtSet;
    }

    @Override
    public Set<VpnRouteTarget> getConfigRouteTargets() {
        return configRtSet;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, routeDistinguisher,
                            exportRtSet, importRtSet, configRtSet);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultVpnInstance) {
            final DefaultVpnInstance that = (DefaultVpnInstance) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.name, that.name)
                    && Objects.equals(this.description, that.description)
                    && Objects.equals(this.routeDistinguisher,
                                      that.routeDistinguisher)
                    && Objects.equals(this.exportRtSet, that.exportRtSet)
                    && Objects.equals(this.importRtSet, that.importRtSet)
                    && Objects.equals(this.configRtSet, that.configRtSet);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add(ID, id)
                .add(DESCRIPTION, description)
                .add(VPNINSTANCE_NAME, name)
                .add(ROUTE_DISTINGUISHER, routeDistinguisher)
                .add("exportRtSet", exportRtSet)
                .add("importRtSet", importRtSet)
                .add("configRtSet", configRtSet)
                .toString();
    }
}
