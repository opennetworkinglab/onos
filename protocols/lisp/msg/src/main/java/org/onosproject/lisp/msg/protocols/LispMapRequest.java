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

import org.onosproject.lisp.msg.types.LispAfiAddress;

import java.util.List;

/**
 * LISP map request message interface.
 * <p>
 * LISP map request message format is defined in RFC6830.
 * https://tools.ietf.org/html/rfc6830#page-27
 *
 * <pre>
 * {@literal
 * <p>
 *      0                   1                   2                   3
 *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |Type=1 |A|M|P|S|p|s|    Reserved     |   IRC   | Record Count  |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                         Nonce . . .                           |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                         . . . Nonce                           |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |         Source-EID-AFI        |   Source EID Address  ...     |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |         ITR-RLOC-AFI 1        |    ITR-RLOC Address 1  ...    |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                              ...                              |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |         ITR-RLOC-AFI n        |    ITR-RLOC Address n  ...    |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * /   |   Reserved    | EID mask-len  |        EID-Prefix-AFI         |
 * Rec +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * \   |                       EID-Prefix  ...                         |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                   Map-Reply Record  ...                       |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public interface LispMapRequest extends LispMessage {

    /**
     * Obtains authoritative flag.
     *
     * @return authoritative flag
     */
    boolean isAuthoritative();

    /**
     * Obtains map data present flag.
     *
     * @return map data present flag
     */
    boolean isMapDataPresent();

    /**
     * Obtains probe flag.
     *
     * @return probe flag
     */
    boolean isProbe();

    /**
     * Obtains SMR flag.
     *
     * @return SMR flag
     */
    boolean isSmr();

    /**
     * Obtains PITR flag.
     *
     * @return PITR flag
     */
    boolean isPitr();

    /**
     * Obtains SMR Invoked flag.
     *
     * @return SMR invoked flag
     */
    boolean isSmrInvoked();

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
     * Obtains source EID.
     *
     * @return source EID
     */
    LispAfiAddress getSourceEid();

    /**
     * Obtains a collection of ITR RLOCs.
     *
     * @return a collection of ITR RLOCs
     */
    List<LispAfiAddress> getItrRlocs();

    /**
     * Obtains a collection of EID records.
     *
     * @return a collection of EID records
     */
    List<LispEidRecord> getEids();

    /**
     * Obtains the size of map-reply record.
     *
     * @return the size of map-reply record
     */
    int getReplyRecord();

    /**
     * A builder of LISP map request message.
     */
    interface RequestBuilder extends Builder {

        /**
         * Sets authoritative flag.
         *
         * @param authoritative authoritative flag
         * @return RequestBuilder object
         */
        RequestBuilder withIsAuthoritative(boolean authoritative);

        /**
         * Sets probe flag.
         *
         * @param probe probe flag
         * @return RequestBuilder object
         */
        RequestBuilder withIsProbe(boolean probe);

        /**
         * Sets map data resent flag.
         *
         * @param mapDataPresent map data present flag
         * @return RequestBuilder object
         */
        RequestBuilder withIsMapDataPresent(boolean mapDataPresent);

        /**
         * Sets smr flag.
         *
         * @param smr smr flag
         * @return RequestBuilder object
         */
        RequestBuilder withIsSmr(boolean smr);

        /**
         * Sets pitr flag.
         *
         * @param pitr pitr flag
         * @return RequestBuilder object
         */
        RequestBuilder withIsPitr(boolean pitr);

        /**
         * Sets smrInvoked flag.
         *
         * @param smrInvoked smrInvoked flag
         * @return RequestBuilder object
         */
        RequestBuilder withIsSmrInvoked(boolean smrInvoked);

        /**
         * Sets nonce value.
         *
         * @param nonce nonce value
         * @return RequestBuilder object
         */
        RequestBuilder withNonce(long nonce);

        /**
         * Sets source EID address.
         *
         * @param sourceEid source EID
         * @return RequestBuilder object
         */
        RequestBuilder withSourceEid(LispAfiAddress sourceEid);

        /**
         * Sets a collection of ITR RLOCs.
         *
         * @param itrRlocs a collection of ITR RLOCs
         * @return RequestBuilder object
         */
        RequestBuilder withItrRlocs(List<LispAfiAddress> itrRlocs);

        /**
         * Sets a collection of EID records.
         *
         * @param records a collection of EID records
         * @return RequestBuilder object
         */
        RequestBuilder withEidRecords(List<LispEidRecord> records);

        /**
         * Sets the size of map-reply record.
         *
         * @param replyRecord the size of map-reply record
         * @return RequestBuilder object
         */
        RequestBuilder withReplyRecord(int replyRecord);

        /**
         * Builds LISP map request message.
         *
         * @return LISP map request message
         */
        LispMapRequest build();
    }
}
