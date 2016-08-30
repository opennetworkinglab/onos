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
import org.onosproject.lisp.msg.types.LispAfiAddress;

import java.util.List;

/**
 * LISP record section which is part of LISP map register message.
 */
public interface LispMapRecord {

    /**
     * Obtains record TTL value.
     *
     * @return record TTL value
     */
    int getRecordTtl();

    /**
     * Obtains locator count value.
     *
     * @return locator count value
     */
    int getLocatorCount();

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
     * Obtains a collection of locator records.
     *
     * @return a collection of locator records
     */
    List<LispLocatorRecord> getLocators();

    /**
     * Writes LISP message object into communication channel.
     *
     * @param byteBuf byte buffer
     */
    void writeTo(ByteBuf byteBuf);

    /**
     * A builder of LISP map record.
     */
    interface MapRecordBuilder {

        /**
         * Sets record TTL value.
         *
         * @param recordTtl record TTL
         * @return MapRecordBuilder object
         */
        MapRecordBuilder withRecordTtl(int recordTtl);

        /**
         * Sets mask length.
         *
         * @param maskLength mask length
         * @return MapRecordBuilder object
         */
        MapRecordBuilder withMaskLength(byte maskLength);

        /**
         * Sets LISP map reply action enum.
         *
         * @param action map reply action
         * @return MapRecordBuilder object
         */
        MapRecordBuilder withAction(LispMapReplyAction action);

        /**
         * Sets authoritative flag.
         *
         * @param authoritative authoritative flag
         * @return MapRecordBuilder object
         */
        MapRecordBuilder withAuthoritative(boolean authoritative);

        /**
         * Sets LISP map version number.
         *
         * @param mapVersionNumber map version number
         * @return MapRecordBuilder object
         */
        MapRecordBuilder withMapVersionNumber(short mapVersionNumber);

        /**
         * Sets EID prefix.
         *
         * @param prefix EID prefix
         * @return MapRecordBuilder object
         */
        MapRecordBuilder withEidPrefixAfi(LispAfiAddress prefix);

        /**
         * Sets a collection of locator records.
         *
         * @param records a collection of locator records
         * @return MapRecordBuilder object
         */
        MapRecordBuilder withLocators(List<LispLocatorRecord> records);

        /**
         * Builds map record.
         *
         * @return map record instance
         */
        LispMapRecord build();
    }
}
