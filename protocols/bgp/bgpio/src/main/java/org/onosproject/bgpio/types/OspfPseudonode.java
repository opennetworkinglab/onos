/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onlab.packet.Ip4Address;
import org.onosproject.bgpio.protocol.IgpRouterId;

import java.util.Objects;

/**
 * Provides implementation of OSPFPseudonode Tlv.
 */
public class OspfPseudonode implements IgpRouterId, BgpValueType {
    public static final short TYPE = 515;
    public static final short LENGTH = 8;

    private final int routerID;
    private final Ip4Address drInterface;

    /**
     * Constructor to initialize parameters.
     *
     * @param routerID routerID
     * @param drInterface IPv4 address of the DR's interface
     */
    public OspfPseudonode(int routerID, Ip4Address drInterface) {
        this.routerID = routerID;
        this.drInterface = drInterface;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param routerID routerID
     * @param drInterface IPv4 address of the DR's interface
     * @return object of OSPFPseudonode
     */
    public static OspfPseudonode of(final int routerID, final Ip4Address drInterface) {
        return new OspfPseudonode(routerID, drInterface);
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
        return Objects.hash(routerID, drInterface);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OspfPseudonode) {
            OspfPseudonode other = (OspfPseudonode) obj;
            return Objects.equals(routerID, other.routerID) && Objects.equals(drInterface, other.drInterface);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeInt(routerID);
        c.writeInt(drInterface.toInt());
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of OSPFPseudonode.
     *
     * @param cb ChannelBuffer
     * @return object of OSPFPseudonode
     */
    public static OspfPseudonode read(ChannelBuffer cb) {
        int routerID = cb.readInt();
        Ip4Address drInterface = Ip4Address.valueOf(cb.readInt());
        return OspfPseudonode.of(routerID, drInterface);
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
        int result = ((Integer) (this.routerID)).compareTo((Integer) (((OspfPseudonode) o).routerID));
        if (result != 0) {
            return this.drInterface.compareTo(((OspfPseudonode) o).drInterface);
        }
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("RouterID", routerID)
                .add("DRInterface", drInterface)
                .toString();
    }
}
