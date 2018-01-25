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

package org.onosproject.bgpio.types;

import com.google.common.base.MoreObjects;

import java.util.Arrays;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Implementation of RouteTarget.
 */
public class RouteTarget implements BgpValueType {

    /*
     * Type 0x00: Local Administrator sub-field uses 2 octets with AS number and
     * Assigned number uses 4 octests Type 0x01: Local Administrator sub-field
     * uses 4 octets with IP address and Assigned number uses 2 octests Type
     * 0x02: Local Administrator sub-field uses 4 octets with AS number and
     * Assigned number uses 2 octests
     */
    private byte[] routeTarget;
    private short type;

    public enum RouteTargetype {

        AS((short) 0x0002), IP((short) 0x0102), LARGEAS((short) 0x0202);
        short value;

        /**
         * Assign val with the value as the tunnel type.
         *
         * @param val tunnel type
         */
        RouteTargetype(short val) {
            value = val;
        }

        /**
         * Returns value of route type.
         *
         * @return route type
         */
        public short getType() {
            return value;
        }
    }

    /**
     * Resets fields.
     */
    public RouteTarget() {
        this.type = 0;
        this.routeTarget = null;
    }

    /**
     * Constructor to initialize parameters.
     *
     * @param type        type
     * @param routeTarget route target
     */
    public RouteTarget(short type, byte[] routeTarget) {
        this.type = type;
        this.routeTarget = routeTarget;
    }

    /**
     * Reads route target from channelBuffer.
     *
     * @param type type
     * @param cb   channelBuffer
     * @return object of RouteTarget
     */
    public static RouteTarget read(short type, ChannelBuffer cb) {
        return new RouteTarget(type, cb.readBytes(6).array());
    }

    /**
     * Returns route target.
     *
     * @return route target
     */
    public byte[] getRouteTarget() {
        return this.routeTarget;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof RouteTarget) {
            RouteTarget that = (RouteTarget) obj;
            if (this.type == that.type
                    && Arrays.equals(this.routeTarget, that.routeTarget)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, Arrays.hashCode(routeTarget));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("type", type)
                .add("routeTarget", routeTarget).toString();
    }

    @Override
    public short getType() {
        return type;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeShort(type);
        cb.writeBytes(routeTarget);
        return cb.writerIndex() - iLenStartIndex;
    }

    @Override
    public int compareTo(Object rd) {
        return 0;
    }
}
