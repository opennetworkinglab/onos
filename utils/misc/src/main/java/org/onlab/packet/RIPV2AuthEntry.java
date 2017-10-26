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
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

import static org.slf4j.LoggerFactory.getLogger;

/*
 * Authentication Entry for RIP version 2 - RFC 2082
 */
public class RIPV2AuthEntry extends BasePacket {
    private static final int ENTRY_LEN = 20;
    private final Logger log = getLogger(getClass());
    protected short addressFamilyId;
    protected short type;
    protected short offset;
    protected byte keyId;
    protected byte authLen;
    protected int sequence;


    @Override
    public byte[] serialize() {
        ByteBuffer byteBuffer;
        byteBuffer = ByteBuffer.allocate(ENTRY_LEN);
        byteBuffer.putShort(addressFamilyId);
        byteBuffer.putShort(type);
        byteBuffer.putShort(offset);
        byteBuffer.put(keyId);
        byteBuffer.put(authLen);
        byteBuffer.putInt(sequence);
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);
        return byteBuffer.array();
    }

    /**
     * Deserializer function for RIPv2 entry.
     *
     * @return deserializer function
     */
    public static Deserializer<RIPV2AuthEntry> deserializer() {
        return (data, offset, length) -> {
            RIPV2AuthEntry authEntry = new RIPV2AuthEntry();

            checkNotNull(data);

            if (offset < 0 || length < 0 ||
                length > data.length || offset >= data.length ||
                offset + length > data.length) {
               throw new DeserializationException("Illegal offset or length");
            }
            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            if (bb.remaining() < ENTRY_LEN) {
               throw new DeserializationException(
                          "Buffer underflow while reading RIP authentication entry");
            }
            authEntry.addressFamilyId = bb.getShort();
            authEntry.type = bb.getShort();
            authEntry.offset = bb.getShort();
            authEntry.keyId = bb.get();
            authEntry.authLen = bb.get();
            authEntry.sequence = bb.getInt();
            return authEntry;
        };
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sequence, authLen, keyId, offset, addressFamilyId, type);
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
        if (!(obj instanceof RIPV2AuthEntry)) {
            return false;
        }
        final RIPV2AuthEntry that = (RIPV2AuthEntry) obj;

        return super.equals(that) &&
                Objects.equals(type, that.type) &&
                Objects.equals(addressFamilyId, that.addressFamilyId) &&
                Objects.equals(offset, that.offset) &&
                Objects.equals(keyId, that.keyId) &&
                Objects.equals(authLen, that.authLen) &&
                Objects.equals(sequence, that.sequence);
    }

    /**
     * @return the Address Family Identifier
     */
    public short getAddressFamilyId() {
        return this.addressFamilyId;
    }

    /**
     * @param addressFamilyIdentifier the address family identifier to set
     * @return this
     */
    public RIPV2AuthEntry setAddressFamilyId(final short addressFamilyIdentifier) {
        this.addressFamilyId = addressFamilyIdentifier;
        return this;
    }

    /**
     * @return the authentication type
     */
    public short getType() {
        return this.type;
    }

    /**
     * @param type the authentication type to set
     * @return this
     */
    public RIPV2AuthEntry setType(final short type) {
        this.type = type;
        return this;
    }

    /**
     * @return the offset of authentication data
     */
    public short getOffset() {
        return this.offset;
    }

    /**
     * @param offset the offset of authentication data  to set
     * @return this
     */
    public RIPV2AuthEntry setOffset(final short offset) {
        this.offset = offset;
        return this;
    }
    /**
     * @return the subnet mask
     */
    public byte getKeyId() {
        return this.keyId;
    }

    /**
     * @param keyId The key id  to set
     * @return this
     */
    public RIPV2AuthEntry setKeyId(final byte keyId) {
        this.keyId = keyId;
        return this;
    }

    /**
     * @return the authentication data length
     */
    public byte getAuthLen() {
        return this.authLen;
    }

    /**
     * @param authlen the length of the authentication data  to set
     * @return this
     */
    public RIPV2AuthEntry setAuthLen(final byte authlen) {
        this.authLen = authlen;
        return this;
    }


    /**
     * @return the sequence number
     */
    public int getSequence() {
        return this.sequence;
    }

    /**
     * @param sequencenumber sequence number to set
     * @return this
     */
    public RIPV2AuthEntry setSequenceNumber(final int sequencenumber) {
        this.sequence = sequencenumber;
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "RIPV2AuthEntry [address family Id=" + this.addressFamilyId + ", type=" + this.type
                + ", offset=" + this.offset
                + ", key ID=" + this.keyId
                + ", authentication length = " + this.authLen
                + ", sequence number=" + this.sequence + "]";
    }
}
