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

package org.onosproject.drivers.juniper;

import com.google.common.annotations.Beta;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.net.intent.Intent.DEFAULT_INTENT_PRIORITY;


/**
 * Definition of a static route for the Juniper.
 */
@Beta
public class StaticRoute {

    /**
     * Maximum metric value for the Juniper.
     */
    private static final int BEST_STATIC_METRIC = 1;

    /**
     * Max FlowRule priority that corresponds to the BEST_STATIC_METRIC of the Juniper.
     * DEFAULT_INTENT_PRIORITY + DEFAULT_METRIC_STATIC_ROUTE - BEST_STATIC_METRIC
     */
    private static final int MAX_FLOWRULE_PRIORITY_ACCEPTED = 104;

    /**
     * Default static route metric.
     */
    public static final int DEFAULT_METRIC_STATIC_ROUTE = 5;

    private final Ip4Prefix ipv4Dest;
    private final Ip4Address nextHop;
    private final int priority;
    private final boolean localRoute;

    public StaticRoute(Ip4Prefix ipv4Dest, Ip4Address nextHop, boolean localRoute, int priority) {
        this.ipv4Dest = ipv4Dest;
        this.nextHop = nextHop;
        this.priority = priority;
        this.localRoute = localRoute;
    }

    public StaticRoute(Ip4Prefix ipv4Dest, Ip4Address nextHop, boolean localRoute) {
        this(ipv4Dest, nextHop, localRoute, DEFAULT_INTENT_PRIORITY);
    }

    public Ip4Prefix ipv4Dst() {
        return ipv4Dest;
    }

    public Ip4Address nextHop() {
        return nextHop;
    }

    /**
     * Method to covert the priority into the Juniper metric.
     * See https://goo.gl/ACo952.
     * @return the metric for the Juniper
     */
    public int getMetric() {
        if (priority > MAX_FLOWRULE_PRIORITY_ACCEPTED) {
            return BEST_STATIC_METRIC;
        }
        return DEFAULT_INTENT_PRIORITY + DEFAULT_METRIC_STATIC_ROUTE - priority;
    }

    /**
     * Method to convert the Juniper metric into a priority.
     *
     * @param metric the metric to be converted
     * @return the priority
     */
    public static int toFlowRulePriority(int metric) {
        return DEFAULT_INTENT_PRIORITY + DEFAULT_METRIC_STATIC_ROUTE - metric;
    }

    /**
     * Directly attached routes are called local route. Such local routes are directly discovered by
     * the router itself.
     *
     * @return true if it is a local route
     */
    public boolean isLocalRoute() {
        return localRoute;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof StaticRoute) {
            StaticRoute that = (StaticRoute) obj;
            return Objects.equals(ipv4Dest, that.ipv4Dest) &&
                    Objects.equals(nextHop, that.nextHop) &&
                    Objects.equals(priority, that.priority) &&
                    Objects.equals(localRoute, that.localRoute);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipv4Dest, nextHop, priority, localRoute);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .omitNullValues()
                .addValue(localRoute ? "Local route" : null)
                .add("IP address destination", ipv4Dest)
                .add("Next Hop IP address", nextHop)
                .add("Priority", priority)
                .toString();
    }
}
