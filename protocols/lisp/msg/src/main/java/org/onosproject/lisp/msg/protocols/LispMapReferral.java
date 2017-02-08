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

import java.util.List;

/**
 * LISP map referral message interface.
 * <p>
 * LISP map referral message format is defined in draft-ietf-lisp-ddt-09.
 * https://tools.ietf.org/html/draft-ietf-lisp-ddt-09
 *
 * <pre>
 * {@literal
 *      0                   1                   2                   3
 *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |Type=6 |                Reserved               | Record Count  |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                         Nonce . . .                           |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                         . . . Nonce                           |
 * +-> +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   |                          Record  TTL                          |
 * |   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * R   | Referral Count| EID mask-len  | ACT |A|I|     Reserved        |
 * e   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * c   |SigCnt |   Map Version Number  |            EID-AFI            |
 * o   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * r   |                          EID-prefix ...                       |
 * d   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  /|    Priority   |    Weight     |  M Priority   |   M Weight    |
 * | R +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | e |       Unused Flags      |L|p|R|            Loc-AFI            |
 * | f +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  \|                             Locator ...                       |
 * |   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   ~                          Sig section                          ~
 * +-> +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public interface LispMapReferral extends LispMessage {

    /**
     * Obtains record count value.
     *
     * @return record count value
     */
    int getRecordCount();

    /**
     * Obtains nonce value.
     *
     * @return nonce
     */
    long getNonce();

    /**
     * Obtains a collection of referral records.
     *
     * @return a collection of referral records
     */
    List<LispReferralRecord> getReferralRecords();

    /**
     * A builder of LISP map referral message.
     */
    interface MapReferralBuilder extends Builder {

        /**
         * Sets nonce value.
         *
         * @param nonce nonce value
         * @return MapReferralBuilder object
         */
        MapReferralBuilder withNonce(long nonce);

        /**
         * Sets a collection of referral records.
         *
         * @param records a collection of referral records
         * @return MapReferralBuilder object
         */
        MapReferralBuilder withReferralRecords(List<LispReferralRecord> records);

        /**
         * Builds LISP map referral message.
         *
         * @return LISP map referral message
         */
        LispMapReferral build();
    }
}
