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

import com.google.common.base.MoreObjects;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.protocol.IgpRouterId;

import java.util.Objects;

/**
 * Provides implementation of OSPFNonPseudonode Tlv.
 */
public class OspfNonPseudonode implements IgpRouterId, BgpValueType {
    public static final short TYPE = 515;
    public static final short LENGTH = 4;

    private final int routerID;

    /**
     * Constructor to initialize routerID.
     *
     * @param routerID routerID
     */
    public OspfNonPseudonode(int routerID) {
        this.routerID = routerID;
    }

    /**
     * Returns object of this class with specified routerID.
     *
     * @param routerID routerID
     * @return object of OSPFNonPseudonode
     */
    public static OspfNonPseudonode of(final int routerID) {
        return new OspfNonPseudonode(routerID);
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

        if (obj instanceof OspfNonPseudonode) {
            OspfNonPseudonode other = (OspfNonPseudonode) obj;
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
    public static OspfNonPseudonode read(ChannelBuffer cb) {
        return OspfNonPseudonode.of(cb.readInt());
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public int compareTo(Object o) {
        if (this.equals(o)) {
            return 0;
        }
        return ((Integer) (this.routerID)).compareTo((Integer) (((OspfNonPseudonode) o).routerID));
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
