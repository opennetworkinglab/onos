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

package org.onosproject.ui;

/**
 * Abstract base class for UI tests.
 */
public abstract class AbstractUiTest {

    /**
     * System agnostic end-of-line character.
     */
    protected static final String EOL = String.format("%n");

    /**
     * Tolerance for Double equality assertions.
     */
    protected static final double TOLERANCE = Double.MIN_VALUE * 2;

    /**
     * Prints the given string to stdout.
     *
     * @param s string to print
     */
    protected static void print(String s) {
        System.out.println(s);
    }

    /**
     * Prints the toString() of the given object to stdout.
     *
     * @param o object to print
     */
    protected static void print(Object o) {
        if (o == null) {
            print("<null>");
        } else {
            print(o.toString());
        }
    }

    /**
     * Prints the formatted string to stdout.
     *
     * @param fmt    format string
     * @param params parameters
     * @see String#format(String, Object...)
     */
    protected static void print(String fmt, Object... params) {
        print(String.format(fmt, params));
    }

    /**
     * Prints a title, to delimit individual unit test output.
     *
     * @param s a title for the test
     */
    protected static void title(String s) {
        print(EOL + "=== %s ===", s);
    }
}
