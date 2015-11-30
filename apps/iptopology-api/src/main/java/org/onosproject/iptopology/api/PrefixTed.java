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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onlab.packet.IpAddress;

/**
 * This class provides implementation of prefix traffic engineering data.
 */
public class PrefixTed {
    private final IgpFlags igpFlags;
    private final RouteTag routeTag;
    private final ExtendedRouteTag extendedRouteTag;
    private final Metric metric;
    private final IpAddress fwdingAddress;

    /**
     * Constructor to initialize its parameters.
     *
     * @param igpFlags igp flags
     * @param routeTag ospf route tag
     * @param extendedRouteTag isis route tag
     * @param metric prefix metric
     * @param fwdingAddress forwarding address
     */
    /**
     * Constructor to initialize its parameters.
     *
     * @param igpFlags IS-IS and OSPF flags assigned to the prefix
     * @param routeTag IGP (ISIS or OSPF) tags of the prefix
     * @param extendedRouteTag extended ISIS route tags of the prefix
     * @param metric metric of the prefix
     * @param fwdingAddress OSPF forwarding address
     */
    public PrefixTed(IgpFlags igpFlags, RouteTag routeTag, ExtendedRouteTag extendedRouteTag,
            Metric metric, IpAddress fwdingAddress) {
        this.igpFlags = igpFlags;
        this.routeTag = routeTag;
        this.extendedRouteTag = extendedRouteTag;
        this.metric = metric;
        this.fwdingAddress = fwdingAddress;
    }

    /**
     * Provides IS-IS and OSPF flags assigned to the prefix.
     *
     * @return IGP flags
     */
    public IgpFlags igpFlags() {
        return igpFlags;
    }

    /**
     * Provides IGP (ISIS or OSPF) tags of the prefix.
     *
     * @return IGP route tag.
     */
    public RouteTag routeTag() {
        return routeTag;
    }

    /**
     * Provides extended ISIS route tags of the prefix.
     *
     * @return extended IS-IS route tag
     */
    public ExtendedRouteTag extendedRouteTag() {
        return extendedRouteTag;
    }

    /**
     * Provides metric of the prefix.
     *
     * @return prefix metric
     */
    public Metric metric() {
        return metric;
    }

    /**
     * Provides OSPF forwarding address.
     *
     * @return forwarding address
     */
    public IpAddress fwdingAddress() {
        return fwdingAddress;
    }


    @Override
    public int hashCode() {
        return Objects.hash(igpFlags, routeTag, extendedRouteTag, metric, fwdingAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PrefixTed) {
            PrefixTed other = (PrefixTed) obj;
            return Objects.equals(igpFlags, other.igpFlags) && Objects.equals(extendedRouteTag, other.extendedRouteTag)
                   && Objects.equals(routeTag, other.routeTag) && Objects.equals(metric, other.metric)
                   && Objects.equals(fwdingAddress, other.fwdingAddress);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .omitNullValues()
                .add("igpFlags", igpFlags)
                .add("extendedRouteTag", extendedRouteTag)
                .add("routeTag", routeTag)
                .add("metric", metric)
                .add("fwdingAddress", fwdingAddress)
                .toString();
    }
}