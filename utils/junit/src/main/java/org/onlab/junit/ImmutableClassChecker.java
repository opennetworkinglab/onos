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

import org.hamcrest.Description;
import org.hamcrest.StringDescription;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Hamcrest style class for verifying that a class follows the
 * accepted rules for immutable classes.
 *
 * The rules that are enforced for immutable classes:
 *    - the class must be declared final
 *    - all data members of the class must be declared private and final
 *    - the class must not define any setter methods
 */

public class ImmutableClassChecker {

    private String failureReason = "";

    /**
     * Method to determine if a given class is a properly specified
     * immutable class.
     *
     * @param clazz the class to check
     * @return true if the given class is a properly specified immutable class.
     */
    private boolean isImmutableClass(Class<?> clazz, boolean allowNonFinalClass) {
        // class must be declared final
        if (!allowNonFinalClass && !Modifier.isFinal(clazz.getModifiers())) {
            failureReason = "a class that is not final";
            return false;
        }

        // class must have only final and private data members
        for (final Field field : clazz.getDeclaredFields()) {
            if (field.getName().startsWith("_") ||
                field.getName().startsWith("$")) {
                //  eclipse generated code may insert switch table - ignore
                //  cobertura sticks these fields into classes - ignore them
                continue;
            }
            if (!Modifier.isFinal(field.getModifiers())) {
                failureReason = "a field named '" + field.getName() +
                                "' that is not final";
                return false;
            }
            if (!Modifier.isPrivate(field.getModifiers())) {
                //
                // NOTE: We relax the recommended rules for defining immutable
                // objects and allow "static final" fields that are not
                // private. The "final" check was already done above so we
                // don't repeat it here.
                //
                if (!Modifier.isStatic(field.getModifiers())) {
                    failureReason = "a field named '" + field.getName() +
                                "' that is not private and is not static";
                    return false;
                }
            }
        }

        //  class must not define any setters
        for (final Method method : clazz.getMethods()) {
            if (method.getDeclaringClass().equals(clazz)) {
                if (method.getName().startsWith("set")) {
                    failureReason = "a class with a setter named '" + method.getName() + "'";
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Describe why an error was reported.  Uses Hamcrest style Description
     * interfaces.
     *
     * @param description the Description object to use for reporting the
     *                    mismatch
     */
    public void describeMismatch(Description description) {
        description.appendText(failureReason);
    }

    /**
     * Describe the source object that caused an error, using a Hamcrest
     * Matcher style interface.  In this case, it always returns
     * that we are looking for a properly defined utility class.
     *
     * @param description the Description object to use to report the "to"
     *                    object
     */
    public void describeTo(Description description) {
        description.appendText("a properly defined immutable class");
    }

    /**
     * Assert that the given class adheres to the immutable class rules.
     *
     * @param clazz the class to check
     *
     * @throws java.lang.AssertionError if the class is not an
     *         immutable class
     */
    public static void assertThatClassIsImmutable(Class<?> clazz) {
        final ImmutableClassChecker checker = new ImmutableClassChecker();
        if (!checker.isImmutableClass(clazz, false)) {
            final Description toDescription = new StringDescription();
            final Description mismatchDescription = new StringDescription();

            checker.describeTo(toDescription);
            checker.describeMismatch(mismatchDescription);
            final String reason =
                    "\n" +
                    "Expected: is \"" + toDescription.toString() + "\"\n" +
                    "    but : was \"" + mismatchDescription.toString() + "\"";

            throw new AssertionError(reason);
        }
    }

    /**
     * Assert that the given class adheres to the immutable class rules, but
     * is not declared final.  Classes that need to be inherited from cannot be
     * declared final.
     *
     * @param clazz the class to check
     *
     * @throws java.lang.AssertionError if the class is not an
     *         immutable class
     */
    public static void assertThatClassIsImmutableBaseClass(Class<?> clazz) {
        final ImmutableClassChecker checker = new ImmutableClassChecker();
        if (!checker.isImmutableClass(clazz, true)) {
            final Description toDescription = new StringDescription();
            final Description mismatchDescription = new StringDescription();

            checker.describeTo(toDescription);
            checker.describeMismatch(mismatchDescription);
            final String reason =
                    "\n" +
                            "Expected: is \"" + toDescription.toString() + "\"\n" +
                            "    but : was \"" + mismatchDescription.toString() + "\"";

            throw new AssertionError(reason);
        }
    }
}
