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

package org.onosproject.yangutils.parser.impl.parseutils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangRevision;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.YANGBASE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsEmpty;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/**
 * Test case for testing listener validation util.
 */
public class ListenerValidationTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Checks for exception in case parsable stack is empty while validating for
     * not empty scenario.
     */
    @Test
    public void validateStackIsNotEmptyForEmptyStack() {

        String expectedError = constructListenerErrorMessage(MISSING_HOLDER, YANGBASE_DATA, "", EXIT);

        // Get the exception occurred during parsing.
        thrown.expect(ParserException.class);
        thrown.expectMessage(expectedError);

        // Create test walker and assign test error to it.
        TreeWalkListener testWalker = new TreeWalkListener();

        checkStackIsNotEmpty(testWalker, MISSING_HOLDER, YANGBASE_DATA, "", EXIT);
    }

    /**
     * Checks if there is no exception in case parsable stack is not empty while
     * validating for not empty scenario.
     */
    @Test
    public void validateStackIsNotEmptyForNonEmptyStack() {

        // Create test walker and assign test error to it.
        TreeWalkListener testWalker = new TreeWalkListener();

        // Create a temporary node of parsable.
        YangRevision tmpNode = new YangRevision();
        testWalker.getParsedDataStack().push(tmpNode);

        checkStackIsNotEmpty(testWalker, MISSING_HOLDER, YANGBASE_DATA, "", EXIT);
    }

    /**
     * Checks for exception in case parsable stack is not empty while validating
     * for empty scenario.
     */
    @Test
    public void validateStackIsEmptyForNonEmptyStack() {

        String expectedError = constructListenerErrorMessage(MISSING_HOLDER, YANGBASE_DATA, "", EXIT);

        // Get the exception occurred during parsing.
        thrown.expect(ParserException.class);
        thrown.expectMessage(expectedError);

        // Create test walker and assign test error to it.
        TreeWalkListener testWalker = new TreeWalkListener();

        // Create a temporary node of parsable.
        YangRevision tmpNode = new YangRevision();
        testWalker.getParsedDataStack().push(tmpNode);

        checkStackIsEmpty(testWalker, MISSING_HOLDER, YANGBASE_DATA, "", EXIT);
    }

    /**
     * Checks if there is no exception in case parsable stack is empty while
     * validating for empty scenario.
     */
    @Test
    public void validateStackIsEmptyForEmptyStack() {

        // Create test walker and assign test error to it.
        TreeWalkListener testWalker = new TreeWalkListener();

        checkStackIsEmpty(testWalker, MISSING_HOLDER, YANGBASE_DATA, "", EXIT);
    }
}
