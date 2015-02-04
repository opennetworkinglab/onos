/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.sdnip;

import org.easymock.IArgumentMatcher;
import org.onosproject.net.intent.Intent;
import org.onosproject.sdnip.IntentSynchronizer.IntentKey;

import static org.easymock.EasyMock.reportMatcher;

/**
 * Helper class for testing operations submitted to the IntentService.
 */
public final class TestIntentServiceHelper {
    /**
     * Default constructor to prevent instantiation.
     */
    private TestIntentServiceHelper() {
    }

    /**
     * Matcher method to set the expected intent to match against
     * (ignoring the intent ID for the intent).
     *
     * @param intent the expected Intent
     * @return the submitted Intent
     */
    static Intent eqExceptId(Intent intent) {
        reportMatcher(new IdAgnosticIntentMatcher(intent));
        return intent;
    }

    /**
     * Matcher method to set the expected intent operations to match against
     * (ignoring the intent ID for each intent).
     *
     * param intentOperations the expected Intent Operations
     * @return the submitted Intent Operations
     */
    /*
    static IntentOperations eqExceptId(IntentOperations intentOperations) {
        reportMatcher(new IdAgnosticIntentOperationsMatcher(intentOperations));
        return intentOperations;
    }
    */

    /*
     * EasyMock matcher that matches {@link Intent} but
     * ignores the {@link IntentId} when matching.
     * <p/>
     * The normal intent equals method tests that the intent IDs are equal,
     * however in these tests we can't know what the intent IDs will be in
     * advance, so we can't set up expected intents with the correct IDs. Thus,
     * the solution is to use an EasyMock matcher that verifies that all the
     * value properties of the provided intent match the expected values, but
     * ignores the intent ID when testing equality.
     */
    private static final class IdAgnosticIntentMatcher implements
                IArgumentMatcher {

        private final Intent intent;
        private String providedString;

        /**
         * Constructor taking the expected intent to match against.
         *
         * @param intent the expected intent
         */
        public IdAgnosticIntentMatcher(Intent intent) {
            this.intent = intent;
        }

        @Override
        public void appendTo(StringBuffer strBuffer) {
            strBuffer.append("IntentMatcher unable to match: "
                    + providedString);
        }

        @Override
        public boolean matches(Object object) {
            if (!(object instanceof Intent)) {
                return false;
            }

            Intent providedIntent = (Intent) object;
            providedString = providedIntent.toString();

            IntentKey thisIntentKey = new IntentKey(intent);
            IntentKey providedIntentKey = new IntentKey(providedIntent);
            return thisIntentKey.equals(providedIntentKey);
        }
    }

    /*
     * EasyMock matcher that matches {@link IntenOperations} but
     * ignores the {@link IntentId} when matching.
     * <p/>
     * The normal intent equals method tests that the intent IDs are equal,
     * however in these tests we can't know what the intent IDs will be in
     * advance, so we can't set up expected intents with the correct IDs. Thus,
     * the solution is to use an EasyMock matcher that verifies that all the
     * value properties of the provided intent match the expected values, but
     * ignores the intent ID when testing equality.
     */
    /*
    private static final class IdAgnosticIntentOperationsMatcher implements
                IArgumentMatcher {

        //private final IntentOperations intentOperations;
        private String providedString;

        @Override
        public void appendTo(StringBuffer strBuffer) {
            strBuffer.append("IntentOperationsMatcher unable to match: "
                    + providedString);
        }

        @Override
        public boolean matches(Object object) {
            if (!(object instanceof IntentOperations)) {
                return false;
            }

            IntentOperations providedIntentOperations =
                (IntentOperations) object;
            providedString = providedIntentOperations.toString();

            List<IntentKey> thisSubmitIntents = new LinkedList<>();
            List<IntentId> thisWithdrawIntentIds = new LinkedList<>();
            List<IntentKey> thisReplaceIntents = new LinkedList<>();
            List<IntentKey> thisUpdateIntents = new LinkedList<>();
            List<IntentKey> providedSubmitIntents = new LinkedList<>();
            List<IntentId> providedWithdrawIntentIds = new LinkedList<>();
            List<IntentKey> providedReplaceIntents = new LinkedList<>();
            List<IntentKey> providedUpdateIntents = new LinkedList<>();

            extractIntents(intentOperations, thisSubmitIntents,
                           thisWithdrawIntentIds, thisReplaceIntents,
                           thisUpdateIntents);
            extractIntents(providedIntentOperations, providedSubmitIntents,
                           providedWithdrawIntentIds, providedReplaceIntents,
                           providedUpdateIntents);

            return CollectionUtils.isEqualCollection(thisSubmitIntents,
                                                     providedSubmitIntents) &&
                CollectionUtils.isEqualCollection(thisWithdrawIntentIds,
                                                  providedWithdrawIntentIds) &&
                CollectionUtils.isEqualCollection(thisUpdateIntents,
                                                  providedUpdateIntents) &&
                CollectionUtils.isEqualCollection(thisReplaceIntents,
                                                  providedReplaceIntents);
        }


        /**
         * Extracts the intents per operation type. Each intent is encapsulated
         * in IntentKey so it can be compared by excluding the Intent ID.
         *
         * @param intentOperations the container with the intent operations
         * to extract the intents from
         * @param submitIntents the SUBMIT intents
         * @param withdrawIntentIds the WITHDRAW intents IDs
         * @param replaceIntents the REPLACE intents
         * @param updateIntents the UPDATE intents
         */
        /*
        private void extractIntents(IntentOperations intentOperations,
                                    List<IntentKey> submitIntents,
                                    List<IntentId> withdrawIntentIds,
                                    List<IntentKey> replaceIntents,
                                    List<IntentKey> updateIntents) {
            for (IntentOperation oper : intentOperations.operations()) {
                IntentId intentId;
                IntentKey intentKey;
                switch (oper.type()) {
                case SUBMIT:
                    intentKey = new IntentKey(oper.intent());
                    submitIntents.add(intentKey);
                    break;
                case WITHDRAW:
                    intentId = oper.intentId();
                    withdrawIntentIds.add(intentId);
                    break;
                case REPLACE:
                    intentKey = new IntentKey(oper.intent());
                    replaceIntents.add(intentKey);
                    break;
                case UPDATE:
                    intentKey = new IntentKey(oper.intent());
                    updateIntents.add(intentKey);
                    break;
                default:
                    break;
                }
            }
        }
    }
    */
}
