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
import org.onlab.packet.Ip4Address;
import org.onosproject.bgpio.protocol.IGPRouterID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides implementation of OSPFPseudonode Tlv.
 */
public class OSPFPseudonode implements IGPRouterID, BGPValueType {

    protected static final Logger log = LoggerFactory.getLogger(OSPFPseudonode.class);

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
    public OSPFPseudonode(int routerID, Ip4Address drInterface) {
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
    public static OSPFPseudonode of(final int routerID, final Ip4Address drInterface) {
        return new OSPFPseudonode(routerID, drInterface);
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
        if (obj instanceof OSPFPseudonode) {
            OSPFPseudonode other = (OSPFPseudonode) obj;
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
    public static OSPFPseudonode read(ChannelBuffer cb) {
        int routerID = cb.readInt();
        Ip4Address drInterface = Ip4Address.valueOf(cb.readInt());
        return OSPFPseudonode.of(routerID, drInterface);
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
                .add("DRInterface", drInterface)
                .toString();
    }
}