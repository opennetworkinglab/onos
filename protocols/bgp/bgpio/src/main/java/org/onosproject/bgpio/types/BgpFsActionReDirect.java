/*
 * Copyright 2016-present Open Networking Laboratory
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

import java.util.Arrays;

import org.onosproject.bgpio.util.Constants;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;

import com.google.common.base.MoreObjects;

/**
 * Provides implementation of BGP flow specification action.
 */
public class BgpFsActionReDirect implements BgpValueType {

    public static final short TYPE = Constants.BGP_FLOWSPEC_ACTION_TRAFFIC_REDIRECT;
    private byte[] routeTarget;
    public static final byte ROUTE_TARGET_LEN = 6;

    /**
     * Constructor to initialize the value.
     *
     * @param routeTarget route target
     */
    public BgpFsActionReDirect(byte[] routeTarget) {
        this.routeTarget = Arrays.copyOf(routeTarget, routeTarget.length);
    }

    @Override
    public short getType() {
        return this.TYPE;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(routeTarget);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BgpFsActionReDirect) {
            BgpFsActionReDirect other = (BgpFsActionReDirect) obj;
            return Arrays.equals(this.routeTarget, other.routeTarget);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();

        cb.writeShort(TYPE);

        cb.writeBytes(routeTarget);

        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object.
     *
     * @param cb channelBuffer
     * @return object of flow spec action redirect
     * @throws BgpParseException while parsing BgpFsActionReDirect
     */
    public static BgpFsActionReDirect read(ChannelBuffer cb) throws BgpParseException {
        byte[] routeTarget;

        routeTarget = cb.readBytes(ROUTE_TARGET_LEN).array();
        return new BgpFsActionReDirect(routeTarget);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("TYPE", TYPE)
                .add("routeTarget", routeTarget).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
