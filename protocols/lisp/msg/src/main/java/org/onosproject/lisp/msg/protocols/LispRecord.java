/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAfiAddress;

/**
 * Generic LISP record interface.
 */
public interface LispRecord {

    /**
     * Obtains record TTL value.
     *
     * @return record TTL value
     */
    int getRecordTtl();

    /**
     * Obtains address mask length.
     *
     * @return mask length
     */
    byte getMaskLength();

    /**
     * Obtains LispMapReplyAction enum code.
     *
     * @return LispMapReplyAction enum code
     */
    LispMapReplyAction getAction();

    /**
     * Obtains authoritative flag.
     *
     * @return authoritative flag
     */
    boolean isAuthoritative();

    /**
     * Obtains map version number.
     *
     * @return map version number
     */
    short getMapVersionNumber();

    /**
     * Obtains EID prefix.
     *
     * @return EID prefix
     */
    LispAfiAddress getEidPrefixAfi();

    /**
     * Writes LISP message object into communication channel.
     *
     * @param byteBuf byte buffer
     * @throws LispWriterException on error
     */
    void writeTo(ByteBuf byteBuf) throws LispWriterException;

    /**
     * A builder for LISP record.
     *
     * @param <T> sub-builder type
     */
    interface RecordBuilder<T> {

        /**
         * Sets record TTL value.
         *
         * @param recordTtl record TTL
         * @return parameterized object
         */
        T withRecordTtl(int recordTtl);

        /**
         * Sets mask length.
         *
         * @param maskLength mask length
         * @return parameterized object
         */
        T withMaskLength(byte maskLength);

        /**
         * Sets LISP map reply action enum.
         *
         * @param action map reply action
         * @return parameterized object
         */
        T withAction(LispMapReplyAction action);

        /**
         * Sets authoritative flag.
         *
         * @param authoritative authoritative flag
         * @return parameterized object
         */
        T withIsAuthoritative(boolean authoritative);

        /**
         * Sets LISP map version number.
         *
         * @param mapVersionNumber map version number
         * @return parameterized object
         */
        T withMapVersionNumber(short mapVersionNumber);

        /**
         * Sets EID prefix.
         *
         * @param prefix EID prefix
         * @return parameterized object
         */
        T withEidPrefixAfi(LispAfiAddress prefix);
    }
}
