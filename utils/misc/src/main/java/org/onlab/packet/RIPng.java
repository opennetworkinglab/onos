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

/**
 * Implemented according to RFC 2080
 */

package org.onlab.packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.onlab.packet.PacketUtils.checkInput;
import static com.google.common.base.MoreObjects.toStringHelper;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Representation of an RIPng Packet.
 */
public class RIPng extends BasePacket {
    /**
     * Routing Information Protocol Next Generation packet.
     * ------------------------------------------ |cmdType (1) | version(1) |
     * ------------------------------------------ |reserved (2) |
     * ------------------------------------------ | route entries (n*20) |
     * ------------------------------------------
     *
     */
    // the case of no route entry
    public static final int MIN_HEADER_LENGTH = 4;


    private final Logger log = getLogger(getClass());

    public enum CmdType {
        RIPngREQUEST(1),
        RIPngRESPONSE(2);

        protected int value;

        CmdType(final int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static CmdType getType(final int value) {
            switch (value) {
                case 1:
                    return RIPngREQUEST;
                case 2:
                    return RIPngRESPONSE;
                default:
                    return null;
            }
        }
    }

    protected byte cmdType;
    protected byte version;
    protected short reserved;
    protected List<RIPngEntry> rtEntries = new ArrayList<RIPngEntry>();

    /**
     * @return the cmdType
     */
    public byte getCmdType() {
        return this.cmdType;
    }

    /**
     * @param cmdType
     *            the cmdType to set
     * @return this
     */
    public RIPng setCmdType(final byte cmdType) {
        this.cmdType = cmdType;
        return this;
    }

    /**
     * @return the version
     */
    public byte getVersion() {
        return this.version;
    }

    /**
     * @param version
     *            the version to set
     * @return this
     */
    public RIPng setVersion(final byte version) {
        this.version = version;
        return this;
    }

    /**
     * @return the reserved short
     */
    public short getReserved() {
        return this.reserved;
    }

    /**
     * @param reserved
     *            the reserved short to set
     * @return this
     */
    public RIPng setReserved(final short reserved) {
        this.reserved = reserved;
        return this;
    }

    /**
     * @return the route entries
     */
    public List<RIPngEntry> getRtEntries() {
        return this.rtEntries;
    }

    /**
     * @param entries
     *            the route entries to set
     * @return this
     */
    public RIPng setRtEntries(final List<RIPngEntry> entries) {
        this.rtEntries = entries;
        return this;
    }

    @Override
    public byte[] serialize() {
        // not guaranteed to retain length/exact format
        this.resetChecksum();

        final byte[] data = new byte[MIN_HEADER_LENGTH + RIPngEntry.ENTRY_LEN * rtEntries.size()];
        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(this.cmdType);
        bb.put(this.version);
        bb.putShort(this.reserved);
        for (final RIPngEntry entry : this.rtEntries) {
            bb.put(entry.serialize());
        }
        // assume the rest is padded out with zeroes
        return data;
    }

    /**
     * Deserializer function for RIPng packets.
     *
     * @return deserializer function
     */
    public static Deserializer<RIPng> deserializer() {
        return (data, offset, length) -> {
            RIPng ripng = new RIPng();

            checkInput(data, offset, length, MIN_HEADER_LENGTH);

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            ripng.cmdType = bb.get();
            ripng.version = bb.get();
            ripng.reserved = bb.getShort();

            // read route entries
            while (bb.hasRemaining() && (bb.remaining() >= RIPngEntry.ENTRY_LEN)) {
                RIPngEntry rtEntry;
                byte[] rtData = new byte[RIPngEntry.ENTRY_LEN];
                bb.get(rtData);

                rtEntry = RIPngEntry.deserializer().deserialize(rtData, 0, rtData.length);
                ripng.rtEntries.add(rtEntry);
            }

            return ripng;
        };
    }
    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cmdType, reserved, version, rtEntries);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RIPng)) {
            return false;
        }
        final RIPng that = (RIPng) obj;

        return super.equals(that) &&
                Objects.equals(version, that.version) &&
                Objects.equals(reserved, that.reserved) &&
                Objects.equals(cmdType, that.cmdType) &&
                Objects.equals(rtEntries, that.rtEntries);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("cmdType", Byte.toString(cmdType))
                .add("version", Byte.toString(version))
                .add("reserved", Short.toString(reserved))
                .toString();
        // TODO: need to handle route entries
    }
}
