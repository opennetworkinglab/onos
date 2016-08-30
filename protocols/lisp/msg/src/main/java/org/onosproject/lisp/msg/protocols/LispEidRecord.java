/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAfiAddress;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.lisp.msg.types.LispAfiAddress.AfiAddressWriter;

/**
 * LISP EID record section which is part of LISP map request message.
 */
public final class LispEidRecord {

    private final byte maskLength;
    private final LispAfiAddress prefix;

    /**
     * Initializes LispEidRecord with mask length and EID prefix.
     *
     * @param maskLength mask length
     * @param prefix EID prefix
     */
    public LispEidRecord(byte maskLength, LispAfiAddress prefix) {
        this.maskLength = maskLength;

        checkNotNull(prefix, "Must specify an address prefix");

        this.prefix = prefix;
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

            LispAfiAddress prefix = new LispAfiAddress.AfiAddressReader().readFrom(byteBuf);

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
