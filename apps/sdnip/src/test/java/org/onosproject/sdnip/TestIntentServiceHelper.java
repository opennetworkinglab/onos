/*
 * Copyright 2014-2015 Open Networking Laboratory
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

            return IntentUtils.equals(intent, providedIntent);
        }
    }

}
