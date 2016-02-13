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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangRevision;
import org.onosproject.yangutils.parser.ParsableDataType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation;

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

        String expectedError = ListenerErrorMessageConstruction
                .constructListenerErrorMessage(ListenerErrorType.MISSING_HOLDER, ParsableDataType.YANGBASE_DATA, "",
                        ListenerErrorLocation.EXIT);

        // Get the exception occurred during parsing.
        thrown.expect(ParserException.class);
        thrown.expectMessage(expectedError);

        // Create test walker and assign test error to it.
        TreeWalkListener testWalker = new TreeWalkListener();

        ListenerValidation.checkStackIsNotEmpty(testWalker, ListenerErrorType.MISSING_HOLDER,
                ParsableDataType.YANGBASE_DATA, "", ListenerErrorLocation.EXIT);
    }

    /**
     * Checks if there is no exception in case parsable stack is not empty while validating
     * for not empty scenario.
     */
    @Test
    public void validateStackIsNotEmptyForNonEmptyStack() {

        // Create test walker and assign test error to it.
        TreeWalkListener testWalker = new TreeWalkListener();

        // Create a temporary node of parsable.
        YangRevision tmpNode = new YangRevision();
        testWalker.getParsedDataStack().push(tmpNode);

        ListenerValidation.checkStackIsNotEmpty(testWalker, ListenerErrorType.MISSING_HOLDER,
                                                ParsableDataType.YANGBASE_DATA, "", ListenerErrorLocation.EXIT);
    }

    /**
     * Checks for exception in case parsable stack is not empty while validating
     * for empty scenario.
     */
    @Test
    public void validateStackIsEmptyForNonEmptyStack() {

        String expectedError = ListenerErrorMessageConstruction
                .constructListenerErrorMessage(ListenerErrorType.MISSING_HOLDER, ParsableDataType.YANGBASE_DATA, "",
                        ListenerErrorLocation.EXIT);

        // Get the exception occurred during parsing.
        thrown.expect(ParserException.class);
        thrown.expectMessage(expectedError);

        // Create test walker and assign test error to it.
        TreeWalkListener testWalker = new TreeWalkListener();

        // Create a temporary node of parsable.
        YangRevision tmpNode = new YangRevision();
        testWalker.getParsedDataStack().push(tmpNode);

        ListenerValidation.checkStackIsEmpty(testWalker, ListenerErrorType.MISSING_HOLDER,
                                             ParsableDataType.YANGBASE_DATA, "", ListenerErrorLocation.EXIT);
    }

    /**
     * Checks if there is no exception in case parsable stack is empty while validating
     * for empty scenario.
     */
    @Test
    public void validateStackIsEmptyForEmptyStack() {

        // Create test walker and assign test error to it.
        TreeWalkListener testWalker = new TreeWalkListener();

        ListenerValidation.checkStackIsEmpty(testWalker, ListenerErrorType.MISSING_HOLDER,
                                             ParsableDataType.YANGBASE_DATA, "", ListenerErrorLocation.EXIT);
    }
}