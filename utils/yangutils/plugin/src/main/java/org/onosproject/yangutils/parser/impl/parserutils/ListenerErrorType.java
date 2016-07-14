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

package org.onosproject.yangutils.parser.impl.parserutils;

/**
 * Represents listener error type.
 */
public enum ListenerErrorType {
    /**
     * Represents the parent holder in parsable stack for given YANG construct
     * is invalid.
     */
    INVALID_HOLDER(),

    /**
     * Represents the parent holder in parsable stack for given YANG construct
     * is missing.
     */
    MISSING_HOLDER(),

    /**
     * Represents the current holder in parsable stack for given YANG construct
     * is missing.
     */
    MISSING_CURRENT_HOLDER(),

    /**
     * Represents that the child in parsable stack for given YANG construct is
     * invalid.
     */
    INVALID_CHILD(),

    /**
     * Represents that the cardinality for given YANG construct is invalid.
     */
    INVALID_CARDINALITY(),

    /**
     * Represents that the entry is duplicate.
     */
    DUPLICATE_ENTRY(),

    /**
     * Represents that the content is invalid.
     */
    INVALID_CONTENT(),

    /**
     * Represents that the identifier collision is detected.
     */
    IDENTIFIER_COLLISION(),

    /**
     * Represents that some of earlier parsed data is not handled correctly.
     */
    UNHANDLED_PARSED_DATA();

    /**
     * Returns the message corresponding to listener error type.
     *
     * @param errorType enum value for type of error
     * @return message corresponding to listener error type
     */
    public static String getErrorType(ListenerErrorType errorType) {

        switch (errorType) {
            case INVALID_HOLDER:
                return "Invalid holder for";
            case MISSING_HOLDER:
                return "Missing holder at";
            case MISSING_CURRENT_HOLDER:
                return "Missing";
            case INVALID_CHILD:
                return "Invalid child in";
            case INVALID_CARDINALITY:
                return "Invalid cardinality in";
            case DUPLICATE_ENTRY:
                return "Duplicate";
            case INVALID_CONTENT:
                return "Invalid content in";
            case IDENTIFIER_COLLISION:
                return "Identifier collision detected for";
            case UNHANDLED_PARSED_DATA:
                return "Unhandled parsed data at";
            default:
                return "Problem in";
        }
    }
}
