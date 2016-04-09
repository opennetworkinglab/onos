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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.onlab.packet.IpAddress;

/**
 * This class provides implementation of prefix traffic engineering data.
 */
public class PrefixTed {
    private final IgpFlags igpFlags;
    private final List<RouteTag> routeTag;
    private final List<ExtendedRouteTag> extendedRouteTag;
    private final Metric metric;
    private final IpAddress fwdingAddress;

    /**
     * Constructor to initialize its fields.
     *
     * @param igpFlags IS-IS and OSPF flags assigned to the prefix
     * @param routeTag IGP (ISIS or OSPF) tags of the prefix
     * @param extendedRouteTag extended ISIS route tags of the prefix
     * @param metric metric of the prefix
     * @param fwdingAddress OSPF forwarding address
     */
    public PrefixTed(IgpFlags igpFlags, List<RouteTag> routeTag, List<ExtendedRouteTag> extendedRouteTag,
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
    public List<RouteTag> routeTag() {
        return routeTag;
    }

    /**
     * Provides extended ISIS route tags of the prefix.
     *
     * @return extended IS-IS route tag
     */
    public List<ExtendedRouteTag> extendedRouteTag() {
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
            Iterator<RouteTag> objListIterator = other.routeTag.iterator();
            int countOtherCommonRouteTag = other.routeTag.size();
            int countCommonRouteTag = routeTag.size();

            Iterator<ExtendedRouteTag> objListIterator1 = other.extendedRouteTag.iterator();
            int countOtherCommonExtRouteTag = other.extendedRouteTag.size();
            int countCommonExtRouteTag = extendedRouteTag.size();

            boolean isCommonRouteType = true;
            boolean isCommonExtRouteType = true;
            if (countOtherCommonRouteTag != countCommonRouteTag
                    || countOtherCommonExtRouteTag != countCommonExtRouteTag) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonRouteType) {
                    RouteTag subTlv = objListIterator.next();
                    if (routeTag.contains(subTlv) && other.routeTag.contains(subTlv)) {
                        isCommonRouteType = Objects.equals(routeTag.get(routeTag.indexOf(subTlv)),
                                other.routeTag.get(other.routeTag.indexOf(subTlv)));
                    } else {
                        isCommonRouteType = false;
                    }
                }
                while (objListIterator1.hasNext() && isCommonExtRouteType) {
                    ExtendedRouteTag subTlv = objListIterator1.next();
                    if (extendedRouteTag.contains(subTlv) && other.extendedRouteTag.contains(subTlv)) {
                        isCommonExtRouteType = Objects.equals(extendedRouteTag.get(extendedRouteTag.indexOf(subTlv)),
                                other.extendedRouteTag.get(other.extendedRouteTag.indexOf(subTlv)));
                    } else {
                        isCommonExtRouteType = false;
                    }
                }
                return isCommonRouteType && isCommonExtRouteType && Objects.equals(igpFlags, other.igpFlags)
                        && Objects.equals(metric, other.metric) && Objects.equals(fwdingAddress, other.fwdingAddress);
            }
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
