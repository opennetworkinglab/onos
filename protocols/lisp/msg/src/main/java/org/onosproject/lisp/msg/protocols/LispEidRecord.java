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
import org.onosproject.lisp.msg.types.LispAfiAddress;

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
     * A private LISP message reader for EidRecord portion.
     */
    public static class EidRecordReader implements LispMessageReader<LispEidRecord> {

        private static final int RESERVED_SKIP_LENGTH = 1;

        @Override
        public LispEidRecord readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException {

            // let's skip the reserved field
            byteBuf.skipBytes(RESERVED_SKIP_LENGTH);

            short maskLength = (short) byteBuf.readUnsignedShort();

            LispAfiAddress prefix = new LispAfiAddress.AfiAddressReader().readFrom(byteBuf);

            return new LispEidRecord((byte) maskLength, prefix);
        }
    }
}
