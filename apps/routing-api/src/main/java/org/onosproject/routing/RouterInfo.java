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

package org.onosproject.routing;

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.routing.config.RoutersConfig;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores configuration information about a router.
 */
public class RouterInfo {

    private final ConnectPoint controlPlaneConnectPoint;
    private final boolean ospfEnabled;
    private final Set<String> interfaces;

    /**
     * Creates a new router info.
     *
     * @param controlPlaneConnectPoint control plane connect point
     * @param ospfEnabled whether OSPF is enabled
     * @param interfaces set of interface names
     */
    public RouterInfo(ConnectPoint controlPlaneConnectPoint, boolean ospfEnabled, Set<String> interfaces) {
        this.controlPlaneConnectPoint = checkNotNull(controlPlaneConnectPoint);
        this.ospfEnabled = ospfEnabled;
        this.interfaces = ImmutableSet.copyOf(checkNotNull(interfaces));
    }

    /**
     * Returns the control plane connect point.
     *
     * @return connect point
     */
    public ConnectPoint controlPlaneConnectPoint() {
        return controlPlaneConnectPoint;
    }

    /**
     * Returns the router device ID.
     *
     * @return device ID
     */
    public DeviceId deviceId() {
        return controlPlaneConnectPoint.deviceId();
    }

    /**
     * Returns whether OSPF is enabled on the router.
     *
     * @return OSPF enabled
     */
    public boolean ospfEnabled() {
        return ospfEnabled;
    }

    /**
     * Returns the set of interfaces belonging to the router.
     *
     * @return set of interface names
     */
    public Set<String> interfaces() {
        return interfaces;
    }

    /**
     * Creates a router info from a router config.
     *
     * @param config router config
     * @return new router info object
     */
    public static RouterInfo from(RoutersConfig.Router config) {
        return new RouterInfo(config.controlPlaneConnectPoint(), config.isOspfEnabled(), config.interfaces());
    }
}
