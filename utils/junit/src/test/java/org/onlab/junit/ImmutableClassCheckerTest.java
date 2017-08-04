/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onlab.junit;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Set of unit tests to check the implementation of the immutable class
 * checker.
 */
public class ImmutableClassCheckerTest {
    /**
     * Test class for non final class check.
     */
    // CHECKSTYLE IGNORE FinalClass FOR NEXT 1 LINES
    static class NonFinal {
        private NonFinal() { }
    }

    /**
     * Check that a non final class correctly produces an error.
     * @throws Exception if any of the reflection lookups fail.
     */
    @Test
    public void testNonFinalClass() throws Exception {
        boolean gotException = false;
        try {
            assertThatClassIsImmutable(NonFinal.class);
        } catch (AssertionError assertion) {
            assertThat(assertion.getMessage(),
                    containsString("is not final"));
            gotException = true;
        }
        assertThat(gotException, is(true));
    }

    /**
     * Test class for non private member class check.
     */
    static final class FinalProtectedMember {
        protected final int x = 0;
    }

    /**
     * Check that a final class with a non-private member is properly detected.
     *
     * @throws Exception if any of the reflection lookups fail.
     */
    @Test
    public void testFinalProtectedMember() throws Exception {
        boolean gotException = false;
        try {
            assertThatClassIsImmutable(FinalProtectedMember.class);
        } catch (AssertionError assertion) {
            assertThat(assertion.getMessage(),
                       containsString("a field named 'x' that is not private"));
            gotException = true;
        }
        assertThat(gotException, is(true));
    }

    /**
     * Test class for non private member class check.
     */
    static final class NotFinalPrivateMember {
        private int x = 0;
    }

    /**
     * Check that a final class with a non-final private
     * member is properly detected.
     *
     * @throws Exception if any of the reflection lookups fail.
     */
    @Test
    public void testNotFinalPrivateMember() throws Exception {
        boolean gotException = false;
        try {
            assertThatClassIsImmutable(NotFinalPrivateMember.class);
        } catch (AssertionError assertion) {
            assertThat(assertion.getMessage(),
                    containsString("a field named 'x' that is not final"));
            gotException = true;
        }
        assertThat(gotException, is(true));
    }

    /**
     * Test class for non private member class check.
     */
    static final class ClassWithSetter {
        private final int x = 0;
        public void setX(int newX) {
        }
    }

    /**
     * Check that a final class with a final private
     * member that is modifyable by a setter is properly detected.
     *
     * @throws Exception if any of the reflection lookups fail.
     */
    @Test
    public void testClassWithSetter() throws Exception {
        boolean gotException = false;
        try {
            assertThatClassIsImmutable(ClassWithSetter.class);
        } catch (AssertionError assertion) {
            assertThat(assertion.getMessage(),
                    containsString("a class with a setter named 'setX'"));
            gotException = true;
        }
        assertThat(gotException, is(true));
    }

}

