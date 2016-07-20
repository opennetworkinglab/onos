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

package org.onosproject.bgpio.types;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.base.MoreObjects;

/**
 * Implementation of RouteDistinguisher.
 */
public class RouteDistinguisher implements Comparable<RouteDistinguisher> {

    private long routeDistinguisher;

    /**
     * Resets fields.
     */
    public RouteDistinguisher() {
        this.routeDistinguisher = 0;
    }

    /**
     * Constructor to initialize parameters.
     *
     * @param routeDistinguisher route distinguisher
     */
    public RouteDistinguisher(long routeDistinguisher) {
        this.routeDistinguisher = routeDistinguisher;
    }

    /**
     * Reads route distinguisher from channelBuffer.
     *
     * @param cb channelBuffer
     * @return object of RouteDistinguisher
     */
    public static RouteDistinguisher read(ChannelBuffer cb) {
        return new RouteDistinguisher(cb.readLong());
    }

    /**
     * Returns route distinguisher.
     *
     * @return route distinguisher
     */
    public long getRouteDistinguisher() {
        return this.routeDistinguisher;
    }

    @Override
    public int compareTo(RouteDistinguisher rd) {
        if (this.equals(rd)) {
            return 0;
        }
        return ((Long) (this.getRouteDistinguisher())).compareTo((Long) (rd.getRouteDistinguisher()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof RouteDistinguisher) {

            RouteDistinguisher that = (RouteDistinguisher) obj;

            if (this.routeDistinguisher == that.routeDistinguisher) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(routeDistinguisher);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("routeDistinguisher", routeDistinguisher)
                .toString();
    }
}
