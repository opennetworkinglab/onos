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
 * LISP map reply message interface.
 * <p>
 * LISP map reply message format is defined in RFC6830.
 * https://tools.ietf.org/html/rfc6830#page-31
 *
 * <pre>
 * {@literal
 *      0                   1                   2                   3
 *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |Type=2 |P|E|S|          Reserved               | Record Count  |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                         Nonce . . .                           |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                         . . . Nonce                           |
 * +-> +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   |                          Record TTL                           |
 * |   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * R   | Locator Count | EID mask-len  | ACT |A|      Reserved         |
 * e   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * c   | Rsvd  |  Map-Version Number   |       EID-Prefix-AFI          |
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
public interface LispMapReply extends LispMessage {

    /**
     * Obtains probe flag.
     *
     * @return probe flag
     */
    boolean isProbe();

    /**
     * Obtains ETR flag.
     *
     * @return ETR flag
     */
    boolean isEtr();

    /**
     * Obtains security flag.
     *
     * @return security flag
     */
    boolean isSecurity();

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
     * Obtains map records.
     *
     * @return map records
     */
    List<LispMapRecord> getMapRecords();

    /**
     * A builder of LISP map reply message.
     */
    interface ReplyBuilder extends Builder {

        /**
         * Sets probe flag.
         *
         * @param probe probe flag
         * @return ReplyBuilder object
         */
        ReplyBuilder withIsProbe(boolean probe);

        /**
         * Sets etr flag.
         *
         * @param etr etr flag
         * @return ReplyBuilder object
         */
        ReplyBuilder withIsEtr(boolean etr);

        /**
         * Sets security flag.
         *
         * @param security security flag
         * @return ReplyBuilder object
         */
        ReplyBuilder withIsSecurity(boolean security);

        /**
         * Sets nonce value.
         *
         * @param nonce nonce value
         * @return ReplyBuilder object
         */
        ReplyBuilder withNonce(long nonce);

        /**
         * Sets a collection of map records.
         *
         * @param mapRecords a collection of map records
         * @return ReplyBuilder object
         */
        ReplyBuilder withMapRecords(List<LispMapRecord> mapRecords);

        /**
         * Builds LISP map reply message.
         *
         * @return LISP map reply message
         */
        LispMapReply build();
    }
}
