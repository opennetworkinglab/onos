/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.intent.impl;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentBatchDelegate;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentTestsMocks.MockIntent;
import org.onosproject.net.intent.IntentTestsMocks.MockTimestamp;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * Unit tests for the intent accumulator.
 */
public class IntentAccumulatorTest extends AbstractIntentTest {

    Intent intent1;
    Intent intent2;
    Intent intent3;

    private static IntentDataMatcher containsIntent(Intent intent) {
        return new IntentDataMatcher(intent);
    }

    /**
     * Creates mock intents used by the test.
     */
    @Before
    public void setUp() {
        super.setUp();

        intent1 = new MockIntent(1L);
        intent2 = new MockIntent(2L);
        intent3 = new MockIntent(3L);
    }

    /**
     * Hamcrest matcher to check that a collection of intent data objects
     * contains an entry for a given intent.
     */
    private static final class IntentDataMatcher
            extends TypeSafeDiagnosingMatcher<Collection<IntentData>> {

        final Intent intent;

        public IntentDataMatcher(Intent intent) {
            this.intent = intent;
        }

        /**
         * Check that the given collection of intent data contains a specific
         * intent.
         *
         * @param operations  collection of intent data
         * @param description description
         * @return true if the collection contains the intent, false otherwise.
         */
        public boolean matchesSafely(Collection<IntentData> operations,
                                     Description description) {
            for (IntentData operation : operations) {
                if (operation.key().equals(intent.key())) {
                    if (operation.state() != IntentState.INSTALLED) {
                        description.appendText("state was " + operation.state());
                        return false;
                    }
                    return true;
                }
            }
            description.appendText("key was not found " + intent.key());
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("INSTALLED state intent with key " + intent.key());
        }
    }

    /**
     * Mock batch delegate class.  Gets calls from the accumulator and checks
     * that the operations have been properly compressed.
     */
    private class MockIntentBatchDelegate
                   implements IntentBatchDelegate {
        public void execute(Collection<IntentData> operations) {
            assertThat(operations, hasSize(3));
            assertThat(operations, containsIntent(intent1));
            assertThat(operations, containsIntent(intent2));
            assertThat(operations, containsIntent(intent3));
        }
    }

    /**
     * Tests that the accumulator properly compresses operations on the same
     * intents.
     */
    @Test
    public void checkAccumulator() {

        MockIntentBatchDelegate delegate = new MockIntentBatchDelegate();
        IntentAccumulator accumulator = new IntentAccumulator(delegate);

        List<IntentData> intentDataItems = ImmutableList.of(
                new IntentData(intent1, IntentState.INSTALLING,
                        new MockTimestamp(1)),
                new IntentData(intent2, IntentState.INSTALLING,
                        new MockTimestamp(1)),
                new IntentData(intent3, IntentState.INSTALLED,
                        new MockTimestamp(1)),
                new IntentData(intent2, IntentState.INSTALLED,
                        new MockTimestamp(1)),
                new IntentData(intent2, IntentState.INSTALLED,
                        new MockTimestamp(1)),
                new IntentData(intent1, IntentState.INSTALLED,
                        new MockTimestamp(1)));


        accumulator.processItems(intentDataItems);
    }


}
