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
package org.onlab.packet;

import java.nio.ByteBuffer;
import static org.onlab.packet.PacketUtils.checkBufferLength;

public class IGMPMembership extends IGMPGroup {

    // TODO should be an enum
    public static final byte MODE_IS_INCLUDE = 0x1;
    public static final byte MODE_IS_EXCLUDE = 0x2;
    public static final byte CHANGE_TO_INCLUDE_MODE = 0x3;
    public static final byte CHANGE_TO_EXCLUDE_MODE = 0x4;
    public static final byte ALLOW_NEW_SOURCES = 0x5;
    public static final byte BLOCK_OLD_SOURCES = 0x6;

    private final int minGroupRecordLen = Ip4Address.BYTE_LENGTH + 4;

    protected byte recordType;
    protected byte auxDataLength = 0;
    protected byte[] auxData;

    /**
     * Constructor initialized with a multicast group address.
     *
     * @param gaddr A multicast group address.
     */
    public IGMPMembership(Ip4Address gaddr) {
        super(gaddr, 0);
    }

    /**
     * Default constructor.
     */
    public IGMPMembership() {
        super();
    }

    /**
     * Gets the IGMP record type.
     *
     * @return record type
     */
    public byte getRecordType() {
        return recordType;
    }

    /**
     * Sets the IGMP record type.
     *
     * @param type A multicast record type, like MODE_IS_INCLUDE or MODE_IS_EXCLUDE.
     */
    public void setRecordType(byte type) {
        recordType = type;
    }


    /**
     * Serialize this Membership Report.
     *
     * @param bb the ByteBuffer to write into, positioned at the next spot to be written to.
     * @return serialized IGMP message.
     */
    @Override
    public byte[] serialize(ByteBuffer bb) {

        bb.put(recordType);
        bb.put(auxDataLength);      // reserved
        bb.putShort((short) sources.size());
        bb.put(gaddr.toOctets());
        for (IpAddress ipaddr : sources) {
            bb.put(ipaddr.toOctets());
        }

        if (auxDataLength > 0) {
            bb.put(auxData);
        }

        return bb.array();
    }

    /**
     * Deserialize the IGMP Membership report packet.
     *
     * @param bb the ByteBuffer wrapping the serialized message.  The position of the
     *           ByteBuffer should be pointing at the head of either message type.
     * @return IGMP Group
     * @throws DeserializationException if deserialization fails
     */
    public IGMPGroup deserialize(ByteBuffer bb) throws DeserializationException {

        // Make sure there is enough buffer to read the header,
        // including the number of sources
        checkBufferLength(bb.remaining(), 0, minGroupRecordLen);
        recordType = bb.get();
        auxDataLength = bb.get();
        int nsrcs = bb.getShort();

        gaddr = Ip4Address.valueOf(bb.getInt());


        for (; nsrcs > 0; nsrcs--) {
            // Make sure we have enough buffer to hold all of these sources
            checkBufferLength(bb.remaining(), 0, Ip4Address.BYTE_LENGTH * nsrcs);
            Ip4Address src = Ip4Address.valueOf(bb.getInt());
            this.sources.add(src);
        }

        if (auxDataLength > 0) {
            auxData = new byte[auxDataLength];
            bb.get(auxData, 0, auxDataLength);
        }
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals()
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IGMPMembership)) {
            return false;
        }
        IGMPMembership other = (IGMPMembership) obj;

        if (!this.gaddr.equals(other.gaddr)) {
            return false;
        }
        if (this.recordType != other.recordType) {
            return false;
        }
        if (this.auxDataLength != other.auxDataLength) {
            return false;
        }
        if (this.sources.size() != other.sources.size()) {
            return false;
        }
        // TODO: make these tolerant of order
        if (!this.sources.equals(other.sources)) {
            return false;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 2521;
        int result = super.hashCode();
        result = prime * result + this.gaddr.hashCode();
        result = prime * result + this.recordType;
        result = prime * result + this.auxDataLength;
        result = prime * result + this.sources.hashCode();
        return result;
    }
}
