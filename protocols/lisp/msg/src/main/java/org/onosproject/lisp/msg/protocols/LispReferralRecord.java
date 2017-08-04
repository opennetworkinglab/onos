/*
 * Copyright 2017-present Open Networking Foundation
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
 * LISP referral record section which is part of LISP map referral message.
 */
public interface LispReferralRecord extends LispRecord {

    /**
     * Obtains referral count value.
     *
     * @return referral count value
     */
    int getReferralCount();

    /**
     * Obtains signature count value.
     *
     * @return signature count value
     */
    int getSignatureCount();

    /**
     * Obtains incomplete flag.
     *
     * @return incomplete flag
     */
    boolean isIncomplete();

    /**
     * Obtains a collection of referrals.
     *
     * @return a collection of referrals
     */
    List<LispReferral> getReferrals();

    /**
     * Obtains a collection of signatures.
     *
     * @return a collection of signatures
     */
    List<LispSignature> getSignatures();

    /**
     * A builder of LISP referral record.
     */
    interface ReferralRecordBuilder extends RecordBuilder<ReferralRecordBuilder> {

        /**
         * Sets a collection of referrals.
         *
         * @param referrals a collection of referrals
         * @return ReferralRecordBuilder object
         */
        ReferralRecordBuilder withReferrals(List<LispReferral> referrals);

        /**
         * Sets a collection of signatures.
         *
         * @param signatures a collection of signatures
         * @return ReferralRecordBuilder object
         */
        ReferralRecordBuilder withSignatures(List<LispSignature> signatures);

        /**
         * Sets incomplete flag.
         *
         * @param incomplete incomplete flag
         * @return ReferralRecordBuilder object
         */
        ReferralRecordBuilder withIsIncomplete(boolean incomplete);

        /**
         * Builds LISP referral record object.
         *
         * @return LISP referral record object
         */
        LispReferralRecord build();
    }
}
