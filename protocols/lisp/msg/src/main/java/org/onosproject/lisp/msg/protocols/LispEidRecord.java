/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.lisp.msg.protocols;

import io.netty.buffer.ByteBuf;
import com.google.common.base.Objects;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispAfiAddress.AfiAddressReader;
import org.onosproject.lisp.msg.types.LispAfiAddress.AfiAddressWriter;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.LispIpv6Address;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * LISP EID record section which is part of LISP map request message.
 */
public final class LispEidRecord {

    private final byte maskLength;
    private final LispAfiAddress prefix;

    // Cache the hash code for the string, default to 0
    private final int hash;

    /**
     * Initializes LispEidRecord with mask length and EID prefix.
     *
     * @param maskLength mask length
     * @param prefix EID prefix
     */
    public LispEidRecord(byte maskLength, LispAfiAddress prefix) {
        this.maskLength = maskLength;

        checkNotNull(prefix, "Must specify an address prefix");

        // re-calculate the IP address based on the maskLength
        switch (prefix.getAfi()) {
            case IP4:
                this.prefix = new LispIpv4Address(IpAddress.makeMaskedAddress(
                        IpAddress.valueOf(prefix.toString()), maskLength));
                break;
            case IP6:
                this.prefix = new LispIpv6Address(IpAddress.makeMaskedAddress(
                        IpAddress.valueOf(prefix.toString()), maskLength));
                break;
            default:
                this.prefix = prefix;
        }

        this.hash = 31 * 17 + Objects.hashCode(maskLength, prefix);
    }

    /**
     * Obtains mask length of the EID Record.
     *
     * @return mask length of the EID Record
     */
    public byte getMaskLength() {
        return maskLength;
    }

    /**
     * Obtains EID prefix.
     *
     * @return EID prefix
     */
    public LispAfiAddress getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("maskLength", maskLength)
                .add("prefix", prefix).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LispEidRecord that = (LispEidRecord) o;
        return Objects.equal(maskLength, that.maskLength) &&
                Objects.equal(prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * A LISP message reader for EidRecord portion.
     */
    public static final class EidRecordReader implements LispMessageReader<LispEidRecord> {

        private static final int RESERVED_SKIP_LENGTH = 1;

        @Override
        public LispEidRecord readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException {

            // let's skip the reserved field
            byteBuf.skipBytes(RESERVED_SKIP_LENGTH);

            // mask length -> 8 bits
            short maskLength = byteBuf.readUnsignedByte();

            LispAfiAddress prefix = new AfiAddressReader().readFrom(byteBuf);

            return new LispEidRecord((byte) maskLength, prefix);
        }
    }

    /**
     * A LISP message writer for EidRecord portion.
     */
    public static final class EidRecordWriter implements LispMessageWriter<LispEidRecord> {

        private static final int UNUSED_ZERO = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispEidRecord message) throws LispWriterException {

            // fill zero into reserved field
            byteBuf.writeByte((short) UNUSED_ZERO);

            // mask length
            byteBuf.writeByte(message.getMaskLength());

            // EID prefix AFI with EID prefix
            AfiAddressWriter afiAddressWriter = new AfiAddressWriter();
            afiAddressWriter.writeTo(byteBuf, message.getPrefix());
        }
    }
}
