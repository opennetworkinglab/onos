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

package org.onosproject.bgpio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.protocol.IGPRouterID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides implementation of OSPFNonPseudonode Tlv.
 */
public class OSPFNonPseudonode implements IGPRouterID, BGPValueType {

    protected static final Logger log = LoggerFactory.getLogger(OSPFNonPseudonode.class);

    public static final short TYPE = 515;
    public static final short LENGTH = 4;

    private final int routerID;

    /**
     * Constructor to initialize routerID.
     *
     * @param routerID routerID
     */
    public OSPFNonPseudonode(int routerID) {
        this.routerID = routerID;
    }

    /**
     * Returns object of this class with specified routerID.
     *
     * @param routerID routerID
     * @return object of OSPFNonPseudonode
     */
    public static OSPFNonPseudonode of(final int routerID) {
        return new OSPFNonPseudonode(routerID);
    }

    /**
     * Returns RouterID.
     *
     * @return RouterID
     */
    public int getrouterID() {
        return this.routerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(routerID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof OSPFNonPseudonode) {
            OSPFNonPseudonode other = (OSPFNonPseudonode) obj;
            return Objects.equals(routerID, other.routerID);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeInt(routerID);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of OSPFNonPseudonode.
     *
     * @param cb ChannelBuffer
     * @return object of OSPFNonPseudonode
     */
    public static OSPFNonPseudonode read(ChannelBuffer cb) {
        return OSPFNonPseudonode.of(cb.readInt());
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("RouterID", routerID)
                .toString();
    }
}