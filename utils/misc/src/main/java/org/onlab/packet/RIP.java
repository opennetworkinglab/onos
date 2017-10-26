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
package org.onlab.packet;

import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implements RIP Packet format, according to RFC 2453.
 */

public class RIP extends BasePacket {
    /**
     * Routing Information Protocol packet.
     * ------------------------------------------ |cmdType (1) | version(1) |
     * ------------------------------------------ |reserved (2) |
     * ------------------------------------------ | route entries (n*20) |
     * ------------------------------------------
     *
     */
    // the case of no RIP entry
    public static final int MIN_HEADER_LENGTH = 4;


    private final Logger log = getLogger(getClass());

    public enum CmdType {
        RIPREQUEST(1),
        RIPRESPONSE(2);

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
                    return RIPREQUEST;
                case 2:
                    return RIPRESPONSE;
                default:
                    return null;
            }
        }
    }

    protected byte cmdType;
    protected byte version;
    protected short reserved;
    protected List<RIPV2Entry> rtEntries = new ArrayList<RIPV2Entry>();
    protected RIPV2AuthEntry authEntry = null;
    protected byte[] rawData = null;

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
    public RIP setCmdType(final byte cmdType) {
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
    public RIP setVersion(final byte version) {
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
    public RIP setReserved(final short reserved) {
        this.reserved = reserved;
        return this;
    }

    /**
     * @return the route entries
     */
    public List<RIPV2Entry> getRtEntries() {
        return this.rtEntries;
    }

    /**
     * @param entries
     *            the route entries to set
     * @return this
     */
    public RIP setRtEntries(final List<RIPV2Entry> entries) {
        this.rtEntries = entries;
        return this;
    }
    /**
     * @return the authentication entry
     */
    public RIPV2AuthEntry getAuthEntry() {
        return this.authEntry;
    }

    /**
     * @return the raw data of whole RIP packet (after RIP header)
     */
    public byte[] getRawData() {
        return this.rawData;
    }

    @Override
    public byte[] serialize() {
        // not guaranteed to retain length/exact format
        this.resetChecksum();
       int dataLength = MIN_HEADER_LENGTH + RIPV2Entry.ENTRY_LEN * rtEntries.size();
       if (authEntry != null) {
           dataLength += RIPV2Entry.ENTRY_LEN;
       }
        final byte[] data = new byte[dataLength];
        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(this.cmdType);
        bb.put(this.version);
        bb.putShort(this.reserved);
        if (authEntry != null) {
            bb.put(authEntry.serialize());
        }
        for (final RIPV2Entry entry : this.rtEntries) {
            bb.put(entry.serialize());
        }
        // assume the rest is padded out with zeroes
        return data;
    }

    /**
     * Deserializer function for RIP packets.
     *
     * @return deserializer function
     */
    public static Deserializer<RIP> deserializer() {
        return (data, offset, length) -> {
            RIP rip = new RIP();

            checkInput(data, offset, length, MIN_HEADER_LENGTH);

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            rip.rawData = Arrays.copyOfRange(data, offset, offset + length);


            rip.cmdType = bb.get();
            rip.version = bb.get();
            rip.reserved = bb.getShort();

            // read route entries
            while (bb.hasRemaining() && (bb.remaining() >= RIPV2Entry.ENTRY_LEN)) {
                byte[] rtData = new byte[RIPV2Entry.ENTRY_LEN];
                bb.get(rtData);

                if (rtData[0] == -1 && rtData[1] == -1) {
                    // second time reaching here is the signature at the end of the packet, don't process it
                    if (rip.authEntry == null) {
                        rip.authEntry = RIPV2AuthEntry.deserializer().deserialize(rtData, 0, rtData.length);
                    }
                } else {
                    RIPV2Entry rtEntry = RIPV2Entry.deserializer().deserialize(rtData, 0, rtData.length);
                    rip.rtEntries.add(rtEntry);
                }
            }

            return rip;
        };
    }
    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cmdType, reserved, version, authEntry, rtEntries);
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
        if (!(obj instanceof RIP)) {
            return false;
        }
        final RIP that = (RIP) obj;


        return super.equals(that) &&
                Objects.equals(version, that.version) &&
                Objects.equals(reserved, that.reserved) &&
                Objects.equals(cmdType, that.cmdType) &&
                Objects.equals(authEntry, that.authEntry) &&
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
