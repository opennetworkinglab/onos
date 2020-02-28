/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.t3.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.onosproject.mcast.api.McastRoute;
import org.onosproject.mcast.api.McastRouteData;

import java.util.Map;
import java.util.Set;

/**
 * Represents Network Information Base (NIB) for multicast routes
 * and supports alternative functions to
 * {@link org.onosproject.mcast.api.MulticastRouteService} for offline data.
 */
public class MulticastRouteNib extends AbstractNib {

    private Map<McastRoute, McastRouteData> mcastRoutes;

    // use the singleton helper to create the instance
    protected MulticastRouteNib() {
    }

    public void setMcastRoutes(Map<McastRoute, McastRouteData> mcastRoutes) {
        this.mcastRoutes = mcastRoutes;
    }

    public Map<McastRoute, McastRouteData> getMcastRoutes() {
        return ImmutableMap.copyOf(mcastRoutes);
    }

    /**
     * Gets all Multicast routes in the system.
     *
     * @return set of Multicast routes
     */
    public Set<McastRoute> getRoutes() {
        return ImmutableSet.copyOf(mcastRoutes.keySet());
    }

    /**
     * Return the Multicast data for this route.
     *
     * @param route route
     * @return the mcast route data
     */
    public McastRouteData routeData(McastRoute route) {
        return mcastRoutes.get(route);
    }

    /**
     * Returns the singleton instance of multicast routes NIB.
     *
     * @return instance of multicast routes NIB
     */
    public static MulticastRouteNib getInstance() {
        return MulticastRouteNib.SingletonHelper.INSTANCE;
    }

    private static class SingletonHelper {
        private static final MulticastRouteNib INSTANCE = new MulticastRouteNib();
    }

}
