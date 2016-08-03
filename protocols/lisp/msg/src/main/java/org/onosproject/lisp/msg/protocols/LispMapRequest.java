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

import org.onosproject.lisp.msg.types.LispAfiAddress;

import java.util.List;

/**
 * LISP map request message interface.
 *
 * LISP map request message format is defined in RFC6830.
 * https://tools.ietf.org/html/rfc6830#page-27
 *
 * <pre>
 * {@literal
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |Type=1 |A|M|P|S|p|s|    Reserved     |   IRC   | Record Count  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Nonce . . .                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         . . . Nonce                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         Source-EID-AFI        |   Source EID Address  ...     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         ITR-RLOC-AFI 1        |    ITR-RLOC Address 1  ...    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                              ...                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         ITR-RLOC-AFI n        |    ITR-RLOC Address n  ...    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * / |   Reserved    | EID mask-len  |        EID-Prefix-AFI         |
 * Rec +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * \ |                       EID-Prefix  ...                         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                   Map-Reply Record  ...                       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
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
    byte getRecordCount();

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
    List<EidRecord> getEids();

    /**
     * A builder of LISP map request message.
     */
    interface RequestBuilder extends Builder {

        /**
         * Sets isAuthoritative flag.
         *
         * @param isAuthoritative isAuthoritative flag
         * @return RequestBuilder object
         */
        RequestBuilder withIsAuthoritative(boolean isAuthoritative);

        /**
         * Sets isProbe flag.
         *
         * @param isProbe isProbe flag
         * @return RequestBuilder object
         */
        RequestBuilder withIsProbe(boolean isProbe);

        /**
         * Sets isSmr flag.
         *
         * @param isSmr isSmr flag
         * @return RequestBuilder object
         */
        RequestBuilder withIsSmr(boolean isSmr);

        /**
         * Sets isPitr flag.
         *
         * @param isPitr isPitr flag
         * @return RequestBuilder object
         */
        RequestBuilder withIsPitr(boolean isPitr);

        /**
         * Sets isSmrInvoked flag.
         *
         * @param isSmrInvoked isSmrInvoked flag
         * @return RequestBuilder object
         */
        RequestBuilder withIsSmrInvoked(boolean isSmrInvoked);

        /**
         * Sets record count.
         *
         * @param recordCount record count
         * @return RequestBuilder object
         */
        RequestBuilder withRecordCount(byte recordCount);

        /**
         * Sets nonce value.
         *
         * @param nonce nonce value
         * @return RequestBuilder object
         */
        RequestBuilder withNonce(long nonce);

        /**
         * Adds ITR RLOC into RLOC collection.
         *
         * @param itrRloc ITR RLOC
         * @return RequestBuilder object
         */
        RequestBuilder withItrRloc(LispAfiAddress itrRloc);

        /**
         * Adds EID record into record collection.
         *
         * @param record EID record
         * @return RequestBuilder object
         */
        RequestBuilder addEidRecord(EidRecord record);
    }
}
