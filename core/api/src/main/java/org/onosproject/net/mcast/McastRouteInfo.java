/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.mcast;

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.ConnectPoint;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Multicast information as stored in the store.
 */
public final class McastRouteInfo {

    private static final String ROUTE_NOT_NULL = "Route cannot be null";

    private final McastRoute route;
    private final Optional<ConnectPoint> sink;
    private final Optional<ConnectPoint> source;
    private final Set<ConnectPoint> sinks;

    private McastRouteInfo(McastRoute route, ConnectPoint sink,
                           ConnectPoint source, Set<ConnectPoint> sinks) {
        this.route = checkNotNull(route, ROUTE_NOT_NULL);
        this.sink = Optional.ofNullable(sink);
        this.source = Optional.ofNullable(source);
        this.sinks = sinks;
    }

    public static McastRouteInfo mcastRouteInfo(McastRoute route) {
        return new McastRouteInfo(route, null, null, Collections.EMPTY_SET);
    }

    public static McastRouteInfo mcastRouteInfo(McastRoute route,
                                                ConnectPoint sink,
                                                ConnectPoint source) {
        return new McastRouteInfo(route, sink, source, Collections.EMPTY_SET);
    }

    public static McastRouteInfo mcastRouteInfo(McastRoute route,
                                                Set<ConnectPoint> sinks,
                                                ConnectPoint source) {
        return new McastRouteInfo(route, null, source, ImmutableSet.copyOf(sinks));
    }

    public boolean isComplete() {
        return ((sink.isPresent() || !sinks.isEmpty()) && source.isPresent());
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
     * The source which has been removed or added.

     * @return an optional connect point
     */
    public Optional<ConnectPoint> source() {
        return source;
    }

    /**
     * The sink which has been removed or added. The field may not be set
     * if the sink has not been detected yet or has been removed.
     *
     * @return an optional connect point
     */
    public Optional<ConnectPoint> sink() {
        return sink;
    }

    /**
     * Returns the set of sinks associated with this route. Only valid with
     * SOURCE_ADDED events.
     *
     * @return a set of connect points
     */
    public Set<ConnectPoint> sinks() {
        return sinks;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("route", route())
                .add("sink", sink())
                .add("source", source())
                .add("sinks", sinks())
                .toString();
    }
}
