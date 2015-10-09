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
 * Provides implementation of IsIsPseudonode Tlv.
 */
public class IsIsPseudonode implements IGPRouterID, BGPValueType {
    protected static final Logger log = LoggerFactory.getLogger(IsIsPseudonode.class);

    public static final short TYPE = 515;
    public static final short LENGTH = 7;

    private final byte[] isoNodeID;
    private byte psnIdentifier;

    /**
     * Constructor to initialize isoNodeID.
     *
     * @param isoNodeID ISO system-ID
     * @param psnIdentifier PSN identifier
     */
    public IsIsPseudonode(byte[] isoNodeID, byte psnIdentifier) {
        this.isoNodeID = isoNodeID;
        this.psnIdentifier = psnIdentifier;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param isoNodeID ISO system-ID
     * @param psnIdentifier PSN identifier
     * @return object of IsIsPseudonode
     */
    public static IsIsPseudonode of(final byte[] isoNodeID, final byte psnIdentifier) {
        return new IsIsPseudonode(isoNodeID, psnIdentifier);
    }

    /**
     * Returns ISO NodeID.
     *
     * @return ISO NodeID
     */
    public byte[] getISONodeID() {
        return isoNodeID;
    }

    /**
     * Returns PSN Identifier.
     *
     * @return PSN Identifier
     */
    public byte getPSNIdentifier() {
        return this.psnIdentifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isoNodeID, psnIdentifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IsIsPseudonode) {
            IsIsPseudonode other = (IsIsPseudonode) obj;
            return Objects.equals(isoNodeID, other.isoNodeID) && Objects.equals(psnIdentifier, other.psnIdentifier);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeBytes(isoNodeID);
        c.writeByte(psnIdentifier);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of IsIsPseudonode.
     *
     * @param cb ChannelBuffer
     * @return object of IsIsPseudonode
     */
    public static IsIsPseudonode read(ChannelBuffer cb) {
        byte[] isoNodeID = new byte[LENGTH - 1];
        cb.readBytes(isoNodeID, 0, LENGTH - 1);
        byte psnIdentifier = cb.readByte();
        return IsIsPseudonode.of(isoNodeID, psnIdentifier);
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
                .add("isoNodeID", isoNodeID)
                .add("psnIdentifier", psnIdentifier)
                .toString();
    }
}