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

package org.onosproject.evpnrouteservice.store;

import org.onlab.packet.IpAddress;
import org.onosproject.evpnrouteservice.EvpnPrefix;
import org.onosproject.evpnrouteservice.EvpnRoute;
import org.onosproject.evpnrouteservice.EvpnRouteSet;
import org.onosproject.evpnrouteservice.EvpnRouteTableId;
import org.onosproject.evpnrouteservice.EvpnTable;

import java.util.Collection;
import java.util.Collections;

/**
 * Route table that contains no routes.
 */
public final class EmptyEvpnRouteTable implements EvpnTable {

    private final EvpnRouteTableId id = new EvpnRouteTableId("empty");

    private static final EmptyEvpnRouteTable INSTANCE = new EmptyEvpnRouteTable();

    /**
     * Returns the instance of the empty route table.
     *
     * @return empty route table
     */
    public static EmptyEvpnRouteTable instance() {
        return INSTANCE;
    }

    private EmptyEvpnRouteTable() {
    }

    @Override
    public void update(EvpnRoute route) {

    }

    @Override
    public void remove(EvpnRoute route) {

    }

    @Override
    public EvpnRouteTableId id() {
        return id;
    }

    @Override
    public Collection<EvpnRouteSet> getRoutes() {
        return Collections.emptyList();
    }

    @Override
    public EvpnRouteSet getRoutes(EvpnPrefix prefix) {
        return null;
    }

    @Override
    public Collection<EvpnRoute> getRoutesForNextHop(IpAddress nextHop) {
        return Collections.emptyList();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void destroy() {

    }
}
