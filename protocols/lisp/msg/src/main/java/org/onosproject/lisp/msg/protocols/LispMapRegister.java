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

import java.util.List;

/**
 * LISP map register message interface.
 * <p>
 * LISP map register message format is defined in RFC6830.
 * https://tools.ietf.org/html/rfc6830#page-37
 *
 * <pre>
 * {@literal
 *      0                   1                   2                   3
 *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |Type=3 |P|            Reserved               |M| Record Count  |
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
 * c   | Rsvd  |  Map-Version Number   |        EID-Prefix-AFI         |
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
public interface LispMapRegister extends LispMessage {

    /**
     * Obtains proxy map reply flag.
     *
     * @return proxy map reply flag
     */
    boolean isProxyMapReply();

    /**
     * Obtains want map notify flag.
     *
     * @return want map notify flag
     */
    boolean isWantMapNotify();

    /**
     * Obtains record count value.
     *
     * @return record count value
     */
    int getRecordCount();

    /**
     * Obtains nonce value.
     *
     * @return nonce value
     */
    long getNonce();

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
    byte[] getAuthData();

    /**
     * Obtains a collection of records.
     *
     * @return a collection of records
     */
    List<LispMapRecord> getMapRecords();

    /**
     * A builder of LISP map register message.
     */
    interface RegisterBuilder extends Builder {

        /**
         * Sets isProxyMapReply flag.
         *
         * @param isProxyMapReply isProxyMapReply
         * @return RegisterBuilder object
         */
        RegisterBuilder withIsProxyMapReply(boolean isProxyMapReply);

        /**
         * Sets isWantMapNotify flag.
         *
         * @param isWantMapNotify isWantMapNotify
         * @return RegisterBuilder object
         */
        RegisterBuilder withIsWantMapNotify(boolean isWantMapNotify);

        /**
         * Sets nonce value.
         *
         * @param nonce nonce value
         * @return RegisterBuilder object
         */
        RegisterBuilder withNonce(long nonce);

        /**
         * Sets authentication key.
         *
         * @param key authentication key
         * @return RegisterBuilder object
         */
        RegisterBuilder withAuthKey(String key);

        /**
         * Sets authentication data length.
         *
         * @param authDataLength authentication data length
         * @return RegisterBuilder object
         */
        RegisterBuilder withAuthDataLength(short authDataLength);

        /**
         * Sets key identifier.
         *
         * @param keyId key identifier
         * @return RegisterBuilder object
         */
        RegisterBuilder withKeyId(short keyId);

        /**
         * Sets authentication data.
         *
         * @param authData authentication data
         * @return RegisterBuilder object
         */
        RegisterBuilder withAuthData(byte[] authData);

        /**
         * Sets a collection of map records.
         *
         * @param mapRecords a collection of map records
         * @return RegisterBuilder object
         */
        RegisterBuilder withMapRecords(List<LispMapRecord> mapRecords);

        /**
         * Builds LISP map register message.
         *
         * @return LISP map register message
         */
        LispMapRegister build();
    }
}
