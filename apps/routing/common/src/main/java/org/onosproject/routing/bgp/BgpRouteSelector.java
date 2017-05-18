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

import org.onlab.packet.IpPrefix;
import org.onosproject.cluster.ClusterService;
import org.onosproject.incubator.net.routing.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Class to receive and process the BGP routes from each BGP Session/Peer.
 */
class BgpRouteSelector {
    private static final Logger log =
        LoggerFactory.getLogger(BgpRouteSelector.class);

    private BgpSessionManager bgpSessionManager;
    private ClusterService clusterService;

    /**
     * Constructor.
     *
     * @param bgpSessionManager the BGP Session Manager to use
     * @param clusterService the cluster service
     */
    BgpRouteSelector(BgpSessionManager bgpSessionManager, ClusterService clusterService) {
        this.bgpSessionManager = bgpSessionManager;
        this.clusterService = clusterService;
    }

    /**
     * Processes route entry updates: added/updated and deleted route
     * entries.
     *
     * @param addedBgpRouteEntries the added/updated route entries to process
     * @param deletedBgpRouteEntries the deleted route entries to process
     */
    synchronized void routeUpdates(
                        Collection<BgpRouteEntry> addedBgpRouteEntries,
                        Collection<BgpRouteEntry> deletedBgpRouteEntries) {

        Collection<Route> updates = new LinkedList<>();
        Collection<Route> withdraws = new LinkedList<>();

        RouteUpdate routeUpdate;

        if (bgpSessionManager.isShutdown()) {
            return;         // Ignore any leftover updates if shutdown
        }
        // Process the deleted route entries
        for (BgpRouteEntry bgpRouteEntry : deletedBgpRouteEntries) {
            routeUpdate = processDeletedRoute(bgpRouteEntry);
            convertRouteUpdateToRoute(routeUpdate, updates, withdraws);
        }

        // Process the added/updated route entries
        for (BgpRouteEntry bgpRouteEntry : addedBgpRouteEntries) {
            routeUpdate = processAddedRoute(bgpRouteEntry);
            convertRouteUpdateToRoute(routeUpdate, updates, withdraws);
        }

        bgpSessionManager.withdraw(withdraws);
        bgpSessionManager.update(updates);
    }

    private void convertRouteUpdateToRoute(RouteUpdate routeUpdate,
                                           Collection<Route> updates,
                                           Collection<Route> withdraws) {
        if (routeUpdate != null) {
            Route route = new Route(Route.Source.BGP, routeUpdate.routeEntry().prefix(),
                    routeUpdate.routeEntry().nextHop(), clusterService.getLocalNode().id());
            if (routeUpdate.type().equals(RouteUpdate.Type.UPDATE)) {
                updates.add(route);
            } else if (routeUpdate.type().equals(RouteUpdate.Type.DELETE)) {
                withdraws.add(route);
            }
        }
    }

    /**
     * Processes an added/updated route entry.
     *
     * @param bgpRouteEntry the added/updated route entry
     * @return the result route update that should be forwarded to the
     * Route Listener, or null if no route update should be forwarded
     */
    private RouteUpdate processAddedRoute(BgpRouteEntry bgpRouteEntry) {
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
     * @param bgpRouteEntry the deleted route entry
     * @return the result route update that should be forwarded to the
     * Route Listener, or null if no route update should be forwarded
     */
    private RouteUpdate processDeletedRoute(BgpRouteEntry bgpRouteEntry) {
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
