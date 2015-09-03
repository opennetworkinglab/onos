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
package org.onosproject.routing.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.routing.FibListener;
import org.onosproject.routing.RouteEntry;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.StaticRoutingService;

import java.util.Collection;

/**
 * Static router maintains handle to FIB listener.
 *
 * TODO: implement getRoutes methods
 */
@Component(immediate = true, enabled = false)
@Service
public class StaticRouter implements RoutingService, StaticRoutingService {
    private FibListener fibListener;

    @Override
    public void start() {

    }

    @Override
    public void addFibListener(FibListener fibListener) {
        this.fibListener = fibListener;
    }

    @Override
    public void stop() {

    }

    @Override
    public Collection<RouteEntry> getRoutes4() {
        return null;
    }

    @Override
    public Collection<RouteEntry> getRoutes6() {
        return null;
    }

    @Override
    public RouteEntry getLongestMatchableRouteEntry(IpAddress ipAddress) {
        return null;
    }

    @Override
    public FibListener getFibListener() {
        return fibListener;
    }

}
