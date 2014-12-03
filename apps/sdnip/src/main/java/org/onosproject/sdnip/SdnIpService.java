/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.sdnip;

import java.util.Collection;

import org.onosproject.sdnip.bgp.BgpRouteEntry;
import org.onosproject.sdnip.bgp.BgpSession;

/**
 * Service interface exported by SDN-IP.
 */
public interface SdnIpService {
    /**
     * Gets the BGP sessions.
     *
     * @return the BGP sessions
     */
    public Collection<BgpSession> getBgpSessions();

    /**
     * Gets the BGP routes.
     *
     * @return the BGP routes
     */
    public Collection<BgpRouteEntry> getBgpRoutes();

    /**
     * Gets all the routes known to SDN-IP.
     *
     * @return the SDN-IP routes
     */
    public Collection<RouteEntry> getRoutes();

    /**
     * Changes whether this SDN-IP instance is the primary or not based on the
     * boolean parameter.
     *
     * @param isPrimary true if the instance is primary, false if it is not
     */
    public void modifyPrimary(boolean isPrimary);
}
