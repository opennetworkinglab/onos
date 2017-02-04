/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encapsulates information needed to provision a router interface.
 */
public final class InterfaceProvisionRequest {

    private final RouterInfo info;
    private final Interface intf;

    private InterfaceProvisionRequest(RouterInfo info, Interface intf) {
        this.info = checkNotNull(info);
        this.intf = checkNotNull(intf);
    }

    /**
     * Retrieves the router's control plane connect point.
     *
     * @return connect point
     */
    public ConnectPoint controlPlaneConnectPoint() {
        return info.controlPlaneConnectPoint();
    }

    /**
     * Retrieves the router configuration info.
     *
     * @return router configuration info
     */
    public RouterInfo info() {
        return info;
    }

    /**
     * Retrieves the interface to be (un)provisioned.
     *
     * @return interface
     */
    public Interface intf() {
        return intf;
    }

    /**
     * Creates a new provision request from a router configuration and an
     * interface.
     *
     * @param info router configuration info
     * @param intf interface
     * @return provision request
     */
    public static InterfaceProvisionRequest of(RouterInfo info, Interface intf) {
        return new InterfaceProvisionRequest(info, intf);
    }
}
