/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.yangutils.parser.parseutils;

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangRevision;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerError;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test case for testing listener validation util.
 */
public class ListenerValidationTest {

    /**
     * This test case checks in case error pre-exists, listener validate
     * function returns true.
     */
    @Test
    public void listenerValidationErrorExists() {

        // Create an test error.
        ListenerError testError = new ListenerError();
        testError.setErrorFlag(true);
        testError.setErrorMsg("Test Error");

        // Create test walker and assign test error to it.
        TreeWalkListener testWalker = new TreeWalkListener();
        testWalker.setErrorInformation(testError);

        // Create a temporary node of parsable.
        YangRevision tmpNode = new YangRevision();
        testWalker.getParsedDataStack().push(tmpNode);

        boolean errorFlag = ListenerValidation.preValidation(testWalker, "ErrorTest");

        /**
         * Check for the values set in syntax error function. If not set properly
         * report an assert.
         */
        assertThat(errorFlag, is(true));
    }

    /**
     * This test case checks in case parsable stack is empty, listener validate
     * function returns true.
     */
    @Test
    public void listenerValidationEmptyStack() {

        // Create test walker and assign test error to it.
        TreeWalkListener testWalker = new TreeWalkListener();

        boolean errorFlag = ListenerValidation.preValidation(testWalker, "ErrorTest");

        /**
         * Check for the values set in syntax error function. If not set properly
         * report an assert.
         */
        assertThat(errorFlag, is(true));
    }

    /**
     * This test case checks in case of error doesn't pre-exists and stack is,
     * non empty, listener validate function returns false.
     */
    @Test
    public void listenerValidationNoErrorNotExists() {

        // Create test walker and assign test error to it.
        TreeWalkListener testWalker = new TreeWalkListener();

        // Create a temporary node of parsable.
        YangRevision tmpNode = new YangRevision();
        testWalker.getParsedDataStack().push(tmpNode);

        boolean errorFlag = ListenerValidation.preValidation(testWalker, "ErrorTest");

        /**
         * Check for the values set in syntax error function. If not set properly
         * report an assert.
         */
        assertThat(errorFlag, is(false));
    }
}