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

import org.onosproject.evpnrouteservice.VpnRouteTarget;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ID_CANNOT_BE_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.RT_CANNOT_BE_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.RT_TYPE_CANNOT_BE_NULL;

/**
 * Default implementation of VPN AF configuration.
 */
public class DefaultVpnAfConfig implements VpnAfConfig {

    private final String exportRoutePolicy;
    private final String importRoutePolicy;
    private final VpnRouteTarget routeTarget;
    private final String routeTargetType;

    /**
     * creates vpn af configuration object.
     *
     * @param exportRoutePolicy export route policy
     * @param importRoutePolicy import route policy
     * @param routeTarget       route target value
     * @param routeTargetType   route target type
     */
    public DefaultVpnAfConfig(String exportRoutePolicy,
                              String importRoutePolicy,
                              VpnRouteTarget routeTarget,
                              String routeTargetType) {
        this.exportRoutePolicy = checkNotNull(exportRoutePolicy,
                                              ID_CANNOT_BE_NULL);
        this.importRoutePolicy = checkNotNull(importRoutePolicy,
                                              ID_CANNOT_BE_NULL);
        this.routeTarget = checkNotNull(routeTarget, RT_CANNOT_BE_NULL);
        this.routeTargetType = checkNotNull(routeTargetType,
                                            RT_TYPE_CANNOT_BE_NULL);
    }

    @Override
    public String exportRoutePolicy() {
        return exportRoutePolicy;
    }

    @Override
    public String importRoutePolicy() {
        return importRoutePolicy;
    }

    @Override
    public VpnRouteTarget routeTarget() {
        return routeTarget;
    }

    @Override
    public String routeTargetType() {
        return routeTargetType;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("exportRoutePolicy", exportRoutePolicy)
                .add("importRoutePolicy", importRoutePolicy)
                .add("routeTarget", routeTarget)
                .add("routeTargetType", routeTargetType)
                .toString();
    }
}
