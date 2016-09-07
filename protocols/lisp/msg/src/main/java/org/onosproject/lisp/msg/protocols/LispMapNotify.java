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

import java.util.List;

/**
 * LISP map notify message interface.
 * <p>
 * LISP map notify message format is defined in RFC6830.
 * https://tools.ietf.org/html/rfc6830#page-39
 *
 * <pre>
 * {@literal
 *      0                   1                   2                   3
 *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |Type=4 |              Reserved                 | Record Count  |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                         Nonce . . .                           |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                         . . . Nonce                           |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |            Key ID             |  Authentication Data Length   |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     ~                     Authentication Data                       ~
 * +-> +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   |                          Record TTL                           |
 * |   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * R   | Locator Count | EID mask-len  | ACT |A|      Reserved         |
 * e   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * c   | Rsvd  |  Map-Version Number   |         EID-Prefix-AFI        |
 * o   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * r   |                          EID-Prefix                           |
 * d   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  /|    Priority   |    Weight     |  M Priority   |   M Weight    |
 * | L +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | o |        Unused Flags     |L|p|R|           Loc-AFI             |
 * | c +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  \|                             Locator                           |
 * +-> +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public interface LispMapNotify extends LispMessage {

    /**
     * Obtains nonce value.
     *
     * @return nonce value
     */
    long getNonce();

    /**
     * Obtains record count value.
     *
     * @return record count value
     */
    int getRecordCount();

    /**
     * Obtains key identifier.
     *
     * @return key identifier
     */
    short getKeyId();

    /**
     * Obtains authentication data length.
     *
     * @return authentication data length
     */
    short getAuthDataLength();

    /**
     * Obtains authentication data.
     *
     * @return authentication data
     */
    byte[] getAuthenticationData();

    /**
     * Obtains a collection of records.
     *
     * @return a collection of records
     */
    List<LispMapRecord> getMapRecords();

    /**
     * A builder of LISP map notify message.
     */
    interface NotifyBuilder extends Builder {

        /**
         * Sets nonce value.
         *
         * @param nonce nonce value
         * @return NotifyBuilder object
         */
        NotifyBuilder withNonce(long nonce);

        /**
         * Sets key identitifer.
         *
         * @param keyId key identifier
         * @return NotifyBuilder object
         */
        NotifyBuilder withKeyId(short keyId);

        /**
         * Sets authentication data length.
         *
         * @param authDataLength authentication data length
         * @return NotifyBuilder object
         */
        NotifyBuilder withAuthDataLength(short authDataLength);

        /**
         * Sets authentication data.
         *
         * @param authenticationData authentication data
         * @return NotifyBuilder object
         */
        NotifyBuilder withAuthenticationData(byte[] authenticationData);

        /**
         * Sets a collection of map records.
         *
         * @param mapRecords a collection of map records
         * @return RegisterBuilder object
         */
        NotifyBuilder withMapRecords(List<LispMapRecord> mapRecords);

        /**
         * Builds LISP map notify message.
         *
         * @return LISP map notify message
         */
        LispMapNotify build();
    }
}
