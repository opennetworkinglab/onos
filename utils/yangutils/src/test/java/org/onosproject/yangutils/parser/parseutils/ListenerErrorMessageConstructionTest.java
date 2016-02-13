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
import org.onosproject.yangutils.parser.ParsableDataType;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test case for testing listener error message construction util.
 */
public class ListenerErrorMessageConstructionTest {

    /**
     * Checks for error message construction with parsable data type name.
     */
    @Test
    public void checkErrorMsgConstructionWithName() {

        // Create an test error message
        String testErrorMessage = ListenerErrorMessageConstruction
                .constructListenerErrorMessage(ListenerErrorType.INVALID_HOLDER, ParsableDataType.CONTACT_DATA,
                                               "Test Instance", ListenerErrorLocation.ENTRY);

        // Check message.
        assertThat(testErrorMessage, is("Internal parser error detected: Invalid holder for contact "
                + "\"Test Instance\" before processing."));
    }

    /**
     * Checks for error message construction without parsable data type name.
     */
    @Test
    public void checkErrorMsgConstructionWithoutName() {

        // Create an test error message
        String testErrorMessage = ListenerErrorMessageConstruction
                .constructListenerErrorMessage(ListenerErrorType.INVALID_HOLDER, ParsableDataType.CONTACT_DATA,
                                               "Test Instance", ListenerErrorLocation.ENTRY);

        // Check message.
        assertThat(testErrorMessage,
                   is("Internal parser error detected: Invalid holder for contact \"Test Instance\""
                           + " before processing."));
    }

    /**
     * Checks for extended error message construction with parsable data type name.
     */
    @Test
    public void checkExtendedErrorMsgConstructionWithName() {

        // Create an test error message
        String testErrorMessage = ListenerErrorMessageConstruction
                .constructExtendedListenerErrorMessage(ListenerErrorType.INVALID_HOLDER,
                                                       ParsableDataType.CONTACT_DATA, "Test Instance",
                                                       ListenerErrorLocation.ENTRY, "Extended Information");

        // Check message.
        assertThat(testErrorMessage,
                   is("Internal parser error detected: Invalid holder for contact \"Test Instance\""
                           + " before processing.\n" + "Error Information: Extended Information"));
    }

    /**
     * Checks for extended error message construction without parsable data type name.
     */
    @Test
    public void checkExtendedErrorMsgConstructionWithoutName() {

        // Create an test error message
        String testErrorMessage = ListenerErrorMessageConstruction
                .constructExtendedListenerErrorMessage(ListenerErrorType.INVALID_HOLDER,
                                                       ParsableDataType.CONTACT_DATA, "",
                                                       ListenerErrorLocation.ENTRY, "Extended Information");

        // Check message.
        assertThat(testErrorMessage, is("Internal parser error detected: Invalid holder for contact"
                + " before processing.\n" + "Error Information: Extended Information"));
    }
}