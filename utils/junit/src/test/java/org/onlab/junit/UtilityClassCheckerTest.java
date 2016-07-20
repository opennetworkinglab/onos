/*
 * Copyright 2014-present Open Networking Laboratory
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
import static org.onlab.junit.UtilityClassChecker.assertThatClassIsUtility;

/**
 * Set of unit tests to check the implementation of the utility class
 * checker.
 */
public class UtilityClassCheckerTest {

    // CHECKSTYLE:OFF test data intentionally not final
    /**
     * Test class for non final class check.
     */
    static class NonFinal {
        private NonFinal() { }
    }
    // CHECKSTYLE:ON

    /**
     * Check that a non final class correctly produces an error.
     * @throws Exception if any of the reflection lookups fail.
     */
    @Test
    public void testNonFinalClass() throws Exception {
        boolean gotException = false;
        try {
            assertThatClassIsUtility(NonFinal.class);
        } catch (AssertionError assertion) {
            assertThat(assertion.getMessage(),
                       containsString("is not final"));
            gotException = true;
        }
        assertThat(gotException, is(true));
    }

    /**
     * Test class for final no constructor class check.
     */
    static final class FinalNoConstructor {
    }

    /**
     * Check that a final class with no declared constructor correctly produces
     * an error.  In this case, the compiler generates a default constructor
     * for you, but the constructor is 'protected' and will fail the check.
     *
     * @throws Exception if any of the reflection lookups fail.
     */
    @Test
    public void testFinalNoConstructorClass() throws Exception {
        boolean gotException = false;
        try {
            assertThatClassIsUtility(FinalNoConstructor.class);
        } catch (AssertionError assertion) {
            assertThat(assertion.getMessage(),
                    containsString("class with a default constructor that " +
                                   "is not private"));
            gotException = true;
        }
        assertThat(gotException, is(true));
    }

    /**
     * Test class for class with more than one constructor check.
     */
    static final class TwoConstructors {
        private TwoConstructors() { }
        private TwoConstructors(int x) { }
    }

    /**
     * Check that a non static class correctly produces an error.
     * @throws Exception if any of the reflection lookups fail.
     */
    @Test
    public void testOnlyOneConstructor() throws Exception {
        boolean gotException = false;
        try {
            assertThatClassIsUtility(TwoConstructors.class);
        } catch (AssertionError assertion) {
            assertThat(assertion.getMessage(),
                       containsString("more than one constructor"));
            gotException = true;
        }
        assertThat(gotException, is(true));
    }

    /**
     * Test class with a non private constructor.
     */
    static final class NonPrivateConstructor {
        protected NonPrivateConstructor() { }
    }

    /**
     * Check that a class with a non private constructor correctly
     * produces an error.
     * @throws Exception if any of the reflection lookups fail.
     */
    @Test
    public void testNonPrivateConstructor() throws Exception {

        boolean gotException = false;
        try {
            assertThatClassIsUtility(NonPrivateConstructor.class);
        } catch (AssertionError assertion) {
            assertThat(assertion.getMessage(),
                       containsString("constructor that is not private"));
            gotException = true;
        }
        assertThat(gotException, is(true));
    }

    /**
     * Test class with a non static method.
     */
    static final class NonStaticMethod {
        private NonStaticMethod() { }
        public void aPublicMethod() { }
    }

    /**
     * Check that a class with a non static method correctly produces an error.
     * @throws Exception if any of the reflection lookups fail.
     */
    @Test
    public void testNonStaticMethod() throws Exception {

        boolean gotException = false;
        try {
            assertThatClassIsUtility(NonStaticMethod.class);
        } catch (AssertionError assertion) {
            assertThat(assertion.getMessage(),
                       containsString("one or more non-static methods"));
            gotException = true;
        }
        assertThat(gotException, is(true));
    }
}
