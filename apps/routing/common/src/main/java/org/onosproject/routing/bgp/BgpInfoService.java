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

package org.onosproject.routing.bgp;

import java.util.Collection;

/**
 * Provides information about BGP peering and routes.
 */
public interface BgpInfoService {

    /**
     * Gets the BGP sessions.
     *
     * @return the BGP sessions
     */
    Collection<BgpSession> getBgpSessions();

    /**
     * Gets the selected IPv4 BGP routes among all BGP sessions.
     *
     * @return the selected IPv4 BGP routes among all BGP sessions
     */
    Collection<BgpRouteEntry> getBgpRoutes4();

    /**
     * Gets the selected IPv6 BGP routes among all BGP sessions.
     *
     * @return the selected IPv6 BGP routes among all BGP sessions
     */
    Collection<BgpRouteEntry> getBgpRoutes6();
}
