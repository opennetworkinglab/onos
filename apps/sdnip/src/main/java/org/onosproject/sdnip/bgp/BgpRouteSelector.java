/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.sdnip.bgp;

import java.util.Collection;
import java.util.LinkedList;

import org.onlab.packet.IpPrefix;
import org.onosproject.sdnip.RouteUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to receive and process the BGP routes from each BGP Session/Peer.
 */
class BgpRouteSelector {
    private static final Logger log =
        LoggerFactory.getLogger(BgpRouteSelector.class);

    private BgpSessionManager bgpSessionManager;

    /**
     * Constructor.
     *
     * @param bgpSessionManager the BGP Session Manager to use
     */
    BgpRouteSelector(BgpSessionManager bgpSessionManager) {
        this.bgpSessionManager = bgpSessionManager;
    }

    /**
     * Processes route entry updates: added/updated and deleted route
     * entries.
     *
     * @param bgpSession the BGP session the route entry updates were
     * received on
     * @param addedBgpRouteEntries the added/updated route entries to process
     * @param deletedBgpRouteEntries the deleted route entries to process
     */
    synchronized void routeUpdates(BgpSession bgpSession,
                        Collection<BgpRouteEntry> addedBgpRouteEntries,
                        Collection<BgpRouteEntry> deletedBgpRouteEntries) {
        Collection<RouteUpdate> routeUpdates = new LinkedList<>();
        RouteUpdate routeUpdate;

        if (bgpSessionManager.isShutdown()) {
            return;         // Ignore any leftover updates if shutdown
        }
        // Process the deleted route entries
        for (BgpRouteEntry bgpRouteEntry : deletedBgpRouteEntries) {
            routeUpdate = processDeletedRoute(bgpSession, bgpRouteEntry);
            if (routeUpdate != null) {
                routeUpdates.add(routeUpdate);
            }
        }

        // Process the added/updated route entries
        for (BgpRouteEntry bgpRouteEntry : addedBgpRouteEntries) {
            routeUpdate = processAddedRoute(bgpSession, bgpRouteEntry);
            if (routeUpdate != null) {
                routeUpdates.add(routeUpdate);
            }
        }
        bgpSessionManager.getRouteListener().update(routeUpdates);
    }

    /**
     * Processes an added/updated route entry.
     *
     * @param bgpSession the BGP session the route entry update was received on
     * @param bgpRouteEntry the added/updated route entry
     * @return the result route update that should be forwarded to the
     * Route Listener, or null if no route update should be forwarded
     */
    private RouteUpdate processAddedRoute(BgpSession bgpSession,
                                          BgpRouteEntry bgpRouteEntry) {
        RouteUpdate routeUpdate;
        BgpRouteEntry bestBgpRouteEntry =
            bgpSessionManager.findBgpRoute(bgpRouteEntry.prefix());

        //
        // Install the new route entry if it is better than the
        // current best route.
        //
        if ((bestBgpRouteEntry == null) ||
            bgpRouteEntry.isBetterThan(bestBgpRouteEntry)) {
            bgpSessionManager.addBgpRoute(bgpRouteEntry);
            routeUpdate =
                new RouteUpdate(RouteUpdate.Type.UPDATE, bgpRouteEntry);
            return routeUpdate;
        }

        //
        // If the route entry arrived on the same BGP Session as
        // the current best route, then elect the next best route
        // and install it.
        //
        if (bestBgpRouteEntry.getBgpSession() !=
            bgpRouteEntry.getBgpSession()) {
            return null;            // Nothing to do
        }

        // Find the next best route
        bestBgpRouteEntry = findBestBgpRoute(bgpRouteEntry.prefix());
        if (bestBgpRouteEntry == null) {
            //
            // TODO: Shouldn't happen. Install the new route as a
            // pre-caution.
            //
            log.debug("BGP next best route for prefix {} is missing. " +
                      "Adding the route that is currently processed.",
                      bgpRouteEntry.prefix());
            bestBgpRouteEntry = bgpRouteEntry;
        }

        // Install the next best route
        bgpSessionManager.addBgpRoute(bestBgpRouteEntry);
        routeUpdate = new RouteUpdate(RouteUpdate.Type.UPDATE,
                                      bestBgpRouteEntry);
        return routeUpdate;
    }

    /**
     * Processes a deleted route entry.
     *
     * @param bgpSession the BGP session the route entry update was received on
     * @param bgpRouteEntry the deleted route entry
     * @return the result route update that should be forwarded to the
     * Route Listener, or null if no route update should be forwarded
     */
    private RouteUpdate processDeletedRoute(BgpSession bgpSession,
                                            BgpRouteEntry bgpRouteEntry) {
        RouteUpdate routeUpdate;
        BgpRouteEntry bestBgpRouteEntry =
            bgpSessionManager.findBgpRoute(bgpRouteEntry.prefix());

        //
        // Remove the route entry only if it was the best one.
        // Install the the next best route if it exists.
        //
        // NOTE: We intentionally use "==" instead of method equals(),
        // because we need to check whether this is same object.
        //
        if (bgpRouteEntry != bestBgpRouteEntry) {
            return null;            // Nothing to do
        }

        //
        // Find the next best route
        //
        bestBgpRouteEntry = findBestBgpRoute(bgpRouteEntry.prefix());
        if (bestBgpRouteEntry != null) {
            // Install the next best route
            bgpSessionManager.addBgpRoute(bestBgpRouteEntry);
            routeUpdate = new RouteUpdate(RouteUpdate.Type.UPDATE,
                                          bestBgpRouteEntry);
            return routeUpdate;
        }

        //
        // No route found. Remove the route entry
        //
        bgpSessionManager.removeBgpRoute(bgpRouteEntry.prefix());
        routeUpdate = new RouteUpdate(RouteUpdate.Type.DELETE, bgpRouteEntry);
        return routeUpdate;
    }

    /**
     * Finds the best route entry among all BGP Sessions.
     *
     * @param prefix the prefix of the route
     * @return the best route if found, otherwise null
     */
    private BgpRouteEntry findBestBgpRoute(IpPrefix prefix) {
        BgpRouteEntry bestRoute = null;

        // Iterate across all BGP Sessions and select the best route
        for (BgpSession bgpSession : bgpSessionManager.getBgpSessions()) {
            BgpRouteEntry route = bgpSession.findBgpRoute(prefix);
            if (route == null) {
                continue;
            }
            if ((bestRoute == null) || route.isBetterThan(bestRoute)) {
                bestRoute = route;
            }
        }
        return bestRoute;
    }
}
