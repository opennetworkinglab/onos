/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.mcast.api;

import com.google.common.annotations.Beta;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class to propagate updates to multicast route information stored in the store.
 */
@Beta
public final class McastRouteUpdate {

    private static final String ROUTE_NOT_NULL = "Route cannot be null";
    private static final String SOURCE_NOT_NULL = "Source cannot be null";
    private static final String SINK_NOT_NULL = "Sink cannot be null";

    private final McastRoute route;
    private final Map<HostId, Set<ConnectPoint>> sources;
    private final Map<HostId, Set<ConnectPoint>> sinks;

    private McastRouteUpdate(McastRoute route,
                             Map<HostId, Set<ConnectPoint>> sources,
                             Map<HostId, Set<ConnectPoint>> sinks) {
        this.route = checkNotNull(route, ROUTE_NOT_NULL);
        this.sources = checkNotNull(sources, SOURCE_NOT_NULL);
        this.sinks = checkNotNull(sinks, SINK_NOT_NULL);
    }

    /**
     * Static method to create an McastRoutUpdate object.
     *
     * @param route   the route updated
     * @param sources the different sources
     * @param sinks   the different sinks
     * @return the McastRouteUpdate object.
     */
    public static McastRouteUpdate mcastRouteUpdate(McastRoute route,
                                                    Map<HostId, Set<ConnectPoint>> sources,
                                                    Map<HostId, Set<ConnectPoint>> sinks) {
        return new McastRouteUpdate(route, sources, sinks);
    }

    /**
     * The route associated with this multicast information.
     *
     * @return a mulicast route
     */
    public McastRoute route() {
        return route;
    }

    /**
     * The sources.
     *
     * @return a set of connect points
     */
    public Map<HostId, Set<ConnectPoint>> sources() {
        return sources;
    }

    /**
     * The sinks.
     *
     * @return a set of connect points
     */
    public Map<HostId, Set<ConnectPoint>> sinks() {
        return sinks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        McastRouteUpdate that = (McastRouteUpdate) o;
        return Objects.equals(route, that.route) &&
                Objects.equals(sources, that.sources) &&
                Objects.equals(sinks, that.sinks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(route, sources, sinks);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("route", route())
                .add("sources", sources)
                .add("sinks", sinks)
                .toString();
    }
}
