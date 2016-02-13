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

package org.onosproject.yangutils.parser.impl.parserutils;

import org.onosproject.yangutils.parser.ParsableDataType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

/**
 * It's a utility to carry out listener validation.
 */
public final class ListenerValidation {

    /**
     * Creates a new listener validation.
     */
    private ListenerValidation() {
    }

    /**
     * Checks parsed data stack is not empty.
     *
     * @param listener Listener's object.
     * @param errorType error type needs to be set in error message.
     * @param parsableDataType type of parsable data in which error occurred.
     * @param parsableDataTypeName name of parsable data type in which error occurred.
     * @param errorLocation location where error occurred.
     */
    public static void checkStackIsNotEmpty(TreeWalkListener listener, ListenerErrorType errorType,
                                               ParsableDataType parsableDataType, String parsableDataTypeName,
                                               ListenerErrorLocation errorLocation) {
        if (listener.getParsedDataStack().empty()) {
            /*
             * If stack is empty it indicates error condition, value of parsableDataTypeName will be null in case there
             * is no name attached to parsable data type.
             */
            String message = ListenerErrorMessageConstruction.constructListenerErrorMessage(errorType, parsableDataType,
                    parsableDataTypeName, errorLocation);
            throw new ParserException(message);
        }
    }

    /**
     * Checks parsed data stack is empty.
     *
     * @param listener Listener's object.
     * @param errorType error type needs to be set in error message.
     * @param parsableDataType type of parsable data in which error occurred.
     * @param parsableDataTypeName name of parsable data type in which error occurred.
     * @param errorLocation location where error occurred.
     */

    public static void checkStackIsEmpty(TreeWalkListener listener, ListenerErrorType errorType,
                                            ParsableDataType parsableDataType, String parsableDataTypeName,
                                            ListenerErrorLocation errorLocation) {

        if (!listener.getParsedDataStack().empty()) {
            /*
             * If stack is empty it indicates error condition, value of parsableDataTypeName will be null in case there
             * is no name attached to parsable data type.
             */
            String message = ListenerErrorMessageConstruction.constructListenerErrorMessage(errorType, parsableDataType,
                    parsableDataTypeName, errorLocation);
            throw new ParserException(message);
        }
    }
}