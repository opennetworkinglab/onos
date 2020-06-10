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
import com.google.common.collect.ImmutableMap;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Data regarding a multicast route, comprised of a type, multiple sources and multiple sinks.
 */
@Beta
public final class McastRouteData {

    private final ConcurrentHashMap<HostId, Set<ConnectPoint>> sources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<HostId, Set<ConnectPoint>> sinks = new ConcurrentHashMap<>();

    private McastRouteData() {
    }

    /**
     * Sources contained in the associated route.
     *
     * @return set of sources
     */
    public Map<HostId, Set<ConnectPoint>> sources() {
        return ImmutableMap.copyOf(sources);
    }

    /**
     * Sources contained in the associated route.
     *
     * @return map of hostIds and associated sources
     */
    public Set<ConnectPoint> allSources() {
        return sources.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    /**
     * Sources contained in the associated route for the given host.
     *
     * @param hostId the host
     * @return set of sources
     */
    public Set<ConnectPoint> sources(HostId hostId) {
        return sources.get(hostId);
    }

    /**
     * Sources contained in the associated route that are not bound to any host.
     *
     * @return set of sources
     */
    public Set<ConnectPoint> nonHostSources() {
        return sources.get(HostId.NONE);
    }

    /**
     * Sinks contained in the associated route.
     *
     * @return map of hostIds and associated sinks
     */
    public Map<HostId, Set<ConnectPoint>> sinks() {
        return ImmutableMap.copyOf(sinks);
    }

    /**
     * Sources contained in the associated route.
     *
     * @return map of hostIds and associated sinks
     */
    public Set<ConnectPoint> allSinks() {
        return sinks.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    /**
     * Sinks contained in the associated route for the given host.
     *
     * @param hostId the host
     * @return set of sinks
     */
    public Set<ConnectPoint> sinks(HostId hostId) {
        return sinks.get(hostId);
    }

    /**
     * Sinks contained in the associated route that are not bound to any host.
     *
     * @return set of sinks
     */
    public Set<ConnectPoint> nonHostSinks() {
        return sinks.get(HostId.NONE);
    }

    /**
     * Adds sources for a given host Id. If the Host Id is {@link HostId#NONE} the sources are intended to be
     * used at all times independently of the attached host.
     *
     * @param hostId  the host
     * @param sources the sources
     */
    public void addSources(HostId hostId, Set<ConnectPoint> sources) {
        checkNotNull(hostId);
        checkArgument(!sources.contains(null));
        //if existing we add to current set, otherwise we put them all
        this.sources.compute(hostId, (host, existingSources) -> {
            if (existingSources != null) {
                existingSources.addAll(sources);
                return existingSources;
            } else {
                return sources;
            }
        });
    }

    /**
     * Adds sources for this route that are not associated directly with a given host.
     *
     * @param sources the sources
     */
    public void addNonHostSources(Set<ConnectPoint> sources) {
        checkArgument(!sources.contains(null));
        addSources(HostId.NONE, sources);
    }

    /**
     * Removes all the sources contained in the associated route.
     */
    public void removeSources() {
        sources.clear();
    }

    /**
     * Removes the given source contained in the associated route.
     *
     * @param source the source to remove
     */
    public void removeSource(HostId source) {
        checkNotNull(source);
        sources.remove(source);
    }

    /**
     * Removes all the given sources for the given host for this route.
     *
     * @param hostId  the host
     * @param sources the sources to remove
     */
    public void removeSources(HostId hostId, Set<ConnectPoint> sources) {
        checkNotNull(hostId);
        checkArgument(!sources.contains(null));
        //if existing we remove from current set, otherwise just skip them
        this.sources.compute(hostId, (host, existingSources) -> {
            if (existingSources != null) {
                existingSources.removeAll(sources);
            }
            return existingSources;
        });
    }

    /**
     * Adds sinks for a given host Id. If the Host Id is {@link HostId#NONE} the sinks are intended to be
     * used at all times independently of the attached host.
     *
     * @param hostId the host
     * @param sinks  the sinks
     */
    public void addSinks(HostId hostId, Set<ConnectPoint> sinks) {
        checkNotNull(hostId);
        checkArgument(!sinks.contains(null));
        //if existing we add to current set, otherwise we put them all
        this.sinks.compute(hostId, (host, existingSinks) -> {
            if (existingSinks != null) {
                existingSinks.addAll(sinks);
                return existingSinks;
            } else {
                return sinks;
            }
        });
    }

    /**
     * Adds sink for this route that are not associated directly with a given host.
     *
     * @param sinks the sinks
     */
    public void addNonHostSinks(Set<ConnectPoint> sinks) {
        checkArgument(!sinks.contains(null));
        this.addSinks(HostId.NONE, sinks);
    }

    /**
     * Removes all the sinks for this route.
     */
    public void removeSinks() {
        sinks.clear();
    }

    /**
     * Removes all the sinks for the given host for this route.
     *
     * @param hostId the host
     */
    public void removeSinks(HostId hostId) {
        checkNotNull(hostId);
        this.sinks.remove(hostId);
    }

    /**
     * Removes all the given sinks for the given host for this route.
     *
     * @param hostId the host
     * @param sinks  the sinks to remove
     */
    public void removeSinks(HostId hostId, Set<ConnectPoint> sinks) {
        checkNotNull(hostId);
        checkArgument(!sinks.contains(null));
        //if existing we remove from current set, otherwise just skip them
        this.sinks.compute(hostId, (host, existingSinks) -> {
            if (existingSinks != null) {
                existingSinks.removeAll(sinks);
            }
            return existingSinks;
        });
    }

    /**
     * Returns if the route has no sinks.
     *
     * @return true if no sinks
     */
    public boolean isEmpty() {
        return sinks.isEmpty();
    }

    /**
     * Creates an empty route object.
     *
     * @return an empty muticast rout data object.
     */
    public static McastRouteData empty() {
        return new McastRouteData();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sources, sinks);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof McastRouteData)) {
            return false;
        }
        final McastRouteData other = (McastRouteData) obj;

        return super.equals(obj) &&
                Objects.equals(sources(), other.sources()) &&
                Objects.equals(sinks(), other.sinks());
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("sources", sources())
                .add("sinks", sinks())
                .toString();
    }
}
